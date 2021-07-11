package awais.instagrabber.activities

import android.animation.LayoutTransition
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.provider.FontRequest
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiCompat.InitCallback
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import awais.instagrabber.BuildConfig
import awais.instagrabber.R
import awais.instagrabber.customviews.emoji.EmojiVariantManager
import awais.instagrabber.customviews.helpers.RootViewDeferringInsetsCallback
import awais.instagrabber.customviews.helpers.TextWatcherAdapter
import awais.instagrabber.databinding.ActivityMainBinding
import awais.instagrabber.fragments.main.FeedFragment
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.models.IntentModel
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Tab
import awais.instagrabber.models.enums.IntentModelType
import awais.instagrabber.services.ActivityCheckerService
import awais.instagrabber.services.DMSyncAlarmReceiver
import awais.instagrabber.utils.*
import awais.instagrabber.utils.AppExecutors.tasksThread
import awais.instagrabber.utils.DownloadUtils.ReselectDocumentTreeException
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.emoji.EmojiParser
import awais.instagrabber.viewmodels.AppStateViewModel
import awais.instagrabber.viewmodels.DirectInboxViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.common.collect.ImmutableList
import java.util.*


class MainActivity : BaseLanguageActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var searchMenuItem: MenuItem? = null
    private var startNavRootId: Int = 0

    private var lastSelectedNavMenuId = 0
    private var isActivityCheckerServiceBound = false
    private var isLoggedIn = false
    private var deviceUuid: String? = null
    private var csrfToken: String? = null
    private var userId: Long = 0
    private var toolbarOwner: Fragment? = null

    lateinit var toolbar: Toolbar
        private set

    var currentTabs: List<Tab> = emptyList()
        private set
    private var showBottomViewDestinations: List<Int> = emptyList()

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // final ActivityCheckerService.LocalBinder binder = (ActivityCheckerService.LocalBinder) service;
            // final ActivityCheckerService activityCheckerService = binder.getService();
            isActivityCheckerServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isActivityCheckerServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            DownloadUtils.init(
                this,
                Utils.settingsHelper.getString(PreferenceKeys.PREF_BARINSTA_DIR_URI)
            )
        } catch (e: ReselectDocumentTreeException) {
            super.onCreate(savedInstanceState)
            val intent = Intent(this, DirectorySelectActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(EXTRA_INITIAL_URI, e.initialUri)
            }
            startActivity(intent)
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        instance = this
        binding = ActivityMainBinding.inflate(layoutInflater)
        toolbar = binding.toolbar
        setupCookie()
        if (Utils.settingsHelper.getBoolean(PreferenceKeys.FLAG_SECURE)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupInsetsCallback()
        createNotificationChannels()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHostFragment.navController
        if (savedInstanceState == null) {
            setupNavigation(true)
        }
        if (!BuildConfig.isPre) {
            val checkUpdates = Utils.settingsHelper.getBoolean(PreferenceKeys.CHECK_UPDATES)
            if (checkUpdates) FlavorTown.updateCheck(this)
        }
        FlavorTown.changelogCheck(this)
        ViewModelProvider(this).get(AppStateViewModel::class.java) // Just initiate the App state here
        handleIntent(intent)
        if (isLoggedIn && Utils.settingsHelper.getBoolean(PreferenceKeys.CHECK_ACTIVITY)) {
            bindActivityCheckerService()
        }
        // Initialise the internal map
        tasksThread.execute {
            EmojiParser.getInstance(this)
            EmojiVariantManager.getInstance()
        }
        initEmojiCompat()
        // initDmService();
        initDmUnreadCount()
        initSearchInput()
    }

    private fun setupInsetsCallback() {
        val deferringInsetsCallback = RootViewDeferringInsetsCallback(
            WindowInsetsCompat.Type.systemBars(),
            WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setWindowInsetsAnimationCallback(binding.root, deferringInsetsCallback)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsCallback)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupCookie() {
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        userId = 0
        csrfToken = null
        if (cookie.isNotBlank()) {
            userId = getUserIdFromCookie(cookie)
            csrfToken = getCsrfTokenFromCookie(cookie)
        }
        if (cookie.isBlank() || userId == 0L || csrfToken.isNullOrBlank()) {
            isLoggedIn = false
            return
        }
        deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        if (isEmpty(deviceUuid)) {
            Utils.settingsHelper.putString(Constants.DEVICE_UUID, UUID.randomUUID().toString())
        }
        setupCookies(cookie)
        isLoggedIn = true
    }

    @Suppress("unused")
    private fun initDmService() {
        if (!isLoggedIn) return
        val enabled = Utils.settingsHelper.getBoolean(PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH)
        if (!enabled) return
        DMSyncAlarmReceiver.setAlarm(this)
    }

    private fun initDmUnreadCount() {
        if (!isLoggedIn) return
        val directInboxViewModel = ViewModelProvider(this).get(DirectInboxViewModel::class.java)
        directInboxViewModel.unseenCount.observe(this, { unseenCountResource: Resource<Int?>? ->
            if (unseenCountResource == null) return@observe
            val unseenCount = unseenCountResource.data
            setNavBarDMUnreadCountBadge(unseenCount ?: 0)
        })
    }

    private fun initSearchInput() {
        binding.searchInputLayout.setEndIconOnClickListener {
            val editText = binding.searchInputLayout.editText ?: return@setEndIconOnClickListener
            editText.setText("")
        }
        binding.searchInputLayout.addOnEditTextAttachedListener { textInputLayout: TextInputLayout ->
            textInputLayout.isEndIconVisible = false
            val editText = textInputLayout.editText ?: return@addOnEditTextAttachedListener
            editText.addTextChangedListener(object : TextWatcherAdapter() {
                override fun afterTextChanged(s: Editable) {
                    binding.searchInputLayout.isEndIconVisible = !isEmpty(s)
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        searchMenuItem = menu.findItem(R.id.search)
        val currentDestination = navController.currentDestination
        if (currentDestination != null) {
            val backStack = navController.backQueue
            setupMenu(backStack.size, currentDestination.id)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.search) {
            try {
                navController.navigate(getSearchDeepLink())
                return true
            } catch (e: Exception) {
                Log.e(TAG, "onOptionsItemSelected: ", e)
            }
            return false
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // outState.putString(FIRST_FRAGMENT_GRAPH_INDEX_KEY, firstFragmentGraphIndex.toString())
        outState.putString(LAST_SELECT_NAV_MENU_ID, binding.bottomNavView.selectedItemId.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastSelected = savedInstanceState[LAST_SELECT_NAV_MENU_ID] as String?
        if (lastSelected != null) {
            try {
                lastSelectedNavMenuId = lastSelected.toInt()
            } catch (ignored: NumberFormatException) {
            }
        }
        setupNavigation(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: ", e)
        }
        unbindActivityCheckerService()
        // try {
        //     RetrofitFactory.getInstance().destroy()
        // } catch (e: Exception) {
        //     Log.e(TAG, "onDestroy: ", e)
        // }
        DownloadUtils.destroy()
        instance = null
    }

    // override fun onBackPressed() {
    // Log.d(TAG, "onBackPressed: ")
    // navController.navigateUp()
    //     val backStack = navController.backQueue
    //     val currentNavControllerBackStack = backStack.size
    //     if (isTaskRoot && isBackStackEmpty && currentNavControllerBackStack == 2) {
    //         finishAfterTransition()
    //         return
    //     }
    //     if (!isFinishing) {
    //         try {
    //             super.onBackPressed()
    //         } catch (e: Exception) {
    //             Log.e(TAG, "onBackPressed: ", e)
    //             finish()
    //         }
    //     }
    // }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                Constants.DOWNLOAD_CHANNEL_ID,
                Constants.DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                Constants.ACTIVITY_CHANNEL_ID,
                Constants.ACTIVITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                Constants.DM_UNREAD_CHANNEL_ID,
                Constants.DM_UNREAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val silentNotificationChannel = NotificationChannel(
            Constants.SILENT_NOTIFICATIONS_CHANNEL_ID,
            Constants.SILENT_NOTIFICATIONS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        silentNotificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(silentNotificationChannel)
    }

    private fun setupNavigation(setDefaultTabFromSettings: Boolean) {
        currentTabs = if (isLoggedIn) setupMainBottomNav() else setupAnonBottomNav()
        showBottomViewDestinations = currentTabs.asSequence().map {
            it.startDestinationFragmentId
        }.toMutableList().apply {
            add(R.id.postViewFragment)
            add(R.id.favorites_non_top)
            add(R.id.notifications_viewer_non_top)
            add(R.id.profile_non_top)
        }
        if (setDefaultTabFromSettings) {
            setSelectedTab(currentTabs)
        } else {
            binding.bottomNavView.selectedItemId = lastSelectedNavMenuId
        }
        val navigatorProvider = navController.navigatorProvider
        val navigator = navigatorProvider.getNavigator<NavGraphNavigator>("navigation")
        val rootNavGraph = NavGraph(navigator)
        val navInflater = navController.navInflater
        val topLevelDestinations = currentTabs.map { navInflater.inflate(it.navigationResId) }
        rootNavGraph.id = R.id.root_nav_graph
        rootNavGraph.label = "root_nav_graph"
        rootNavGraph.addDestinations(topLevelDestinations)
        rootNavGraph.setStartDestination(if (startNavRootId != 0) startNavRootId else R.id.profile_nav_graph)
        navController.graph = rootNavGraph
        binding.bottomNavView.setupWithNavController(navController)
        appBarConfiguration = AppBarConfiguration(currentTabs.map { it.startDestinationFragmentId }.toSet())
        setupActionBarWithNavController(navController, appBarConfiguration)
        navController.addOnDestinationChangedListener { _: NavController?, destination: NavDestination, arguments: Bundle? ->
            if (destination.id == R.id.directMessagesThreadFragment && arguments != null) {
                // Set the thread title earlier for better ux
                val title = arguments.getString("title")
                if (!title.isNullOrBlank()) {
                    supportActionBar?.title = title
                }
            }
            if (destination.id == R.id.profileFragment && arguments != null) {
                // Set the title to username
                val username = arguments.getString("username")
                if (!username.isNullOrBlank()) {
                    supportActionBar?.title = username.substringAfter("@")
                }
            }
            // below is a hack to check if we are at the end of the current stack, to setup the search view
            binding.appBarLayout.setExpanded(true, true)
            val destinationId = destination.id
            val backStack = navController.backQueue
            setupMenu(backStack.size, destinationId)
            val contains = showBottomViewDestinations.contains(destinationId)
            binding.root.post {
                binding.bottomNavView.visibility = if (contains) View.VISIBLE else View.GONE
                // if (contains) {
                //     behavior?.slideUp(binding.bottomNavView)
                // }
            }
            // explicitly hide keyboard when we navigate
            val view = currentFocus
            Utils.hideKeyboard(view)
        }
        setupReselection()
    }

    private fun setupReselection() {
        binding.bottomNavView.setOnItemReselectedListener {
            val navHostFragment = (supportFragmentManager.primaryNavigationFragment ?: return@setOnItemReselectedListener) as NavHostFragment
            val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull() ?: return@setOnItemReselectedListener
            if (currentFragment is FeedFragment) {
                currentFragment.scrollToTop()
                return@setOnItemReselectedListener
            }
            val currentDestination = navController.currentDestination ?: return@setOnItemReselectedListener
            val currentTabStartDestId = (navController.getBackStackEntry(it.itemId).destination as NavGraph).startDestinationId
            if (currentDestination.id == currentTabStartDestId) return@setOnItemReselectedListener
            navController.popBackStack(currentTabStartDestId, false)
        }
    }

    private fun setSelectedTab(tabs: List<Tab>) {
        val defaultTabResNameString = Utils.settingsHelper.getString(Constants.DEFAULT_TAB)
        try {
            var navId = 0
            if (defaultTabResNameString.isNotBlank()) {
                navId = resources.getIdentifier(defaultTabResNameString, "id", packageName)
            }
            val startFragmentNavResId = if (navId <= 0) R.id.profile_nav_graph else navId
            val tab = tabs.firstOrNull { it.navigationRootId == startFragmentNavResId }
            // if (index < 0 || index >= tabs.size) index = 0
            val firstTab = tab ?: tabs[0]
            startNavRootId = firstTab.navigationRootId
            binding.bottomNavView.selectedItemId = firstTab.navigationRootId
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing id", e)
        }
    }

    private fun setupAnonBottomNav(): List<Tab> {
        val selectedItemId = binding.bottomNavView.selectedItemId
        val anonNavTabs = getAnonNavTabs(this)
        val menu = binding.bottomNavView.menu
        menu.clear()
        for (tab in anonNavTabs) {
            menu.add(0, tab.navigationRootId, 0, tab.title).setIcon(tab.iconResId)
        }
        if (selectedItemId != R.id.profile_nav_graph && selectedItemId != R.id.more_nav_graph && selectedItemId != R.id.favorites_nav_graph) {
            binding.bottomNavView.selectedItemId = R.id.profile_nav_graph
        }
        return anonNavTabs
    }

    private fun setupMainBottomNav(): List<Tab> {
        val menu = binding.bottomNavView.menu
        menu.clear()
        val navTabList = getLoggedInNavTabs(this).first
        for (tab in navTabList) {
            menu.add(0, tab.navigationRootId, 0, tab.title).setIcon(tab.iconResId)
        }
        return navTabList
    }

    private fun setupMenu(backStackSize: Int, destinationId: Int) {
        val searchMenuItem = searchMenuItem ?: return
        if (backStackSize >= 2 && SEARCH_VISIBLE_DESTINATIONS.contains(destinationId)) {
            searchMenuItem.isVisible = true
            return
        }
        searchMenuItem.isVisible = false
    }

    private fun setScrollingBehaviour() {
        val layoutParams = binding.mainNavHost.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.behavior = ScrollingViewBehavior()
        binding.mainNavHost.requestLayout()
    }

    private fun removeScrollingBehaviour() {
        val layoutParams = binding.mainNavHost.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.behavior = null
        binding.mainNavHost.requestLayout()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type
        // Log.d(TAG, action + " " + type);
        if (Intent.ACTION_MAIN == action) return
        if (Constants.ACTION_SHOW_ACTIVITY == action) {
            showActivityView()
            return
        }
        if (Constants.ACTION_SHOW_DM_THREAD == action) {
            showThread(intent)
            return
        }
        if (Intent.ACTION_SEND == action && type != null) {
            if (type == "text/plain") {
                handleUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
            return
        }
        if (Intent.ACTION_VIEW == action) {
            val data = intent.data ?: return
            handleUrl(data.toString())
        }
    }

    private fun showThread(intent: Intent) {
        val threadId = intent.getStringExtra(Constants.DM_THREAD_ACTION_EXTRA_THREAD_ID)
        val threadTitle = intent.getStringExtra(Constants.DM_THREAD_ACTION_EXTRA_THREAD_TITLE)
        navigateToThread(threadId, threadTitle)
    }

    fun navigateToThread(threadId: String?, threadTitle: String?) {
        if (threadId == null || threadTitle == null) return
        try {
            navController.navigate(getDirectThreadDeepLink(threadId, threadTitle))
        } catch (e: Exception) {
            Log.e(TAG, "navigateToThread: ", e)
        }
    }

    private fun handleUrl(url: String?) {
        if (url == null) return
        // Log.d(TAG, url);
        val intentModel = IntentUtils.parseUrl(url) ?: return
        showView(intentModel)
    }

    private fun showView(intentModel: IntentModel) {
        when (intentModel.type) {
            IntentModelType.USERNAME -> showProfileView(intentModel)
            IntentModelType.POST -> showPostView(intentModel)
            IntentModelType.LOCATION -> showLocationView(intentModel)
            IntentModelType.HASHTAG -> showHashtagView(intentModel)
            IntentModelType.UNKNOWN -> Log.w(TAG, "Unknown model type received!")
            // else -> Log.w(TAG, "Unknown model type received!")
        }
    }

    private fun showProfileView(intentModel: IntentModel) {
        try {
            val username = intentModel.text
            navController.navigate(getProfileDeepLink(username))
        } catch (e: Exception) {
            Log.e(TAG, "showProfileView: ", e)
        }
    }

    private fun showPostView(intentModel: IntentModel) {
        val shortCode = intentModel.text
        // Log.d(TAG, "shortCode: " + shortCode);
        try {
            navController.navigate(getPostDeepLink(shortCode))
        } catch (e: Exception) {
            Log.e(TAG, "showPostView: ", e)
        }
    }

    private fun showLocationView(intentModel: IntentModel) {
        val locationId = intentModel.text
        // Log.d(TAG, "locationId: " + locationId);
        try {
            navController.navigate(getLocationDeepLink(locationId))
        } catch (e: Exception) {
            Log.e(TAG, "showLocationView: ", e)
        }
    }

    private fun showHashtagView(intentModel: IntentModel) {
        val hashtag = intentModel.text
        // Log.d(TAG, "hashtag: " + hashtag);
        try {
            navController.navigate(getHashtagDeepLink(hashtag))
        } catch (e: Exception) {
            Log.e(TAG, "showHashtagView: ", e)
        }
    }

    private fun showActivityView() {
        try {
            navController.navigate(getNotificationsDeepLink("notif"))
        } catch (e: Exception) {
            Log.e(TAG, "showActivityView: ", e)
        }
    }

    private fun bindActivityCheckerService() {
        bindService(Intent(this, ActivityCheckerService::class.java), serviceConnection, BIND_AUTO_CREATE)
        isActivityCheckerServiceBound = true
    }

    private fun unbindActivityCheckerService() {
        if (!isActivityCheckerServiceBound) return
        unbindService(serviceConnection)
        isActivityCheckerServiceBound = false
    }

    val bottomNavView: BottomNavigationView
        get() = binding.bottomNavView

    // fun setCollapsingView(view: View) {
    //     try {
    //         binding.collapsingToolbarLayout.addView(view, 0)
    //     } catch (e: Exception) {
    //         Log.e(TAG, "setCollapsingView: ", e)
    //     }
    // }
    //
    // fun removeCollapsingView(view: View) {
    //     try {
    //         binding.collapsingToolbarLayout.removeView(view)
    //     } catch (e: Exception) {
    //         Log.e(TAG, "removeCollapsingView: ", e)
    //     }
    // }

    @Synchronized
    fun resetToolbar(owner: Fragment) {
        if (owner != toolbarOwner) return
        binding.appBarLayout.visibility = View.VISIBLE
        setScrollingBehaviour()
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        toolbarOwner = null
    }

    val collapsingToolbarView: CollapsingToolbarLayout
        get() = binding.collapsingToolbarLayout
    val appbarLayout: AppBarLayout
        get() = binding.appBarLayout

    fun removeLayoutTransition() {
        binding.root.layoutTransition = null
    }

    fun setLayoutTransition() {
        binding.root.layoutTransition = LayoutTransition()
    }

    private fun initEmojiCompat() {
        // Use a downloadable font for EmojiCompat
        val fontRequest = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            R.array.com_google_android_gms_fonts_certs
        )
        val config: EmojiCompat.Config = FontRequestEmojiCompatConfig(applicationContext, fontRequest)
        config.setReplaceAll(true) // .setUseEmojiAsDefaultStyle(true)
            .registerInitCallback(object : InitCallback() {
                override fun onInitialized() {
                    Log.i(TAG, "EmojiCompat initialized")
                }

                override fun onFailed(throwable: Throwable?) {
                    Log.e(TAG, "EmojiCompat initialization failed", throwable)
                }
            })
        EmojiCompat.init(config)
    }

    val rootView: View
        get() = binding.root

    @Synchronized
    fun setToolbar(toolbar: Toolbar, owner: Fragment) {
        toolbarOwner = owner
        binding.appBarLayout.visibility = View.GONE
        removeScrollingBehaviour()
        setSupportActionBar(toolbar)
        this.toolbar = toolbar
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)
    }

    private fun setNavBarDMUnreadCountBadge(unseenCount: Int) {
        val badge = binding.bottomNavView.getOrCreateBadge(R.id.direct_messages_nav_graph)
        if (unseenCount == 0) {
            badge.isVisible = false
            badge.clearNumber()
            return
        }
        if (badge.verticalOffset != 10) {
            badge.verticalOffset = 10
        }
        badge.number = unseenCount
        badge.isVisible = true
    }

    fun showSearchView(): TextInputLayout {
        binding.searchInputLayout.visibility = View.VISIBLE
        return binding.searchInputLayout
    }

    fun hideSearchView() {
        binding.searchInputLayout.visibility = View.GONE
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val LAST_SELECT_NAV_MENU_ID = "lastSelectedNavMenuId"

        private val SEARCH_VISIBLE_DESTINATIONS: List<Int> = ImmutableList.of(
            R.id.feedFragment,
            R.id.profileFragment,
            R.id.directMessagesInboxFragment,
            R.id.discoverFragment,
            R.id.favoritesFragment,
            R.id.hashTagFragment,
            R.id.locationFragment
        )

        @JvmStatic
        var instance: MainActivity? = null
            private set
    }
}