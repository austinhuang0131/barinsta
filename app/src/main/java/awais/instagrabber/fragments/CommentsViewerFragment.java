package awais.instagrabber.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.asyncs.CommentsFetcher;
import awais.instagrabber.databinding.FragmentCommentsBinding;
import awais.instagrabber.dialogs.ProfilePicDialogFragment;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.CommentsViewModel;
import awais.instagrabber.webservices.MediaService;
import awais.instagrabber.webservices.ServiceCallback;

import static android.content.Context.INPUT_METHOD_SERVICE;

public final class CommentsViewerFragment extends BottomSheetDialogFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "CommentsViewerFragment";

    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);

    private CommentsAdapter commentsAdapter;
    private FragmentCommentsBinding binding;
    private String shortCode;
    private String userId;
    private Resources resources;
    private InputMethodManager imm;
    private AppCompatActivity fragmentActivity;
    private LinearLayoutCompat root;
    private boolean shouldRefresh = true;
    private MediaService mediaService;
    private String postId;
    private CommentsViewModel commentsViewModel;

    private final CommentsAdapter.CommentCallback commentCallback = new CommentsAdapter.CommentCallback() {
        @Override
        public void onClick(final CommentModel comment) {
            onCommentClick(comment);
        }

        @Override
        public void onHashtagClick(final String hashtag) {
            final NavDirections action = CommentsViewerFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(CommentsViewerFragment.this).navigate(action);
        }

        @Override
        public void onMentionClick(final String mention) {
            openProfile(mention);
        }

        @Override
        public void onURLClick(final String url) {
            Utils.openURL(getContext(), url);
        }

        @Override
        public void onEmailClick(final String emailAddress) {
            Utils.openEmailAddress(getContext(), emailAddress);
        }
    };
    private final View.OnClickListener newCommentListener = v -> {
        final Editable text = binding.commentText.getText();
        final Context context = getContext();
        if (context == null) return;
        if (text == null || TextUtils.isEmpty(text.toString())) {
            Toast.makeText(context, R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
            return;
        }
        final String userId = CookieUtils.getUserIdFromCookie(cookie);
        if (userId == null) return;
        String replyToId = null;
        final CommentModel commentModel = commentsAdapter.getSelected();
        if (commentModel != null) {
            replyToId = commentModel.getId();
        }
        mediaService.comment(postId, text.toString(), userId, replyToId, CookieUtils.getCsrfTokenFromCookie(cookie), new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                commentsAdapter.clearSelection();
                binding.commentText.setText("");
                if (!result) {
                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                onRefresh();
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error during comment", t);
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        mediaService = MediaService.getInstance();
        // setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentCommentsBinding.inflate(getLayoutInflater());
        binding.swipeRefreshLayout.setEnabled(false);
        binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    // @Override
    // public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
    //     inflater.inflate(R.menu.follow, menu);
    //     menu.findItem(R.id.action_compare).setVisible(false);
    //     final MenuItem menuSearch = menu.findItem(R.id.action_search);
    //     final SearchView searchView = (SearchView) menuSearch.getActionView();
    //     searchView.setQueryHint(getResources().getString(R.string.action_search));
    //     searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
    //         @Override
    //         public boolean onQueryTextSubmit(final String query) {
    //             return false;
    //         }
    //
    //         @Override
    //         public boolean onQueryTextChange(final String query) {
    //             // if (commentsAdapter != null) commentsAdapter.getFilter().filter(query);
    //             return true;
    //         }
    //     });
    // }

    @Override
    public void onRefresh() {
        binding.swipeRefreshLayout.setRefreshing(true);
        new CommentsFetcher(shortCode, commentModels -> {
            commentsViewModel.getList().postValue(commentModels);
            binding.swipeRefreshLayout.setRefreshing(false);
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void init() {
        if (getArguments() == null) return;
        final CommentsViewerFragmentArgs fragmentArgs = CommentsViewerFragmentArgs.fromBundle(getArguments());
        shortCode = fragmentArgs.getShortCode();
        postId = fragmentArgs.getPostId();
        userId = fragmentArgs.getPostUserId();
        // setTitle();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        binding.swipeRefreshLayout.setRefreshing(true);
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        commentsAdapter = new CommentsAdapter(commentCallback);
        binding.rvComments.setAdapter(commentsAdapter);
        commentsViewModel.getList().observe(getViewLifecycleOwner(), commentsAdapter::submitList);
        resources = getResources();
        if (!TextUtils.isEmpty(cookie)) {
            binding.commentField.setStartIconVisible(false);
            binding.commentField.setEndIconVisible(false);
            binding.commentField.setVisibility(View.VISIBLE);
            binding.commentText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    binding.commentField.setStartIconVisible(s.length() > 0);
                    binding.commentField.setEndIconVisible(s.length() > 0);
                }

                @Override
                public void afterTextChanged(final Editable s) {}
            });
            binding.commentField.setStartIconOnClickListener(v -> {
                commentsAdapter.clearSelection();
                binding.commentText.setText("");
            });
            binding.commentField.setEndIconOnClickListener(newCommentListener);
        }
        onRefresh();
    }

    // private void setTitle() {
    //     final ActionBar actionBar = fragmentActivity.getSupportActionBar();
    //     if (actionBar == null) return;
    //     actionBar.setTitle(R.string.title_comments);
        // actionBar.setSubtitle(shortCode);
    // }

    private void onCommentClick(final CommentModel commentModel) {
        final String username = commentModel.getProfileModel().getUsername();
        final SpannableString title = new SpannableString(username + ":\n" + commentModel.getText());
        title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        String[] commentDialogList;

        final String userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
        if (!TextUtils.isEmpty(cookie)
                && userIdFromCookie != null
                && (userIdFromCookie.equals(commentModel.getProfileModel().getId()) || userIdFromCookie.equals(userId))) {
            commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_pfp),
//                    resources.getString(R.string.comment_viewer_copy_user),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_reply_comment),
                    commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment)
                                            : resources.getString(R.string.comment_viewer_like_comment),
                    resources.getString(R.string.comment_viewer_delete_comment)
            };
        } else if (!TextUtils.isEmpty(cookie)) {
            commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_pfp),
//                    resources.getString(R.string.comment_viewer_copy_user),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_reply_comment),
                    commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment)
                                            : resources.getString(R.string.comment_viewer_like_comment),
            };
        } else {
            commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_pfp),
