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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import awais.instagrabber.R;
import awais.instagrabber.adapters.NotificationsAdapter;
import awais.instagrabber.asyncs.NotificationsFetcher;
import awais.instagrabber.databinding.ActivityNotificationBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class NotificationsViewer extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private NotificationsAdapter notificationsAdapter;
    private NotificationModel notificationModel;
    private ActivityNotificationBinding notificationsBinding;
    private ArrayAdapter<String> commmentDialogAdapter;
    private String shortCode, postId, userId;
    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    private Resources resources;
    private InputMethodManager imm;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationsBinding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(notificationsBinding.getRoot());
        notificationsBinding.swipeRefreshLayout.setOnRefreshListener(this);

        notificationsBinding.swipeRefreshLayout.setRefreshing(true);
        setSupportActionBar(notificationsBinding.toolbar.toolbar);
        notificationsBinding.toolbar.toolbar.setTitle(R.string.title_notifications);

        resources = getResources();

        new NotificationsFetcher(new FetchListener<NotificationModel[]>() {
            @Override
            public void onResult(final NotificationModel[] notificationModels) {
                notificationsAdapter = new NotificationsAdapter(notificationModels, clickListener, mentionClickListener);

                notificationsBinding.rvComments.setAdapter(notificationsAdapter);
                notificationsBinding.swipeRefreshLayout.setRefreshing(false);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRefresh() {
        notificationsBinding.swipeRefreshLayout.setRefreshing(true);
        new NotificationsFetcher(new FetchListener<NotificationModel[]>() {
            @Override
            public void onResult(final NotificationModel[] notificationModels) {
                notificationsBinding.swipeRefreshLayout.setRefreshing(false);

                notificationsAdapter = new NotificationsAdapter(notificationModels, clickListener, mentionClickListener);

                notificationsBinding.rvComments.setAdapter(notificationsAdapter);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
        if (which == 0)
            searchUsername(notificationModel.getUsername());
        else if (which == 1)
            startActivity(new Intent(getApplicationContext(), PostViewer.class)
                    .putExtra(Constants.EXTRAS_POST, new PostModel(notificationModel.getShortcode(), false)));
    };

    private final View.OnClickListener clickListener = v -> {
        final Object tag = v.getTag();
        if (tag instanceof NotificationModel) {
            notificationModel = (NotificationModel) tag;

            final String username = notificationModel.getUsername();
            final SpannableString title = new SpannableString(username + ":\n" + notificationModel.getText());
            title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            String[] commentDialogList;

            if (notificationModel.getShortcode() != null) commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.view_post)
            };
            else commentDialogList = new String[]{
                    resources.getString(R.string.open_profile)
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


    private void searchUsername(final String text) {
        if (Main.scanHack != null) {
            Main.scanHack.onResult(text);
            setResult(6969);
            finish();
        }
    }
}