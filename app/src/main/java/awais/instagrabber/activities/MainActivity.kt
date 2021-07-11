package awais.instagrabber.activities

import android.animation.LayoutTransition
import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
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
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination
import androidx.navigation.ui.NavigationUI
import awais.instagrabber.BuildConfig
import awais.instagrabber.R
import awais.instagrabber.customviews.emoji.EmojiVariantManager
import awais.instagrabber.customviews.helpers.RootViewDeferringInsetsCallback
import awais.instagrabber.customviews.helpers.TextWatcherAdapter
import awais.instagrabber.databinding.ActivityMainBinding
import awais.instagrabber.fragments.PostViewV2Fragment
import awais.instagrabber.fragments.directmessages.DirectMessageInboxFragmentDirections
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
import awais.instagrabber.utils.TextUtils.shortcodeToId
import awais.instagrabber.utils.emoji.EmojiParser
import awais.instagrabber.viewmodels.AppStateViewModel
import awais.instagrabber.viewmodels.DirectInboxViewModel
import awais.instagrabber.webservices.GraphQLRepository
import awais.instagrabber.webservices.MediaRepository
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.stream.Collectors


class MainActivity : BaseLanguageActivity(), FragmentManager.OnBackStackChangedListener {
    private lateinit var binding: ActivityMainBinding

    private var currentNavControllerLiveData: LiveData<NavController>? = null
    private var searchMenuItem: MenuItem? = null
    private var firstFragmentGraphIndex = 0
    private var lastSelectedNavMenuId = 0
    private var isActivityCheckerServiceBound = false
    private var isBackStackEmpty = false
    private var isLoggedIn = false
    private var deviceUuid: String? = null
    private var csrfToken: String? = null
    private var userId: Long = 0

