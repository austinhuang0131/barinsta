package awais.instagrabber.fragments.directmessages

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import awais.instagrabber.R
import awais.instagrabber.activities.MainActivity
import awais.instagrabber.adapters.DirectMessageInboxAdapter
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtEdge
import awais.instagrabber.databinding.FragmentDirectMessagesInboxBinding
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.directmessages.DirectInbox
import awais.instagrabber.repositories.responses.directmessages.DirectThread
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.viewmodels.DirectInboxViewModel
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.internal.ToolbarUtils
import com.google.android.material.snackbar.Snackbar

class DirectMessageInboxFragment : Fragment(), OnRefreshListener {
    private val viewModel: DirectInboxViewModel by activityViewModels()

    private lateinit var fragmentActivity: MainActivity
    private lateinit var binding: FragmentDirectMessagesInboxBinding
    private lateinit var lazyLoader: RecyclerLazyLoaderAtEdge

    private var scrollToTop = false
    private var navigating = false

    private var pendingRequestsMenuItem: MenuItem? = null
    private var pendingRequestTotalBadgeDrawable: BadgeDrawable? = null
    private var isPendingRequestTotalBadgeAttached = false
    private var inboxAdapter: DirectMessageInboxAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentActivity = requireActivity() as MainActivity
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDirectMessagesInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init()
    }

    override fun onRefresh() {
        lazyLoader.resetState()
        scrollToTop = true
        viewModel.refresh()
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError", "RestrictedApi")
    override fun onPause() {
        super.onPause()
        isPendingRequestTotalBadgeAttached = false
        pendingRequestsMenuItem?.let {
            val menuItemView = ToolbarUtils.getActionMenuItemView(fragmentActivity.toolbar, it.itemId)
            if (menuItemView != null) {
                BadgeUtils.detachBadgeDrawable(pendingRequestTotalBadgeDrawable, fragmentActivity.toolbar, it.itemId)
                pendingRequestTotalBadgeDrawable = null
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dm_inbox_menu, menu)
        pendingRequestsMenuItem = menu.findItem(R.id.pending_requests)
        pendingRequestsMenuItem?.isVisible = isPendingRequestTotalBadgeAttached
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.pending_requests) {
            try {
                val directions = DirectMessageInboxFragmentDirections.actionToPendingInbox()
                findNavController().navigate(directions)
            } catch (e: Exception) {
                Log.e(TAG, "onOptionsItemSelected: ", e)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        init()
    }

    private fun setupObservers() {
        viewModel.threads.observe(viewLifecycleOwner, { list: List<DirectThread?> ->
            inboxAdapter?.submitList(list) {
                if (!scrollToTop) return@submitList
                binding.inboxList.post { binding.inboxList.smoothScrollToPosition(0) }
                scrollToTop = false
            }
        })
        viewModel.inbox.observe(viewLifecycleOwner, { inboxResource: Resource<DirectInbox?>? ->
            if (inboxResource == null) return@observe
            when (inboxResource.status) {
                Resource.Status.SUCCESS -> binding.swipeRefreshLayout.isRefreshing = false
                Resource.Status.ERROR -> {
                    if (inboxResource.message != null) {
                        Snackbar.make(binding.root, inboxResource.message, Snackbar.LENGTH_LONG).show()
                    }
                    if (inboxResource.resId != 0) {
                        Snackbar.make(binding.root, inboxResource.resId, Snackbar.LENGTH_LONG).show()
                    }
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                Resource.Status.LOADING -> binding.swipeRefreshLayout.isRefreshing = true
            }
        })
        viewModel.pendingRequestsTotal.observe(viewLifecycleOwner, { count: Int? -> attachPendingRequestsBadge(count) })
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError", "RestrictedApi")
    private fun attachPendingRequestsBadge(count: Int?) {
        val pendingRequestsMenuItem1 = pendingRequestsMenuItem
        if (pendingRequestsMenuItem1 == null) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({ attachPendingRequestsBadge(count) }, 500)
            return
        }
        if (pendingRequestTotalBadgeDrawable == null) {
            val context = context ?: return
            pendingRequestTotalBadgeDrawable = BadgeDrawable.create(context)
        }
        if (count == null || count == 0) {
            val menuItemView = ToolbarUtils.getActionMenuItemView(
                fragmentActivity.toolbar,
                pendingRequestsMenuItem1.itemId
            )
            if (menuItemView != null) {
                BadgeUtils.detachBadgeDrawable(pendingRequestTotalBadgeDrawable, fragmentActivity.toolbar, pendingRequestsMenuItem1.itemId)
            }
            isPendingRequestTotalBadgeAttached = false
            pendingRequestTotalBadgeDrawable?.number = 0
            pendingRequestsMenuItem1.isVisible = false
            return
        }
        pendingRequestsMenuItem1.isVisible = true
        if (pendingRequestTotalBadgeDrawable?.number == count) return
        pendingRequestTotalBadgeDrawable?.number = count
        if (!isPendingRequestTotalBadgeAttached) {
            pendingRequestTotalBadgeDrawable?.let {
                BadgeUtils.attachBadgeDrawable(it, fragmentActivity.toolbar, pendingRequestsMenuItem1.itemId)
                isPendingRequestTotalBadgeAttached = true
            }
        }
    }

    private fun init() {
        val context = context ?: return
        setupObservers()
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.inboxList.setHasFixedSize(true)
        binding.inboxList.setItemViewCacheSize(20)
        val layoutManager = LinearLayoutManager(context)
        binding.inboxList.layoutManager = layoutManager
        inboxAdapter = DirectMessageInboxAdapter { thread ->
            val threadId = thread.threadId
            val threadTitle = thread.threadTitle
            if (navigating || threadId.isNullOrBlank() || threadTitle.isNullOrBlank()) return@DirectMessageInboxAdapter
            navigating = true
            if (isAdded) {
                try {
                    val directions = DirectMessageInboxFragmentDirections.actionToThread(threadId, threadTitle)
                    findNavController().navigate(directions)
                } catch (e: Exception) {
                    Log.e(TAG, "init: ", e)
                }
            }
            navigating = false
        }.also {
            it.setHasStableIds(true)
        }
        binding.inboxList.adapter = inboxAdapter
        lazyLoader = RecyclerLazyLoaderAtEdge(layoutManager) { viewModel.fetchInbox() }.also {
            binding.inboxList.addOnScrollListener(it)
        }
    }
}