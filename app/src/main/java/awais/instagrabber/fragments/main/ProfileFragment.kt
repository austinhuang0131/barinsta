package awais.instagrabber.fragments.main

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import awais.instagrabber.R
import awais.instagrabber.activities.MainActivity
import awais.instagrabber.adapters.FeedAdapterV2
import awais.instagrabber.adapters.HighlightsAdapter
import awais.instagrabber.asyncs.ProfilePostFetchService
import awais.instagrabber.customviews.PrimaryActionModeCallback
import awais.instagrabber.customviews.RamboTextViewV2
import awais.instagrabber.customviews.RamboTextViewV2.*
import awais.instagrabber.databinding.FragmentProfileBinding
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.dialogs.ConfirmDialogFragment
import awais.instagrabber.dialogs.ConfirmDialogFragment.ConfirmDialogFragmentCallback
import awais.instagrabber.dialogs.MultiOptionDialogFragment
import awais.instagrabber.dialogs.MultiOptionDialogFragment.MultiOptionDialogSingleCallback
import awais.instagrabber.dialogs.MultiOptionDialogFragment.Option
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment
import awais.instagrabber.dialogs.ProfilePicDialogFragment
import awais.instagrabber.fragments.HashTagFragment.ARG_HASHTAG
import awais.instagrabber.fragments.PostViewV2Fragment
import awais.instagrabber.fragments.UserSearchFragment
import awais.instagrabber.fragments.UserSearchFragmentDirections
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.Resource
import awais.instagrabber.models.enums.PostItemType
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.UserProfileContextLink
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.*
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.utils.extensions.isReallyPrivate
import awais.instagrabber.utils.extensions.trimAll
import awais.instagrabber.viewmodels.AppStateViewModel
import awais.instagrabber.viewmodels.ProfileFragmentViewModel
import awais.instagrabber.viewmodels.ProfileFragmentViewModel.ProfileEvent.*
import awais.instagrabber.viewmodels.ProfileFragmentViewModelFactory
import awais.instagrabber.webservices.*

