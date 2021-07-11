package awais.instagrabber.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.managers.InboxManager
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.DirectInbox
import awais.instagrabber.repositories.responses.directmessages.DirectThread

class DirectInboxViewModel : ViewModel() {
    private val inboxManager: InboxManager = DirectMessagesManager.inboxManager
    val inbox: LiveData<Resource<DirectInbox?>> = inboxManager.getInbox()
    val threads: LiveData<List<DirectThread>> = inboxManager.threads
    val unseenCount: LiveData<Resource<Int?>> = inboxManager.getUnseenCount()
    val pendingRequestsTotal: LiveData<Int> = inboxManager.getPendingRequestsTotal()
    val viewer: User? = inboxManager.viewer

    fun fetchInbox() {
        inboxManager.fetchInbox(viewModelScope)
    }

    fun refresh() {
        inboxManager.refresh(viewModelScope)
    }

    fun onDestroy() {
        inboxManager.onDestroy()
    }

    init {
        inboxManager.fetchInbox(viewModelScope)
        inboxManager.fetchUnseenCount(viewModelScope)
    }
}