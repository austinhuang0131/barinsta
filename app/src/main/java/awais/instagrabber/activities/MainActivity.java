package awais.instagrabber.activities;

import android.animation.LayoutTransition;
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
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.adapters.SuggestionsAdapter;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.asyncs.SuggestionsFetcher;
import awais.instagrabber.customviews.emoji.EmojiVariantManager;
import awais.instagrabber.databinding.ActivityMainBinding;
import awais.instagrabber.fragments.PostViewV2Fragment;
import awais.instagrabber.fragments.directmessages.DirectMessageInboxFragmentDirections;
import awais.instagrabber.fragments.main.FeedFragment;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.SuggestionModel;
import awais.instagrabber.models.enums.SuggestionType;
import awais.instagrabber.services.ActivityCheckerService;
import awais.instagrabber.services.DMSyncAlarmReceiver;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.FlavorTown;
import awais.instagrabber.utils.IntentUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.emoji.EmojiParser;
import awais.instagrabber.viewmodels.AppStateViewModel;

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
    private static final Map<Integer, Integer> NAV_TO_MENU_ID_MAP = new HashMap<>();
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
    private boolean isLoggedIn;
    private HideBottomViewOnScrollBehavior<BottomNavigationView> behavior;

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
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != 0;
        if (settingsHelper.getBoolean(Constants.FLAG_SECURE))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(binding.getRoot());
        final Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        createNotificationChannels();
        try {
            final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.bottomNavView.getLayoutParams();
            //noinspection unchecked
            behavior = (HideBottomViewOnScrollBehavior<BottomNavigationView>) layoutParams.getBehavior();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }
        if (savedInstanceState == null) {
            setupBottomNavigationBar(true);
        }
        setupSuggestions();
        final boolean checkUpdates = settingsHelper.getBoolean(Constants.CHECK_UPDATES);
        if (checkUpdates) FlavorTown.updateCheck(this);
        FlavorTown.changelogCheck(this);
        new ViewModelProvider(this).get(AppStateViewModel.class); // Just initiate the App state here
        final Intent intent = getIntent();
        handleIntent(intent);
        if (!TextUtils.isEmpty(cookie) && settingsHelper.getBoolean(Constants.CHECK_ACTIVITY)) {
            bindActivityCheckerService();
        }
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        // Initialise the internal map
        AppExecutors.getInstance().tasksThread().execute(() -> {
            EmojiParser.getInstance();
            EmojiVariantManager.getInstance();
        });
        initEmojiCompat();
        // initDmService();
    }

    private void initDmService() {
        if (!isLoggedIn) return;
        final boolean enabled = settingsHelper.getBoolean(PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH);
        if (!enabled) return;
        DMSyncAlarmReceiver.setAlarm(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        searchMenuItem = menu.findItem(R.id.search);
        if (showSearch && currentNavControllerLiveData != null) {
            final NavController navController = currentNavControllerLiveData.getValue();
            if (navController != null) {
                final NavDestination currentDestination = navController.getCurrentDestination();
                if (currentDestination != null) {
                    final int destinationId = currentDestination.getId();
                    showSearch = destinationId == R.id.profileFragment;
                }
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
        if (currentNavControllerLiveData == null) return false;
        final NavController navController = currentNavControllerLiveData.getValue();
        if (navController == null) return false;
        return navController.navigateUp();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: ", e);
        }
        unbindActivityCheckerService();
    }

    @Override
    public void onBackPressed() {
        int currentNavControllerBackStack = 2;
        if (currentNavControllerLiveData != null) {
            final NavController navController = currentNavControllerLiveData.getValue();
            if (navController != null) {
                @SuppressLint("RestrictedApi") final Deque<NavBackStackEntry> backStack = navController.getBackStack();
                if (backStack != null) {
                    currentNavControllerBackStack = backStack.size();
                }
            }
        }
        if (isTaskRoot() && isBackStackEmpty && currentNavControllerBackStack == 2) {
            finishAfterTransition();
            return;
        }
        if (!isFinishing()) {
            try {
                super.onBackPressed();
            } catch (Exception e) {
                Log.e(TAG, "onBackPressed: ", e);
                finish();
            }
        }
    }

    @Override
    public void onBackStackChanged() {
        final int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        isBackStackEmpty = backStackEntryCount == 0;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.createNotificationChannel(new NotificationChannel(Constants.DOWNLOAD_CHANNEL_ID,
                                                                              Constants.DOWNLOAD_CHANNEL_NAME,
                                                                              NotificationManager.IMPORTANCE_DEFAULT));
        notificationManager.createNotificationChannel(new NotificationChannel(Constants.ACTIVITY_CHANNEL_ID,
                                                                              Constants.ACTIVITY_CHANNEL_NAME,
                                                                              NotificationManager.IMPORTANCE_DEFAULT));
        notificationManager.createNotificationChannel(new NotificationChannel(Constants.DM_UNREAD_CHANNEL_ID,
                                                                              Constants.DM_UNREAD_CHANNEL_NAME,
                                                                              NotificationManager.IMPORTANCE_DEFAULT));
        final NotificationChannel silentNotificationChannel = new NotificationChannel(Constants.SILENT_NOTIFICATIONS_CHANNEL_ID,
                                                                                      Constants.SILENT_NOTIFICATIONS_CHANNEL_NAME,
                                                                                      NotificationManager.IMPORTANCE_LOW);
        silentNotificationChannel.setSound(null, null);
        notificationManager.createNotificationChannel(silentNotificationChannel);
    }

    private void setupSuggestions() {
        suggestionsFetchHandler = new Handler();
        suggestionAdapter = new SuggestionsAdapter(this, (type, query) -> {
            if (searchMenuItem != null) searchMenuItem.collapseActionView();
            if (searchView != null && !searchView.isIconified()) searchView.setIconified(true);
            if (currentNavControllerLiveData == null) return;
            final NavController navController = currentNavControllerLiveData.getValue();
            if (navController == null) return;
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
        });
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
            final String defaultTabResNameString = settingsHelper.getString(Constants.DEFAULT_TAB);
            try {
                int navId = 0;
                if (!TextUtils.isEmpty(defaultTabResNameString)) {
                    navId = getResources().getIdentifier(defaultTabResNameString, "navigation", getPackageName());
                }
                final int defaultNavId = navId <= 0 ? R.navigation.profile_nav_graph
                                                    : navId;
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
        navControllerLiveData.observe(this, navController -> setupNavigation(binding.toolbar, navController));
        currentNavControllerLiveData = navControllerLiveData;
        binding.bottomNavView.setOnNavigationItemReselectedListener(item -> {
            // Log.d(TAG, "setupBottomNavigationBar: item: " + item);
            final Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.main_nav_host);
            if (navHostFragment != null) {
                final Fragment fragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                if (fragment instanceof FeedFragment) {
                    ((FeedFragment) fragment).scrollToTop();
                }
            }
        });
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

    private void setupNavigation(final Toolbar toolbar, final NavController navController) {
        if (navController == null) return;
        NavigationUI.setupWithNavController(toolbar, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.directMessagesThreadFragment && arguments != null) {
                // Set the thread title earlier for better ux
                final String title = arguments.getString("title");
                final ActionBar actionBar = getSupportActionBar();
                if (actionBar != null && !TextUtils.isEmpty(title)) {
                    actionBar.setTitle(title);
                }
            }
            // below is a hack to check if we are at the end of the current stack, to setup the search view
            binding.appBarLayout.setExpanded(true, true);
            final int destinationId = destination.getId();
            @SuppressLint("RestrictedApi") final Deque<NavBackStackEntry> backStack = navController.getBackStack();
            setupMenu(backStack.size(), destinationId);
            final boolean contains = SHOW_BOTTOM_VIEW_DESTINATIONS.contains(destinationId);
            binding.bottomNavView.setVisibility(contains ? View.VISIBLE : View.GONE);
            if (contains && behavior != null) {
                behavior.slideUp(binding.bottomNavView);
            }

            // explicitly hide keyboard when we navigate
            final View view = getCurrentFocus();
            Utils.hideKeyboard(view);
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
        if (Constants.ACTION_SHOW_DM_THREAD.equals(action)) {
            showThread(intent);
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

    private void showThread(@NonNull final Intent intent) {
        final String threadId = intent.getStringExtra(Constants.DM_THREAD_ACTION_EXTRA_THREAD_ID);
        final String threadTitle = intent.getStringExtra(Constants.DM_THREAD_ACTION_EXTRA_THREAD_TITLE);
        navigateToThread(threadId, threadTitle);
    }

    public void navigateToThread(final String threadId, final String threadTitle) {
        if (threadId == null || threadTitle == null) return;
        currentNavControllerLiveData.observe(this, new Observer<NavController>() {
            @Override
            public void onChanged(final NavController navController) {
                if (navController == null) return;
                if (navController.getGraph().getId() != R.id.direct_messages_nav_graph) return;
                try {
                    final NavDestination currentDestination = navController.getCurrentDestination();
                    if (currentDestination != null && currentDestination.getId() == R.id.directMessagesInboxFragment) {
                        // if we are already on the inbox page, navigate to the thread
                        // need handler.post() to wait for the fragment manager to be ready to navigate
                        new Handler().post(() -> {
                            final DirectMessageInboxFragmentDirections.ActionInboxToThread action = DirectMessageInboxFragmentDirections
                                    .actionInboxToThread(threadId, threadTitle);
                            navController.navigate(action);
                        });
                        return;
                    }
                    // add a destination change listener to navigate to thread once we are on the inbox page
                    navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                        @Override
                        public void onDestinationChanged(@NonNull final NavController controller,
                                                         @NonNull final NavDestination destination,
                                                         @Nullable final Bundle arguments) {
                            if (destination.getId() == R.id.directMessagesInboxFragment) {
                                final DirectMessageInboxFragmentDirections.ActionInboxToThread action = DirectMessageInboxFragmentDirections
                                        .actionInboxToThread(threadId, threadTitle);
                                controller.navigate(action);
                                controller.removeOnDestinationChangedListener(this);
                            }
                        }
                    });
                    // pop back stack until we reach the inbox page
                    navController.popBackStack(R.id.directMessagesInboxFragment, false);
                } finally {
                    currentNavControllerLiveData.removeObserver(this);
                }
            }
        });
        final int selectedItemId = binding.bottomNavView.getSelectedItemId();
        if (selectedItemId != R.navigation.direct_messages_nav_graph) {
            setBottomNavSelectedItem(R.navigation.direct_messages_nav_graph);
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
        if (currentNavControllerLiveData == null) return;
        final NavController navController = currentNavControllerLiveData.getValue();
        if (navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("username", "@" + username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    private void showPostView(@NonNull final IntentModel intentModel) {
        final String shortCode = intentModel.getText();
        // Log.d(TAG, "shortCode: " + shortCode);
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(R.layout.dialog_opening_post)
                .create();
        alertDialog.show();
        new PostFetcher(shortCode, feedModel -> {
            if (feedModel != null) {
                final PostViewV2Fragment fragment = PostViewV2Fragment
                        .builder(feedModel)
                        .build();
                fragment.setOnShowListener(dialog -> alertDialog.dismiss());
                fragment.show(getSupportFragmentManager(), "post_view");
                return;
            }
            Toast.makeText(getApplicationContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        }).execute();
    }

    private void showLocationView(@NonNull final IntentModel intentModel) {
        final String locationId = intentModel.getText();
        // Log.d(TAG, "locationId: " + locationId);
        if (currentNavControllerLiveData == null) return;
        final NavController navController = currentNavControllerLiveData.getValue();
        if (navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("locationId", locationId);
        navController.navigate(R.id.action_global_locationFragment, bundle);
    }

    private void showHashtagView(@NonNull final IntentModel intentModel) {
        final String hashtag = intentModel.getText();
        // Log.d(TAG, "hashtag: " + hashtag);
        if (currentNavControllerLiveData == null) return;
        final NavController navController = currentNavControllerLiveData.getValue();
        if (navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("hashtag", hashtag);
        navController.navigate(R.id.action_global_hashTagFragment, bundle);
    }

    private void showActivityView() {
        if (currentNavControllerLiveData == null) return;
        final NavController navController = currentNavControllerLiveData.getValue();
        if (navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("type", "notif");
        navController.navigate(R.id.action_global_notificationsViewerFragment, bundle);
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

    public void setCollapsingView(@NonNull final View view) {
        binding.collapsingToolbarLayout.addView(view, 0);
    }

    public void removeCollapsingView(@NonNull final View view) {
        binding.collapsingToolbarLayout.removeView(view);
    }

    public void setToolbar(final Toolbar toolbar) {
        binding.appBarLayout.setVisibility(View.GONE);
        removeScrollingBehaviour();
        setSupportActionBar(toolbar);
        if (currentNavControllerLiveData == null) return;
        setupNavigation(toolbar, currentNavControllerLiveData.getValue());
    }

    public void resetToolbar() {
        binding.appBarLayout.setVisibility(View.VISIBLE);
        setScrollingBehaviour();
        setSupportActionBar(binding.toolbar);
        if (currentNavControllerLiveData == null) return;
        setupNavigation(binding.toolbar, currentNavControllerLiveData.getValue());
    }

    public CollapsingToolbarLayout getCollapsingToolbarView() {
        return binding.collapsingToolbarLayout;
    }

    public AppBarLayout getAppbarLayout() {
        return binding.appBarLayout;
    }

    public void removeLayoutTransition() {
        binding.getRoot().setLayoutTransition(null);
    }

    public void setLayoutTransition() {
        binding.getRoot().setLayoutTransition(new LayoutTransition());
    }

    private void initEmojiCompat() {
        // Use a downloadable font for EmojiCompat
        final FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(getApplicationContext(), fontRequest);
        config.setReplaceAll(true)
              // .setUseEmojiAsDefaultStyle(true)
              .registerInitCallback(new EmojiCompat.InitCallback() {
                  @Override
                  public void onInitialized() {
                      Log.i(TAG, "EmojiCompat initialized");
                  }

                  @Override
                  public void onFailed(@Nullable Throwable throwable) {
                      Log.e(TAG, "EmojiCompat initialization failed", throwable);
                  }
              });
        EmojiCompat.init(config);
    }

    public Toolbar getToolbar() {
        return binding.toolbar;
    }
}