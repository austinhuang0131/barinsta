package awais.instagrabber.activities;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.adapters.SuggestionsAdapter;
import awais.instagrabber.asyncs.SuggestionsFetcher;
import awais.instagrabber.customviews.helpers.CustomHideBottomViewOnScrollBehavior;
import awais.instagrabber.databinding.ActivityMainBinding;
import awais.instagrabber.fragments.settings.MorePreferencesFragmentDirections;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.SuggestionModel;
import awais.instagrabber.models.enums.SuggestionType;
import awais.instagrabber.services.ActivityCheckerService;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.FlavorTown;
import awais.instagrabber.utils.IntentUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.utils.NavigationExtensions.setupWithNavController;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class MainActivity extends BaseLanguageActivity implements FragmentManager.OnBackStackChangedListener {
    private static final String TAG = "MainActivity";

    private static final List<Integer> SHOW_BOTTOM_VIEW_DESTINATIONS = Arrays.asList(
            R.id.directMessagesInboxFragment,
            R.id.feedFragment,
            R.id.profileFragment,
            R.id.discoverFragment,
            R.id.morePreferencesFragment);
    private static final List<Integer> KEEP_SCROLL_BEHAVIOUR_DESTINATIONS = Arrays.asList(
            R.id.directMessagesInboxFragment,
            R.id.feedFragment,
            R.id.profileFragment,
            R.id.discoverFragment,
            R.id.morePreferencesFragment,
            R.id.settingsPreferencesFragment,
            R.id.aboutFragment,
            R.id.hashTagFragment,
            R.id.locationFragment,
            R.id.savedViewerFragment,
            R.id.commentsViewerFragment,
            R.id.followViewerFragment,
            R.id.directMessagesSettingsFragment,
            R.id.notificationsViewer,
            R.id.themePreferencesFragment,
            R.id.favoritesFragment,
            R.id.backupPreferencesFragment);
    private static final Map<Integer, Integer> NAV_TO_MENU_ID_MAP = new HashMap<>();
    private static final List<Integer> REMOVE_COLLAPSING_TOOLBAR_SCROLL_DESTINATIONS = Collections.singletonList(R.id.commentsViewerFragment);
    private static final String FIRST_FRAGMENT_GRAPH_INDEX_KEY = "firstFragmentGraphIndex";

    private ActivityMainBinding binding;
    private LiveData<NavController> currentNavControllerLiveData;
    private MenuItem searchMenuItem;
    private SuggestionsAdapter suggestionAdapter;
    private AutoCompleteTextView searchAutoComplete;
    private SearchView searchView;
    private boolean showSearch = true;
    private Handler suggestionsFetchHandler;
    private int firstFragmentGraphIndex;
    private boolean isActivityCheckerServiceBound = false;
    private boolean isBackStackEmpty = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            // final ActivityCheckerService.LocalBinder binder = (ActivityCheckerService.LocalBinder) service;
            // final ActivityCheckerService activityCheckerService = binder.getService();
            isActivityCheckerServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            isActivityCheckerServiceBound = false;
        }
    };

    static {
        NAV_TO_MENU_ID_MAP.put(R.navigation.direct_messages_nav_graph, R.id.direct_messages_nav_graph);
        NAV_TO_MENU_ID_MAP.put(R.navigation.feed_nav_graph, R.id.feed_nav_graph);
        NAV_TO_MENU_ID_MAP.put(R.navigation.profile_nav_graph, R.id.profile_nav_graph);
        NAV_TO_MENU_ID_MAP.put(R.navigation.discover_nav_graph, R.id.discover_nav_graph);
        NAV_TO_MENU_ID_MAP.put(R.navigation.more_nav_graph, R.id.more_nav_graph);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        CookieUtils.setupCookies(cookie);
        setContentView(binding.getRoot());
        final Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        createNotificationChannels();
        if (savedInstanceState == null) {
            setupBottomNavigationBar(true);
        }
        setupScrollingListener();
        setupSuggestions();
        final boolean checkUpdates = settingsHelper.getBoolean(Constants.CHECK_UPDATES);
        if (checkUpdates) FlavorTown.updateCheck(this);
        FlavorTown.changelogCheck(this);
        final Intent intent = getIntent();
        handleIntent(intent);
        if (!TextUtils.isEmpty(cookie) && settingsHelper.getBoolean(Constants.CHECK_ACTIVITY)) {
            bindActivityCheckerService();
        }
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        // Log.d("austin_debug", "dir: "+Arrays.toString(StorageUtil.getStorageDirectories(getApplicationContext())));
        // final File sdcard = new File(StorageUtil.getStorageDirectories(getApplicationContext())[0]);
        // Log.d("austin_debug", "files: "+Arrays.toString(sdcard.listFiles()));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        searchMenuItem = menu.findItem(R.id.search);
        if (showSearch && currentNavControllerLiveData != null && currentNavControllerLiveData.getValue() != null) {
            final NavController navController = currentNavControllerLiveData.getValue();
            final NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null) {
                final int destinationId = currentDestination.getId();
                showSearch = destinationId == R.id.profileFragment;
            }
        }
        if (!showSearch) {
            searchMenuItem.setVisible(false);
            return true;
        }
        return setupSearchView();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString(FIRST_FRAGMENT_GRAPH_INDEX_KEY, String.valueOf(firstFragmentGraphIndex));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final String key = (String) savedInstanceState.get(FIRST_FRAGMENT_GRAPH_INDEX_KEY);
        if (key != null) {
            try {
                firstFragmentGraphIndex = Integer.parseInt(key);
            } catch (NumberFormatException ignored) { }
        }
        setupBottomNavigationBar(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (currentNavControllerLiveData != null && currentNavControllerLiveData.getValue() != null) {
            return currentNavControllerLiveData.getValue().navigateUp();
        }
        return false;
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindActivityCheckerService();
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isTaskRoot() && isBackStackEmpty) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        final int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        isBackStackEmpty = backStackEntryCount == 0;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.createNotificationChannel(new NotificationChannel(Constants.DOWNLOAD_CHANNEL_ID,
                                                                                  Constants.DOWNLOAD_CHANNEL_NAME,
                                                                                  NotificationManager.IMPORTANCE_DEFAULT));
            notificationManager.createNotificationChannel(new NotificationChannel(Constants.ACTIVITY_CHANNEL_ID,
                                                                                  Constants.ACTIVITY_CHANNEL_NAME,
                                                                                  NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    private void setupSuggestions() {
        suggestionsFetchHandler = new Handler();
        suggestionAdapter = new SuggestionsAdapter(this, (type, query) -> {
            if (searchMenuItem != null) searchMenuItem.collapseActionView();
            if (searchView != null && !searchView.isIconified()) searchView.setIconified(true);
            if (currentNavControllerLiveData != null && currentNavControllerLiveData.getValue() != null) {
                final NavController navController = currentNavControllerLiveData.getValue();
                final Bundle bundle = new Bundle();
                switch (type) {
                    case TYPE_LOCATION:
                        bundle.putString("locationId", query);
                        navController.navigate(R.id.action_global_locationFragment, bundle);
                        break;
                    case TYPE_HASHTAG:
                        bundle.putString("hashtag", query);
                        navController.navigate(R.id.action_global_hashTagFragment, bundle);
                        break;
                    case TYPE_USER:
                        bundle.putString("username", query);
                        navController.navigate(R.id.action_global_profileFragment, bundle);
                        break;
                }
            }
        });
    }

    private void setupScrollingListener() {
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.bottomNavView.getLayoutParams();
        layoutParams.setBehavior(new CustomHideBottomViewOnScrollBehavior());
        binding.bottomNavView.requestLayout();
    }

    private boolean setupSearchView() {
        final View actionView = searchMenuItem.getActionView();
        if (!(actionView instanceof SearchView)) return false;
        searchView = (SearchView) actionView;
        searchView.setSuggestionsAdapter(suggestionAdapter);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        final View searchText = searchView.findViewById(R.id.search_src_text);
        if (searchText instanceof AutoCompleteTextView) {
            searchAutoComplete = (AutoCompleteTextView) searchText;
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            private boolean searchUser;
            private boolean searchHash;
            private AsyncTask<?, ?, ?> prevSuggestionAsync;
            private final String[] COLUMNS = {
                    BaseColumns._ID,
                    Constants.EXTRAS_USERNAME,
                    Constants.EXTRAS_NAME,
                    Constants.EXTRAS_TYPE,
                    "pfp",
                    "verified"
            };
            private String currentSearchQuery;

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
                                final Object[] objects = {
                                        i,
                                        suggestionType == SuggestionType.TYPE_LOCATION ? suggestionModel.getName() : suggestionModel.getUsername(),
                                        suggestionType == SuggestionType.TYPE_LOCATION ? suggestionModel.getUsername() : suggestionModel.getName(),
                                        suggestionType,
                                        suggestionModel.getProfilePic(),
                                        suggestionModel.isVerified()};
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

            private final Runnable runnable = () -> {
                cancelSuggestionsAsync();
                if (TextUtils.isEmpty(currentSearchQuery)) {
                    suggestionAdapter.changeCursor(null);
                    return;
                }
                searchUser = currentSearchQuery.charAt(0) == '@';
                searchHash = currentSearchQuery.charAt(0) == '#';
                if (currentSearchQuery.length() == 1 && (searchHash || searchUser)) {
                    if (searchAutoComplete != null) {
                        searchAutoComplete.setThreshold(2);
                    }
                } else {
                    if (searchAutoComplete != null) {
                        searchAutoComplete.setThreshold(1);
                    }
                    prevSuggestionAsync = new SuggestionsFetcher(fetchListener).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR,
                            searchUser || searchHash ? currentSearchQuery.substring(1)
                                                     : currentSearchQuery);
                }
            };

            private void cancelSuggestionsAsync() {
                if (prevSuggestionAsync != null)
                    try {
                        prevSuggestionAsync.cancel(true);
                    } catch (final Exception ignored) {}
            }

            @Override
            public boolean onQueryTextSubmit(final String query) {
                return onQueryTextChange(query);
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                suggestionsFetchHandler.removeCallbacks(runnable);
                currentSearchQuery = query;
                suggestionsFetchHandler.postDelayed(runnable, 800);
                return true;
            }
        });
        return true;
    }

    private void setupBottomNavigationBar(final boolean setDefaultFromSettings) {
        int main_nav_ids = R.array.main_nav_ids;
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != null;
        if (!isLoggedIn) {
            main_nav_ids = R.array.logged_out_main_nav_ids;
            final int selectedItemId = binding.bottomNavView.getSelectedItemId();
            binding.bottomNavView.getMenu().clear();
            binding.bottomNavView.inflateMenu(R.menu.logged_out_bottom_navigation_menu);
            if (selectedItemId == R.id.profile_nav_graph
                    || selectedItemId == R.id.more_nav_graph) {
                binding.bottomNavView.setSelectedItemId(selectedItemId);
            } else {
                setBottomNavSelectedItem(R.navigation.profile_nav_graph);
            }
        }
        final List<Integer> mainNavList = getMainNavList(main_nav_ids);
        if (setDefaultFromSettings) {
            final String defaultTabIdString = settingsHelper.getString(Constants.DEFAULT_TAB);
            try {
                final int defaultNavId = TextUtils.isEmpty(defaultTabIdString)
                                         ? R.navigation.profile_nav_graph
                                         : Integer.parseInt(defaultTabIdString);
                final int index = mainNavList.indexOf(defaultNavId);
                if (index >= 0) firstFragmentGraphIndex = index;
                setBottomNavSelectedItem(defaultNavId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing id", e);
            }
        }
        final LiveData<NavController> navControllerLiveData = setupWithNavController(
                binding.bottomNavView,
                mainNavList,
                getSupportFragmentManager(),
                R.id.main_nav_host,
                getIntent(),
                firstFragmentGraphIndex);
        navControllerLiveData.observe(this, this::setupNavigation);
        currentNavControllerLiveData = navControllerLiveData;
    }

    private void setBottomNavSelectedItem(final int navId) {
        final Integer menuId = NAV_TO_MENU_ID_MAP.get(navId);
        if (menuId != null) {
            binding.bottomNavView.setSelectedItemId(menuId);
        }
    }

    @NonNull
    private List<Integer> getMainNavList(final int main_nav_ids) {
        final TypedArray navIds = getResources().obtainTypedArray(main_nav_ids);
        final List<Integer> mainNavList = new ArrayList<>(navIds.length());
        final int length = navIds.length();
        for (int i = 0; i < length; i++) {
            final int resourceId = navIds.getResourceId(i, -1);
            if (resourceId < 0) continue;
            mainNavList.add(resourceId);
        }
        navIds.recycle();
        return mainNavList;
    }

    private void setupNavigation(final NavController navController) {
        NavigationUI.setupWithNavController(binding.toolbar, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // below is a hack to check if we are at the end of the current stack, to setup the search view
            binding.appBarLayout.setExpanded(true, true);
            final int destinationId = destination.getId();
            @SuppressLint("RestrictedApi") final Deque<NavBackStackEntry> backStack = navController.getBackStack();
            setupMenu(backStack.size(), destinationId);
            binding.bottomNavView.setVisibility(SHOW_BOTTOM_VIEW_DESTINATIONS.contains(destinationId) ? View.VISIBLE : View.GONE);
            if (KEEP_SCROLL_BEHAVIOUR_DESTINATIONS.contains(destinationId)) {
                setScrollingBehaviour();
            } else {
                removeScrollingBehaviour();
            }
            if (REMOVE_COLLAPSING_TOOLBAR_SCROLL_DESTINATIONS.contains(destinationId)) {
                removeCollapsingToolbarScrollFlags();
            } else {
                setCollapsingToolbarScrollFlags();
            }
        });
    }

    private void setupMenu(final int backStackSize, final int destinationId) {
        if (searchMenuItem == null) return;
        if (backStackSize >= 2 && destinationId == R.id.profileFragment) {
            showSearch = true;
            searchMenuItem.setVisible(true);
            return;
        }
        showSearch = false;
        searchMenuItem.setVisible(false);
    }

    private void setScrollingBehaviour() {
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.mainNavHost.getLayoutParams();
        layoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        binding.mainNavHost.requestLayout();
    }

    private void removeScrollingBehaviour() {
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.mainNavHost.getLayoutParams();
        layoutParams.setBehavior(null);
        binding.mainNavHost.requestLayout();
    }

    private void setCollapsingToolbarScrollFlags() {
        final CollapsingToolbarLayout collapsingToolbarLayout = binding.collapsingToolbarLayout;
        final AppBarLayout.LayoutParams toolbarLayoutLayoutParams = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();
        toolbarLayoutLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                                         | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                                                         | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        binding.collapsingToolbarLayout.requestLayout();
    }

    private void removeCollapsingToolbarScrollFlags() {
        final CollapsingToolbarLayout collapsingToolbarLayout = binding.collapsingToolbarLayout;
        final AppBarLayout.LayoutParams toolbarLayoutLayoutParams = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();
        toolbarLayoutLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
        binding.collapsingToolbarLayout.requestLayout();
    }

    private void handleIntent(final Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        final String type = intent.getType();
        // Log.d(TAG, action + " " + type);
        if (Intent.ACTION_MAIN.equals(action)) return;
        if (Constants.ACTION_SHOW_ACTIVITY.equals(action)) {
            showActivityView();
            return;
        }
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.equals("text/plain")) {
                handleUrl(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
            return;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri data = intent.getData();
            if (data == null) return;
            handleUrl(data.toString());
        }
    }

    private void handleUrl(final String url) {
        if (url == null) return;
        // Log.d(TAG, url);
        final IntentModel intentModel = IntentUtils.parseUrl(url);
        if (intentModel == null) return;
        showView(intentModel);
    }

    private void showView(final IntentModel intentModel) {
        switch (intentModel.getType()) {
            case USERNAME:
                showProfileView(intentModel);
                break;
            case POST:
                showPostView(intentModel);
                break;
            case LOCATION:
                showLocationView(intentModel);
                break;
            case HASHTAG:
                showHashtagView(intentModel);
                break;
            case UNKNOWN:
            default:
                Log.w(TAG, "Unknown model type received!");
        }
    }

    private void showProfileView(@NonNull final IntentModel intentModel) {
        final String username = intentModel.getText();
        // Log.d(TAG, "username: " + username);
        final NavController navController = currentNavControllerLiveData.getValue();
        if (currentNavControllerLiveData == null || navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("username", "@" + username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    private void showPostView(@NonNull final IntentModel intentModel) {
        final String shortCode = intentModel.getText();
        // Log.d(TAG, "shortCode: " + shortCode);
        final NavController navController = currentNavControllerLiveData.getValue();
        if (currentNavControllerLiveData == null || navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putStringArray("idOrCodeArray", new String[]{shortCode});
        bundle.putInt("index", 0);
        bundle.putBoolean("isId", false);
        navController.navigate(R.id.action_global_postViewFragment, bundle);
    }

    private void showLocationView(@NonNull final IntentModel intentModel) {
        final String locationId = intentModel.getText();
        // Log.d(TAG, "locationId: " + locationId);
        final NavController navController = currentNavControllerLiveData.getValue();
        if (currentNavControllerLiveData == null || navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("locationId", locationId);
        navController.navigate(R.id.action_global_locationFragment, bundle);
    }

    private void showHashtagView(@NonNull final IntentModel intentModel) {
        final String hashtag = intentModel.getText();
        // Log.d(TAG, "hashtag: " + hashtag);
        final NavController navController = currentNavControllerLiveData.getValue();
        if (currentNavControllerLiveData == null || navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("hashtag", "#" + hashtag);
        navController.navigate(R.id.action_global_hashTagFragment, bundle);
    }

    private void showActivityView() {
        binding.bottomNavView.setSelectedItemId(R.id.more_nav_graph);
        binding.bottomNavView.post(() -> {
            final NavController navController = currentNavControllerLiveData.getValue();
            if (currentNavControllerLiveData == null || navController == null) return;
            final NavDirections navDirections = MorePreferencesFragmentDirections.actionMorePreferencesFragmentToNotificationsViewer();
            navController.navigate(navDirections);
        });
    }

    private void bindActivityCheckerService() {
        bindService(new Intent(this, ActivityCheckerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        isActivityCheckerServiceBound = true;
    }

    private void unbindActivityCheckerService() {
        if (!isActivityCheckerServiceBound) return;
        unbindService(serviceConnection);
        isActivityCheckerServiceBound = false;
    }

    @NonNull
    public BottomNavigationView getBottomNavView() {
        return binding.bottomNavView;
    }
}