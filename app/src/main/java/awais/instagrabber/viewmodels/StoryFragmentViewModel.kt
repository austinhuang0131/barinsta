package awais.instagrabber.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import awais.instagrabber.R
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.models.enums.StoryPaginationType
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.models.enums.BroadcastItemType
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.repositories.responses.stories.*
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.utils.*
import awais.instagrabber.webservices.MediaRepository
import awais.instagrabber.webservices.StoriesRepository
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StoryFragmentViewModel : ViewModel() {
    // large data
    private val currentStory = MutableLiveData<Story>()
    private val currentMedia = MutableLiveData<StoryMedia>()

    // small data
    private val storyTitle = MutableLiveData<String>()
    private val date = MutableLiveData<String>()
    private val type = MutableLiveData<MediaItemType>()
    private val poll = MutableLiveData<PollSticker>()
    private val quiz = MutableLiveData<QuizSticker>()
    private val question = MutableLiveData<QuestionSticker>()
    private val slider = MutableLiveData<SliderSticker>()
    private val swipeUp = MutableLiveData<String>()
    private val linkedPost = MutableLiveData<String>()
    private val appAttribution = MutableLiveData<StoryAppAttribution>()
    private val reelMentions = MutableLiveData<List<Triple<String, String?, FavoriteType>>>()

    // process
    private val currentIndex = MutableLiveData<Int>()
    private val pagination = MutableLiveData(StoryPaginationType.DO_NOTHING)
    private val options = MutableLiveData<Triple<List<Pair<Int, Int>>, String?, String?>>()
    private val seen = MutableLiveData<Triple<String, Long, Long>>()

    // utils
    private var messageManager: DirectMessagesManager? = null
    private val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
    private val deviceId = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
    private val csrfToken = getCsrfTokenFromCookie(cookie)
    private val userId = getUserIdFromCookie(cookie)
    private val storiesRepository: StoriesRepository by lazy { StoriesRepository.getInstance() }
    private val mediaRepository: MediaRepository by lazy { MediaRepository.getInstance() }

    /* set functions */

    fun setStory(story: Story) {
        if (story.items == null || story.items.size == 0) {
            pagination.postValue(StoryPaginationType.ERROR)
            return
        }
        currentStory.postValue(story)
        storyTitle.postValue(story.title ?: story.user?.username)
        if (story.broadcast != null) {
            date.postValue(story.dateTime)
            type.postValue(MediaItemType.MEDIA_TYPE_LIVE)
            pagination.postValue(StoryPaginationType.DO_NOTHING)
        }
    }

    fun setMedia(index: Int) {
        if (currentStory.value?.items == null) return
        if (index < 0 || index >= currentStory.value!!.items!!.size) {
            pagination.postValue(if (index < 0) StoryPaginationType.BACKWARD else StoryPaginationType.FORWARD)
            return
        }
        currentIndex.postValue(index)
        val story: Story? = currentStory.value
        val media = story!!.items!!.get(index)
        currentMedia.postValue(media)
        date.postValue(media.date)
        type.postValue(media.type)
        initStickers(media)
    }

    fun setSingleMedia(media: StoryMedia) {
        currentStory.postValue(null)
        currentIndex.postValue(0)
        currentMedia.postValue(media)
        date.postValue(media.date)
        type.postValue(media.type)
    }

    private fun initStickers(media: StoryMedia) {
        val builder = ImmutableList.builder<Pair<Int, Int>>()
        var linkedText: String? = null
        var appText: String? = null
        if (setMentions(media)) builder.add(Pair(R.id.mentions, R.string.story_mentions))
        if (setQuiz(media)) builder.add(Pair(R.id.quiz, R.string.story_quiz))
        if (setQuestion(media)) builder.add(Pair(R.id.question, R.string.story_question))
        if (setPoll(media)) builder.add(Pair(R.id.poll, R.string.story_poll))
        if (setSlider(media)) builder.add(Pair(R.id.slider, R.string.story_slider))
        if (setLinkedPost(media)) builder.add(Pair(R.id.viewStoryPost, R.string.view_post))
        if (setStoryCta(media)) {
            linkedText = media.linkText
            builder.add(Pair(R.id.swipeUp, 0))
        }
        if (setStoryAppAttribution(media)) {
            appText = media.storyAppAttribution!!.appActionText
            builder.add(Pair(R.id.spotify, 0))
        }
        options.postValue(Triple(builder.build(), linkedText, appText))
    }

    private fun setMentions(media: StoryMedia): Boolean {
        val mentions: MutableList<Triple<String, String?, FavoriteType>> = mutableListOf()
        if (media.reelMentions != null)
            mentions.addAll(media.reelMentions.map{
                Triple("@" + it.user?.username, it.user?.username, FavoriteType.USER)
            })
        if (media.storyHashtags != null)
            mentions.addAll(media.storyHashtags.map{
                Triple("#" + it.hashtag?.name, it.hashtag?.name, FavoriteType.HASHTAG)
            })
        if (media.storyLocations != null)
            mentions.addAll(media.storyLocations.map{
                Triple(it.location?.name ?: "", it.location?.pk?.toString(10), FavoriteType.LOCATION)
            })
        reelMentions.postValue(mentions.filterNot { it.second.isNullOrEmpty() } .distinct())
        return !mentions.isEmpty()
    }

    private fun setPoll(media: StoryMedia): Boolean {
        poll.postValue(media.storyPolls?.get(0)?.pollSticker ?: return false)
        return true
    }

    private fun setQuiz(media: StoryMedia): Boolean {
        quiz.postValue(media.storyQuizs?.get(0)?.quizSticker ?: return false)
        return true
    }

    private fun setQuestion(media: StoryMedia): Boolean {
        val questionSticker = media.storyQuestions?.get(0)?.questionSticker ?: return false
        if (questionSticker.questionType.equals("music")) return false
        question.postValue(questionSticker)
        return true
    }

    private fun setSlider(media: StoryMedia): Boolean {
        slider.postValue(media.storySliders?.get(0)?.sliderSticker ?: return false)
        return true
    }

    private fun setLinkedPost(media: StoryMedia): Boolean {
        linkedPost.postValue(media.storyFeedMedia?.get(0)?.mediaId ?: return false)
        return true
    }

    private fun setStoryCta(media: StoryMedia): Boolean {
        val webUri = media.storyCta?.get(0)?.links?.get(0)?.webUri ?: return false
        val parsedUri = Uri.parse(webUri)
        val cleanUri = if (parsedUri.host.equals("l.instagram.com")) parsedUri.getQueryParameter("u")
                       else null
        swipeUp.postValue(if (cleanUri != null && Uri.parse(cleanUri).scheme?.startsWith("http") == true) cleanUri
                          else webUri)
        return true
    }

    private fun setStoryAppAttribution(media: StoryMedia): Boolean {
        appAttribution.postValue(media.storyAppAttribution ?: return false)
        return true
    }

    /* get functions */

    fun getCurrentStory(): LiveData<Story> {
        return currentStory
    }

    fun getCurrentIndex(): LiveData<Int> {
        return currentIndex
    }

    fun getCurrentMedia(): LiveData<StoryMedia> {
        return currentMedia
    }

    fun getPagination(): LiveData<StoryPaginationType> {
        return pagination
    }

    fun getDate(): LiveData<String> {
        return date
    }

    fun getTitle(): LiveData<String> {
        return storyTitle
    }

    fun getType(): LiveData<MediaItemType> {
        return type
    }

    fun getMedia(): LiveData<StoryMedia> {
        return currentMedia
    }

    fun getMention(index: Int): Triple<String, String?, FavoriteType>? {
        return reelMentions.value?.get(index)
    }

    fun getMentionTexts(): Array<String> {
        return reelMentions.value!!.map { it.first } .toTypedArray()
    }

    fun getPoll(): LiveData<PollSticker> {
        return poll
    }

    fun getQuestion(): LiveData<QuestionSticker> {
        return question
    }

    fun getQuiz(): LiveData<QuizSticker> {
        return quiz
    }

    fun getSlider(): LiveData<SliderSticker> {
        return slider
    }

    fun getLinkedPost(): LiveData<Resource<Media?>> {
        val data = MutableLiveData<Resource<Media?>>()
        data.postValue(loading(null))
        val postId = linkedPost.value
        if (postId == null) data.postValue(error("No post ID supplied", null))
        else viewModelScope.launch(Dispatchers.IO) {
            try {
                val media = mediaRepository.fetch(postId.toLong())
                data.postValue(success(media))
            }
            catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun getSwipeUp(): String? {
        return swipeUp.value
    }

    fun getAppAttribution(): String? {
        return appAttribution.value?.url
    }

    fun getOptions(): LiveData<Triple<List<Pair<Int, Int>>, String?, String?>> {
        return options
    }

    /* action functions */

    fun answerPoll(w: Int): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldPoll: PollSticker = poll.value!!
                val response = storiesRepository.respondToPoll(
                    csrfToken!!,
                    userId,
                    deviceId,
                    currentMedia.value!!.pk,
                    oldPoll.pollId,
                    w
                )
                if (!"ok".equals(response.status))
                    throw Exception("Instagram returned status \"" + response.status + "\"")
                val tally = oldPoll.tallies.get(w)
                val newTally = tally.copy(count = tally.count + 1)
                val newTallies = oldPoll.tallies.toMutableList()
                newTallies.set(w, newTally)
                poll.postValue(oldPoll.copy(viewerVote = w, tallies = newTallies.toList()))
                data.postValue(success(null))
            }
            catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun answerQuiz(w: Int): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldQuiz = quiz.value!!
                val response = storiesRepository.respondToQuiz(
                    csrfToken!!,
                    userId,
                    deviceId,
                    currentMedia.value!!.pk,
                    oldQuiz.quizId,
                    w
                )
                if (!"ok".equals(response.status))
                    throw Exception("Instagram returned status \"" + response.status + "\"")
                val tally = oldQuiz.tallies.get(w)
                val newTally = tally.copy(count = tally.count + 1)
                val newTallies = oldQuiz.tallies.toMutableList()
                newTallies.set(w, newTally)
                quiz.postValue(oldQuiz.copy(viewerAnswer = w, tallies = newTallies.toList()))
                data.postValue(success(null))
            }
            catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun answerQuestion(a: String): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = storiesRepository.respondToQuestion(
                    csrfToken!!,
                    userId,
                    deviceId,
                    currentMedia.value!!.pk,
                    question.value!!.questionId,
                    a
                )
                if (!"ok".equals(response.status))
                    throw Exception("Instagram returned status \"" + response.status + "\"")
                data.postValue(success(null))
            }
            catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun answerSlider(a: Double): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldSlider = slider.value!!
                val response = storiesRepository.respondToSlider(
                    csrfToken!!,
                    userId,
                    deviceId,
                    currentMedia.value!!.pk,
                    oldSlider.sliderId,
                    a
                )
                if (!"ok".equals(response.status))
                    throw Exception("Instagram returned status \"" + response.status + "\"")
                val newVoteCount = (oldSlider.sliderVoteCount ?: 0) + 1
                val newAverage = if (oldSlider.sliderVoteAverage == null) a
                                 else (oldSlider.sliderVoteAverage * oldSlider.sliderVoteCount!! + a) / newVoteCount
                slider.postValue(oldSlider.copy(viewerCanVote = false,
                                                sliderVoteCount = newVoteCount,
                                                viewerVote = a,
                                                sliderVoteAverage = newAverage))
                data.postValue(success(null))
            }
            catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun reply(a: String): LiveData<Resource<Any?>>? {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        return messageManager?.replyToStory(
            currentStory.value?.user?.pk,
            currentStory.value?.id,
            currentMedia.value?.id,
            a,
            viewModelScope
        )
    }

    fun shareDm(result: RankedRecipient) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = currentMedia.value?.id ?: return
        val reelId = currentStory.value?.id ?: return
        messageManager?.sendMedia(result, mediaId, reelId, BroadcastItemType.STORY, viewModelScope)
    }

    fun shareDm(recipients: Set<RankedRecipient>) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = currentMedia.value?.id ?: return
        val reelId = currentStory.value?.id ?: return
        messageManager?.sendMedia(recipients, mediaId, reelId, BroadcastItemType.STORY, viewModelScope)
    }

    fun paginate(backward: Boolean) {
        var index = currentIndex.value!!
        index = if (backward) index - 1 else index + 1
        if (index < 0 || index >= currentStory.value!!.items!!.size) skip(backward)
        setMedia(index)
    }

    fun skip(backward: Boolean) {
        pagination.postValue(if (backward) StoryPaginationType.BACKWARD else StoryPaginationType.FORWARD)
    }

    fun fetchStory(fetchOptions: StoryViewerOptions?): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val story = storiesRepository.getStories(fetchOptions!!)
                setStory(story!!)
                data.postValue(success(null))
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun fetchSingleMedia(mediaId: Long): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storyMedia = storiesRepository.fetch(mediaId)
                setSingleMedia(storyMedia!!)
                data.postValue(success(null))
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }
}