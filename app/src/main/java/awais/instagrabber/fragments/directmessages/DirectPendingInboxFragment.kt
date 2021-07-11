package awais.instagrabber.fragments.directmessages

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import awais.instagrabber.activities.MainActivity
import awais.instagrabber.adapters.DirectMessageInboxAdapter
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtEdge
import awais.instagrabber.databinding.FragmentDirectPendingInboxBinding
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.directmessages.DirectInbox
import awais.instagrabber.repositories.responses.directmessages.DirectThread
import awais.instagrabber.viewmodels.DirectPendingInboxViewModel
import com.google.android.material.snackbar.Snackbar

class DirectPendingInboxFragment : Fragment(), OnRefreshListener {
    private val viewModel: DirectPendingInboxViewModel by activityViewModels()

    private lateinit var root: CoordinatorLayout
    private lateinit var lazyLoader: RecyclerLazyLoaderAtEdge
    private lateinit var binding: FragmentDirectPendingInboxBinding
    private lateinit var fragmentActivity: MainActivity
    private lateinit var inboxAdapter: DirectMessageInboxAdapter

    private var shouldRefresh = true
    private var scrollToTop = false
    private var navigating = false
    private var threadsObserver: Observer<List<DirectThread?>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentActivity = requireActivity() as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (this::root.isInitialized) {
            shouldRefresh = false
            return root
        }
        binding = FragmentDirectPendingInboxBinding.inflate(inflater, container, false)
        root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldRefresh) return
        init()
    }

    override fun onRefresh() {
        lazyLoader.resetState()
        scrollToTop = true
        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        setupObservers()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeViewModelObservers()
        viewModel.onDestroy()
    }

    private fun setupObservers() {
        removeViewModelObservers()
        threadsObserver = Observer { list: List<DirectThread?>? ->
            if (!this::inboxAdapter.isInitialized) return@Observer
            if (binding.swipeRefreshLayout.visibility == View.GONE) {
                binding.swipeRefreshLayout.visibility = View.VISIBLE
                binding.empty.visibility = View.GONE
            }
            inboxAdapter.submitList(list ?: emptyList()) {
                if (!scrollToTop) return@submitList
                binding.pendingInboxList.smoothScrollToPosition(0)
                scrollToTop = false
            }
            if (list == null || list.isEmpty()) {
                binding.swipeRefreshLayout.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
        }
        threadsObserver?.let { viewModel.threads.observe(fragmentActivity, it) }
        viewModel.inbox.observe(viewLifecycleOwner, { inboxResource: Resource<DirectInbox?>? ->
            if (inboxResource == null) return@observe
            when (inboxResource.status) {
                Resource.Status.SUCCESS -> binding.swipeRefreshLayout.isRefreshing = false
                Resource.Status.ERROR -> {
                    if (inboxResource.message != null) {
                        Snackbar.make(binding.root, inboxResource.message, Snackbar.LENGTH_LONG).show()
                    }
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                Resource.Status.LOADING -> binding.swipeRefreshLayout.isRefreshing = true
            }
        })
    }

    private fun removeViewModelObservers() {
        threadsObserver?.let { viewModel.threads.removeObserver(it) }
    }

    private fun init() {
        val context = context ?: return
        setupObservers()
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.pendingInboxList.setHasFixedSize(true)
        binding.pendingInboxList.setItemViewCacheSize(20)
        val layoutManager = LinearLayoutManager(context)
        binding.pendingInboxList.layoutManager = layoutManager
        inboxAdapter = DirectMessageInboxAdapter { thread ->
            if (navigating) return@DirectMessageInboxAdapter
            val threadId = thread.threadId ?: return@DirectMessageInboxAdapter
            val threadTitle = thread.threadTitle ?: return@DirectMessageInboxAdapter
            navigating = true
            if (isAdded) {
                val directions = DirectPendingInboxFragmentDirections.actionToThread(threadId, threadTitle)
                directions.pending = true
                NavHostFragment.findNavController(this).navigate(directions)
            }
            navigating = false
        }
        inboxAdapter.setHasStableIds(true)
        binding.pendingInboxList.adapter = inboxAdapter
        lazyLoader = RecyclerLazyLoaderAtEdge(layoutManager) { viewModel.fetchInbox() }
        binding.pendingInboxList.addOnScrollListener(lazyLoader)
    }
}