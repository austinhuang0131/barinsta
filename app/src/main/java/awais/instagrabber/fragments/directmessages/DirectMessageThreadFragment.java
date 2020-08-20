package awais.instagrabber.fragments.directmessages;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import awais.instagrabber.R;
import awais.instagrabber.activities.PostViewer;
import awais.instagrabber.activities.ProfileViewer;
import awais.instagrabber.activities.StoryViewer;
import awais.instagrabber.adapters.MessageItemsAdapter;
import awais.instagrabber.asyncs.ImageUploader;
import awais.instagrabber.asyncs.direct_messages.DirectMessageInboxThreadFetcher;
import awais.instagrabber.asyncs.direct_messages.DirectThreadBroadcaster;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentDirectMessagesThreadBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ImageUploadOptions;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.UserInboxDirection;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public class DirectMessageThreadFragment extends Fragment {
    private static final String TAG = "DirectMessagesThreadFmt";
    private static final int PICK_IMAGE = 100;

    private FragmentActivity fragmentActivity;
    private String threadId;
    private String cursor;
    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    private final String myId = Utils.getUserIdFromCookie(cookie);
    private FragmentDirectMessagesThreadBinding binding;
    private DirectItemModelListViewModel listViewModel;
    private DirectItemModel directItemModel;
    private RecyclerView messageList;
    private boolean hasSentSomething, hasDeletedSomething;
    private boolean hasOlder = true;

    private final ProfileModel myProfileHolder = ProfileModel.getDefaultProfileModel();
    private final List<ProfileModel> users = new ArrayList<>();
    private final List<ProfileModel> leftUsers = new ArrayList<>();
    private ArrayAdapter<String> dialogAdapter;

    private final View.OnClickListener clickListener = v -> {
        if (v == binding.commentSend) {
            final String text = binding.commentText.getText().toString();
            if (Utils.isEmpty(text)) {
                Toast.makeText(requireContext(), R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
                return;
            }
            sendText(text, null, false);
            return;
        }
        if (v == binding.image) {
            final Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE);
        }
    };

    private final FetchListener<InboxThreadModel> fetchListener = new FetchListener<InboxThreadModel>() {
        @Override
        public void doBefore() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final InboxThreadModel result) {
            if (result == null && ("MINCURSOR".equals(cursor) || "MAXCURSOR".equals(cursor) || Utils.isEmpty(cursor)))
                Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();

            if (result != null) {
                cursor = result.getOldestCursor();
                hasOlder = result.hasOlder();
                if ("MINCURSOR".equals(cursor) || "MAXCURSOR".equals(cursor)) {
                    cursor = null;
                }
                users.clear();
                users.addAll(Arrays.asList(result.getUsers()));
                leftUsers.clear();
                leftUsers.addAll(Arrays.asList(result.getLeftUsers()));

                // thread title is already comma separated username, so no need to set by ourselves
                // String[] users = new String[result.getUsers().length];
                // for (int i = 0; i < users.length; ++i) {
                //     users[i] = result.getUsers()[i].getUsername();
                // }

                List<DirectItemModel> list = listViewModel.getList().getValue();
                final List<DirectItemModel> newList = Arrays.asList(result.getItems());
                list = list != null ? new LinkedList<>(list) : new LinkedList<>();
                if (hasSentSomething || hasDeletedSomething) {
                    list = newList;
                    final Handler handler = new Handler();
                    if (hasSentSomething) handler.postDelayed(() -> {
                        if (messageList != null) {
                            messageList.smoothScrollToPosition(0);
                        }
                    }, 200);
                    hasSentSomething = false;
                    hasDeletedSomething = false;
                } else {
                    list.addAll(newList);
                }
                listViewModel.getList().postValue(list);
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = requireActivity();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = FragmentDirectMessagesThreadBinding.inflate(inflater, container, false);
        final LinearLayout root = binding.getRoot();
        if (getArguments() == null) {
            return root;
        }
        threadId = DirectMessageThreadFragmentArgs.fromBundle(getArguments()).getThreadId();
        binding.swipeRefreshLayout.setEnabled(false);
        messageList = binding.messageList;
        messageList.setHasFixedSize(true);
        binding.commentSend.setOnClickListener(clickListener);
        binding.image.setOnClickListener(clickListener);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setReverseLayout(true);
        // layoutManager.setStackFromEnd(true);
        messageList.setLayoutManager(layoutManager);
        messageList.addOnScrollListener(new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (Utils.isEmpty(cursor) || !hasOlder) {
                return;
            }
            new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, cursor, fetchListener).execute(); // serial because we don't want messages to be randomly ordered
        }));

        final DialogInterface.OnClickListener onDialogListener = (d,w) -> {
            if (w == 0) {
                final DirectItemType itemType = directItemModel.getItemType();
                switch (itemType) {
                    case MEDIA_SHARE:
                        startActivity(new Intent(requireContext(), PostViewer.class)
                                .putExtra(Constants.EXTRAS_POST, new PostModel(directItemModel.getMediaModel().getCode(), false)));
                        break;
                    case LINK:
                        Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                        linkIntent.setData(Uri.parse(directItemModel.getLinkModel().getLinkContext().getLinkUrl()));
                        startActivity(linkIntent);
                        break;
                    case TEXT:
                    case REEL_SHARE:
                        Utils.copyText(requireContext(), directItemModel.getText());
                        Toast.makeText(requireContext(), R.string.clipboard_copied, Toast.LENGTH_SHORT).show();
                        break;
                    case RAVEN_MEDIA:
                    case MEDIA:
                        final ProfileModel user = getUser(directItemModel.getUserId());
                        Utils.dmDownload(requireContext(), user.getUsername(), DownloadMethod.DOWNLOAD_DIRECT, Collections.singletonList(itemType == DirectItemType.MEDIA ? directItemModel.getMediaModel() : directItemModel.getRavenMediaModel().getMedia()));
                        Toast.makeText(requireContext(), R.string.downloader_downloading_media, Toast.LENGTH_SHORT).show();
                        break;
                    case STORY_SHARE:
                        if (directItemModel.getReelShare() != null) {
                            StoryModel sm = new StoryModel(
                                    directItemModel.getReelShare().getReelId(),
                                    directItemModel.getReelShare().getMedia().getVideoUrl(),
                                    directItemModel.getReelShare().getMedia().getMediaType(),
                                    directItemModel.getTimestamp(),
                                    directItemModel.getReelShare().getReelOwnerName(),
                                    String.valueOf(directItemModel.getReelShare().getReelOwnerId()),
                                    false
                            );
                            sm.setVideoUrl(directItemModel.getReelShare().getMedia().getVideoUrl());
                            StoryModel[] sms = {sm};
                            startActivity(new Intent(requireContext(), StoryViewer.class)
                                    .putExtra(Constants.EXTRAS_USERNAME, directItemModel.getReelShare().getReelOwnerName())
                                    .putExtra(Constants.EXTRAS_STORIES, sms)
                            );
                        } else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
                            searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
                        }
                        break;
                    case PLACEHOLDER:
                        if (directItemModel.getText().toString().contains("@"))
                            searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
                        break;
                    default:
                        Log.d("austin_debug", "unsupported type " + itemType);
                }
            }
            else if (w == 1) {
                sendText(null, directItemModel.getItemId(), directItemModel.isLiked());
            }
            else if (w == 2) {
                if (String.valueOf(directItemModel.getUserId()).equals(myId)) new Unsend().execute();
                else searchUsername(getUser(directItemModel.getUserId()).getUsername());
            }
        };
        final View.OnClickListener onClickListener = v -> {
            Object tag = v.getTag();
            if (tag instanceof ProfileModel) {
                searchUsername(((ProfileModel) tag).getUsername());
            }
            else if (tag instanceof DirectItemModel) {
                directItemModel = (DirectItemModel) tag;
                final DirectItemType itemType = directItemModel.getItemType();
                int firstOption = R.string.dms_inbox_raven_message_unknown;
                String[] dialogList;

                switch (itemType) {
                    case MEDIA_SHARE:
                        firstOption = R.string.view_post;
                        break;
                    case LINK:
                        firstOption = R.string.dms_inbox_open_link;
                        break;
                    case TEXT:
                    case REEL_SHARE:
                        firstOption = R.string.dms_inbox_copy_text;
                        break;
                    case RAVEN_MEDIA:
                    case MEDIA:
                        firstOption = R.string.dms_inbox_download;
                        break;
                    case STORY_SHARE:
                        if (directItemModel.getReelShare() != null) {
                            firstOption = R.string.show_stories;
                        } else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
                            firstOption = R.string.open_profile;
                        }
                        break;
                    case PLACEHOLDER:
                        if (directItemModel.getText().toString().contains("@"))
                            firstOption = R.string.open_profile;
                        break;
                }

                dialogList = new String[]{
                        getString(firstOption),
                        getString(directItemModel.isLiked() ? R.string.dms_inbox_unlike : R.string.dms_inbox_like),
                        getString(String.valueOf(directItemModel.getUserId()).equals(myId) ? R.string.dms_inbox_unsend : R.string.dms_inbox_author)
                };

                dialogAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dialogList);

                new AlertDialog.Builder(requireContext())
                        //.setTitle(title)
                        .setAdapter(dialogAdapter, onDialogListener)
                        .setNeutralButton(R.string.cancel, null)
                        .show();
            }
        };
        final MentionClickListener mentionClickListener = (view, text, isHashtag) -> searchUsername(text);
        final MessageItemsAdapter adapter = new MessageItemsAdapter(users, leftUsers, onClickListener, mentionClickListener);
        messageList.setAdapter(adapter);
        listViewModel = new ViewModelProvider(fragmentActivity).get(DirectItemModelListViewModel.class);
        listViewModel.getList().observe(fragmentActivity, adapter::submitList);
        new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Log.w(TAG, "data is null!");
                return;
            }
            final Uri uri = data.getData();
            sendImage(uri);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listViewModel.getList().postValue(Collections.emptyList());
    }

    private void sendText(final String text, final String itemId, final boolean delete) {
        DirectThreadBroadcaster.TextBroadcastOptions textOptions = null;
        DirectThreadBroadcaster.ReactionBroadcastOptions reactionOptions = null;
        if (text != null) {
            try {
                textOptions = new DirectThreadBroadcaster.TextBroadcastOptions(text);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error", e);
                return;
            }
        }
        else {
            reactionOptions = new DirectThreadBroadcaster.ReactionBroadcastOptions(itemId, delete);
        }
        broadcast(text != null ? textOptions : reactionOptions, result -> {
            if (result == null || result.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (text != null) {
                binding.commentText.setText("");
            }
            else {
                LinearLayout dim = (LinearLayout) binding.messageList.findViewWithTag(directItemModel);
                if (dim.findViewById(R.id.liked) != null) dim.findViewById(R.id.liked).setVisibility(delete ? View.GONE : View.VISIBLE);
                directItemModel.setLiked();
            }
            hasSentSomething = true;
            new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
    }

    private void sendImage(final Uri imageUri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Toast.makeText(requireContext(), R.string.uploading, Toast.LENGTH_SHORT).show();
            // Upload Image
            final ImageUploader imageUploader = new ImageUploader();
            imageUploader.setOnTaskCompleteListener(response -> {
                if (response == null || response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    if (response != null && response.getResponse() != null) {
                        Log.e(TAG, response.getResponse().toString());
                    }
                    return;
                }
                final JSONObject responseJson = response.getResponse();
                try {
                    final String uploadId = responseJson.getString("upload_id");
                    // Broadcast
                    final DirectThreadBroadcaster.ImageBroadcastOptions options = new DirectThreadBroadcaster.ImageBroadcastOptions(true, uploadId);
                    hasSentSomething = true;
                    broadcast(options, onBroadcastCompleteListener -> new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing json response", e);
                }
            });
            final ImageUploadOptions options = ImageUploadOptions.builder(bitmap).build();
            imageUploader.execute(options);
        }
        catch (IOException e) {
            Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening file", e);
        }
    }

    private void broadcast(final DirectThreadBroadcaster.BroadcastOptions broadcastOptions, final DirectThreadBroadcaster.OnBroadcastCompleteListener listener) {
        final DirectThreadBroadcaster broadcaster = new DirectThreadBroadcaster(threadId);
        broadcaster.setOnTaskCompleteListener(listener);
        broadcaster.execute(broadcastOptions);
    }

    @NonNull
    private ProfileModel getUser(final long userId) {
        ProfileModel result = myProfileHolder;
        for (final ProfileModel user : users) {
            if (Long.toString(userId).equals(user.getId())) result = user;
        }
        for (final ProfileModel leftUser : leftUsers) {
            if (Long.toString(userId).equals(leftUser.getId())) result = leftUser;
        }
        return result;
    }

    private void searchUsername(final String text) {
        startActivity(new Intent(requireContext(), ProfileViewer.class).putExtra(Constants.EXTRAS_USERNAME, text));
    }

    public static class DirectItemModelListViewModel extends ViewModel {
        private MutableLiveData<List<DirectItemModel>> list;

        public MutableLiveData<List<DirectItemModel>> getList() {
            if (list == null) {
                list = new MutableLiveData<>();
            }
            return list;
        }
    }

    class Unsend extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... lmao) {
            final String url = "https://i.instagram.com/api/v1/direct_v2/threads/"+threadId+"/items/"+directItemModel.getItemId()+"/delete/";
            try {
                String urlParameters = "_csrftoken=" + cookie.split("csrftoken=")[1].split(";")[0]
                        +"&_uuid=" + Utils.settingsHelper.getString(Constants.DEVICE_UUID);
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    hasDeletedSomething = true;
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "unsend: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (hasDeletedSomething) {
                directItemModel = null;
                new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
}