//                    resources.getString(R.string.comment_viewer_copy_user),
                    resources.getString(R.string.comment_viewer_copy_comment)
            };
        }
        final Context context = getContext();
        if (context == null) return;
        final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
            final ProfileModel profileModel = commentModel.getProfileModel();
            final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
            switch (which) {
                case 0: // open profile
                    openProfile("@" + profileModel.getUsername());
                    break;
                case 1: // view profile pic
                    final FragmentManager fragmentManager = getParentFragmentManager();
                    final ProfilePicDialogFragment fragment = new ProfilePicDialogFragment(profileModel.getId(),
                                                                                           profileModel.getUsername(),
                                                                                           profileModel.getHdProfilePic());
                    final FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                      .add(fragment, "profilePicDialog")
                      .commit();
                    break;
//              case 2: // copy username
//                  Utils.copyText(context, profileModel.getUsername());
//                  break;
                case 2: // copy comment
                    Utils.copyText(context, "@" + profileModel.getUsername() + ": " + commentModel.getText());
                    break;
                case 3: // reply to comment
                    // final View focus = binding.rvComments.findViewWithTag(commentModel);
                    // focus.setBackgroundColor(0x80888888);
                    commentsAdapter.setSelected(commentModel);
                    String mention = "@" + profileModel.getUsername() + " ";
                    binding.commentText.setText(mention);
                    binding.commentText.requestFocus();
                    binding.commentText.setSelection(mention.length());
                    binding.commentText.postDelayed(() -> {
                        imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                        if (imm == null) return;
                        imm.showSoftInput(binding.commentText, 0);
                    }, 200);
                    break;
                case 4: // like/unlike comment
                    if (csrfToken == null) {
                        return;
                    }
                    if (!commentModel.getLiked()) {
                        mediaService.commentLike(commentModel.getId(), csrfToken, new ServiceCallback<Boolean>() {
                            @Override
                            public void onSuccess(final Boolean result) {
                                if (!result) {
                                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                onRefresh();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error liking comment", t);
                                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    mediaService.commentUnlike(commentModel.getId(), csrfToken, new ServiceCallback<Boolean>() {
                        @Override
                        public void onSuccess(final Boolean result) {
                            if (!result) {
                                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            onRefresh();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error unliking comment", t);
                            Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case 5: // delete comment
                    final String userId = CookieUtils.getUserIdFromCookie(cookie);
                    if (userId == null) return;
                    mediaService.deleteComment(
                            postId, userId, commentModel.getId(), csrfToken,
                            new ServiceCallback<Boolean>() {
                                @Override
                                public void onSuccess(final Boolean result) {
                                    if (!result) {
                                        Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    onRefresh();
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    Log.e(TAG, "Error deleting comment", t);
                                    Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
            }
        };
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(commentDialogList, profileDialogListener)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openProfile(final String username) {
        final NavDirections action = CommentsViewerFragmentDirections.actionGlobalProfileFragment(username);
        NavHostFragment.findNavController(this).navigate(action);
    }
}