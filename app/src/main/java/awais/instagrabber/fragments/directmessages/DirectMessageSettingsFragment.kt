package awais.instagrabber.fragments.directmessages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import awais.instagrabber.R
import awais.instagrabber.activities.MainActivity
import awais.instagrabber.adapters.DirectPendingUsersAdapter
import awais.instagrabber.adapters.DirectPendingUsersAdapter.PendingUser
import awais.instagrabber.adapters.DirectPendingUsersAdapter.PendingUserCallback
import awais.instagrabber.adapters.DirectUsersAdapter
import awais.instagrabber.customviews.helpers.TextWatcherAdapter
import awais.instagrabber.databinding.FragmentDirectMessagesSettingsBinding
import awais.instagrabber.dialogs.ConfirmDialogFragment
import awais.instagrabber.dialogs.ConfirmDialogFragment.ConfirmDialogFragmentCallback
import awais.instagrabber.dialogs.MultiOptionDialogFragment
import awais.instagrabber.dialogs.MultiOptionDialogFragment.MultiOptionDialogSingleCallback
import awais.instagrabber.fragments.UserSearchMode
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.DirectThreadParticipantRequestsResponse
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.viewmodels.AppStateViewModel
import awais.instagrabber.viewmodels.DirectSettingsViewModel
import awais.instagrabber.viewmodels.factories.DirectSettingsViewModelFactory
import com.google.android.material.snackbar.Snackbar
import java.util.*

class DirectMessageSettingsFragment : Fragment(), ConfirmDialogFragmentCallback {
    private lateinit var viewModel: DirectSettingsViewModel
    private lateinit var binding: FragmentDirectMessagesSettingsBinding

    private var usersAdapter: DirectUsersAdapter? = null
    private var isPendingRequestsSetupDone = false
    private var pendingUsersAdapter: DirectPendingUsersAdapter? = null
    private var approvalRequiredUsers: Set<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments ?: return
        val args = DirectMessageSettingsFragmentArgs.fromBundle(arguments)
        val fragmentActivity = requireActivity() as MainActivity
        val appStateViewModel: AppStateViewModel by activityViewModels()
        val currentUser = appStateViewModel.currentUser?.data ?: return
        val viewModelFactory = DirectSettingsViewModelFactory(
            fragmentActivity.application,
            args.threadId,
            args.pending,
            currentUser
        )
        viewModel = ViewModelProvider(this, viewModelFactory).get(DirectSettingsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDirectMessagesSettingsBinding.inflate(inflater, container, false)
        // currentlyRunning = new DirectMessageInboxThreadFetcher(threadId, null, null, fetchListener).execute();
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPendingRequestsSetupDone = false
    }

    private fun setupObservers() {
        viewModel.inputMode.observe(viewLifecycleOwner, { inputMode: Int? ->
            if (inputMode == null || inputMode == 0) return@observe
            if (inputMode == 1) {
                binding.groupSettings.visibility = View.GONE
                binding.pendingMembersGroup.visibility = View.GONE
                binding.approvalRequired.visibility = View.GONE
                binding.approvalRequiredLabel.visibility = View.GONE
                binding.muteMessagesLabel.visibility = View.GONE
                binding.muteMessages.visibility = View.GONE
            }
        })
        // Need to observe, so that getValue is correct
        viewModel.getUsers().observe(viewLifecycleOwner, { })
        viewModel.getLeftUsers().observe(viewLifecycleOwner, { })
        viewModel.getUsersAndLeftUsers().observe(viewLifecycleOwner, { usersAdapter?.submitUsers(it.first, it.second) })
        viewModel.getTitle().observe(viewLifecycleOwner, { binding.titleEdit.setText(it) })
        viewModel.getAdminUserIds().observe(viewLifecycleOwner, { usersAdapter?.setAdminUserIds(it) })
        viewModel.isMuted().observe(viewLifecycleOwner, { binding.muteMessages.isChecked = it })
        viewModel.isPending().observe(viewLifecycleOwner, { binding.muteMessages.visibility = if (it) View.GONE else View.VISIBLE })
        viewModel.isViewerAdmin().observe(viewLifecycleOwner, { setApprovalRelatedUI(it) })
        viewModel.getApprovalRequiredToJoin().observe(viewLifecycleOwner, { binding.approvalRequired.isChecked = it })
        viewModel.getPendingRequests().observe(viewLifecycleOwner, { setPendingRequests(it) })
        viewModel.isGroup().observe(viewLifecycleOwner, { isGroup: Boolean -> setupSettings(isGroup) })
        val navController = NavHostFragment.findNavController(this)
        val backStackEntry = navController.currentBackStackEntry
        if (backStackEntry != null) {
            val resultLiveData = backStackEntry.savedStateHandle.getLiveData<Any>("result")
            resultLiveData.observe(viewLifecycleOwner, { result: Any? ->
                if (result == null) return@observe
                if (result is RankedRecipient) {
                    val user = getUser(result)
                    // Log.d(TAG, "result: " + user);
                    if (user != null) {
                        addMembers(setOf(user))
                    }
                } else if (result is Set<*>) {
                    try {
                        @Suppress("UNCHECKED_CAST") val recipients = result as Set<RankedRecipient>
                        val users: Set<User> = recipients.asSequence()
                            .filterNotNull()
                            .map { getUser(it) }
                            .filterNotNull()
                            .toSet()
                        // Log.d(TAG, "result: " + users);
                        addMembers(users)
                    } catch (e: Exception) {
                        Log.e(TAG, "search users result: ", e)
                        Snackbar.make(binding.root, e.message ?: "", Snackbar.LENGTH_LONG).show()
                    }
                }
            })
        }
    }

