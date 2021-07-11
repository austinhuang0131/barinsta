package awais.instagrabber.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import awais.instagrabber.activities.MainActivity
import awais.instagrabber.adapters.UserSearchResultsAdapter
import awais.instagrabber.customviews.helpers.TextWatcherAdapter
import awais.instagrabber.databinding.FragmentUserSearchBinding
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.trimAll
import awais.instagrabber.utils.measure
import awais.instagrabber.viewmodels.UserSearchViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

class UserSearchFragment : Fragment() {

    private lateinit var binding: FragmentUserSearchBinding

    private var resultsAdapter: UserSearchResultsAdapter? = null
    private var paddingOffset = 0
    private var actionLabel: String? = null
    private var title: String? = null
    private var multiple = false

    private val viewModel: UserSearchViewModel by viewModels()
    private val windowWidth = Utils.displayMetrics.widthPixels
    private val minInputWidth = Utils.convertDpToPx(50f)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUserSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        paddingOffset = with(binding) {
            search.paddingStart + search.paddingEnd + group.paddingStart + group.paddingEnd + group.chipSpacingHorizontal
        }
        init()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.cleanup()
    }

    private fun init() {
        val arguments = arguments
        if (arguments != null) {
            val fragmentArgs = UserSearchFragmentArgs.fromBundle(arguments)
            actionLabel = fragmentArgs.actionLabel
            title = fragmentArgs.title
            multiple = fragmentArgs.multiple
            viewModel.setHideThreadIds(fragmentArgs.hideThreadIds)
            viewModel.setHideUserIds(fragmentArgs.hideUserIds)
            viewModel.setSearchMode(fragmentArgs.searchMode)
            viewModel.setShowGroups(fragmentArgs.showGroups)
        }
        setupTitles()
        setupInput()
        setupResults()
        setupObservers()
        // show cached results
        viewModel.showCachedResults()
    }

    private fun setupTitles() {
        if (!actionLabel.isNullOrBlank()) {
            binding.done.text = actionLabel
        }
        if (title.isNullOrBlank()) return
        (activity as MainActivity?)?.supportActionBar?.title = title
    }

    private fun setupResults() {
        val context = context ?: return
        binding.results.layoutManager = LinearLayoutManager(context)
        resultsAdapter = UserSearchResultsAdapter(multiple) { _: Int, recipient: RankedRecipient, selected: Boolean ->
            if (!multiple) {
                val navController = NavHostFragment.findNavController(this)
                if (!setResult(navController, recipient)) return@UserSearchResultsAdapter
                navController.navigateUp()
                return@UserSearchResultsAdapter
            }
            viewModel.setSelectedRecipient(recipient, !selected)
            resultsAdapter?.setSelectedRecipient(recipient, !selected)
            if (!selected) {
                createChip(recipient)
                return@UserSearchResultsAdapter
            }
            val chip = findChip(recipient) ?: return@UserSearchResultsAdapter
            removeChipFromGroup(chip)
        }
        binding.results.adapter = resultsAdapter
        binding.done.setOnClickListener {
            val navController = NavHostFragment.findNavController(this)
            if (!setResult(navController, viewModel.selectedRecipients)) return@setOnClickListener
            navController.navigateUp()
        }
    }

    private fun setResult(navController: NavController, rankedRecipient: RankedRecipient): Boolean {
        navController.previousBackStackEntry?.savedStateHandle?.set("result", rankedRecipient) ?: return false
        return true
    }

    private fun setResult(navController: NavController, rankedRecipients: Set<RankedRecipient>): Boolean {
        navController.previousBackStackEntry?.savedStateHandle?.set("result", rankedRecipients) ?: return false
        return true
    }

    private fun setupInput() {
        binding.search.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.search(s.toString().trimAll())
            }
        })
        binding.search.setOnKeyListener { _: View?, _: Int, event: KeyEvent? ->
            if (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                val chip = lastChip ?: return@setOnKeyListener false
                removeChip(chip)
            }
            false
        }
        binding.group.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {}
            override fun onChildViewRemoved(parent: View, child: View) {
                binding.group.post {
                    TransitionManager.beginDelayedTransition(binding.root)
                    calculateInputWidth(0)
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.recipients.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            when (it.status) {
                Resource.Status.SUCCESS -> if (it.data != null) {
                    resultsAdapter?.submitList(it.data)
                }
                Resource.Status.ERROR -> {
                    if (it.message != null) {
                        Snackbar.make(binding.root, it.message, Snackbar.LENGTH_LONG).show()
                    }
                    if (it.resId != 0) {
                        Snackbar.make(binding.root, it.resId, Snackbar.LENGTH_LONG).show()
                    }
                    if (it.data != null) {
                        resultsAdapter?.submitList(it.data)
                    }
                }
                Resource.Status.LOADING -> if (it.data != null) {
                    resultsAdapter?.submitList(it.data)
                }
            }
        }
        viewModel.showAction().observe(viewLifecycleOwner) { binding.done.visibility = if (it) View.VISIBLE else View.GONE }
    }

    private fun createChip(recipient: RankedRecipient) {
        val context = context ?: return
        val chip = Chip(context).apply {
            tag = recipient
            text = getRecipientText(recipient)
            isCloseIconVisible = true
            setOnCloseIconClickListener { removeChip(this) }
        }
        binding.group.post {
            val measure = measure(chip, binding.group)
            TransitionManager.beginDelayedTransition(binding.root)
            calculateInputWidth(if (measure.second != null) measure.second else 0)
            binding.group.addView(chip, binding.group.childCount - 1)
        }
    }

    private fun getRecipientText(recipient: RankedRecipient?): String? = when {
        recipient == null -> null
        recipient.user != null -> recipient.user.fullName
        recipient.thread != null -> recipient.thread.threadTitle
        else -> null
    }

    private fun removeChip(chip: View) {
        val recipient = chip.tag as RankedRecipient
        viewModel.setSelectedRecipient(recipient, false)
        resultsAdapter?.setSelectedRecipient(recipient, false)
        removeChipFromGroup(chip)
    }

    private fun findChip(recipient: RankedRecipient?): View? {
        if (recipient == null || recipient.user == null && recipient.thread == null) return null
        val isUser = recipient.user != null
        val childCount = binding.group.childCount
        if (childCount == 0) return null
        for (i in childCount - 1 downTo 0) {
            val child = binding.group.getChildAt(i) ?: continue
            val tag = child.tag as RankedRecipient
            if (isUser && tag.user == null || !isUser && tag.thread == null) continue
            if (isUser && tag.user?.pk == recipient.user?.pk || !isUser && tag.thread?.threadId == recipient.thread?.threadId) {
                return child
            }
        }
        return null
    }

    private fun removeChipFromGroup(chip: View) {
        binding.group.post {
            TransitionManager.beginDelayedTransition(binding.root)
            binding.group.removeView(chip)
        }
    }

    private fun calculateInputWidth(newChipWidth: Int) {
        var lastRight = lastChip?.right ?: 0
        val remainingSpaceInRow = windowWidth - lastRight
        if (remainingSpaceInRow < newChipWidth) {
            // next chip will go to the next row, so assume no chips present
            lastRight = 0
        }
        val newRight = lastRight + newChipWidth
        val newInputWidth = windowWidth - newRight - paddingOffset
        binding.search.layoutParams.width = if (newInputWidth < minInputWidth) windowWidth else newInputWidth
        binding.search.requestLayout()
    }

    private val lastChip: View?
        get() {
            val childCount = binding.group.childCount
            if (childCount == 0) return null
            for (i in childCount - 1 downTo 0) {
                val child = binding.group.getChildAt(i)
                if (child is Chip) {
                    return child
                }
            }
            return null
        }
}