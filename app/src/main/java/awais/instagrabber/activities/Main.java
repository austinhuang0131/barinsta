package awais.instagrabber.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.Stack;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.MainHelper;
import awais.instagrabber.R;
import awais.instagrabber.adapters.HighlightsAdapter;
import awais.instagrabber.adapters.SuggestionsAdapter;
import awais.instagrabber.asyncs.SuggestionsFetcher;
import awais.instagrabber.asyncs.UsernameFetcher;
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
                startActivity(new Intent(Main.this, StoryViewer.class)
                        .putExtra(Constants.EXTRAS_USERNAME, userQuery)
                        .putExtra(Constants.EXTRAS_HIGHLIGHT, highlightModel.getTitle())
                        .putExtra(Constants.EXTRAS_STORIES, highlightModel.getStoryModels()));
            }
        }
    });
    private SuggestionsAdapter suggestionAdapter;
    private MenuItem searchAction;
    public ActivityMainBinding mainBinding;
    public SearchView searchView;
    public MenuItem downloadAction, settingsAction, dmsAction;
    public StoryModel[] storyModels;
    public String userQuery = null;
    public MainHelper mainHelper;
    public ProfileModel profileModel;
    public HashtagModel hashtagModel;
    private AutoCompleteTextView searchAutoComplete;
    private ArrayAdapter<String> profileDialogAdapter;
    private DialogInterface.OnClickListener profileDialogListener;
    private Stack<String> queriesStack;
    private DataBox.CookieModel cookieModel;

    public Main() {
        super();
        Utils.changeTheme();
    }

    @Override
    protected void onCreate(@Nullable final Bundle bundle) {
        super.onCreate(bundle);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        FlavorTown.updateCheck(this);
        FlavorTown.changelogCheck(this);

        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String uid = Utils.getUserIdFromCookie(cookie);
        Utils.setupCookies(cookie);

        if (settingsHelper.getInteger(Constants.PROFILE_FETCH_MODE) == 2)
            settingsHelper.putInteger(Constants.PROFILE_FETCH_MODE, 1);

        MainHelper.stopCurrentExecutor();
        mainHelper = new MainHelper(this);
        if (bundle == null) {
            queriesStack = new Stack<>();
            userQuery = null;
        } else {
            setStack(bundle);
            userQuery = bundle.getString("query");
        }

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
                userQuery = result;
                mainHelper.onRefresh();
            }
        };

        // searches for your userid and returns username
        if (uid != null) {
            final FetchListener<String> fetchListener = username -> {
                if (!Utils.isEmpty(username)) {
                    if (!BuildConfig.DEBUG) {
                        userQuery = username;
                        if (mainHelper != null && !mainBinding.swipeRefreshLayout.isRefreshing()) mainHelper.onRefresh();
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
                    fetchListener.onResult(username);
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
                intent = new Intent(this, ProfileViewer.class).putExtra(
                        ((hashtagModel != null) ? Constants.EXTRAS_HASHTAG : Constants.EXTRAS_PROFILE),
                        ((hashtagModel != null) ? hashtagModel : profileModel));
            }
            else intent = new Intent(this, StoryViewer.class).putExtra(Constants.EXTRAS_USERNAME, userQuery)
                    .putExtra(Constants.EXTRAS_STORIES, storyModels)
                    .putExtra(Constants.EXTRAS_HASHTAG, (hashtagModel != null));
            startActivity(intent);
        };

        final View.OnClickListener onClickListener = v -> {
            if (v == mainBinding.mainBiography) {
                Utils.copyText(this, mainBinding.mainBiography.getText().toString());
            } else if (v == mainBinding.mainProfileImage || v == mainBinding.mainHashtagImage) {
                if (storyModels == null || storyModels.length <= 0) {
                    profileDialogListener.onClick(null, 0);
                } else {
                    // because sometimes configuration changes made this crash on some phones
                    new AlertDialog.Builder(this).setAdapter(profileDialogAdapter, profileDialogListener)
                            .setNeutralButton(R.string.cancel, null).show();
                }
            }
        };

        mainBinding.mainBiography.setOnClickListener(onClickListener);
        mainBinding.mainProfileImage.setOnClickListener(onClickListener);
        mainBinding.mainHashtagImage.setOnClickListener(onClickListener);

        mainBinding.mainBiography.setEnabled(false);
        mainBinding.mainProfileImage.setEnabled(false);
        mainBinding.mainHashtagImage.setEnabled(false);

        final boolean isQueryNull = userQuery == null;
        if (isQueryNull) allItems.clear();
        if (BuildConfig.DEBUG && isQueryNull) userQuery = "austinhuang.me";
        if (!mainBinding.swipeRefreshLayout.isRefreshing() && userQuery != null) mainHelper.onRefresh();

        mainHelper.onIntent(getIntent());
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
            if (item == downloadAction) {
                downloadSelectedItems();
            } else if (item == dmsAction)
                startActivity(new Intent(this, DirectMessages.class));
            else if (item == settingsAction)
                new SettingsDialog().show(fragmentManager, "settings");
            else if (item == quickAccessAction)
                new QuickAccessDialog().setQuery(userQuery).show(fragmentManager, "quickAccess");
            else
                new AboutDialog().show(fragmentManager, "about");
            return true;
        };

        quickAccessAction.setOnMenuItemClickListener(clickListener);
        menu.findItem(R.id.action_about).setVisible(true).setOnMenuItemClickListener(clickListener);
        dmsAction = menu.findItem(R.id.action_dms).setOnMenuItemClickListener(clickListener);
        settingsAction = menu.findItem(R.id.action_settings).setVisible(true).setOnMenuItemClickListener(clickListener);
        downloadAction = menu.findItem(R.id.action_download).setOnMenuItemClickListener(clickListener);

        if (!Utils.isEmpty(Utils.settingsHelper.getString(Constants.COOKIE))) {
            //settingsAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
            searchView.setQuery((cookieModel != null && userQuery.equals(cookieModel.getUsername())) ? "" : userQuery, false);
            menu.findItem(R.id.action_about).setVisible(false);
            menu.findItem(R.id.action_settings).setVisible(false);
            menu.findItem(R.id.action_dms).setVisible(false);
            menu.findItem(R.id.action_quickaccess).setVisible(false);
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
                menu.findItem(R.id.action_dms).setVisible(true);
                menu.findItem(R.id.action_quickaccess).setVisible(true);
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
                                final Object[] objects = {i, suggestionModel.getUsername(), suggestionModel.getName(),
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
                    try { prevSuggestionAsync.cancel(true); } catch (final Exception ignored) { }
            }

            @Override
            public boolean onQueryTextSubmit(final String query) {
                cancelSuggestionsAsync();
                menu.findItem(R.id.action_about).setVisible(true);
                menu.findItem(R.id.action_settings).setVisible(true);

                closeAnyOpenDrawer();
                addToStack();
                userQuery = query;
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

        final GridLayoutManager layoutManager = (GridLayoutManager) mainBinding.mainPosts.getLayoutManager();
        if (layoutManager != null && layoutManager.findFirstCompletelyVisibleItemPosition() >= layoutManager.getSpanCount()) {
            mainBinding.mainPosts.smoothScrollToPosition(0);
            mainBinding.appBarLayout.setExpanded(true, true);
            return;
        }

        if (queriesStack != null && queriesStack.size() > 0) {
            userQuery = queriesStack.pop();
            if (userQuery != null) {
                mainHelper.onRefresh();
                return;
            }
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
}