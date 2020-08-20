package awais.instagrabber.activities;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Stack;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.MainHelper;
import awais.instagrabber.R;
import awais.instagrabber.adapters.HighlightsAdapter;
import awais.instagrabber.adapters.SuggestionsAdapter;
import awais.instagrabber.asyncs.SuggestionsFetcher;
import awais.instagrabber.asyncs.UsernameFetcher;
import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
import awais.instagrabber.customviews.MouseDrawer;
import awais.instagrabber.databinding.ActivityMainBinding;
import awais.instagrabber.dialogs.AboutDialog;
import awais.instagrabber.dialogs.QuickAccessDialog;
import awais.instagrabber.dialogs.SettingsDialog;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.ItemGetter;
import awais.instagrabber.models.DiscoverItemModel;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.HashtagModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.SuggestionModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.models.enums.SuggestionType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.FlavorTown;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.CHANNEL_ID;
import static awais.instagrabber.utils.Utils.notificationManager;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class Main extends BaseLanguageActivity {
    public static FetchListener<String> scanHack;
    public static ItemGetter itemGetter;
    // -------- items --------
    public final ArrayList<PostModel> allItems = new ArrayList<>();
    public final ArrayList<FeedModel> feedItems = new ArrayList<>();
    public final ArrayList<DiscoverItemModel> discoverItems = new ArrayList<>();
    // -------- items --------
    public final ArrayList<PostModel> selectedItems = new ArrayList<>();
    public final ArrayList<DiscoverItemModel> selectedDiscoverItems = new ArrayList<>();
    // -------- items --------
    public final HighlightsAdapter highlightsAdapter = new HighlightsAdapter(null, new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Object tag = v.getTag();
            if (tag instanceof HighlightModel) {
                final HighlightModel highlightModel = (HighlightModel) tag;
                new iStoryStatusFetcher(highlightModel.getId(), null, false, false,
                        (!mainHelper.isLoggedIn && Utils.settingsHelper.getBoolean(Constants.STORIESIG)), true, result -> {
                    if (result != null && result.length > 0)
                        startActivity(new Intent(Main.this, StoryViewer.class)
                                .putExtra(Constants.EXTRAS_USERNAME, userQuery.replace("@", ""))
                                .putExtra(Constants.EXTRAS_HIGHLIGHT, highlightModel.getTitle())
                                .putExtra(Constants.EXTRAS_STORIES, result)
                        );
                    else
                        Toast.makeText(Main.this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    });
    private SuggestionsAdapter suggestionAdapter;
    private MenuItem searchAction;
    public ActivityMainBinding mainBinding;
    public SearchView searchView;
    public MenuItem downloadAction, settingsAction, dmsAction, notifAction;
    public StoryModel[] storyModels;
    public String userQuery = null, cookie, uid = null;
    public MainHelper mainHelper;
    public ProfileModel profileModel;
    public HashtagModel hashtagModel;
    public LocationModel locationModel;
    private AutoCompleteTextView searchAutoComplete;
    private ArrayAdapter<String> profileDialogAdapter;
    private DialogInterface.OnClickListener profileDialogListener;
    private Stack<String> queriesStack;
    private DataBox.CookieModel cookieModel;

    @Override
    protected void onCreate(@Nullable final Bundle bundle) {
        super.onCreate(bundle);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        FlavorTown.updateCheck(this);
        FlavorTown.changelogCheck(this);

        cookie = settingsHelper.getString(Constants.COOKIE);
        uid = Utils.getUserIdFromCookie(cookie);
        Utils.setupCookies(cookie);

        MainHelper.stopCurrentExecutor();
        mainHelper = new MainHelper(this);
        if (bundle == null) {
            queriesStack = new Stack<>();
            userQuery = null;
        } else {
            setStack(bundle);
            userQuery = bundle.getString("query");
        }
        mainHelper.isLoggedIn = !Utils.isEmpty(Utils.settingsHelper.getString(Constants.COOKIE));

        itemGetter = itemGetType -> {
            if (itemGetType == ItemGetType.MAIN_ITEMS) return allItems;
            if (itemGetType == ItemGetType.DISCOVER_ITEMS) return discoverItems;
            if (itemGetType == ItemGetType.FEED_ITEMS) return feedItems;
            return null;
        };

        scanHack = result -> {
            if (mainHelper != null && !Utils.isEmpty(result)) {
                closeAnyOpenDrawer();
                addToStack();
                userQuery = (result.contains("/") || result.startsWith("#") || result.startsWith("@")) ? result : ("@" + result);
                mainHelper.onRefresh();
            }
        };

        // searches for your userid and returns username
        if (uid != null) {
            final FetchListener<String> fetchListener = username -> {
                if (!Utils.isEmpty(username)) {
                    if (!BuildConfig.DEBUG) {
                        userQuery = username;
                        if (mainHelper != null && !mainBinding.profileView.swipeRefreshLayout.isRefreshing())
                            mainHelper.onRefresh();
                    }
                    // adds cookies to database for quick access
                    cookieModel = Utils.dataBox.getCookie(uid);
                    if (Utils.dataBox.getCookieCount() == 0 || cookieModel == null || Utils.isEmpty(cookieModel.getUsername()))
                        Utils.dataBox.addUserCookie(new DataBox.CookieModel(uid, username, cookie));
                }
            };
            boolean found = false;
            cookieModel = Utils.dataBox.getCookie(uid);
            if (cookieModel != null) {
                final String username = cookieModel.getUsername();
                if (username != null) {
                    found = true;
                    fetchListener.onResult("@" + username);
                }
            }

            if (!found) // if not in database, fetch info from instagram
                new UsernameFetcher(uid, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        suggestionAdapter = new SuggestionsAdapter(this, v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) {
                addToStack();
                userQuery = tag.toString();
                mainHelper.onRefresh();
            }
            if (searchView != null && !searchView.isIconified()) {
                if (searchAction != null) searchAction.collapseActionView();
                searchView.setIconified(true);
                searchView.setIconified(true);
            }
        });

        final Resources resources = getResources();
        profileDialogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new String[]{resources.getString(R.string.view_pfp), resources.getString(R.string.show_stories)});
        profileDialogListener = (dialog, which) -> {
            final Intent intent;
            if (which == 0 || storyModels == null || storyModels.length < 1) {
                intent = new Intent(this, ProfilePicViewer.class).putExtra(
                        ((hashtagModel != null) ? Constants.EXTRAS_HASHTAG : (locationModel != null ? Constants.EXTRAS_LOCATION : Constants.EXTRAS_PROFILE)),
                        ((hashtagModel != null) ? hashtagModel : (locationModel != null ? locationModel : profileModel)));
            } else
                intent = new Intent(this, StoryViewer.class).putExtra(Constants.EXTRAS_USERNAME, userQuery.replace("@", ""))
                        .putExtra(Constants.EXTRAS_STORIES, storyModels)
                        .putExtra(Constants.EXTRAS_HASHTAG, (hashtagModel != null));
            startActivity(intent);
        };

        final View.OnClickListener onClickListener = v -> {
            if (v == mainBinding.profileView.mainBiography) {
                Utils.copyText(this, mainBinding.profileView.mainBiography.getText().toString());
            } else if (v == mainBinding.profileView.locationBiography) {
                Utils.copyText(this, mainBinding.profileView.locationBiography.getText().toString());
            } else if (v == mainBinding.profileView.mainProfileImage || v == mainBinding.profileView.mainHashtagImage || v == mainBinding.profileView.mainLocationImage) {
                if (storyModels == null || storyModels.length <= 0) {
                    profileDialogListener.onClick(null, 0);
                } else {
                    // because sometimes configuration changes made this crash on some phones
                    new AlertDialog.Builder(this).setAdapter(profileDialogAdapter, profileDialogListener)
                            .setNeutralButton(R.string.cancel, null).show();
                }
            }
        };

        mainBinding.profileView.mainBiography.setOnClickListener(onClickListener);
        mainBinding.profileView.locationBiography.setOnClickListener(onClickListener);
        mainBinding.profileView.mainProfileImage.setOnClickListener(onClickListener);
        mainBinding.profileView.mainHashtagImage.setOnClickListener(onClickListener);
        mainBinding.profileView.mainLocationImage.setOnClickListener(onClickListener);

        mainBinding.profileView.mainBiography.setEnabled(false);
        mainBinding.profileView.mainProfileImage.setEnabled(false);
        mainBinding.profileView.mainHashtagImage.setEnabled(false);
        mainBinding.profileView.mainLocationImage.setEnabled(false);

        final boolean isQueryNull = userQuery == null;
        if (isQueryNull) {
            allItems.clear();
            mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_info);
            mainBinding.profileView.privatePage2.setTextSize(20);
            mainBinding.profileView.privatePage2.setText(mainHelper.isLoggedIn ? R.string.no_acc_logged_in : R.string.no_acc);
            mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
        }
        if (!mainBinding.profileView.swipeRefreshLayout.isRefreshing() && userQuery != null)
            mainHelper.onRefresh();

        mainHelper.onIntent(getIntent());

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                new GetActivity().execute();
                handler.postDelayed(this, 60000);
            }
        }, 200);
    }

    private void downloadSelectedItems() {
        if (selectedItems.size() > 0) {
            Utils.batchDownload(this, userQuery, DownloadMethod.DOWNLOAD_MAIN, selectedItems);
        } else if (selectedDiscoverItems.size() > 0) {
            Utils.batchDownload(this, null, DownloadMethod.DOWNLOAD_DISCOVER, selectedDiscoverItems);
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        mainHelper.onIntent(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState, @NonNull final PersistableBundle outPersistentState) {
        outState.putString("query", userQuery);
        outState.putSerializable("stack", queriesStack);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onRestoreInstanceState(@Nullable final Bundle savedInstanceState, @Nullable final PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        if (savedInstanceState != null) {
            userQuery = savedInstanceState.getString("query");
            setStack(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString("query", userQuery);
        outState.putSerializable("stack", queriesStack);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userQuery = savedInstanceState.getString("query");
        setStack(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final MenuItem quickAccessAction = menu.findItem(R.id.action_quickaccess).setVisible(true);

        final MenuItem.OnMenuItemClickListener clickListener = item -> {
            if (item == downloadAction)
                downloadSelectedItems();
            else if (item == dmsAction)
                startActivity(new Intent(this, DirectMessagesActivity.class));
            else if (item == notifAction)
                startActivity(new Intent(this, NotificationsViewer.class));
            else if (item == settingsAction)
                new SettingsDialog().show(fragmentManager, "settings");
            else if (item == quickAccessAction)
                new QuickAccessDialog()
                        .setQuery(userQuery, locationModel != null ? locationModel.getName() : userQuery)
                        .show(fragmentManager, "quickAccess");
            else
                new AboutDialog().show(fragmentManager, "about");
            return true;
        };

        quickAccessAction.setOnMenuItemClickListener(clickListener);
        menu.findItem(R.id.action_about).setVisible(true).setOnMenuItemClickListener(clickListener);
        dmsAction = menu.findItem(R.id.action_dms).setOnMenuItemClickListener(clickListener);
        notifAction = menu.findItem(R.id.action_notif).setOnMenuItemClickListener(clickListener);
        settingsAction = menu.findItem(R.id.action_settings).setVisible(true).setOnMenuItemClickListener(clickListener);
        downloadAction = menu.findItem(R.id.action_download).setOnMenuItemClickListener(clickListener);

        if (!Utils.isEmpty(Utils.settingsHelper.getString(Constants.COOKIE))) {
            notifAction.setVisible(true);
            dmsAction.setVisible(true).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        searchAction = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchAction.getActionView();
        final View searchText = searchView.findViewById(R.id.search_src_text);
        if (searchText instanceof AutoCompleteTextView)
            searchAutoComplete = (AutoCompleteTextView) searchText;

        searchView.setQueryHint(getResources().getString(R.string.action_search));
        searchView.setSuggestionsAdapter(suggestionAdapter);
        searchView.setOnSearchClickListener(v -> {
            searchView.setQuery((cookieModel != null && userQuery != null && userQuery.equals("@" + cookieModel.getUsername())) ? "" : userQuery, false);
            menu.findItem(R.id.action_about).setVisible(false);
            menu.findItem(R.id.action_settings).setVisible(false);
            menu.findItem(R.id.action_dms).setVisible(false);
            menu.findItem(R.id.action_quickaccess).setVisible(false);
            menu.findItem(R.id.action_notif).setVisible(false);
        });
        searchAction.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                menu.findItem(R.id.action_about).setVisible(true);
                menu.findItem(R.id.action_settings).setVisible(true);
                menu.findItem(R.id.action_dms).setVisible(!Utils.isEmpty(Utils.settingsHelper.getString(Constants.COOKIE)));
                menu.findItem(R.id.action_quickaccess).setVisible(true);
                menu.findItem(R.id.action_notif).setVisible(true);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            private boolean searchUser, searchHash;
            private AsyncTask<?, ?, ?> prevSuggestionAsync;
            private final String[] COLUMNS = {BaseColumns._ID, Constants.EXTRAS_USERNAME, Constants.EXTRAS_NAME,
                    Constants.EXTRAS_TYPE, "pfp", "verified"};
            private final FetchListener<SuggestionModel[]> fetchListener = new FetchListener<SuggestionModel[]>() {
                @Override
                public void doBefore() {
                    suggestionAdapter.changeCursor(null);
                }

                @Override
                public void onResult(final SuggestionModel[] result) {
                    final MatrixCursor cursor;
                    if (result == null) cursor = null;
                    else {
                        cursor = new MatrixCursor(COLUMNS, 0);
                        for (int i = 0; i < result.length; i++) {
                            final SuggestionModel suggestionModel = result[i];
                            if (suggestionModel != null) {
                                final SuggestionType suggestionType = suggestionModel.getSuggestionType();
                                final Object[] objects = {i,
                                        (suggestionType == SuggestionType.TYPE_LOCATION) ? suggestionModel.getName() : suggestionModel.getUsername(),
                                        (suggestionType == SuggestionType.TYPE_LOCATION) ? suggestionModel.getUsername() : suggestionModel.getName(),
                                        suggestionType, suggestionModel.getProfilePic(), suggestionModel.isVerified()};

                                if (!searchHash && !searchUser) cursor.addRow(objects);
                                else {
                                    final boolean isCurrHash = suggestionType == SuggestionType.TYPE_HASHTAG;
                                    if (searchHash && isCurrHash || !searchHash && !isCurrHash)
                                        cursor.addRow(objects);
                                }
                            }
                        }
                    }
                    suggestionAdapter.changeCursor(cursor);
                }
            };

            private void cancelSuggestionsAsync() {
                if (prevSuggestionAsync != null)
                    try {
                        prevSuggestionAsync.cancel(true);
                    } catch (final Exception ignored) {
                    }
            }

            @Override
            public boolean onQueryTextSubmit(final String query) {
                cancelSuggestionsAsync();
                menu.findItem(R.id.action_about).setVisible(true);
                menu.findItem(R.id.action_settings).setVisible(true);

                closeAnyOpenDrawer();
                addToStack();
                userQuery = (query.contains("@") || query.contains("#")) ? query : ("@" + query);
                searchAction.collapseActionView();
                searchView.setIconified(true);
                searchView.setIconified(true);
                mainHelper.onRefresh();
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                cancelSuggestionsAsync();

                if (!Utils.isEmpty(newText)) {
                    searchUser = newText.charAt(0) == '@';
                    searchHash = newText.charAt(0) == '#';

                    if (newText.length() == 1 && (searchHash || searchUser)) {
                        if (searchAutoComplete != null) searchAutoComplete.setThreshold(2);
                    } else {
                        if (searchAutoComplete != null) searchAutoComplete.setThreshold(1);
                        prevSuggestionAsync = new SuggestionsFetcher(fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                searchUser || searchHash ? newText.substring(1) : newText);
                    }
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        if (closeAnyOpenDrawer()) return;

        if (searchView != null && !searchView.isIconified()) {
            if (searchAction != null) searchAction.collapseActionView();
            searchView.setIconified(true);
            searchView.setIconified(true);
            return;
        }

        if (!mainHelper.isSelectionCleared()) return;

        final GridLayoutManager layoutManager = (GridLayoutManager) mainBinding.profileView.mainPosts.getLayoutManager();
        if (layoutManager != null && layoutManager.findFirstCompletelyVisibleItemPosition() >= layoutManager.getSpanCount()) {
            mainBinding.profileView.mainPosts.smoothScrollToPosition(0);
            mainBinding.profileView.appBarLayout.setExpanded(true, true);
            return;
        }

        if (queriesStack != null && queriesStack.size() > 0) {
            userQuery = queriesStack.pop();
            if (userQuery != null) {
                mainHelper.onRefresh();
                return;
            }
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            downloadSelectedItems();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9629 && (resultCode == 1692 || resultCode == RESULT_CANCELED))
            finish();
        else if (requestCode == 6007)
            Utils.showImportExportDialog(this);
        else if (requestCode == 6969 && mainHelper.currentFeedPlayer != null)
            mainHelper.currentFeedPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        if (mainHelper != null) mainHelper.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mainHelper != null) mainHelper.onResume();
        super.onResume();
    }

    private void setStack(final Bundle bundle) {
        final Object stack = bundle != null ? bundle.get("stack") : null;
        if (stack instanceof Stack) //noinspection unchecked
            queriesStack = (Stack<String>) stack;
    }

    public void addToStack() {
        if (userQuery != null) {
            if (queriesStack == null) queriesStack = new Stack<>();
            queriesStack.add(userQuery);
        }
    }

    private boolean closeAnyOpenDrawer() {
        final int childCount = mainBinding.drawerLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = mainBinding.drawerLayout.getChildAt(i);
            final MouseDrawer.LayoutParams childLp = (MouseDrawer.LayoutParams) child.getLayoutParams();

            if ((childLp.openState & MouseDrawer.LayoutParams.FLAG_IS_OPENED) == 1 ||
                    (childLp.openState & MouseDrawer.LayoutParams.FLAG_IS_OPENING) == 2 ||
                    childLp.onScreen >= 0.6 || childLp.isPeeking) {
                mainBinding.drawerLayout.closeDrawer(child);
                return true;
            }
        }
        return false;
    }

    class GetActivity extends AsyncTask<Void, Void, Void> {
        String ok = null;

        protected Void doInBackground(Void... lmao) {
            final String url = "https://www.instagram.com/graphql/query/?query_hash=0f318e8cfff9cc9ef09f88479ff571fb"
            + "&variables={\"id\":\""+uid+"\"}";
            if (!Utils.isEmpty(cookie)) try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    final JSONObject data = new JSONObject(Utils.readFromConnection(urlConnection)).getJSONObject("data")
                            .getJSONObject("user").getJSONObject("edge_activity_count").getJSONArray("edges").getJSONObject(0)
                            .getJSONObject("node");
                    ok = (getString(R.string.activity_count_prefix) + " " + String.join(", ",
                            data.getInt("relationships") == 0 ? null : getString(R.string.activity_count_relationship, data.getInt("relationships")),
                            data.getInt("usertags") == 0 ? null : getString(R.string.activity_count_usertags, data.getInt("usertags")),
                            data.getInt("comments") == 0 ? null : getString(R.string.activity_count_comments, data.getInt("comments")),
                            data.getInt("comment_likes") == 0 ? null : getString(R.string.activity_count_commentlikes, data.getInt("comment_likes")),
                            data.getInt("likes") == 0 ? null :  getString(R.string.activity_count_likes, data.getInt("likes"))) + ".")
                        .replaceAll("null, ", "").replaceAll(",,+", ",").replaceAll("(,+|null).", ".");
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "getactivity: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok == null) {
                if (!Utils.isEmpty(cookie)) Toast.makeText(Main.this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            }
            else if (!ok.equals(getString(R.string.activity_count_prefix) + " .")) {
                final Notification notif = new NotificationCompat.Builder(Main.this, CHANNEL_ID)
                        .setCategory(NotificationCompat.CATEGORY_STATUS).setSmallIcon(R.drawable.ic_notif)
                        .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_MIN).setContentText(ok)
                        .setContentIntent(
                                PendingIntent.getActivity(getApplicationContext(), 1738,
                                        new Intent(getApplicationContext(), NotificationsViewer.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        , PendingIntent.FLAG_UPDATE_CURRENT))
                        .build();
                if (notificationManager != null) {
                    notificationManager.cancel(1800000000);
                    notificationManager.notify(1800000000, notif);
                }
            }
        }
    }
}