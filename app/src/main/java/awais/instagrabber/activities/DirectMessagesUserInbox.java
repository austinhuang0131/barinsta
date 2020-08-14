package awais.instagrabber.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import awais.instagrabber.R;
import awais.instagrabber.adapters.MessageItemsAdapter;
import awais.instagrabber.asyncs.direct_messages.UserInboxFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.ActivityDmsBinding;
import awais.instagrabber.interfaces.FetchListener;
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

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class DirectMessagesUserInbox extends BaseLanguageActivity {
    private DirectItemModel directItemModel;
    private final ProfileModel myProfileHolder =
            new ProfileModel(false, false, false, null, null, null, null, null, null, null, 0, 0, 0, false, false, false, false);
    private final ArrayList<ProfileModel> users = new ArrayList<>(), leftusers = new ArrayList<>();
    private final ArrayList<DirectItemModel> directItemModels = new ArrayList<>();
    private String threadid;
    private final FetchListener<InboxThreadModel> fetchListener = new FetchListener<InboxThreadModel>() {
        @Override
        public void doBefore() {
            dmsBinding.swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final InboxThreadModel result) {
            if (result == null && ("MINCURSOR".equals(endCursor) || "MAXCURSOR".equals(endCursor) || Utils.isEmpty(endCursor)))
                Toast.makeText(DirectMessagesUserInbox.this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();

            if (result != null) {
                endCursor = result.getPrevCursor();
                if ("MINCURSOR".equals(endCursor) || "MAXCURSOR".equals(endCursor)) endCursor = null;

                users.clear();
                users.addAll(Arrays.asList(result.getUsers()));

                leftusers.clear();
                leftusers.addAll(Arrays.asList(result.getLeftUsers()));

                threadid = result.getThreadId();
                dmsBinding.toolbar.toolbar.setTitle(result.getThreadTitle());
                String[] users = new String[result.getUsers().length];
                for (int i = 0; i < users.length; ++i) {
                    users[i] = result.getUsers()[i].getUsername();
                }
                dmsBinding.toolbar.toolbar.setSubtitle(String.join(", ", users));

                final int oldSize = directItemModels.size();
                final List<DirectItemModel> itemModels = Arrays.asList(result.getItems());
                directItemModels.addAll(itemModels);
                messageItemsAdapter.notifyItemRangeInserted(oldSize, itemModels.size());
            }

            dmsBinding.swipeRefreshLayout.setRefreshing(false);
        }
    };
    private String endCursor;
    private ActivityDmsBinding dmsBinding;
    private MessageItemsAdapter messageItemsAdapter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dmsBinding = ActivityDmsBinding.inflate(getLayoutInflater());
        setContentView(dmsBinding.getRoot());

        final InboxThreadModel threadModel;
        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_THREAD_MODEL) ||
                (threadModel = (InboxThreadModel) intent.getSerializableExtra(Constants.EXTRAS_THREAD_MODEL)) == null) {
            Utils.errorFinish(this);
            return;
        }

        dmsBinding.swipeRefreshLayout.setEnabled(false);
        dmsBinding.commentText.setVisibility(View.VISIBLE);
        dmsBinding.commentSend.setVisibility(View.VISIBLE);
        dmsBinding.commentSend.setOnClickListener(newCommentListener);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, true);
        dmsBinding.rvDirectMessages.setLayoutManager(layoutManager);

        dmsBinding.rvDirectMessages.addOnScrollListener(new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!Utils.isEmpty(endCursor)) {
                new UserInboxFetcher(threadModel.getThreadId(), UserInboxDirection.OLDER,
                        endCursor, fetchListener).execute(); // serial because we don't want messages to be randomly ordered
            }
        }));

        messageItemsAdapter = new MessageItemsAdapter(directItemModels, users, leftusers, v -> {
            Object tag = v.getTag();
            if (tag instanceof DirectItemModel) {
                directItemModel = (DirectItemModel) tag;
                final DirectItemType itemType = directItemModel.getItemType();
                switch (itemType) {
                    case MEDIA_SHARE:
                        startActivity(new Intent(this, PostViewer.class)
                                .putExtra(Constants.EXTRAS_POST, new PostModel(directItemModel.getMediaModel().getCode(), false)));
                        break;
                    case LINK:
                        Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                        linkIntent.setData(Uri.parse(directItemModel.getLinkModel().getLinkContext().getLinkUrl()));
                        startActivity(linkIntent);
                        break;
                    case TEXT:
                    case REEL_SHARE:
                        Utils.copyText(v.getContext(), directItemModel.getText());
                        Toast.makeText(v.getContext(), R.string.clipboard_copied, Toast.LENGTH_SHORT).show();
                        break;
                    case RAVEN_MEDIA:
                    case MEDIA:
                        Utils.dmDownload(this, getUser(directItemModel.getUserId()).getUsername(), DownloadMethod.DOWNLOAD_DIRECT,
                                Collections.singletonList(itemType == DirectItemType.MEDIA ? directItemModel.getMediaModel() : directItemModel.getRavenMediaModel().getMedia()));
                        Toast.makeText(v.getContext(), R.string.downloader_downloading_media, Toast.LENGTH_SHORT).show();
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
                            startActivity(new Intent(this, StoryViewer.class)
                                    .putExtra(Constants.EXTRAS_USERNAME, directItemModel.getReelShare().getReelOwnerName())
                                    .putExtra(Constants.EXTRAS_STORIES, sms)
                            );
                        }
                        else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
                            searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
                        }
                        break;
                    case PLACEHOLDER:
                        if (directItemModel.getText().toString().contains("@"))
                            searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
                        break;
                    default:
                        Log.d("austin_debug", "unsupported type "+itemType);
                }
            }
        },
        (view, text, isHashtag) -> {
            searchUsername(text);
        });

        dmsBinding.rvDirectMessages.setAdapter(messageItemsAdapter);

        new UserInboxFetcher(threadModel.getThreadId(), UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Nullable
    private ProfileModel getUser(final long userId) {
        if (users != null) {
            ProfileModel result = myProfileHolder;
            for (final ProfileModel user : users) {
                if (Long.toString(userId).equals(user.getId())) result = user;
            }
            if (leftusers != null)
                for (final ProfileModel leftuser : leftusers) {
                    if (Long.toString(userId).equals(leftuser.getId())) result = leftuser;
                }
            return result;
        }
        return null;
    }

    private void searchUsername(final String text) {
        startActivity(new Intent(getApplicationContext(), ProfileViewer.class).putExtra(Constants.EXTRAS_USERNAME, text));
    }

    private final View.OnClickListener newCommentListener = v -> {
        if (Utils.isEmpty(dmsBinding.commentText.getText().toString()) && v == dmsBinding.commentSend)
            Toast.makeText(getApplicationContext(), R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
        else if (v == dmsBinding.commentSend) new CommentAction().execute();
    };

    class CommentAction extends AsyncTask<Void, Void, Void> {
        boolean ok = false;

        protected Void doInBackground(Void... lmao) {
            final String url2 = "https://i.instagram.com/api/v1/direct_v2/threads/broadcast/text/";
            final String cookie = settingsHelper.getString(Constants.COOKIE);
            try {
                final HttpURLConnection urlConnection2 = (HttpURLConnection) new URL(url2).openConnection();
                urlConnection2.setRequestMethod("POST");
                urlConnection2.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                urlConnection2.setUseCaches(false);
                final String commentText = URLEncoder.encode(dmsBinding.commentText.getText().toString(), "UTF-8")
                        .replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'")
                        .replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
                final String cc = UUID.randomUUID().toString();
                final String urlParameters2 = Utils.sign("{\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                        +"\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                        +"\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                        +"\",\"client_context\":\"" + cc
                        +"\",\"mutation_token\":\"" + cc
                        +"\",\"text\":\"" + commentText
                        +"\",\"thread_ids\":\"["+threadid
                        +"]\",\"action\":\"send_item\"}");
                urlConnection2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection2.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters2.getBytes().length));
                urlConnection2.setDoOutput(true);
                DataOutputStream wr2 = new DataOutputStream(urlConnection2.getOutputStream());
                wr2.writeBytes(urlParameters2);
                wr2.flush();
                wr2.close();
                urlConnection2.connect();
                Log.d("austin_debug", urlConnection2.getResponseCode() + " " + urlParameters2 + " " + cookie);
                if (urlConnection2.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = true;
                }
                urlConnection2.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "dm send: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!ok) Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            else {
                dmsBinding.commentText.setText("");
                dmsBinding.commentText.clearFocus();
                directItemModels.clear();
                messageItemsAdapter.notifyDataSetChanged();
                new UserInboxFetcher(threadid, UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
}