    private fun addMembers(users: Set<User>) {
        val approvalRequired = viewModel.getApprovalRequiredToJoin().value
        var isViewerAdmin = viewModel.isViewerAdmin().value
        if (isViewerAdmin == null) {
            isViewerAdmin = false
        }
        if (!isViewerAdmin && approvalRequired != null && approvalRequired) {
            approvalRequiredUsers = users
            val confirmDialogFragment = ConfirmDialogFragment.newInstance(
                APPROVAL_REQUIRED_REQUEST_CODE,
                R.string.admin_approval_required,
                R.string.admin_approval_required_description,
                R.string.ok,
                R.string.cancel,
                0
            )
            confirmDialogFragment.show(childFragmentManager, "approval_required_dialog")
            return
        }
        val detailsChangeResourceLiveData = viewModel.addMembers(users)
        observeDetailsChange(detailsChangeResourceLiveData)
    }

    private fun getUser(recipient: RankedRecipient): User? {
        var user: User? = null
        if (recipient.user != null) {
            user = recipient.user
        } else if (recipient.thread != null && !recipient.thread.isGroup) {
            user = recipient.thread.users?.get(0)
        }
        return user
    }

    private fun init() {
        // setupSettings();
        setupMembers()
    }

    private fun setupSettings(isGroup: Boolean) {
        binding.groupSettings.visibility = if (isGroup) View.VISIBLE else View.GONE
        binding.muteMessagesLabel.setOnClickListener { binding.muteMessages.toggle() }
        binding.muteMessages.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            val resourceLiveData = if (isChecked) viewModel.mute() else viewModel.unmute()
            handleSwitchChangeResource(resourceLiveData, buttonView)
        }
        if (!isGroup) return
        binding.titleEdit.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' } == viewModel.getTitle().value) {
                    binding.titleEditInputLayout.suffixText = null
                    return
                }
                binding.titleEditInputLayout.suffixText = getString(R.string.save)
            }
        })
        binding.titleEditInputLayout.suffixTextView.setOnClickListener {
            val text = binding.titleEdit.text ?: return@setOnClickListener
            val newTitle = text.toString().trim { it <= ' ' }
            if (newTitle == viewModel.getTitle().value) return@setOnClickListener
            observeDetailsChange(viewModel.updateTitle(newTitle))
        }
        binding.addMembers.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            try {
                val navController = findNavController()
                if (navController.currentDestination?.id != R.id.directMessagesSettingsFragment) return@setOnClickListener
                val users = viewModel.getUsers().value ?: return@setOnClickListener
                val currentUserIds = users.asSequence().map(User::pk).sorted().toList().toLongArray()
                val actionGlobalUserSearch = DirectMessageSettingsFragmentDirections.actionToUserSearch().apply {
                    title = getString(R.string.add_members)
                    actionLabel = getString(R.string.add)
                    hideUserIds = currentUserIds
                    searchMode = UserSearchMode.RAVEN
                    multiple = true
                }
                navController.navigate(actionGlobalUserSearch)
            } catch (e: Exception) {
                Log.e(TAG, "setupSettings: ", e)
            }
        }
        binding.muteMentionsLabel.setOnClickListener { binding.muteMentions.toggle() }
        binding.muteMentions.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            val resourceLiveData = if (isChecked) viewModel.muteMentions() else viewModel.unmuteMentions()
            handleSwitchChangeResource(resourceLiveData, buttonView)
        }
        binding.leave.setOnClickListener {
            val confirmDialogFragment = ConfirmDialogFragment.newInstance(
                LEAVE_THREAD_REQUEST_CODE,
                R.string.dms_action_leave_question,
                0,
                R.string.yes,
                R.string.no,
                0
            )
            confirmDialogFragment.show(childFragmentManager, "leave_thread_confirmation_dialog")
        }
        var isViewerAdmin = viewModel.isViewerAdmin().value
        if (isViewerAdmin == null) isViewerAdmin = false
        if (isViewerAdmin) {
            binding.end.visibility = View.VISIBLE
            binding.end.setOnClickListener {
                val confirmDialogFragment = ConfirmDialogFragment.newInstance(
                    END_THREAD_REQUEST_CODE,
                    R.string.dms_action_end_question,
                    R.string.dms_action_end_description,
                    R.string.yes,
                    R.string.no,
                    0
                )
                confirmDialogFragment.show(childFragmentManager, "end_thread_confirmation_dialog")
            }
        } else {
            binding.end.visibility = View.GONE
        }
    }

    private fun setApprovalRelatedUI(isViewerAdmin: Boolean) {
        if (!isViewerAdmin) {
            binding.pendingMembersGroup.visibility = View.GONE
            binding.approvalRequired.visibility = View.GONE
            binding.approvalRequiredLabel.visibility = View.GONE
            return
        }
        binding.approvalRequired.visibility = View.VISIBLE
        binding.approvalRequiredLabel.visibility = View.VISIBLE
        binding.approvalRequiredLabel.setOnClickListener { binding.approvalRequired.toggle() }
        binding.approvalRequired.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            val resourceLiveData = if (isChecked) viewModel.approvalRequired() else viewModel.approvalNotRequired()
            handleSwitchChangeResource(resourceLiveData, buttonView)
        }
    }

    private fun handleSwitchChangeResource(resourceLiveData: LiveData<Resource<Any?>>, buttonView: CompoundButton) {
        resourceLiveData.observe(viewLifecycleOwner, { resource: Resource<Any?>? ->
            if (resource == null) return@observe
            when (resource.status) {
                Resource.Status.SUCCESS -> buttonView.isEnabled = true
                Resource.Status.ERROR -> {
                    buttonView.isEnabled = true
                    buttonView.isChecked = !buttonView.isChecked
                    if (resource.message != null) {
                        Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                    }
                    if (resource.resId != 0) {
                        Snackbar.make(binding.root, resource.resId, Snackbar.LENGTH_LONG).show()
                    }
                }
                Resource.Status.LOADING -> buttonView.isEnabled = false
            }
        })
    }

    private fun setupMembers() {
        val context = context ?: return
        binding.users.layoutManager = LinearLayoutManager(context)
        val inviter = viewModel.getInviter().value
        usersAdapter = DirectUsersAdapter(
            inviter?.pk ?: -1,
            { _: Int, user: User, _: Boolean ->
                if (user.username.isBlank() && !user.interopMessagingUserFbid.isNullOrBlank()) {
                    Utils.openURL(context, "https://facebook.com/" + user.interopMessagingUserFbid)
                    return@DirectUsersAdapter
                }
                if (user.username.isBlank()) return@DirectUsersAdapter
                try {
                    val directions = DirectMessageSettingsFragmentDirections.actionToProfile().apply { this.username = user.username }
                    findNavController().navigate(directions)
                } catch (e: Exception) {
                    Log.e(TAG, "setupMembers: ", e)
                }
            },
            { _: Int, user: User? ->
                val options = viewModel.createUserOptions(user)
                if (options.isEmpty()) return@DirectUsersAdapter true
                val fragment = MultiOptionDialogFragment.newInstance(0, -1, options)
                fragment.setSingleCallback(object : MultiOptionDialogSingleCallback<String?> {
                    override fun onSelect(requestCode: Int, action: String?) {
                        if (action == null) return
                        val resourceLiveData = viewModel.doAction(user, action)
                        if (resourceLiveData != null) {
                            observeDetailsChange(resourceLiveData)
                        }
                    }

                    override fun onCancel(requestCode: Int) {}
                })
                val fragmentManager = childFragmentManager
                fragment.show(fragmentManager, "actions")
                true
            }
        )
        binding.users.adapter = usersAdapter
    }

    private fun setPendingRequests(requests: DirectThreadParticipantRequestsResponse?) {
        val nullOrEmpty: Boolean = requests?.users?.isNullOrEmpty() ?: true
        if (nullOrEmpty) {
            binding.pendingMembersGroup.visibility = View.GONE
            return
        }
        if (!isPendingRequestsSetupDone) {
            val context = context ?: return
            binding.pendingMembers.layoutManager = LinearLayoutManager(context)
            pendingUsersAdapter = DirectPendingUsersAdapter(object : PendingUserCallback {
                override fun onClick(position: Int, pendingUser: PendingUser) {
                    try {
                        val directions = DirectMessageSettingsFragmentDirections.actionToProfile().apply { this.username = pendingUser.user.username }
                        findNavController().navigate(directions)
                    } catch (e: Exception) {
                        Log.e(TAG, "onClick: ", e)
                    }
                }

                override fun onApprove(position: Int, pendingUser: PendingUser) {
                    val resourceLiveData = viewModel.approveUsers(listOf(pendingUser.user))
                    observeApprovalChange(resourceLiveData, position, pendingUser)
                }

                override fun onDeny(position: Int, pendingUser: PendingUser) {
                    val resourceLiveData = viewModel.denyUsers(listOf(pendingUser.user))
                    observeApprovalChange(resourceLiveData, position, pendingUser)
                }
            })
            binding.pendingMembers.adapter = pendingUsersAdapter
            binding.pendingMembersGroup.visibility = View.VISIBLE
            isPendingRequestsSetupDone = true
        }
        pendingUsersAdapter?.submitPendingRequests(requests)
    }

    private fun observeDetailsChange(resourceLiveData: LiveData<Resource<Any?>>) {
        resourceLiveData.observe(viewLifecycleOwner, { resource: Resource<Any?>? ->
            if (resource == null) return@observe
            when (resource.status) {
                Resource.Status.SUCCESS,
                Resource.Status.LOADING,
                -> {
                }
                Resource.Status.ERROR -> {
                    if (resource.message != null) {
                        Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                    }
                    if (resource.resId != 0) {
                        Snackbar.make(binding.root, resource.resId, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun observeApprovalChange(
        detailsChangeResourceLiveData: LiveData<Resource<Any?>>,
        position: Int,
        pendingUser: PendingUser,
    ) {
        detailsChangeResourceLiveData.observe(viewLifecycleOwner, { resource: Resource<Any?>? ->
            if (resource == null) return@observe
            when (resource.status) {
                Resource.Status.SUCCESS -> {
                }
                Resource.Status.LOADING -> pendingUser.isInProgress = true
                Resource.Status.ERROR -> {
                    pendingUser.isInProgress = false
                    if (resource.message != null) {
                        Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                    }
                    if (resource.resId != 0) {
                        Snackbar.make(binding.root, resource.resId, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            pendingUsersAdapter?.notifyItemChanged(position)
        })
    }

    override fun onPositiveButtonClicked(requestCode: Int) {
        if (requestCode == APPROVAL_REQUIRED_REQUEST_CODE) {
            approvalRequiredUsers?.let {
                val detailsChangeResourceLiveData = viewModel.addMembers(it)
                observeDetailsChange(detailsChangeResourceLiveData)
            }
            return
        }
        if (requestCode == LEAVE_THREAD_REQUEST_CODE) {
            val resourceLiveData = viewModel.leave()
            resourceLiveData.observe(viewLifecycleOwner, { resource: Resource<Any?>? ->
                if (resource == null) return@observe
                when (resource.status) {
                    Resource.Status.SUCCESS -> {
                        val directions = DirectMessageSettingsFragmentDirections.actionToInbox()
                        NavHostFragment.findNavController(this).navigate(directions)
                    }
                    Resource.Status.ERROR -> {
                        binding.leave.isEnabled = true
                        if (resource.message != null) {
                            Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                        }
                        if (resource.resId != 0) {
                            Snackbar.make(binding.root, resource.resId, Snackbar.LENGTH_LONG).show()
                        }
                    }
                    Resource.Status.LOADING -> binding.leave.isEnabled = false
                }
            })
            return
        }
        if (requestCode == END_THREAD_REQUEST_CODE) {
            val resourceLiveData = viewModel.end()
            resourceLiveData.observe(viewLifecycleOwner, { resource: Resource<Any?>? ->
                if (resource == null) return@observe
                when (resource.status) {
                    Resource.Status.SUCCESS -> {
                    }
                    Resource.Status.ERROR -> {
                        binding.end.isEnabled = true
                        if (resource.message != null) {
                            Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                        }
                        if (resource.resId != 0) {
                            Snackbar.make(binding.root, resource.resId, Snackbar.LENGTH_LONG).show()
                        }
                    }
                    Resource.Status.LOADING -> binding.end.isEnabled = false
                }
            })
        }
    }

    override fun onNegativeButtonClicked(requestCode: Int) {
        if (requestCode == APPROVAL_REQUIRED_REQUEST_CODE) {
            approvalRequiredUsers = null
        }
    }

    override fun onNeutralButtonClicked(requestCode: Int) {}

    companion object {
        private const val APPROVAL_REQUIRED_REQUEST_CODE = 200
        private const val LEAVE_THREAD_REQUEST_CODE = 201
        private const val END_THREAD_REQUEST_CODE = 202
    }
}