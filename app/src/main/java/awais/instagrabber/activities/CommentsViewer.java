package awais.instagrabber.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.asyncs.CommentsFetcher;
import awais.instagrabber.databinding.ActivityCommentsBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class CommentsViewer extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private CommentsAdapter commentsAdapter;
    private CommentModel commentModel;
    private ActivityCommentsBinding commentsBinding;
    private ArrayAdapter<String> commmentDialogAdapter;
    private String shortCode, postId, userId;
    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    private Resources resources;
    private InputMethodManager imm;
    private View focus;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commentsBinding = ActivityCommentsBinding.inflate(getLayoutInflater());
        setContentView(commentsBinding.getRoot());
        commentsBinding.swipeRefreshLayout.setOnRefreshListener(this);

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_SHORTCODE)
                || Utils.isEmpty((shortCode = intent.getStringExtra(Constants.EXTRAS_SHORTCODE)))
                || !intent.hasExtra(Constants.EXTRAS_POST)
                || Utils.isEmpty((postId = intent.getStringExtra(Constants.EXTRAS_POST)))
                || !intent.hasExtra(Constants.EXTRAS_USER)
                || Utils.isEmpty((userId = intent.getStringExtra(Constants.EXTRAS_USER)))) {
            Utils.errorFinish(this);
            return;
        }

        commentsBinding.swipeRefreshLayout.setRefreshing(true);
        setSupportActionBar(commentsBinding.toolbar.toolbar);
        commentsBinding.toolbar.toolbar.setTitle(R.string.title_comments);
        commentsBinding.toolbar.toolbar.setSubtitle(shortCode);

        resources = getResources();

        if (!Utils.isEmpty(cookie)) {
            commentsBinding.commentText.setVisibility(View.VISIBLE);
            commentsBinding.commentSend.setVisibility(View.VISIBLE);

            commentsBinding.commentSend.setOnClickListener(newCommentListener);
            commentsBinding.commentCancelParent.setOnClickListener(newCommentListener);
        }

        new CommentsFetcher(shortCode, new FetchListener<CommentModel[]>() {
            @Override
            public void onResult(final CommentModel[] commentModels) {
                commentsAdapter = new CommentsAdapter(commentModels, true, clickListener, mentionClickListener);

                commentsBinding.rvComments.setAdapter(commentsAdapter);
                commentsBinding.swipeRefreshLayout.setRefreshing(false);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRefresh() {
        commentsBinding.swipeRefreshLayout.setRefreshing(true);
        new CommentsFetcher(shortCode, new FetchListener<CommentModel[]>() {
            @Override
            public void onResult(final CommentModel[] commentModels) {
                commentsBinding.swipeRefreshLayout.setRefreshing(false);

                commentsAdapter = new CommentsAdapter(commentModels, true, clickListener, mentionClickListener);

                commentsBinding.rvComments.setAdapter(commentsAdapter);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
        final ProfileModel profileModel = commentModel.getProfileModel();

        if (which == 0) {
            searchUsername(profileModel.getUsername());
        } else if (which == 1) {
            startActivity(new Intent(this, ProfileViewer.class).putExtra(Constants.EXTRAS_PROFILE, profileModel));
        } else if (which == 2) {
            Utils.copyText(this, profileModel.getUsername());
        } else if (which == 3) {
            Utils.copyText(this, commentModel.getText().toString());
        } else if (which == 4) {
            focus = commentsBinding.rvComments.findViewWithTag(commentModel);
            focus.setBackgroundColor(0x80888888);
            commentsBinding.commentCancelParent.setVisibility(View.VISIBLE);
            String mention = "@"+profileModel.getUsername()+" ";
            commentsBinding.commentText.setText(mention);
            commentsBinding.commentText.requestFocus();
            commentsBinding.commentText.setSelection(mention.length());
            commentsBinding.commentText.postDelayed(new Runnable(){
               @Override
               public void run(){
                   imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                   imm.showSoftInput(commentsBinding.commentText, 0);
               }
            }
            ,200);
        } else if (which == 5) {
            new CommentAction().execute((commentModel.getLiked() ? "unlike/" : "like/")+commentModel.getId());
        } else if (which == 6) {
            new CommentAction().execute("delete/"+commentModel.getId());
        }
    };

    private final View.OnClickListener clickListener = v -> {
        final Object tag = v.getTag();
        if (tag instanceof CommentModel) {
            commentModel = (CommentModel) tag;

            final String username = commentModel.getProfileModel().getUsername();
            final SpannableString title = new SpannableString(username + ":\n" + commentModel.getText());
            title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            String[] commentDialogList;

            if (!Utils.isEmpty(cookie) &&
                    (Utils.getUserIdFromCookie(cookie).equals(commentModel.getProfileModel().getId()) ||
                            Utils.getUserIdFromCookie(cookie).equals(userId))) commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_pfp),
                    resources.getString(R.string.comment_viewer_copy_user),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_reply_comment),
                    commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment) : resources.getString(R.string.comment_viewer_like_comment),
                    resources.getString(R.string.comment_viewer_delete_comment)
            };
            else if (!Utils.isEmpty(cookie)) commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_pfp),
                    resources.getString(R.string.comment_viewer_copy_user),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_reply_comment),
                    commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment) : resources.getString(R.string.comment_viewer_like_comment),
            };
            else commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_pfp),
                    resources.getString(R.string.comment_viewer_copy_user),
                    resources.getString(R.string.comment_viewer_copy_comment)
            };

            commmentDialogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, commentDialogList);

            new AlertDialog.Builder(this).setTitle(title)
                    .setAdapter(commmentDialogAdapter, profileDialogListener)
                    .setNeutralButton(R.string.cancel, null)
                    .show();
        }
    };

    private final MentionClickListener mentionClickListener = (view, text, isHashtag) ->
            new AlertDialog.Builder(this).setTitle(text)
                    .setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
                    .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok,
                    (dialog, which) -> searchUsername(text)).show();

    private final View.OnClickListener newCommentListener = v -> {
        if (Utils.isEmpty(commentsBinding.commentText.getText().toString()) && v == commentsBinding.commentSend)
            Toast.makeText(getApplicationContext(), R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
        else if (v == commentsBinding.commentSend) new CommentAction().execute("add");
        else if (v == commentsBinding.commentCancelParent) {
            focus.setBackgroundColor(commentModel.getLiked() ? 0x40FF69B4 : 0x00000000);
            commentsBinding.commentCancelParent.setVisibility(View.GONE);
            commentsBinding.commentText.setText("");
            commentModel = null;
            focus = null;
        }
    };

    private void searchUsername(final String text) {
        if (Main.scanHack != null) {
            Main.scanHack.onResult(text);
            setResult(6969);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.follow, menu);

        final MenuItem menuSearch = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                if (commentsAdapter != null) commentsAdapter.getFilter().filter(query);
                return true;
            }
        });

        menu.findItem(R.id.action_compare).setVisible(false);

        return true;
    }

    class CommentAction extends AsyncTask<String, Void, Void> {
        boolean ok = false;

        protected Void doInBackground(String... rawAction) {
            final String action = rawAction[0];
            final String url = "https://www.instagram.com/web/comments/"+postId+"/"+action+"/";
            try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
                if (action == "add") {
                    // https://stackoverflow.com/questions/14321873/java-url-encoding-urlencoder-vs-uri
                    final String commentText = URLEncoder.encode(commentsBinding.commentText.getText().toString(), "UTF-8")
                            .replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'")
                            .replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
                    final String urlParameters = "comment_text="+commentText+"&replied_to_comment_id="+
                            (commentModel == null ? "" : commentModel.getId());
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Content-Length", "" +
                            Integer.toString(urlParameters.getBytes().length));
                    urlConnection.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();
                }
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = true;
                    if (action == "add") {
                        commentsBinding.commentText.setText("");
                        commentsBinding.commentText.clearFocus();
                    }
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", action+": " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok == true) {
                if (focus != null) {
                    focus.setBackgroundColor(commentModel.getLiked() ? 0x40FF69B4 : 0x00000000);
                    commentsBinding.commentCancelParent.setVisibility(View.GONE);
                    commentModel = null;
                    focus = null;
                }
                onRefresh();
            }
            else Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        }
    }
}