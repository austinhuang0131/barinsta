package awais.instagrabber.managers

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.Transformations.map
import awais.instagrabber.R
import awais.instagrabber.customviews.emoji.Emoji
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.models.enums.DirectItemType
import awais.instagrabber.repositories.requests.UploadFinishOptions
import awais.instagrabber.repositories.requests.VideoOptions
import awais.instagrabber.repositories.requests.directmessages.ThreadIdsOrUserIds
import awais.instagrabber.repositories.requests.directmessages.ThreadIdsOrUserIds.Companion.of
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.*
import awais.instagrabber.utils.MediaUploader.MediaUploadResponse
import awais.instagrabber.utils.MediaUploader.uploadPhoto
import awais.instagrabber.utils.MediaUploader.uploadVideo
import awais.instagrabber.utils.MediaUtils.OnInfoLoadListener
import awais.instagrabber.utils.MediaUtils.VideoInfo
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.webservices.DirectMessagesRepository
import awais.instagrabber.webservices.FriendshipRepository
import awais.instagrabber.webservices.MediaRepository
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*
import java.util.stream.Collectors

class ThreadManager(
    private val threadId: String,
    pending: Boolean,
    private val currentUser: User?,
    private val contentResolver: ContentResolver,
    private val viewerId: Long,
    private val csrfToken: String,
    private val deviceUuid: String,
) {
    private val _fetching = MutableLiveData<Resource<Any?>>()
    val fetching: LiveData<Resource<Any?>> = _fetching
    private val _replyToItem = MutableLiveData<DirectItem?>()
    val replyToItem: LiveData<DirectItem?> = _replyToItem
    private val _pendingRequests = MutableLiveData<DirectThreadParticipantRequestsResponse?>(null)
    val pendingRequests: LiveData<DirectThreadParticipantRequestsResponse?> = _pendingRequests
    private val inboxManager: InboxManager = if (pending) DirectMessagesManager.pendingInboxManager else DirectMessagesManager.inboxManager
    private val threadIdsOrUserIds: ThreadIdsOrUserIds = of(threadId)
    private val friendshipRepository: FriendshipRepository by lazy { FriendshipRepository.getInstance() }
    private val mediaRepository: MediaRepository by lazy { MediaRepository.getInstance() }
    private val directMessagesRepository by lazy { DirectMessagesRepository.getInstance() }

    val thread: LiveData<DirectThread?> by lazy {
        distinctUntilChanged(map(inboxManager.getInbox()) { inboxResource: Resource<DirectInbox?>? ->
            if (inboxResource == null) return@map null
            val (threads) = inboxResource.data ?: return@map null
            if (threads.isNullOrEmpty()) return@map null
            val thread = threads.firstOrNull { it.threadId == threadId }
            thread?.also {
                cursor = thread.oldestCursor
                hasOlder = thread.hasOlder
            }
        })
    }
    val inputMode: LiveData<Int> by lazy { distinctUntilChanged(map(thread) { it?.inputMode ?: 1 }) }
    val threadTitle: LiveData<String?> by lazy { distinctUntilChanged(map(thread) { it?.threadTitle }) }
    val users: LiveData<List<User>> by lazy { distinctUntilChanged(map(thread) { it?.users ?: emptyList() }) }
    val usersWithCurrent: LiveData<List<User>> by lazy {
        distinctUntilChanged(map(thread) {
            if (it == null) return@map emptyList()
            getUsersWithCurrentUser(it)
        })
    }
    val leftUsers: LiveData<List<User>> by lazy { distinctUntilChanged(map(thread) { it?.leftUsers ?: emptyList() }) }
    val usersAndLeftUsers: LiveData<Pair<List<User>, List<User>>> by lazy {
        distinctUntilChanged(map(thread) {
            if (it == null) return@map Pair<List<User>, List<User>>(emptyList(), emptyList())
            val users = getUsersWithCurrentUser(it)
            val leftUsers = it.leftUsers
            Pair(users, leftUsers)
        })
    }
    val isPending: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.pending ?: true }) }
    val adminUserIds: LiveData<List<Long>> by lazy { distinctUntilChanged(map(thread) { it?.adminUserIds ?: emptyList() }) }
    val items: LiveData<List<DirectItem>> by lazy { distinctUntilChanged(map(thread) { it?.items ?: emptyList() }) }
    val isViewerAdmin: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.adminUserIds?.contains(viewerId) ?: false }) }
    val isGroup: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.isGroup ?: false }) }
    val isMuted: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.muted ?: false }) }
    val isApprovalRequiredToJoin: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.approvalRequiredForNewMembers ?: false }) }
    val isMentionsMuted: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.mentionsMuted ?: false }) }
    val pendingRequestsCount: LiveData<Int> by lazy { distinctUntilChanged(map(_pendingRequests) { it?.totalParticipantRequests ?: 0 }) }
    val inviter: LiveData<User?> by lazy { distinctUntilChanged(map(thread) { it?.inviter }) }

    private var hasOlder = true
    private var cursor: String? = null
    private var chatsRequest: Call<DirectThreadFeedResponse?>? = null

    private fun getUsersWithCurrentUser(t: DirectThread): List<User> {
        val builder = ImmutableList.builder<User>()
        if (currentUser != null) {
            builder.add(currentUser)
        }
        val users: List<User>? = t.users
        if (users != null) {
            builder.addAll(users)
        }
        return builder.build()
    }

    fun fetchChats(scope: CoroutineScope) {
        val fetchingValue = _fetching.value
        if (fetchingValue != null && fetchingValue.status === Resource.Status.LOADING || !hasOlder) return
        _fetching.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                val threadFeedResponse = directMessagesRepository.fetchThread(threadId, cursor)
                if (threadFeedResponse.status != null && threadFeedResponse.status != "ok") {
                    _fetching.postValue(error(R.string.generic_not_ok_response, null))
                    return@launch
                }
                val thread = threadFeedResponse.thread
                if (thread == null) {
                    _fetching.postValue(error("thread is null!", null))
                    return@launch
                }
                setThread(thread)
                _fetching.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "Failed fetching dm chats", e)
                _fetching.postValue(error(e.message, null))
                hasOlder = false
            }
        }
        if (cursor == null) {
            fetchPendingRequests(scope)
        }
    }

    fun fetchPendingRequests(scope: CoroutineScope) {
        val isGroup = isGroup.value
        if (isGroup == null || !isGroup) return
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.participantRequests(threadId, 1)
                _pendingRequests.postValue(response)
            } catch (e: Exception) {
                Log.e(TAG, "fetchPendingRequests: ", e)
            }
        }
    }

    private fun setThread(thread: DirectThread, skipItems: Boolean) {
        // if (thread.getInputMode() != 1 && thread.isGroup() && viewerIsAdmin) {
        //     fetchPendingRequests();
        // }
        val items = thread.items
        if (skipItems) {
            val currentThread = this.thread.value
            if (currentThread != null) {
                thread.items = currentThread.items
            }
        }
        if (!skipItems && !cursor.isNullOrBlank()) {
            val currentThread = this.thread.value
            if (currentThread != null) {
                val currentItems = currentThread.items
                val list = if (currentItems == null) LinkedList() else LinkedList(currentItems)
                if (items != null) {
                    list.addAll(items)
                }
                thread.items = list
            }
        }
        inboxManager.setThread(threadId, thread)
    }

    private fun setThread(thread: DirectThread) {
        setThread(thread, false)
    }

    private fun setThreadUsers(users: List<User>?, leftUsers: List<User>?) {
        val currentThread = thread.value ?: return
        val thread: DirectThread = try {
            currentThread.clone() as DirectThread
        } catch (e: CloneNotSupportedException) {
            Log.e(TAG, "setThreadUsers: ", e)
            return
        }
        if (users != null) {
            thread.users = users
        }
        if (leftUsers != null) {
            thread.leftUsers = leftUsers
        }
        inboxManager.setThread(threadId, thread)
    }

    private fun addItems(index: Int, items: Collection<DirectItem>) {
        inboxManager.addItemsToThread(threadId, index, items)
    }

    private fun addReaction(item: DirectItem, emoji: Emoji) {
        if (currentUser == null) return
        val isLike = emoji.unicode == "❤️"
        var reactions = item.reactions
        reactions = if (reactions == null) {
            DirectItemReactions(null, null)
        } else {
            try {
                reactions.clone() as DirectItemReactions
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "addReaction: ", e)
                return
            }
        }
        if (isLike) {
            val likes = addEmoji(reactions.likes, null, false)
            reactions.likes = likes
        }
        val emojis = addEmoji(reactions.emojis, emoji.unicode, true)
        reactions.emojis = emojis
        val currentItems = items.value
        val items = if (currentItems == null) LinkedList() else LinkedList(currentItems)
        val index = getItemIndex(item, items)
        if (index >= 0) {
            try {
                val clone = items[index].clone() as DirectItem
                clone.reactions = reactions
                items[index] = clone
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "addReaction: error cloning", e)
            }
        }
        inboxManager.setItemsToThread(threadId, items)
    }

    private fun removeReaction(item: DirectItem) {
        try {
            val itemClone = item.clone() as DirectItem
            val reactions = itemClone.reactions
            var reactionsClone: DirectItemReactions? = null
            if (reactions != null) {
                reactionsClone = reactions.clone() as DirectItemReactions
            }
            var likes: List<DirectItemEmojiReaction>? = null
            if (reactionsClone != null) {
                likes = reactionsClone.likes
            }
            if (likes != null) {
                val updatedLikes = likes.stream()
                    .filter { (senderId) -> senderId != viewerId }
                    .collect(Collectors.toList())
                if (reactionsClone != null) {
                    reactionsClone.likes = updatedLikes
                }
            }
            var emojis: List<DirectItemEmojiReaction>? = null
            if (reactionsClone != null) {
                emojis = reactionsClone.emojis
            }
            if (emojis != null) {
                val updatedEmojis = emojis.stream()
                    .filter { (senderId) -> senderId != viewerId }
                    .collect(Collectors.toList())
                if (reactionsClone != null) {
                    reactionsClone.emojis = updatedEmojis
                }
            }
            itemClone.reactions = reactionsClone
            val items = items.value
            val list = if (items == null) LinkedList() else LinkedList(items)
            val index = getItemIndex(item, list)
            if (index >= 0) {
                list[index] = itemClone
            }
            inboxManager.setItemsToThread(threadId, list)
        } catch (e: Exception) {
            Log.e(TAG, "removeReaction: ", e)
        }
    }

    private fun removeItem(item: DirectItem): Int {
        val items = items.value
        val list = if (items == null) LinkedList() else LinkedList(items)
        val index = getItemIndex(item, list)
        if (index >= 0) {
            list.removeAt(index)
            inboxManager.setItemsToThread(threadId, list)
        }
        return index
    }

    private fun addEmoji(
        reactionList: List<DirectItemEmojiReaction>?,
        emoji: String?,
        shouldReplaceIfAlreadyReacted: Boolean,
    ): List<DirectItemEmojiReaction>? {
        if (currentUser == null) return reactionList
        val temp: MutableList<DirectItemEmojiReaction> = if (reactionList == null) ArrayList() else ArrayList(reactionList)
        var index = -1
        for (i in temp.indices) {
            val (senderId) = temp[i]
            if (senderId == currentUser.pk) {
                index = i
                break
            }
        }
        val reaction = DirectItemEmojiReaction(
            currentUser.pk,
            System.currentTimeMillis() * 1000,
            emoji,
            "none"
        )
        if (index < 0) {
            temp.add(0, reaction)
        } else if (shouldReplaceIfAlreadyReacted) {
            temp[index] = reaction
        }
        return temp
    }

    fun sendText(text: String, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val userId = getCurrentUserId(data) ?: return data
        val clientContext = UUID.randomUUID().toString()
        val replyToItemValue = _replyToItem.value
        val directItem = createText(userId, clientContext, text, replyToItemValue)
        // Log.d(TAG, "sendText: sending: itemId: " + directItem.getItemId());
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val repliedToItemId = replyToItemValue?.itemId
        val repliedToClientContext = replyToItemValue?.clientContext
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.broadcastText(
                    csrfToken,
                    viewerId,
                    deviceUuid,
                    clientContext,
                    threadIdsOrUserIds,
                    text,
                    repliedToItemId,
                    repliedToClientContext
                )
                parseResponse(response, data, directItem)
            } catch (e: Exception) {
                data.postValue(error(e.message, directItem))
                Log.e(TAG, "sendText: ", e)
            }
        }
        return data
    }

    fun sendUri(uri: Uri, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val mimeType = Utils.getMimeType(uri, contentResolver)
        if (isEmpty(mimeType)) {
            data.postValue(error("Unknown MediaType", null))
            return data
        }
        val isPhoto = mimeType != null && mimeType.startsWith("image")
        if (isPhoto) {
            sendPhoto(data, uri, scope)
            return data
        }
        if (mimeType != null && mimeType.startsWith("video")) {
            sendVideo(data, uri, scope)
        }
        return data
    }

    fun sendAnimatedMedia(giphyGif: GiphyGif, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val userId = getCurrentUserId(data) ?: return data
        val clientContext = UUID.randomUUID().toString()
        val directItem = createAnimatedMedia(userId, clientContext, giphyGif)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        scope.launch(Dispatchers.IO) {
            try {
                val request = directMessagesRepository.broadcastAnimatedMedia(
                    csrfToken,
                    userId,
                    deviceUuid,
                    clientContext,
                    threadIdsOrUserIds,
                    giphyGif
                )
                parseResponse(request, data, directItem)
            } catch (e: Exception) {
                data.postValue(error(e.message, directItem))
                Log.e(TAG, "sendAnimatedMedia: ", e)
            }
        }
        return data
    }

    fun sendVoice(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        waveform: List<Float>,
        samplingFreq: Int,
        duration: Long,
        byteLength: Long,
        scope: CoroutineScope,
    ) {
        if (duration > 60000) {
            // instagram does not allow uploading audio longer than 60 secs for Direct messages
            data.postValue(error(R.string.dms_ERROR_AUDIO_TOO_LONG, null))
            return
        }
        val userId = getCurrentUserId(data) ?: return
        val clientContext = UUID.randomUUID().toString()
        val directItem = createVoice(userId, clientContext, uri, duration, waveform, samplingFreq)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val uploadDmVoiceOptions = createUploadDmVoiceOptions(byteLength, duration)
        scope.launch(Dispatchers.IO) {
            try {
                val response = uploadVideo(uri, contentResolver, uploadDmVoiceOptions)
                // Log.d(TAG, "onUploadComplete: " + response);
                if (handleInvalidResponse(data, response)) return@launch
                val uploadFinishOptions = UploadFinishOptions(
                    uploadDmVoiceOptions.uploadId,
                    "4",
                    null
                )
                mediaRepository.uploadFinish(csrfToken, userId, deviceUuid, uploadFinishOptions)
                val broadcastResponse = directMessagesRepository.broadcastVoice(
                    csrfToken,
                    viewerId,
                    deviceUuid,
                    clientContext,
                    threadIdsOrUserIds,
                    uploadDmVoiceOptions.uploadId,
                    waveform,
                    samplingFreq
                )
                parseResponse(broadcastResponse, data, directItem)
            } catch (e: Exception) {
                data.postValue(error(e.message, directItem))
                Log.e(TAG, "sendVoice: ", e)
            }
        }
    }

    fun sendReaction(
        item: DirectItem,
        emoji: Emoji,
        scope: CoroutineScope,
    ): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val userId = getCurrentUserId(data)
        if (userId == null) {
            data.postValue(error("userId is null", null))
            return data
        }
        val clientContext = UUID.randomUUID().toString()
        // Log.d(TAG, "sendText: sending: itemId: " + directItem.getItemId());
        data.postValue(loading(item))
        addReaction(item, emoji)
        var emojiUnicode: String? = null
        if (emoji.unicode != "❤️") {
            emojiUnicode = emoji.unicode
        }
        val itemId = item.itemId
        if (itemId == null) {
            data.postValue(error("itemId is null", null))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.broadcastReaction(
                    csrfToken,
                    userId,
                    deviceUuid,
                    clientContext,
                    threadIdsOrUserIds,
                    itemId,
                    emojiUnicode,
                    false
                )
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
                Log.e(TAG, "sendReaction: ", e)
            }
        }
        return data
    }

    fun sendDeleteReaction(itemId: String, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val item = getItem(itemId)
        if (item == null) {
            data.postValue(error("Invalid item", null))
            return data
        }
        val reactions = item.reactions
        if (reactions == null) {
            // already removed?
            data.postValue(success(item))
            return data
        }
        removeReaction(item)
        val clientContext = UUID.randomUUID().toString()
        val itemId1 = item.itemId
        if (itemId1 == null) {
            data.postValue(error("itemId is null", null))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.broadcastReaction(
                    csrfToken,
                    viewerId,
                    deviceUuid,
                    clientContext,
                    threadIdsOrUserIds,
                    itemId1,
                    null,
                    true
                )
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
                Log.e(TAG, "sendDeleteReaction: ", e)
            }
        }
        return data
    }

    fun unsend(item: DirectItem, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val index = removeItem(item)
        val itemId = item.itemId
        if (itemId == null) {
            data.postValue(error("itemId is null", null))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.deleteItem(csrfToken, deviceUuid, threadId, itemId)
            } catch (e: Exception) {
                // add the item back if unsuccessful
                addItems(index, listOf(item))
                data.postValue(error(e.message, item))
                Log.e(TAG, "unsend: ", e)
            }
        }
        return data
    }

    fun forward(
        recipients: Set<RankedRecipient>,
        itemToForward: DirectItem,
        scope: CoroutineScope,
    ) {
        for (recipient in recipients) {
            forward(recipient, itemToForward, scope)
        }
    }

    fun forward(
        recipient: RankedRecipient,
        itemToForward: DirectItem,
        scope: CoroutineScope,
    ) {
        if (recipient.thread == null && recipient.user != null) {
            scope.launch(Dispatchers.IO) {
                // create thread and forward
                val thread = DirectMessagesManager.createThread(recipient.user.pk)
                forward(thread, itemToForward, scope)
            }
            return
        }
        if (recipient.thread != null) {
            // just forward
            val thread = recipient.thread
            forward(thread, itemToForward, scope)
        }
    }

    fun setReplyToItem(item: DirectItem?) {
        // Log.d(TAG, "setReplyToItem: " + item);
        _replyToItem.postValue(item)
    }

    private fun forward(
        thread: DirectThread,
        itemToForward: DirectItem,
        scope: CoroutineScope,
    ): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val forwardItemId = itemToForward.itemId
        if (forwardItemId == null) {
            data.postValue(error("item id is null", null))
            return data
        }
        val itemType = itemToForward.itemType
        if (itemType == null) {
            data.postValue(error("item type is null", null))
            return data
        }
        val itemTypeName = DirectItemType.getName(itemType)
        if (itemTypeName == null) {
            Log.e(TAG, "forward: itemTypeName was null!")
            data.postValue(error("itemTypeName is null", null))
            return data
        }
        data.postValue(loading(null))
        if (thread.threadId == null) {
            Log.e(TAG, "forward: threadId was null!")
            data.postValue(error("threadId is null", null))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.forward(
                    thread.threadId,
                    itemTypeName,
                    threadId,
                    forwardItemId
                )
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "forward: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun acceptRequest(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.approveRequest(csrfToken, deviceUuid, threadId)
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "acceptRequest: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun declineRequest(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.declineRequest(csrfToken, deviceUuid, threadId)
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "declineRequest: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun refreshChats(scope: CoroutineScope) {
        val isFetching = _fetching.value
        if (isFetching != null && isFetching.status === Resource.Status.LOADING) {
            stopCurrentRequest()
        }
        cursor = null
        hasOlder = true
        fetchChats(scope)
    }

    private fun sendPhoto(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        scope: CoroutineScope,
    ) {
        try {
            val dimensions = BitmapUtils.decodeDimensions(contentResolver, uri)
            if (dimensions == null) {
                data.postValue(error("Decoding dimensions failed", null))
                return
            }
            sendPhoto(data, uri, dimensions.first, dimensions.second, scope)
        } catch (e: IOException) {
            data.postValue(error(e.message, null))
            Log.e(TAG, "sendPhoto: ", e)
        }
    }

    private fun sendPhoto(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        width: Int,
        height: Int,
        scope: CoroutineScope,
    ) {
        val clientContext = UUID.randomUUID().toString()
        val directItem = createImageOrVideo(viewerId, clientContext, uri, width, height, false)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        scope.launch(Dispatchers.IO) {
            try {
                val response = uploadPhoto(uri, contentResolver)
                if (handleInvalidResponse(data, response)) return@launch
                val response1 = response.response ?: return@launch
                val uploadId = response1.optString("upload_id")
                val response2 = directMessagesRepository.broadcastPhoto(csrfToken, viewerId, deviceUuid, clientContext, threadIdsOrUserIds, uploadId)
                parseResponse(response2, data, directItem)
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
                Log.e(TAG, "sendPhoto: ", e)
            }
        }
    }

    private fun sendVideo(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        scope: CoroutineScope,
    ) {
        MediaUtils.getVideoInfo(contentResolver, uri, object : OnInfoLoadListener<VideoInfo?> {
            override fun onLoad(info: VideoInfo?) {
                if (info == null) {
                    data.postValue(error("Could not get the video info", null))
                    return
                }
                sendVideo(data, uri, info.size, info.duration, info.width, info.height, scope)
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, null))
            }
        })
    }

    private fun sendVideo(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        byteLength: Long,
        duration: Long,
        width: Int,
        height: Int,
        scope: CoroutineScope,
    ) {
        if (duration > 60000) {
            // instagram does not allow uploading videos longer than 60 secs for Direct messages
            data.postValue(error(R.string.dms_ERROR_VIDEO_TOO_LONG, null))
            return
        }
        val userId = getCurrentUserId(data) ?: return
        val clientContext = UUID.randomUUID().toString()
        val directItem = createImageOrVideo(userId, clientContext, uri, width, height, true)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val uploadDmVideoOptions = createUploadDmVideoOptions(byteLength, duration, width, height)
        scope.launch(Dispatchers.IO) {
            try {
                val response = uploadVideo(uri, contentResolver, uploadDmVideoOptions)
                // Log.d(TAG, "onUploadComplete: " + response);
                if (handleInvalidResponse(data, response)) return@launch
                val uploadFinishOptions = UploadFinishOptions(
                    uploadDmVideoOptions.uploadId,
                    "2",
                    VideoOptions(duration / 1000f, emptyList(), 0, false)
                )
                mediaRepository.uploadFinish(csrfToken, userId, deviceUuid, uploadFinishOptions)
                val broadcastResponse = directMessagesRepository.broadcastVideo(
                    csrfToken,
                    viewerId,
                    deviceUuid,
                    clientContext,
                    threadIdsOrUserIds,
                    uploadDmVideoOptions.uploadId,
                    "",
                    true
                )
                parseResponse(broadcastResponse, data, directItem)
            } catch (e: Exception) {
                data.postValue(error(e.message, directItem))
                Log.e(TAG, "sendVideo: ", e)
            }
        }
    }

    private fun parseResponse(
        response: DirectThreadBroadcastResponse,
        data: MutableLiveData<Resource<Any?>>,
        directItem: DirectItem,
    ) {
        val payloadClientContext: String?
        val timestamp: Long
        val itemId: String?
        val payload = response.payload
        if (payload == null) {
            val messageMetadata = response.messageMetadata
            if (messageMetadata == null || messageMetadata.isEmpty()) {
                data.postValue(success(directItem))
                return
            }
            val (clientContext, itemId1, timestamp1) = messageMetadata[0]
            payloadClientContext = clientContext
            itemId = itemId1
            timestamp = timestamp1
        } else {
            payloadClientContext = payload.clientContext
            timestamp = payload.timestamp
            itemId = payload.itemId
        }
        if (payloadClientContext == null) {
            data.postValue(error("clientContext in response was null", null))
            return
        }
        updateItemSent(payloadClientContext, timestamp, itemId)
        data.postValue(success(directItem))
    }

    private fun updateItemSent(
        clientContext: String,
        timestamp: Long,
        itemId: String?,
    ) {
        val items = items.value
        val list = if (items == null) LinkedList() else LinkedList(items)
        val index = list.indexOfFirst { it?.clientContext == clientContext }
        if (index < 0) return
        val directItem = list[index]
        try {
            val itemClone = directItem.clone() as DirectItem
            itemClone.itemId = itemId
            itemClone.isPending = false
            itemClone.setTimestamp(timestamp)
            list[index] = itemClone
            inboxManager.setItemsToThread(threadId, list)
        } catch (e: CloneNotSupportedException) {
            Log.e(TAG, "updateItemSent: ", e)
        }
    }

    private fun handleInvalidResponse(
        data: MutableLiveData<Resource<Any?>>,
        response: MediaUploadResponse,
    ): Boolean {
        val responseJson = response.response
        if (responseJson == null || response.responseCode != HttpURLConnection.HTTP_OK) {
            data.postValue(error(R.string.generic_not_ok_response, null))
            return true
        }
        val status = responseJson.optString("status")
        if (isEmpty(status) || status != "ok") {
            data.postValue(error(R.string.generic_not_ok_response, null))
            return true
        }
        return false
    }

    private fun getItemIndex(item: DirectItem, list: List<DirectItem?>): Int {
        return Iterables.indexOf(list) { i: DirectItem? -> i != null && i.itemId == item.itemId }
    }

    private fun getItem(itemId: String): DirectItem? {
        val items = items.value ?: return null
        return items.asSequence()
            .filter { it.itemId == itemId }
            .firstOrNull()
    }

    private fun stopCurrentRequest() {
        chatsRequest?.let {
            if (it.isExecuted || it.isCanceled) return
            it.cancel()
        }
        _fetching.postValue(success(Any()))
    }

    private fun getCurrentUserId(data: MutableLiveData<Resource<Any?>>): Long? {
        if (currentUser == null || currentUser.pk <= 0) {
            data.postValue(error(R.string.dms_ERROR_INVALID_USER, null))
            return null
        }
        return currentUser.pk
    }

    fun removeThread() {
        val pendingValue = isPending.value
        val threadInPending = pendingValue != null && pendingValue
        inboxManager.removeThread(threadId)
        if (threadInPending) {
            val totalValue = inboxManager.getPendingRequestsTotal().value ?: return
            inboxManager.setPendingRequestsTotal(totalValue - 1)
        }
    }

    fun updateTitle(newTitle: String, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.updateTitle(csrfToken, deviceUuid, threadId, newTitle.trim())
                handleDetailsChangeResponse(data, response)
            } catch (e: Exception) {
            }
        }
        return data
    }

    fun addMembers(users: Set<User>, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.addUsers(
                    csrfToken,
                    deviceUuid,
                    threadId,
                    users.map { obj: User -> obj.pk }
                )
                handleDetailsChangeResponse(data, response)
            } catch (e: Exception) {
                Log.e(TAG, "addMembers: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun removeMember(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.removeUsers(csrfToken, deviceUuid, threadId, setOf(user.pk))
                data.postValue(success(Any()))
                var activeUsers = users.value
                var leftUsersValue = leftUsers.value
                if (activeUsers == null) {
                    activeUsers = emptyList()
                }
                if (leftUsersValue == null) {
                    leftUsersValue = emptyList()
                }
                val updatedActiveUsers = activeUsers.filter { u: User -> u.pk != user.pk }
                val updatedLeftUsersBuilder = ImmutableList.builder<User>().addAll(leftUsersValue)
                if (!leftUsersValue.contains(user)) {
                    updatedLeftUsersBuilder.add(user)
                }
                val updatedLeftUsers = updatedLeftUsersBuilder.build()
                setThreadUsers(updatedActiveUsers, updatedLeftUsers)
            } catch (e: Exception) {
                Log.e(TAG, "removeMember: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun isAdmin(user: User): Boolean {
        val adminUserIdsValue = adminUserIds.value
        return adminUserIdsValue != null && adminUserIdsValue.contains(user.pk)
    }

    fun makeAdmin(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        if (isAdmin(user)) return data
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.addAdmins(csrfToken, deviceUuid, threadId, setOf(user.pk))
                val currentAdminIds = adminUserIds.value
                val updatedAdminIds = ImmutableList.builder<Long>()
                    .addAll(currentAdminIds ?: emptyList())
                    .add(user.pk)
                    .build()
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.adminUserIds = updatedAdminIds
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "makeAdmin: ", e)
                }
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "makeAdmin: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun removeAdmin(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        if (!isAdmin(user)) return data
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.removeAdmins(csrfToken, deviceUuid, threadId, setOf(user.pk))
                val currentAdmins = adminUserIds.value ?: return@launch
                val updatedAdminUserIds = currentAdmins.filter { userId1: Long -> userId1 != user.pk }
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.adminUserIds = updatedAdminUserIds
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "removeAdmin: ", e)
                }
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "removeAdmin: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun mute(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val muted = isMuted.value
        if (muted != null && muted) {
            data.postValue(success(Any()))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.mute(csrfToken, deviceUuid, threadId)
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.muted = true
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "mute: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "mute: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun unmute(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val muted = isMuted.value
        if (muted != null && !muted) {
            data.postValue(success(Any()))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.unmute(csrfToken, deviceUuid, threadId)
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.muted = false
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "unmute: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "unmute: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun muteMentions(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val mentionsMuted = isMentionsMuted.value
        if (mentionsMuted != null && mentionsMuted) {
            data.postValue(success(Any()))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.muteMentions(csrfToken, deviceUuid, threadId)
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.mentionsMuted = true
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "muteMentions: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "muteMentions: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun unmuteMentions(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val mentionsMuted = isMentionsMuted.value
        if (mentionsMuted != null && !mentionsMuted) {
            data.postValue(success(Any()))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                directMessagesRepository.unmuteMentions(csrfToken, deviceUuid, threadId)
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.mentionsMuted = false
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "unmuteMentions: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "unmuteMentions: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun blockUser(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                friendshipRepository.changeBlock(csrfToken, viewerId, deviceUuid, false, user.pk)
                refreshChats(scope)
            } catch (e: Exception) {
                Log.e(TAG, "onFailure: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun unblockUser(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                friendshipRepository.changeBlock(csrfToken, viewerId, deviceUuid, true, user.pk)
                refreshChats(scope)
            } catch (e: Exception) {
                Log.e(TAG, "onFailure: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun restrictUser(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                friendshipRepository.toggleRestrict(csrfToken, deviceUuid, user.pk, true)
                refreshChats(scope)
            } catch (e: Exception) {
                Log.e(TAG, "onFailure: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun unRestrictUser(user: User, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        scope.launch(Dispatchers.IO) {
            try {
                friendshipRepository.toggleRestrict(csrfToken, deviceUuid, user.pk, false)
                refreshChats(scope)
            } catch (e: Exception) {
                Log.e(TAG, "onFailure: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun approveUsers(users: List<User>, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.approveParticipantRequests(
                    csrfToken,
                    deviceUuid,
                    threadId,
                    users.map { obj: User -> obj.pk }
                )
                handleDetailsChangeResponse(data, response)
                pendingUserApproveDenySuccessAction(users)
            } catch (e: Exception) {
                Log.e(TAG, "approveUsers: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun denyUsers(users: List<User>, scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.declineParticipantRequests(
                    csrfToken,
                    deviceUuid,
                    threadId,
                    users.map { obj: User -> obj.pk }
                )
                handleDetailsChangeResponse(data, response)
                pendingUserApproveDenySuccessAction(users)
            } catch (e: Exception) {
                Log.e(TAG, "denyUsers: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    private fun pendingUserApproveDenySuccessAction(users: List<User>) {
        val pendingRequestsValue = _pendingRequests.value ?: return
        val pendingUsers = pendingRequestsValue.users
        if (pendingUsers == null || pendingUsers.isEmpty()) return
        val filtered = pendingUsers.filter { o: User -> !users.contains(o) }
        try {
            val clone = pendingRequestsValue.clone() as DirectThreadParticipantRequestsResponse
            clone.users = filtered
            val totalParticipantRequests = clone.totalParticipantRequests
            clone.totalParticipantRequests = if (totalParticipantRequests > 0) totalParticipantRequests - 1 else 0
            _pendingRequests.postValue(clone)
        } catch (e: CloneNotSupportedException) {
            Log.e(TAG, "pendingUserApproveDenySuccessAction: ", e)
        }
    }

    fun approvalRequired(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val approvalRequiredToJoin = isApprovalRequiredToJoin.value
        if (approvalRequiredToJoin != null && approvalRequiredToJoin) {
            data.postValue(success(Any()))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.approvalRequired(csrfToken, deviceUuid, threadId)
                handleDetailsChangeResponse(data, response)
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.approvalRequiredForNewMembers = true
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "approvalRequired: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun approvalNotRequired(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val approvalRequiredToJoin = isApprovalRequiredToJoin.value
        if (approvalRequiredToJoin != null && !approvalRequiredToJoin) {
            data.postValue(success(Any()))
            return data
        }
        scope.launch(Dispatchers.IO) {
            try {
                val request = directMessagesRepository.approvalNotRequired(csrfToken, deviceUuid, threadId)
                handleDetailsChangeResponse(data, request)
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.approvalRequiredForNewMembers = false
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "approvalNotRequired: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun leave(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                val request = directMessagesRepository.leave(csrfToken, deviceUuid, threadId)
                handleDetailsChangeResponse(data, request)
            } catch (e: Exception) {
                Log.e(TAG, "leave: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun end(scope: CoroutineScope): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                val request = directMessagesRepository.end(csrfToken, deviceUuid, threadId)
                handleDetailsChangeResponse(data, request)
                val currentThread = thread.value ?: return@launch
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.inputMode = 1
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "leave: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    private fun handleDetailsChangeResponse(
        data: MutableLiveData<Resource<Any?>>,
        response: DirectThreadDetailsChangeResponse,
    ) {
        data.postValue(success(Any()))
        val thread = response.thread
        if (thread != null) {
            setThread(thread, true)
        }
    }

    fun markAsSeen(
        directItem: DirectItem,
        scope: CoroutineScope,
    ): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                val response = directMessagesRepository.markAsSeen(csrfToken, deviceUuid, threadId, directItem)
                if (response == null) {
                    data.postValue(error(R.string.generic_null_response, null))
                    return@launch
                }
                if (currentUser == null) return@launch
                inboxManager.fetchUnseenCount(scope)
                val payload = response.payload ?: return@launch
                val timestamp = payload.timestamp
                val thread = thread.value ?: return@launch
                val currentLastSeenAt = thread.lastSeenAt
                val lastSeenAt = if (currentLastSeenAt == null) HashMap() else HashMap(currentLastSeenAt)
                lastSeenAt[currentUser.pk] = DirectThreadLastSeenAt(timestamp, directItem.itemId)
                thread.lastSeenAt = lastSeenAt
                setThread(thread, true)
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "markAsSeen: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }
}