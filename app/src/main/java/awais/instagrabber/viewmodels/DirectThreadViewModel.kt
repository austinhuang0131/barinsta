package awais.instagrabber.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.*
import awais.instagrabber.customviews.emoji.Emoji
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.managers.DirectMessagesManager.inboxManager
import awais.instagrabber.managers.ThreadManager
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.DirectItem
import awais.instagrabber.repositories.responses.directmessages.DirectThread
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.*
import awais.instagrabber.utils.MediaUtils.OnInfoLoadListener
import awais.instagrabber.utils.MediaUtils.VideoInfo
import awais.instagrabber.utils.VoiceRecorder.VoiceRecorderCallback
import awais.instagrabber.utils.VoiceRecorder.VoiceRecordingResult


class DirectThreadViewModel(
    application: Application,
    val threadId: String,
    pending: Boolean,
    val currentUser: User,
) : AndroidViewModel(application) {
    // private val TAG = DirectThreadViewModel::class.java.simpleName

    // private static final String ERROR_INVALID_THREAD = "Invalid thread";
    private val contentResolver: ContentResolver = application.contentResolver
    private val recordingsDir: DocumentFile? = DownloadUtils.recordingsDir
    private var voiceRecorder: VoiceRecorder? = null
    private lateinit var threadManager: ThreadManager

    val viewerId: Long
    val threadTitle: LiveData<String?> by lazy { threadManager.threadTitle }
    val thread: LiveData<DirectThread?> by lazy { threadManager.thread }
    val items: LiveData<List<DirectItem>> by lazy {
        Transformations.map(threadManager.items) { it.filter { thread -> thread.hideInThread == 0 } }
    }
    val isFetching: LiveData<Resource<Any?>> by lazy { threadManager.fetching }
    val users: LiveData<List<User>> by lazy { threadManager.users }
    val leftUsers: LiveData<List<User>> by lazy { threadManager.leftUsers }
    val pendingRequestsCount: LiveData<Int> by lazy { threadManager.pendingRequestsCount }
    val inputMode: LiveData<Int> by lazy { threadManager.inputMode }
    val isPending: LiveData<Boolean> by lazy { threadManager.isPending }
    val replyToItem: LiveData<DirectItem?> by lazy { threadManager.replyToItem }

    fun moveFromPending() {
        val messagesManager = DirectMessagesManager
        messagesManager.moveThreadFromPending(threadId)
        threadManager = messagesManager.getThreadManager(threadId, false, currentUser, contentResolver)
    }

    fun removeThread() {
        threadManager.removeThread()
    }

    fun fetchChats() {
        threadManager.fetchChats(viewModelScope)
    }

    fun refreshChats() {
        threadManager.refreshChats(viewModelScope)
    }

    fun sendText(text: String): LiveData<Resource<Any?>> {
        return threadManager.sendText(text, viewModelScope)
    }

    fun sendUri(uri: Uri): LiveData<Resource<Any?>> {
        return threadManager.sendUri(uri, viewModelScope)
    }

    fun startRecording(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        voiceRecorder = VoiceRecorder(recordingsDir!!, object : VoiceRecorderCallback {
            override fun onStart() {}
            override fun onComplete(result: VoiceRecordingResult) {
                // Log.d(TAG, "onComplete: recording complete. Scanning file...");
                MediaUtils.getVoiceInfo(
                    contentResolver,
                    result.file.uri,
                    object : OnInfoLoadListener<VideoInfo?> {
                        override fun onLoad(videoInfo: VideoInfo?) {
                            if (videoInfo == null) return
                            threadManager.sendVoice(
                                data,
                                result.file.uri,
                                result.waveform,
                                result.samplingFreq,
                                videoInfo.duration,
                                result.file.length(),
                                viewModelScope
                            )
                        }

                        override fun onFailure(t: Throwable) {
                            data.postValue(error(t.message, null))
                        }
                    })
            }

            override fun onCancel() {}
        })
        voiceRecorder?.startRecording(contentResolver)
        return data
    }

    fun stopRecording(delete: Boolean) {
        voiceRecorder?.stopRecording(delete)
        voiceRecorder = null
    }

    fun sendReaction(item: DirectItem, emoji: Emoji): LiveData<Resource<Any?>> {
        return threadManager.sendReaction(item, emoji, viewModelScope)
    }

    fun sendDeleteReaction(itemId: String): LiveData<Resource<Any?>> {
        return threadManager.sendDeleteReaction(itemId, viewModelScope)
    }

    fun unsend(item: DirectItem): LiveData<Resource<Any?>> {
        return threadManager.unsend(item, viewModelScope)
    }

    fun sendAnimatedMedia(giphyGif: GiphyGif): LiveData<Resource<Any?>> {
        return threadManager.sendAnimatedMedia(giphyGif, viewModelScope)
    }

    fun getUser(userId: Long): User? {
        var match: User? = null
        users.value?.let { match = it.firstOrNull { user -> user.pk == userId } }
        if (match == null) {
            leftUsers.value?.let { match = it.firstOrNull { user -> user.pk == userId } }
        }
        return match
    }

    fun forward(recipients: Set<RankedRecipient>, itemToForward: DirectItem) {
        threadManager.forward(recipients, itemToForward, viewModelScope)
    }

    fun forward(recipient: RankedRecipient, itemToForward: DirectItem) {
        threadManager.forward(recipient, itemToForward, viewModelScope)
    }

    fun setReplyToItem(item: DirectItem?) {
        // Log.d(TAG, "setReplyToItem: " + item);
        threadManager.setReplyToItem(item)
    }

    fun acceptRequest(): LiveData<Resource<Any?>> {
        return threadManager.acceptRequest(viewModelScope)
    }

    fun declineRequest(): LiveData<Resource<Any?>> {
        return threadManager.declineRequest(viewModelScope)
    }

    fun markAsSeen(): LiveData<Resource<Any?>> {
        val thread = thread.value ?: return successEventResObjectLiveData
        val items = thread.items
        if (items.isNullOrEmpty()) return successEventResObjectLiveData
        val directItem = items.firstOrNull { (_, userId) -> userId != currentUser.pk } ?: return successEventResObjectLiveData
        val lastSeenAt = thread.lastSeenAt
        if (lastSeenAt != null) {
            val seenAt = lastSeenAt[currentUser.pk] ?: return successEventResObjectLiveData
            try {
                val timestamp = seenAt.timestamp ?: return successEventResObjectLiveData
                val itemIdMatches = seenAt.itemId == directItem.itemId
                val timestampMatches = timestamp.toLong() >= directItem.getTimestamp()
                if (itemIdMatches || timestampMatches) {
                    return successEventResObjectLiveData
                }
            } catch (ignored: Exception) {
                return successEventResObjectLiveData
            }
        }
        return threadManager.markAsSeen(directItem, viewModelScope)
    }

    private val successEventResObjectLiveData: MutableLiveData<Resource<Any?>>
        get() {
            val data = MutableLiveData<Resource<Any?>>()
            data.postValue(success(Any()))
            return data
        }

    fun deleteThreadIfRequired() {
        val thread = thread.value ?: return
        if (thread.isTemp && thread.items.isNullOrEmpty()) {
            val inboxManager = inboxManager
            inboxManager.removeThread(threadId)
        }
    }

    init {
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        viewerId = getUserIdFromCookie(cookie)
        val deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        val csrfToken = getCsrfTokenFromCookie(cookie)
        require(!csrfToken.isNullOrBlank() && viewerId != 0L && deviceUuid.isNotBlank()) { "User is not logged in!" }
        threadManager = DirectMessagesManager.getThreadManager(threadId, pending, currentUser, contentResolver)
        threadManager.fetchPendingRequests(viewModelScope)
    }
}