    // private var behavior: HideBottomViewOnScrollBehavior<BottomNavigationView>? = null
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
    private val mediaRepository: MediaRepository by lazy { MediaRepository.getInstance() }
    private val graphQLRepository: GraphQLRepository by lazy { GraphQLRepository.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            DownloadUtils.init(
                this,
                Utils.settingsHelper.getString(PreferenceKeys.PREF_BARINSTA_DIR_URI)
            )
        } catch (e: ReselectDocumentTreeException) {
            super.onCreate(savedInstanceState)
            val intent = Intent(this, DirectorySelectActivity::class.java)
            intent.putExtra(EXTRA_INITIAL_URI, e.initialUri)
            startActivity(intent)
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        instance = this
        binding = ActivityMainBinding.inflate(layoutInflater)
        setupCookie()
        if (Utils.settingsHelper.getBoolean(PreferenceKeys.FLAG_SECURE)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupInsetsCallback()
        createNotificationChannels()
        // try {
        //     val layoutParams = binding.bottomNavView.layoutParams as CoordinatorLayout.LayoutParams
        //     @Suppress("UNCHECKED_CAST")
        //     behavior = layoutParams.behavior as HideBottomViewOnScrollBehavior<BottomNavigationView>
        // } catch (e: Exception) {
        //     Log.e(TAG, "onCreate: ", e)
        // }
        if (savedInstanceState == null) {
            setupBottomNavigationBar(true)
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
        supportFragmentManager.addOnBackStackChangedListener(this)
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
        val navController = currentNavControllerLiveData?.value
        if (navController != null) {
            val currentDestination = navController.currentDestination
            if (currentDestination != null) {
                @SuppressLint("RestrictedApi") val backStack = navController.backStack
                setupMenu(backStack.size, currentDestination.id)
            }
        }
        // if (binding.searchInputLayout.getVisibility() == View.VISIBLE) {
        //     searchMenuItem.setVisible(false).setEnabled(false);
        //     return true;
        // }
        // searchMenuItem.setVisible(true).setEnabled(true);
        // if (showSearch && currentNavControllerLiveData != null) {
        //     final NavController navController = currentNavControllerLiveData.getValue();
        //     if (navController != null) {
        //         final NavDestination currentDestination = navController.getCurrentDestination();
        //         if (currentDestination != null) {
        //             final int destinationId = currentDestination.getId();
        //             showSearch = destinationId == R.id.profileFragment;
        //         }
        //     }
        // }
        // if (!showSearch) {
        //     searchMenuItem.setVisible(false);
        //     return true;
        // }
        // return setupSearchView();
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.search) {
            val navController = currentNavControllerLiveData?.value ?: return false
            try {
                navController.navigate(R.id.action_global_search)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "onOptionsItemSelected: ", e)
            }
            return false
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(FIRST_FRAGMENT_GRAPH_INDEX_KEY, firstFragmentGraphIndex.toString())
        outState.putString(LAST_SELECT_NAV_MENU_ID, binding.bottomNavView.selectedItemId.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val key = savedInstanceState[FIRST_FRAGMENT_GRAPH_INDEX_KEY] as String?
        if (key != null) {
            try {
                firstFragmentGraphIndex = key.toInt()
            } catch (ignored: NumberFormatException) {
            }
        }
        val lastSelected = savedInstanceState[LAST_SELECT_NAV_MENU_ID] as String?
        if (lastSelected != null) {
            try {
                lastSelectedNavMenuId = lastSelected.toInt()
            } catch (ignored: NumberFormatException) {
            }
        }
        setupBottomNavigationBar(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (currentNavControllerLiveData == null) return false
        val navController = currentNavControllerLiveData?.value ?: return false
        return navController.navigateUp()
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

    override fun onBackPressed() {
        var currentNavControllerBackStack = 2
        currentNavControllerLiveData?.let {
            val navController = it.value
            if (navController != null) {
                @SuppressLint("RestrictedApi") val backStack = navController.backStack
                currentNavControllerBackStack = backStack.size
            }
        }
        if (isTaskRoot && isBackStackEmpty && currentNavControllerBackStack == 2) {
            finishAfterTransition()
            return
        }
        if (!isFinishing) {
            try {
                super.onBackPressed()
            } catch (e: Exception) {
                Log.e(TAG, "onBackPressed: ", e)
                finish()
            }
        }
    }

    override fun onBackStackChanged() {
        val backStackEntryCount = supportFragmentManager.backStackEntryCount
        isBackStackEmpty = backStackEntryCount == 0
    }

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

    private fun setupBottomNavigationBar(setDefaultTabFromSettings: Boolean) {
        currentTabs = if (!isLoggedIn) setupAnonBottomNav() else setupMainBottomNav()
        val mainNavList = currentTabs.stream()
            .map(Tab::navigationResId)
            .collect(Collectors.toList())
        showBottomViewDestinations = currentTabs.asSequence().map {
            it.startDestinationFragmentId
        }.toMutableList().apply {
            add(R.id.postViewFragment)
            add(R.id.favoritesFragment)
        }
        if (setDefaultTabFromSettings) {
            setSelectedTab(currentTabs)
        } else {
            binding.bottomNavView.selectedItemId = lastSelectedNavMenuId
        }
        val navControllerLiveData = NavigationExtensions.setupWithNavController(
            binding.bottomNavView,
            mainNavList,
            supportFragmentManager,
            R.id.main_nav_host,
            intent,
            firstFragmentGraphIndex
        )
        navControllerLiveData.observe(this, { navController: NavController? -> setupNavigation(binding.toolbar, navController) })
        currentNavControllerLiveData = navControllerLiveData
    }

    private fun setSelectedTab(tabs: List<Tab>) {
        val defaultTabResNameString = Utils.settingsHelper.getString(Constants.DEFAULT_TAB)
        try {
            var navId = 0
            if (!isEmpty(defaultTabResNameString)) {
                navId = resources.getIdentifier(defaultTabResNameString, "navigation", packageName)
            }
            val navGraph = if (isLoggedIn) R.navigation.feed_nav_graph else R.navigation.profile_nav_graph
            val defaultNavId = if (navId <= 0) navGraph else navId
            var index = Iterators.indexOf(tabs.iterator()) { tab: Tab? ->
                if (tab == null) return@indexOf false
                tab.navigationResId == defaultNavId
            }
            if (index < 0 || index >= tabs.size) index = 0
            firstFragmentGraphIndex = index
            setBottomNavSelectedTab(tabs[index])
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing id", e)
        }
    }

    private fun setupAnonBottomNav(): List<Tab> {
        val selectedItemId = binding.bottomNavView.selectedItemId
        val favoriteTab = Tab(
            R.drawable.ic_star_24,
            getString(R.string.title_favorites),
            false,
            "favorites_nav_graph",
            R.navigation.favorites_nav_graph,
            R.id.favorites_nav_graph,
            R.id.favoritesFragment
        )
        val profileTab = Tab(
            R.drawable.ic_person_24,
            getString(R.string.profile),
            false,
            "profile_nav_graph",
            R.navigation.profile_nav_graph,
            R.id.profile_nav_graph,
            R.id.profileFragment
        )
        val moreTab = Tab(
            R.drawable.ic_more_horiz_24,
            getString(R.string.more),
            false,
            "more_nav_graph",
            R.navigation.more_nav_graph,
            R.id.more_nav_graph,
            R.id.morePreferencesFragment
        )
        val menu = binding.bottomNavView.menu
        menu.clear()
        menu.add(0, favoriteTab.navigationRootId, 0, favoriteTab.title).setIcon(favoriteTab.iconResId)
        menu.add(0, profileTab.navigationRootId, 0, profileTab.title).setIcon(profileTab.iconResId)
        menu.add(0, moreTab.navigationRootId, 0, moreTab.title).setIcon(moreTab.iconResId)
        if (selectedItemId != R.id.profile_nav_graph && selectedItemId != R.id.more_nav_graph && selectedItemId != R.id.favorites_nav_graph) {
            setBottomNavSelectedTab(profileTab)
        }
        return ImmutableList.of(favoriteTab, profileTab, moreTab)
    }

    private fun setupMainBottomNav(): List<Tab> {
        val menu = binding.bottomNavView.menu
        menu.clear()
        val navTabList = Utils.getNavTabList(this).first
        for ((iconResId, title, _, _, _, navigationRootId) in navTabList) {
            menu.add(0, navigationRootId, 0, title).setIcon(iconResId)
        }
        return navTabList
    }

    private fun setBottomNavSelectedTab(tab: Tab) {
        binding.bottomNavView.selectedItemId = tab.navigationRootId
    }

    private fun setBottomNavSelectedTab(@IdRes navGraphRootId: Int) {
        binding.bottomNavView.selectedItemId = navGraphRootId
    }

    private fun setupNavigation(toolbar: Toolbar, navController: NavController?) {
        if (navController == null) return
        NavigationUI.setupWithNavController(toolbar, navController)
        navController.addOnDestinationChangedListener(OnDestinationChangedListener { _: NavController?, destination: NavDestination, arguments: Bundle? ->
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
            @SuppressLint("RestrictedApi") val backStack = navController.backStack
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
        })
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
        currentNavControllerLiveData?.observe(this, object : Observer<NavController?> {
            override fun onChanged(navController: NavController?) {
                if (navController == null) return
                if (navController.graph.id != R.id.direct_messages_nav_graph) return
                try {
                    val currentDestination = navController.currentDestination
                    if (currentDestination != null && currentDestination.id == R.id.directMessagesInboxFragment) {
                        // if we are already on the inbox page, navigate to the thread
                        // need handler.post() to wait for the fragment manager to be ready to navigate
                        Handler(Looper.getMainLooper()).post {
                            val action = DirectMessageInboxFragmentDirections
                                .actionInboxToThread(threadId, threadTitle)
                            navController.navigate(action)
                        }
                        return
                    }
                    // add a destination change listener to navigate to thread once we are on the inbox page
                    navController.addOnDestinationChangedListener(object : OnDestinationChangedListener {
                        override fun onDestinationChanged(
                            controller: NavController,
                            destination: NavDestination,
                            arguments: Bundle?,
                        ) {
                            if (destination.id == R.id.directMessagesInboxFragment) {
                                val action = DirectMessageInboxFragmentDirections
                                    .actionInboxToThread(threadId, threadTitle)
                                controller.navigate(action)
                                controller.removeOnDestinationChangedListener(this)
                            }
                        }
                    })
                    // pop back stack until we reach the inbox page
                    navController.popBackStack(R.id.directMessagesInboxFragment, false)
                } finally {
                    currentNavControllerLiveData?.removeObserver(this)
                }
            }
        })
        val selectedItemId = binding.bottomNavView.selectedItemId
        if (selectedItemId != R.navigation.direct_messages_nav_graph) {
            setBottomNavSelectedTab(R.id.direct_messages_nav_graph)
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
        val username = intentModel.text
        // Log.d(TAG, "username: " + username);
        val currentNavControllerLiveData = currentNavControllerLiveData ?: return
        val navController = currentNavControllerLiveData.value
        val bundle = Bundle()
        bundle.putString("username", "@$username")
        try {
            navController?.navigate(R.id.action_global_profileFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "showProfileView: ", e)
        }
    }

    private fun showPostView(intentModel: IntentModel) {
        val shortCode = intentModel.text
        // Log.d(TAG, "shortCode: " + shortCode);
        val alertDialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(R.layout.dialog_opening_post)
            .create()
        alertDialog.show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val media = if (isLoggedIn) mediaRepository.fetch(shortcodeToId(shortCode)) else graphQLRepository.fetchPost(shortCode)
                withContext(Dispatchers.Main) {
                    if (media == null) {
                        Toast.makeText(applicationContext, R.string.post_not_found, Toast.LENGTH_SHORT).show()
                        return@withContext
                    }
                    val currentNavControllerLiveData = currentNavControllerLiveData ?: return@withContext
                    val navController = currentNavControllerLiveData.value
                    val bundle = Bundle()
                    bundle.putSerializable(PostViewV2Fragment.ARG_MEDIA, media)
                    try {
                        navController?.navigate(R.id.action_global_post_view, bundle)
                    } catch (e: Exception) {
                        Log.e(TAG, "showPostView: ", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "showPostView: ", e)
            } finally {
                withContext(Dispatchers.Main) {
                    alertDialog.dismiss()
                }
            }
        }
    }

    private fun showLocationView(intentModel: IntentModel) {
        val locationId = intentModel.text
        // Log.d(TAG, "locationId: " + locationId);
        val currentNavControllerLiveData = currentNavControllerLiveData ?: return
        val navController = currentNavControllerLiveData.value
        val bundle = Bundle()
        bundle.putLong("locationId", locationId.toLong())
        navController?.navigate(R.id.action_global_locationFragment, bundle)
    }

    private fun showHashtagView(intentModel: IntentModel) {
        val hashtag = intentModel.text
        // Log.d(TAG, "hashtag: " + hashtag);
        val currentNavControllerLiveData = currentNavControllerLiveData ?: return
        val navController = currentNavControllerLiveData.value
        val bundle = Bundle()
        bundle.putString("hashtag", hashtag)
        navController?.navigate(R.id.action_global_hashTagFragment, bundle)
    }

    private fun showActivityView() {
        val currentNavControllerLiveData = currentNavControllerLiveData ?: return
        val navController = currentNavControllerLiveData.value
        val bundle = Bundle()
        bundle.putString("type", "notif")
        navController?.navigate(R.id.action_global_notificationsViewerFragment, bundle)
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

    fun setCollapsingView(view: View) {
        try {
            binding.collapsingToolbarLayout.addView(view, 0)
        } catch (e: Exception) {
            Log.e(TAG, "setCollapsingView: ", e)
        }
    }

    fun removeCollapsingView(view: View) {
        try {
            binding.collapsingToolbarLayout.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "removeCollapsingView: ", e)
        }
    }

    fun resetToolbar() {
        binding.appBarLayout.visibility = View.VISIBLE
        setScrollingBehaviour()
        setSupportActionBar(binding.toolbar)
        val currentNavControllerLiveData = currentNavControllerLiveData ?: return
        setupNavigation(binding.toolbar, currentNavControllerLiveData.value)
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

    var toolbar: Toolbar
        get() = binding.toolbar
        set(toolbar) {
            binding.appBarLayout.visibility = View.GONE
            removeScrollingBehaviour()
            setSupportActionBar(toolbar)
            if (currentNavControllerLiveData == null) return
            setupNavigation(toolbar, currentNavControllerLiveData?.value)
        }
    val rootView: View
        get() = binding.root

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
        private const val FIRST_FRAGMENT_GRAPH_INDEX_KEY = "firstFragmentGraphIndex"
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