class ProfileFragment : Fragment(), OnRefreshListener, ConfirmDialogFragmentCallback, MultiOptionDialogSingleCallback<String> {
    private var backStackSavedStateResultLiveData: MutableLiveData<Any?>? = null
    private var shareDmMenuItem: MenuItem? = null
    private var shareLinkMenuItem: MenuItem? = null
    private var removeFollowerMenuItem: MenuItem? = null
    private var chainingMenuItem: MenuItem? = null
    private var mutePostsMenuItem: MenuItem? = null
    private var muteStoriesMenuItem: MenuItem? = null
    private var restrictMenuItem: MenuItem? = null
    private var blockMenuItem: MenuItem? = null
    private var setupPostsDone: Boolean = false
    private var selectedMedia: List<Media>? = null
    private var actionMode: ActionMode? = null
    private var disableDm: Boolean = false
    private var shouldRefresh: Boolean = true
    private var highlightsAdapter: HighlightsAdapter? = null
    private var layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_PROFILE_POSTS_LAYOUT)

    private lateinit var mainActivity: MainActivity
    private lateinit var root: MotionLayout
    private lateinit var binding: FragmentProfileBinding
    private lateinit var appStateViewModel: AppStateViewModel
    private lateinit var viewModel: ProfileFragmentViewModel

    private val confirmDialogFragmentRequestCode = 100
    private val ppOptsDialogRequestCode = 101
    private val bioDialogRequestCode = 102
    private val translationDialogRequestCode = 103
    private val feedItemCallback: FeedAdapterV2.FeedItemCallback = object : FeedAdapterV2.FeedItemCallback {
        override fun onPostClick(media: Media?, profilePicView: View?, mainPostImage: View?) {
            openPostDialog(media ?: return, -1)
        }

        override fun onProfilePicClick(media: Media?, profilePicView: View?) {
            navigateToProfile(media?.user?.username)
        }

        override fun onNameClick(media: Media?, profilePicView: View?) {
            navigateToProfile(media?.user?.username)
        }

        override fun onLocationClick(media: Media?) {
            val action = FeedFragmentDirections.actionGlobalLocationFragment(media?.location?.pk ?: return)
            NavHostFragment.findNavController(this@ProfileFragment).navigate(action)
        }

        override fun onMentionClick(mention: String?) {
            navigateToProfile(mention?.trimAll() ?: return)
        }

        override fun onHashtagClick(hashtag: String?) {
            val action = FeedFragmentDirections.actionGlobalHashTagFragment(hashtag ?: return)
            NavHostFragment.findNavController(this@ProfileFragment).navigate(action)
        }

        override fun onCommentsClick(media: Media?) {
            val commentsAction = ProfileFragmentDirections.actionGlobalCommentsViewerFragment(
                media?.code ?: return,
                media.pk ?: return,
                media.user?.pk ?: return
            )
            NavHostFragment.findNavController(this@ProfileFragment).navigate(commentsAction)
        }

        override fun onDownloadClick(media: Media?, childPosition: Int, popupLocation: View) {
            DownloadUtils.showDownloadDialog(context ?: return, media ?: return, childPosition, popupLocation)
        }

        override fun onEmailClick(emailId: String?) {
            Utils.openEmailAddress(context ?: return, emailId ?: return)
        }

        override fun onURLClick(url: String?) {
            Utils.openURL(context ?: return, url ?: return)
        }

        override fun onSliderClick(media: Media?, position: Int) {
            openPostDialog(media ?: return, position)
        }
    }
    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.postsRecyclerView.endSelection()
        }
    }
    private val multiSelectAction = PrimaryActionModeCallback(
        R.menu.multi_select_download_menu,
        object : PrimaryActionModeCallback.CallbacksHelper() {
            override fun onDestroy(mode: ActionMode?) {
                binding.postsRecyclerView.endSelection()
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                val item1 = item ?: return false
                if (item1.itemId == R.id.action_download) {
                    val selectedMedia = this@ProfileFragment.selectedMedia ?: return false
                    val context = context ?: return false
                    DownloadUtils.download(context, selectedMedia)
                    binding.postsRecyclerView.endSelection()
                    return true
                }
                return false
            }
        }
    )
    private val selectionModeCallback = object : FeedAdapterV2.SelectionModeCallback {
        override fun onSelectionStart() {
            if (!onBackPressedCallback.isEnabled) {
                onBackPressedCallback.isEnabled = true
                mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
            }
            if (actionMode == null) {
                actionMode = mainActivity.startActionMode(multiSelectAction)
            }
        }

        override fun onSelectionChange(mediaSet: Set<Media>?) {
            if (mediaSet == null) {
                selectedMedia = null
                return
            }
            val title = getString(R.string.number_selected, mediaSet.size)
            actionMode?.title = title
            selectedMedia = mediaSet.toList()
        }

        override fun onSelectionEnd() {
            if (onBackPressedCallback.isEnabled) {
                onBackPressedCallback.isEnabled = false
                onBackPressedCallback.remove()
            }
            (actionMode ?: return).finish()
            actionMode = null
        }
    }
    private val onProfilePicClickListener = View.OnClickListener {
        val hasStories = viewModel.userStories.value?.data != null
        if (!hasStories) {
            showProfilePicDialog()
            return@OnClickListener
        }
        val dialog = MultiOptionDialogFragment.newInstance(
            ppOptsDialogRequestCode,
            0,
            arrayListOf(
                Option(getString(R.string.view_pfp), "profile_pic"),
                Option(getString(R.string.show_stories), "show_stories")
            )
        )
        dialog.show(childFragmentManager, MultiOptionDialogFragment::class.java.simpleName)
    }
    private val onFollowersClickListener = View.OnClickListener {
        try {
            val action = ProfileFragmentDirections.actionProfileFragmentToFollowViewerFragment(
                viewModel.profile.value?.data?.pk ?: return@OnClickListener,
                true,
                viewModel.profile.value?.data?.username ?: return@OnClickListener
            )
            NavHostFragment.findNavController(this).navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "onFollowersClickListener: ", e)
        }
    }
    private val onFollowingClickListener = View.OnClickListener {
        try {
            val action = ProfileFragmentDirections.actionProfileFragmentToFollowViewerFragment(
                viewModel.profile.value?.data?.pk ?: return@OnClickListener,
                false,
                viewModel.profile.value?.data?.username ?: return@OnClickListener
            )
            NavHostFragment.findNavController(this).navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "onFollowersClickListener: ", e)
        }
    }
    private val onEmailClickListener = OnEmailClickListener {
        Utils.openEmailAddress(context ?: return@OnEmailClickListener, it.originalText.trimAll())
    }
    private val onHashtagClickListener = OnHashtagClickListener {
        try {
            val bundle = Bundle()
            bundle.putString(ARG_HASHTAG, it.originalText.trimAll())
            NavHostFragment.findNavController(this).navigate(R.id.action_global_hashTagFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "onHashtagClickListener: ", e)
        }
    }
    private val onMentionClickListener = OnMentionClickListener {
        navigateToProfile(it.originalText.trimAll())
    }
    private val onURLClickListener = OnURLClickListener {
        Utils.openURL(context ?: return@OnURLClickListener, it.originalText.trimAll())
    }

    @Suppress("UNCHECKED_CAST")
    private val backStackSavedStateObserver = Observer<Any?> { result ->
        if (result == null) return@Observer
        if ((result is RankedRecipient)) {
            if (context != null) {
                Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show()
            }
            viewModel.shareDm(result)
        } else if ((result is Set<*>)) {
            try {
                if (context != null) {
                    Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show()
                }
                viewModel.shareDm(result as Set<RankedRecipient>)
            } catch (e: Exception) {
                Log.e(TAG, "share: ", e)
            }
        }
        // clear result
        backStackSavedStateResultLiveData?.postValue(null)
    }

    private fun openPostDialog(media: Media, position: Int) {
        val bundle = Bundle().apply {
            putSerializable(PostViewV2Fragment.ARG_MEDIA, media)
            putInt(PostViewV2Fragment.ARG_SLIDER_POSITION, position)
        }
        try {
            val navController = NavHostFragment.findNavController(this)
            navController.navigate(R.id.action_global_post_view, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "openPostDialog: ", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        appStateViewModel = ViewModelProvider(mainActivity).get(AppStateViewModel::class.java)
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        val deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        val csrfToken = getCsrfTokenFromCookie(cookie)
        val userId = getUserIdFromCookie(cookie)
        val isLoggedIn = !csrfToken.isNullOrBlank() && userId != 0L && deviceUuid.isNotBlank()
        viewModel = ViewModelProvider(
            this,
            ProfileFragmentViewModelFactory(
                csrfToken,
                deviceUuid,
                UserRepository.getInstance(),
                FriendshipRepository.getInstance(),
                StoriesRepository.getInstance(),
                MediaRepository.getInstance(),
                GraphQLRepository.getInstance(),
                FavoriteRepository.getInstance(requireContext()),
                DirectMessagesRepository.getInstance(),
                if (isLoggedIn) DirectMessagesManager else null,
                this,
                arguments
            )
        ).get(ProfileFragmentViewModel::class.java)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (this::root.isInitialized) {
            shouldRefresh = false
            return root
        }
        appStateViewModel.currentUserLiveData.observe(viewLifecycleOwner, viewModel::setCurrentUser)
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldRefresh) {
            setupObservers()
            return
        }
        init()
        shouldRefresh = false
    }

    override fun onRefresh() {
        viewModel.refresh()
        binding.postsRecyclerView.refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)
        blockMenuItem = menu.findItem(R.id.block)
        restrictMenuItem = menu.findItem(R.id.restrict)
        muteStoriesMenuItem = menu.findItem(R.id.mute_stories)
        mutePostsMenuItem = menu.findItem(R.id.mute_posts)
        chainingMenuItem = menu.findItem(R.id.chaining)
        removeFollowerMenuItem = menu.findItem(R.id.remove_follower)
        shareLinkMenuItem = menu.findItem(R.id.share_link)
        shareDmMenuItem = menu.findItem(R.id.share_dm)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.layout -> showPostsLayoutPreferences()
            R.id.restrict -> viewModel.restrictUser()
            R.id.block -> viewModel.blockUser()
            R.id.chaining -> navigateToChaining()
            R.id.mute_stories -> viewModel.muteStories()
            R.id.mute_posts -> viewModel.mutePosts()
            R.id.remove_follower -> viewModel.removeFollower()
            R.id.share_link -> shareProfileLink()
            R.id.share_dm -> shareProfileViaDm()
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        try {
            val backStackEntry = NavHostFragment.findNavController(this).currentBackStackEntry
            if (backStackEntry != null) {
                backStackSavedStateResultLiveData = backStackEntry.savedStateHandle.getLiveData("result")
                backStackSavedStateResultLiveData?.observe(viewLifecycleOwner, backStackSavedStateObserver)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onResume: ", e)
        }
    }

    private fun shareProfileViaDm() {
        val actionGlobalUserSearch = UserSearchFragmentDirections.actionGlobalUserSearch().apply {
            setTitle(getString(R.string.share))
            setActionLabel(getString(R.string.send))
            showGroups = true
            multiple = true
            setSearchMode(UserSearchFragment.SearchMode.RAVEN)
        }
        try {
            val navController = NavHostFragment.findNavController(this@ProfileFragment)
            navController.navigate(actionGlobalUserSearch)
        } catch (e: Exception) {
            Log.e(TAG, "shareProfileViaDm: ", e)
        }
    }

    private fun shareProfileLink() {
        val profile = viewModel.profile.value?.data ?: return
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, "https://instagram.com/" + profile.username)
        startActivity(Intent.createChooser(sharingIntent, null))
    }

    private fun navigateToChaining() {
        viewModel.currentUser.value?.data ?: return
        val profile = viewModel.profile.value?.data ?: return
        val bundle = Bundle().apply {
            putString("type", "chaining")
            putLong("targetId", profile.pk)
        }
        try {
            NavHostFragment.findNavController(this).navigate(R.id.action_global_notificationsViewerFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "navigateToChaining: ", e)
        }
    }

    private fun init() {
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        disableDm = !Utils.isNavRootInCurrentTabs("direct_messages_nav_graph")
        setupHighlights()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isLoggedIn.observe(viewLifecycleOwner) {} // observe so that `isLoggedIn.value` is correct
        viewModel.currentUserProfileActionLiveData.observe(viewLifecycleOwner) {
            val (currentUserResource, profileResource) = it
            if (currentUserResource.status == Resource.Status.ERROR || profileResource.status == Resource.Status.ERROR) {
                context?.let { ctx -> Toast.makeText(ctx, R.string.error_loading_profile, Toast.LENGTH_LONG).show() }
                return@observe
            }
            if (currentUserResource.status == Resource.Status.LOADING || profileResource.status == Resource.Status.LOADING) {
                binding.swipeRefreshLayout.isRefreshing = true
                return@observe
            }
            binding.swipeRefreshLayout.isRefreshing = false
            val currentUser = currentUserResource.data
            val profile = profileResource.data
            val stateUsername = arguments?.getString("username")
            setupOptionsMenuItems(currentUser, profile)
            if (currentUser == null && profile == null && stateUsername.isNullOrBlank()) {
                // default anonymous state, show default message
                showDefaultMessage()
                return@observe
            }
            if (profile == null && !stateUsername.isNullOrBlank()) {
                context?.let { ctx -> Toast.makeText(ctx, R.string.error_loading_profile, Toast.LENGTH_LONG).show() }
                return@observe
            }
            root.loadLayoutDescription(R.xml.header_list_scene)
            setupFavChip(profile, currentUser)
            setupFavButton(currentUser, profile)
            setupSavedButton(currentUser, profile)
            setupTaggedButton(currentUser, profile)
            setupLikedButton(currentUser, profile)
            setupDMButton(currentUser, profile)
            if (profile == null) return@observe
            if (profile.isReallyPrivate(currentUser)) {
                showPrivateAccountMessage()
                return@observe
            }
            if (!setupPostsDone) {
                setupPosts(profile, currentUser)
            }
        }
        viewModel.username.observe(viewLifecycleOwner) {
            mainActivity.supportActionBar?.title = it
            mainActivity.supportActionBar?.subtitle = null
        }
        viewModel.profilePicUrl.observe(viewLifecycleOwner) {
            val visibility = if (it.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
            binding.header.mainProfileImage.visibility = visibility
            binding.header.mainProfileImage.setImageURI(if (it.isNullOrBlank()) null else it)
            binding.header.mainProfileImage.setOnClickListener(if (it.isNullOrBlank()) null else onProfilePicClickListener)
        }
        viewModel.fullName.observe(viewLifecycleOwner) { binding.header.mainFullName.text = it ?: "" }
        viewModel.biography.observe(viewLifecycleOwner, this::setupBiography)
        viewModel.url.observe(viewLifecycleOwner, this::setupProfileURL)
        viewModel.followersCount.observe(viewLifecycleOwner, this::setupFollowers)
        viewModel.followingCount.observe(viewLifecycleOwner, this::setupFollowing)
        viewModel.postCount.observe(viewLifecycleOwner, this::setupPostsCount)
        viewModel.friendshipStatus.observe(viewLifecycleOwner) {
            setupFollowButton(it)
            setupMainStatus(it)
        }
        viewModel.isVerified.observe(viewLifecycleOwner) {
            binding.header.isVerified.visibility = if (it == true) View.VISIBLE else View.GONE
        }
        viewModel.isPrivate.observe(viewLifecycleOwner) {
            binding.header.isPrivate.visibility = if (it == true) View.VISIBLE else View.GONE
        }
        viewModel.isFavorite.observe(viewLifecycleOwner) {
            if (!it) {
                binding.header.favChip.setChipIconResource(R.drawable.ic_outline_star_plus_24)
                binding.header.favChip.setText(R.string.add_to_favorites)
                return@observe
            }
            binding.header.favChip.setChipIconResource(R.drawable.ic_star_check_24)
            binding.header.favChip.setText(R.string.favorite_short)
        }
        viewModel.profileContext.observe(viewLifecycleOwner, this::setupProfileContext)
        viewModel.userHighlights.observe(viewLifecycleOwner) {
            binding.header.highlightsList.visibility = if (it.data.isNullOrEmpty()) View.GONE else View.VISIBLE
            highlightsAdapter?.submitList(it.data)
        }
        viewModel.userStories.observe(viewLifecycleOwner) {
            binding.header.mainProfileImage.setStoriesBorder(if (it.data == null) 0 else 1)
        }
        viewModel.eventLiveData.observe(viewLifecycleOwner) {
            val event = it?.getContentIfNotHandled() ?: return@observe
            when (event) {
                ShowConfirmUnfollowDialog -> showConfirmUnfollowDialog()
                is DMButtonState -> binding.header.btnDM.isEnabled = !event.disabled
                is NavigateToThread -> mainActivity.navigateToThread(event.threadId, event.username)
                is ShowTranslation -> showTranslationDialog(event.result)
            }
        }
    }

    private fun showPrivateAccountMessage() {
        binding.header.mainFollowers.isClickable = false
        binding.header.mainFollowing.isClickable = false
        binding.privatePage1.setImageResource(R.drawable.lock)
        binding.privatePage2.setText(R.string.priv_acc)
        binding.privatePage.visibility = VISIBLE
        binding.privatePage1.visibility = VISIBLE
        binding.privatePage2.visibility = VISIBLE
        binding.postsRecyclerView.visibility = GONE
        binding.swipeRefreshLayout.isRefreshing = false
        root.getTransition(R.id.transition)?.setEnable(false)
    }

    private fun setupProfileContext(contextPair: Pair<String?, List<UserProfileContextLink>?>) {
        val (profileContext, contextLinkList) = contextPair
        if (profileContext == null || contextLinkList == null) {
            binding.header.profileContext.visibility = GONE
            binding.header.profileContext.clearOnMentionClickListeners()
            return
        }
        var updatedProfileContext: String = profileContext
        contextLinkList.forEachIndexed { i, link ->
            if (link.username == null) return@forEachIndexed
            updatedProfileContext = updatedProfileContext.substring(0, link.start + i) + "@" + updatedProfileContext.substring(link.start + i)
        }
        binding.header.profileContext.visibility = VISIBLE
        binding.header.profileContext.text = updatedProfileContext
        binding.header.profileContext.addOnMentionClickListener(onMentionClickListener)
    }

    private fun setupProfileURL(url: String?) {
        if (url.isNullOrBlank()) {
            binding.header.mainUrl.visibility = GONE
            binding.header.mainUrl.clearOnURLClickListeners()
            binding.header.mainUrl.setOnLongClickListener(null)
            return
        }
        binding.header.mainUrl.visibility = VISIBLE
        binding.header.mainUrl.text = url
        binding.header.mainUrl.addOnURLClickListener { Utils.openURL(context ?: return@addOnURLClickListener, it.originalText.trimAll()) }
        binding.header.mainUrl.setOnLongClickListener {
            Utils.copyText(context ?: return@setOnLongClickListener false, url.trimAll())
            return@setOnLongClickListener true
        }
    }

    private fun showTranslationDialog(result: String) {
        val dialog = ConfirmDialogFragment.newInstance(
            translationDialogRequestCode,
            0,
            result,
            R.string.ok,
            0,
            0
        )
        dialog.show(childFragmentManager, ConfirmDialogFragment::class.java.simpleName)
    }

    private fun setupBiography(bio: String?) {
        if (bio.isNullOrBlank()) {
            binding.header.mainBiography.visibility = View.GONE
            binding.header.mainBiography.clearAllAutoLinkListeners()
            binding.header.mainBiography.setOnLongClickListener(null)
            return
        }
        binding.header.mainBiography.visibility = View.VISIBLE
        binding.header.mainBiography.text = bio
        setCommonAutoLinkListeners(binding.header.mainBiography)
        binding.header.mainBiography.setOnLongClickListener {
            val isLoggedIn = viewModel.isLoggedIn.value ?: false
            val options = arrayListOf(Option(getString(R.string.bio_copy), "copy"))
            if (isLoggedIn) {
                options.add(Option(getString(R.string.bio_translate), "translate"))
            }
            val dialog = MultiOptionDialogFragment.newInstance(
                bioDialogRequestCode,
                0,
                options
            )
            dialog.show(childFragmentManager, MultiOptionDialogFragment::class.java.simpleName)
            return@setOnLongClickListener true
        }
    }

    private fun setCommonAutoLinkListeners(textView: RamboTextViewV2) {
        textView.addOnEmailClickListener(onEmailClickListener)
        textView.addOnHashtagListener(onHashtagClickListener)
        textView.addOnMentionClickListener(onMentionClickListener)
        textView.addOnURLClickListener(onURLClickListener)
    }

    private fun setupOptionsMenuItems(currentUser: User?, profile: User?) {
        val isMe = currentUser?.pk == profile?.pk
        if (profile == null || (currentUser != null && isMe)) {
            hideAllOptionsMenuItems()
            return
        }
        if (currentUser == null) {
            hideAllOptionsMenuItems()
            shareLinkMenuItem?.isVisible = profile.username.isNotBlank()
            return
        }

        blockMenuItem?.isVisible = true
        blockMenuItem?.setTitle(if (profile.friendshipStatus?.blocking == true) R.string.unblock else R.string.block)

        restrictMenuItem?.isVisible = true
        restrictMenuItem?.setTitle(if (profile.friendshipStatus?.isRestricted == true) R.string.unrestrict else R.string.restrict)

        muteStoriesMenuItem?.isVisible = true
        muteStoriesMenuItem?.setTitle(if (profile.friendshipStatus?.isMutingReel == true) R.string.mute_stories else R.string.unmute_stories)

        mutePostsMenuItem?.isVisible = true
        mutePostsMenuItem?.setTitle(if (profile.friendshipStatus?.muting == true) R.string.mute_posts else R.string.unmute_posts)

        chainingMenuItem?.isVisible = profile.hasChaining
        removeFollowerMenuItem?.isVisible = profile.friendshipStatus?.followedBy ?: false
        shareLinkMenuItem?.isVisible = profile.username.isNotBlank()
        shareDmMenuItem?.isVisible = profile.pk != 0L
    }

    private fun hideAllOptionsMenuItems() {
        blockMenuItem?.isVisible = false
        restrictMenuItem?.isVisible = false
        muteStoriesMenuItem?.isVisible = false
        mutePostsMenuItem?.isVisible = false
        chainingMenuItem?.isVisible = false
        removeFollowerMenuItem?.isVisible = false
        shareLinkMenuItem?.isVisible = false
        shareDmMenuItem?.isVisible = false
    }

    private fun setupPostsCount(count: Long?) {
        if (count == null) {
            binding.header.mainPostCount.visibility = View.GONE
            return
        }
        binding.header.mainPostCount.visibility = View.VISIBLE
        binding.header.mainPostCount.text = getCountSpan(R.plurals.main_posts_count, abbreviate(count, null), count)
    }

    private fun setupFollowing(count: Long?) {
        if (count == null) {
            binding.header.mainFollowing.visibility = View.GONE
            return
        }
        val abbreviate = abbreviate(count, null)
        val span = SpannableStringBuilder(getString(R.string.main_posts_following, abbreviate))
        binding.header.mainFollowing.visibility = View.VISIBLE
        binding.header.mainFollowing.text = getCountSpan(span, abbreviate)
        if (count <= 0) {
            binding.header.mainFollowing.setOnClickListener(null)
            return
        }
        binding.header.mainFollowing.setOnClickListener(onFollowingClickListener)
    }

    private fun setupFollowers(count: Long?) {
        if (count == null) {
            binding.header.mainFollowers.visibility = View.GONE
            return
        }
        binding.header.mainFollowers.visibility = View.VISIBLE
        binding.header.mainFollowers.text = getCountSpan(R.plurals.main_posts_followers, abbreviate(count, null), count)
        if (count <= 0) {
            binding.header.mainFollowers.setOnClickListener(null)
            return
        }
        binding.header.mainFollowers.setOnClickListener(onFollowersClickListener)
    }

    private fun setupDMButton(currentUser: User?, profile: User?) {
        val visibility = if (disableDm || (currentUser != null && profile?.pk == currentUser.pk)) View.GONE else View.VISIBLE
        binding.header.btnDM.visibility = visibility
        if (visibility == View.GONE) {
            binding.header.btnDM.setOnClickListener(null)
            return
        }
        binding.header.btnDM.setOnClickListener { viewModel.sendDm() }
    }

    private fun setupLikedButton(currentUser: User?, profile: User?) {
        val visibility = if (currentUser != null && profile?.pk == currentUser.pk) View.VISIBLE else View.GONE
        binding.header.btnLiked.visibility = visibility
        if (visibility == View.GONE) {
            binding.header.btnLiked.setOnClickListener(null)
            return
        }
        binding.header.btnLiked.setOnClickListener {
            try {
                val action = ProfileFragmentDirections.actionProfileFragmentToSavedViewerFragment(
                    viewModel.profile.value?.data?.username ?: return@setOnClickListener,
                    viewModel.profile.value?.data?.pk ?: return@setOnClickListener,
                    PostItemType.LIKED
                )
                NavHostFragment.findNavController(this).navigate(action)
            } catch (e: Exception) {
                Log.e(TAG, "setupTaggedButton: ", e)
            }
        }
    }

    private fun setupTaggedButton(currentUser: User?, profile: User?) {
        val visibility = if (currentUser != null && profile?.pk == currentUser.pk) View.VISIBLE else View.GONE
        binding.header.btnTagged.visibility = visibility
        if (visibility == View.GONE) {
            binding.header.btnTagged.setOnClickListener(null)
            return
        }
        binding.header.btnTagged.setOnClickListener {
            try {
                val action = ProfileFragmentDirections.actionProfileFragmentToSavedViewerFragment(
                    viewModel.profile.value?.data?.username ?: return@setOnClickListener,
                    viewModel.profile.value?.data?.pk ?: return@setOnClickListener,
                    PostItemType.TAGGED
                )
                NavHostFragment.findNavController(this).navigate(action)
            } catch (e: Exception) {
                Log.e(TAG, "setupTaggedButton: ", e)
            }
        }
    }

    private fun setupSavedButton(currentUser: User?, profile: User?) {
        val visibility = if (currentUser != null && profile?.pk == currentUser.pk) View.VISIBLE else View.GONE
        binding.header.btnSaved.visibility = visibility
        if (visibility == View.GONE) {
            binding.header.btnSaved.setOnClickListener(null)
            return
        }
        binding.header.btnSaved.setOnClickListener {
            try {
                val action = ProfileFragmentDirections.actionGlobalSavedCollectionsFragment(false)
                NavHostFragment.findNavController(this).navigate(action)
            } catch (e: Exception) {
                Log.e(TAG, "setupSavedButton: ", e)
            }
        }
    }

    private fun setupFavButton(currentUser: User?, profile: User?) {
        val visibility = if (currentUser != null && profile?.pk != currentUser.pk) View.VISIBLE else View.GONE
        binding.header.btnFollow.visibility = visibility
        if (visibility == View.GONE) {
            binding.header.btnFollow.setOnClickListener(null)
            return
        }
        binding.header.btnFollow.setOnClickListener { viewModel.toggleFollow(false) }
    }

    private fun setupFavChip(profile: User?, currentUser: User?) {
        val visibility = if (profile?.pk != currentUser?.pk) View.VISIBLE else View.GONE
        binding.header.favChip.visibility = visibility
        if (visibility == View.GONE) {
            binding.header.favChip.setOnClickListener(null)
            return
        }
        binding.header.favChip.setOnClickListener { viewModel.toggleFavorite() }
    }

    private fun setupFollowButton(it: FriendshipStatus?) {
        if (it == null) return
        if (it.following) {
            binding.header.btnFollow.setText(R.string.unfollow)
            binding.header.btnFollow.setChipIconResource(R.drawable.ic_outline_person_add_disabled_24)
            return
        }
        if (it.outgoingRequest) {
            binding.header.btnFollow.setText(R.string.cancel)
            binding.header.btnFollow.setChipIconResource(R.drawable.ic_outline_person_add_disabled_24)
            return
        }
        binding.header.btnFollow.setText(R.string.follow)
        binding.header.btnFollow.setChipIconResource(R.drawable.ic_outline_person_add_24)
    }

    private fun setupMainStatus(it: FriendshipStatus?) {
        if (it == null || (!it.following && !it.followedBy)) {
            binding.header.mainStatus.visibility = View.GONE
            return
        }
        binding.header.mainStatus.visibility = View.VISIBLE
        if (it.following && it.followedBy) {
            context?.let { ctx ->
                binding.header.mainStatus.chipBackgroundColor = AppCompatResources.getColorStateList(ctx, R.color.green_800)
                binding.header.mainStatus.setText(R.string.status_mutual)
            }
            return
        }
        if (it.following) {
            context?.let { ctx ->
                binding.header.mainStatus.chipBackgroundColor = AppCompatResources.getColorStateList(ctx, R.color.deep_orange_800)
                binding.header.mainStatus.setText(R.string.status_following)
            }
            return
        }
        context?.let { ctx ->
            binding.header.mainStatus.chipBackgroundColor = AppCompatResources.getColorStateList(ctx, R.color.blue_800)
            binding.header.mainStatus.setText(R.string.status_follower)
        }
    }

    private fun getCountSpan(pluralRes: Int, countString: String, count: Long): SpannableStringBuilder {
        val span = SpannableStringBuilder(resources.getQuantityString(pluralRes, count.toInt(), countString))
        return getCountSpan(span, countString)
    }

    private fun getCountSpan(span: SpannableStringBuilder, countString: String): SpannableStringBuilder {
        span.setSpan(RelativeSizeSpan(1.2f), 0, countString.length, 0)
        span.setSpan(StyleSpan(Typeface.BOLD), 0, countString.length, 0)
        return span
    }

    private fun showDefaultMessage() {
        root.loadLayoutDescription(R.xml.profile_fragment_no_acc_layout)
        binding.privatePage1.visibility = View.VISIBLE
        binding.privatePage2.visibility = View.VISIBLE
        binding.privatePage1.setImageResource(R.drawable.ic_outline_info_24)
        binding.privatePage2.setText(R.string.no_acc)
    }

    private fun setupHighlights() {
        val context = context ?: return
        highlightsAdapter = HighlightsAdapter { model, position ->
            val options = StoryViewerOptions.forHighlight(model.title)
            options.currentFeedStoryIndex = position
            val action = ProfileFragmentDirections.actionProfileFragmentToStoryViewerFragment(options)
            NavHostFragment.findNavController(this).navigate(action)
        }
        binding.header.highlightsList.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.header.highlightsList.adapter = highlightsAdapter
    }

    private fun setupPosts(profile: User, currentUser: User?) {
        binding.postsRecyclerView.setViewModelStoreOwner(this)
            .setLifeCycleOwner(this)
            .setPostFetchService(ProfilePostFetchService(profile, currentUser != null))
            .setLayoutPreferences(layoutPreferences)
            .addFetchStatusChangeListener { binding.swipeRefreshLayout.isRefreshing = it }
            .setFeedItemCallback(feedItemCallback)
            .setSelectionModeCallback(selectionModeCallback)
            .init()
        binding.postsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val canScrollVertically = recyclerView.canScrollVertically(-1)
                root.getTransition(R.id.transition)?.setEnable(!canScrollVertically)
            }
        })
        setupPostsDone = true
    }

    private fun navigateToProfile(username: String?) {
        try {
            val bundle = Bundle()
            bundle.putString("username", username ?: return)
            val navController = NavHostFragment.findNavController(this)
            navController.navigate(R.id.action_global_profileFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "navigateToProfile: ", e)
        }
    }

    private fun showConfirmUnfollowDialog() {
        val isPrivate = viewModel.profile.value?.data?.isPrivate ?: return
        val titleRes = if (isPrivate) R.string.priv_acc else 0
        val messageRes = if (isPrivate) R.string.priv_acc_confirm else R.string.are_you_sure
        val dialog = ConfirmDialogFragment.newInstance(
            confirmDialogFragmentRequestCode,
            titleRes,
            messageRes,
            R.string.confirm,
            R.string.cancel,
            0,
        )
        dialog.show(childFragmentManager, ConfirmDialogFragment::class.java.simpleName)
    }

    override fun onPositiveButtonClicked(requestCode: Int) {
        when (requestCode) {
            confirmDialogFragmentRequestCode -> {
                viewModel.toggleFollow(true)
            }
        }
    }

    override fun onNegativeButtonClicked(requestCode: Int) {}

    override fun onNeutralButtonClicked(requestCode: Int) {}

    override fun onSelect(requestCode: Int, result: String?) {
        val r = result ?: return
        when (requestCode) {
            ppOptsDialogRequestCode -> onPpOptionSelect(r)
            bioDialogRequestCode -> onBioOptionSelect(r)
        }
    }

    private fun onBioOptionSelect(result: String) {
        when (result) {
            "copy" -> Utils.copyText(context ?: return, viewModel.biography.value ?: return)
            "translate" -> viewModel.translateBio()
        }
    }

    private fun onPpOptionSelect(result: String) {
        when (result) {
            "profile_pic" -> showProfilePicDialog()
            "show_stories" -> {
                try {
                    val action = ProfileFragmentDirections.actionProfileFragmentToStoryViewerFragment(
                        StoryViewerOptions.forUser(
                            viewModel.profile.value?.data?.pk ?: return,
                            viewModel.profile.value?.data?.fullName ?: return,
                        )
                    )
                    NavHostFragment.findNavController(this).navigate(action)
                } catch (e: Exception) {
                    Log.e(TAG, "omPpOptionSelect: ", e)
                }
            }
        }
    }

    override fun onCancel(requestCode: Int) {}

    private fun showProfilePicDialog() {
        val profile = viewModel.profile.value?.data ?: return
        val fragment = ProfilePicDialogFragment.getInstance(
            profile.pk,
            profile.username,
            profile.profilePicUrl ?: return
        )
        val ft = childFragmentManager.beginTransaction()
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(fragment, ProfilePicDialogFragment::class.java.simpleName)
            .commit()
    }

    private fun showPostsLayoutPreferences() {
        val fragment = PostsLayoutPreferencesDialogFragment(Constants.PREF_PROFILE_POSTS_LAYOUT) { preferences ->
            layoutPreferences = preferences
            Handler(Looper.getMainLooper()).postDelayed(
                { binding.postsRecyclerView.layoutPreferences = preferences },
                200
            )
        }
        fragment.show(childFragmentManager, PostsLayoutPreferencesDialogFragment::class.java.simpleName)
    }
}