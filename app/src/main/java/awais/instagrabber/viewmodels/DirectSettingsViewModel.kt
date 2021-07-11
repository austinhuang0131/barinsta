package awais.instagrabber.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.core.util.Pair
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import awais.instagrabber.R
import awais.instagrabber.dialogs.MultiOptionDialogFragment.Option
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.DirectThread
import awais.instagrabber.repositories.responses.directmessages.DirectThreadParticipantRequestsResponse
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.getCsrfTokenFromCookie
import awais.instagrabber.utils.getUserIdFromCookie

class DirectSettingsViewModel(
    application: Application,
    threadId: String,
    pending: Boolean,
    currentUser: User,
) : AndroidViewModel(application) {
    private val viewerId: Long
    private val resources = application.resources
    private val threadManager = DirectMessagesManager.getThreadManager(threadId, pending, currentUser, application.contentResolver)

    val thread: LiveData<DirectThread?> = threadManager.thread

    // public void setThread(@NonNull final DirectThread thread) {
    //     this.thread = thread;
    //     inputMode.postValue(thread.getInputMode());
    //     List<User> users = thread.getUsers();
    //     final ImmutableList.Builder<User> builder = ImmutableList.<User>builder().add(currentUser);
    //     if (users != null) {
    //         builder.addAll(users);
    //     }
    //     users = builder.build();
    //     this.users.postValue(new Pair<>(users, thread.getLeftUsers()));
    //     // setTitle(thread.getThreadTitle());
    //     final List<Long> adminUserIds = thread.getAdminUserIds();
    //     this.adminUserIds.postValue(adminUserIds);
    //     viewerIsAdmin = adminUserIds.contains(viewerId);
    //     muted.postValue(thread.getMuted());
    //     mentionsMuted.postValue(thread.isMentionsMuted());
    //     approvalRequiredToJoin.postValue(thread.isApprovalRequiredForNewMembers());
    //     isPending.postValue(thread.isPending());
    //     if (thread.getInputMode() != 1 && thread.isGroup() && viewerIsAdmin) {
    //         fetchPendingRequests();
    //     }
    // }
    val inputMode: LiveData<Int> = threadManager.inputMode

    fun isGroup(): LiveData<Boolean> = threadManager.isGroup

    fun getUsers(): LiveData<List<User>> = threadManager.usersWithCurrent

    fun getLeftUsers(): LiveData<List<User>> = threadManager.leftUsers

    fun getUsersAndLeftUsers(): LiveData<Pair<List<User>, List<User>>> = threadManager.usersAndLeftUsers

    fun getTitle(): LiveData<String?> = threadManager.threadTitle

    // public void setTitle(final String title) {
    //     if (title == null) {
    //         this.title.postValue("");
    //         return;
    //     }
    //     this.title.postValue(title.trim());
    // }
    fun getAdminUserIds(): LiveData<List<Long>> = threadManager.adminUserIds

    fun isMuted(): LiveData<Boolean> = threadManager.isMuted

    fun getApprovalRequiredToJoin(): LiveData<Boolean> = threadManager.isApprovalRequiredToJoin

    fun getPendingRequests(): LiveData<DirectThreadParticipantRequestsResponse?> = threadManager.pendingRequests

    fun isPending(): LiveData<Boolean> = threadManager.isPending

    fun isViewerAdmin(): LiveData<Boolean> = threadManager.isViewerAdmin

    fun updateTitle(newTitle: String): LiveData<Resource<Any?>> = threadManager.updateTitle(newTitle, viewModelScope)

    fun addMembers(users: Set<User>): LiveData<Resource<Any?>> = threadManager.addMembers(users, viewModelScope)

    fun removeMember(user: User): LiveData<Resource<Any?>> = threadManager.removeMember(user, viewModelScope)

    private fun makeAdmin(user: User): LiveData<Resource<Any?>> = threadManager.makeAdmin(user, viewModelScope)

    private fun removeAdmin(user: User): LiveData<Resource<Any?>> = threadManager.removeAdmin(user, viewModelScope)

    fun mute(): LiveData<Resource<Any?>> = threadManager.mute(viewModelScope)

    fun unmute(): LiveData<Resource<Any?>> = threadManager.unmute(viewModelScope)

    fun muteMentions(): LiveData<Resource<Any?>> = threadManager.muteMentions(viewModelScope)

    fun unmuteMentions(): LiveData<Resource<Any?>> = threadManager.unmuteMentions(viewModelScope)

    private fun blockUser(user: User): LiveData<Resource<Any?>> = threadManager.blockUser(user, viewModelScope)

    private fun unblockUser(user: User): LiveData<Resource<Any?>> = threadManager.unblockUser(user, viewModelScope)

    private fun restrictUser(user: User): LiveData<Resource<Any?>> = threadManager.restrictUser(user, viewModelScope)

    private fun unRestrictUser(user: User): LiveData<Resource<Any?>> = threadManager.unRestrictUser(user, viewModelScope)

    fun approveUsers(users: List<User>): LiveData<Resource<Any?>> = threadManager.approveUsers(users, viewModelScope)

    fun denyUsers(users: List<User>): LiveData<Resource<Any?>> = threadManager.denyUsers(users, viewModelScope)

    fun approvalRequired(): LiveData<Resource<Any?>> = threadManager.approvalRequired(viewModelScope)

    fun approvalNotRequired(): LiveData<Resource<Any?>> = threadManager.approvalNotRequired(viewModelScope)

    fun leave(): LiveData<Resource<Any?>> = threadManager.leave(viewModelScope)

    fun end(): LiveData<Resource<Any?>> = threadManager.end(viewModelScope)

    fun createUserOptions(user: User?): ArrayList<Option<String>> {
        val options: ArrayList<Option<String>> = ArrayList()
        if (user == null || isSelf(user) || hasLeft(user)) {
            return options
        }
        val viewerIsAdmin: Boolean? = threadManager.isViewerAdmin.value
        if (viewerIsAdmin != null && viewerIsAdmin) {
            options.add(Option(getString(R.string.dms_action_kick), ACTION_KICK))
            val isAdmin: Boolean = threadManager.isAdmin(user)
            options.add(Option(
                if (isAdmin) getString(R.string.dms_action_remove_admin) else getString(R.string.dms_action_make_admin),
                if (isAdmin) ACTION_REMOVE_ADMIN else ACTION_MAKE_ADMIN
            ))
        }
        val blocking: Boolean = user.friendshipStatus?.blocking ?: false
        options.add(Option(
            if (blocking) getString(R.string.unblock) else getString(R.string.block),
            if (blocking) ACTION_UNBLOCK else ACTION_BLOCK
        ))

        // options.add(new Option<>(getString(R.string.report), ACTION_REPORT));
        val isGroup: Boolean? = threadManager.isGroup.value
        if (isGroup != null && isGroup) {
            val restricted: Boolean = user.friendshipStatus?.isRestricted ?: false
            options.add(Option(
                if (restricted) getString(R.string.unrestrict) else getString(R.string.restrict),
                if (restricted) ACTION_UNRESTRICT else ACTION_RESTRICT
            ))
        }
        return options
    }

    private fun hasLeft(user: User): Boolean {
        val leftUsers: List<User> = getLeftUsers().value ?: return false
        return leftUsers.contains(user)
    }

    private fun isSelf(user: User): Boolean = user.pk == viewerId

    private fun getString(@StringRes resId: Int): String {
        return resources.getString(resId)
    }

    fun doAction(user: User?, action: String?): LiveData<Resource<Any?>>? {
        return if (user == null || action == null) null else when (action) {
            ACTION_KICK -> removeMember(user)
            ACTION_MAKE_ADMIN -> makeAdmin(user)
            ACTION_REMOVE_ADMIN -> removeAdmin(user)
            ACTION_BLOCK -> blockUser(user)
            ACTION_UNBLOCK -> unblockUser(user)
            ACTION_RESTRICT -> restrictUser(user)
            ACTION_UNRESTRICT -> unRestrictUser(user)
            else -> null
        }
    }

    fun getInviter(): LiveData<User?> = threadManager.inviter

    companion object {
        private const val ACTION_KICK = "kick"
        private const val ACTION_MAKE_ADMIN = "make_admin"
        private const val ACTION_REMOVE_ADMIN = "remove_admin"
        private const val ACTION_BLOCK = "block"
        private const val ACTION_UNBLOCK = "unblock"

        // private static final String ACTION_REPORT = "report";
        private const val ACTION_RESTRICT = "restrict"
        private const val ACTION_UNRESTRICT = "unrestrict"
    }

    init {
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        viewerId = getUserIdFromCookie(cookie)
        val deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        val csrfToken = getCsrfTokenFromCookie(cookie)
        require(!csrfToken.isNullOrBlank() && viewerId != 0L && deviceUuid.isNotBlank()) { "User is not logged in!" }
    }
}