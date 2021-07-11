package awais.instagrabber.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import awais.instagrabber.managers.DirectMessagesManager.pendingInboxManager
import awais.instagrabber.managers.InboxManager
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.DirectInbox
import awais.instagrabber.repositories.responses.directmessages.DirectThread

class DirectPendingInboxViewModel : ViewModel() {
    private val inboxManager: InboxManager = pendingInboxManager
    val threads: LiveData<List<DirectThread>> = inboxManager.threads
    val inbox: LiveData<Resource<DirectInbox?>> = inboxManager.getInbox()
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
    }
}