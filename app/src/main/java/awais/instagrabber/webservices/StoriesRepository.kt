package awais.instagrabber.webservices

import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.repositories.StoriesService
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.stories.ArchiveResponse
import awais.instagrabber.repositories.responses.stories.Story
import awais.instagrabber.repositories.responses.stories.StoryMedia
import awais.instagrabber.repositories.responses.stories.StoryStickerResponse
import awais.instagrabber.utils.Utils
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import java.util.UUID

open class StoriesRepository(private val service: StoriesService) {

    suspend fun fetch(mediaId: Long): StoryMedia? {
        val response = service.fetch(mediaId)
        return response.items?.get(0)
    }

    suspend fun getFeedStories(): List<Story> {
        val response = service.getFeedStories()
        val result: MutableList<Story> = mutableListOf()
        if (response?.broadcasts != null) {
            val length = response.broadcasts.size
            for (i in 0 until length) {
                val broadcast = response.broadcasts.get(i)
                result.add(
                    Story(
                        broadcast.id,
                        broadcast.publishedTime,
                        1,
                        0L,
                        broadcast.broadcastOwner,
                        broadcast.muted,
                        false, // unclear
                        null,
                        null,
                        null,
                        null,
                        broadcast
                    )
                )
            }
        }
        if (response?.tray != null) result.addAll(response.tray)
        return sort(result.toList())
    }

    open suspend fun fetchHighlights(profileId: Long): List<Story> {
        val response = service.fetchHighlights(profileId)
        val highlightModels = response?.tray ?: listOf()
        return highlightModels
    }

    suspend fun fetchArchive(maxId: String): ArchiveResponse? {
        val form = mutableMapOf(
            "include_suggested_highlights" to "false",
            "is_in_archive_home" to "true",
            "include_cover" to "1",
        )
        if (!maxId.isNullOrEmpty()) {
            form["max_id"] = maxId // NOT TESTED
        }
        return service.fetchArchive(form)
    }

    open suspend fun getStories(options: StoryViewerOptions): Story? {
        return when (options.type) {
            StoryViewerOptions.Type.HIGHLIGHT,
            StoryViewerOptions.Type.STORY_ARCHIVE
            -> {
                val response = service.getReelsMedia(options.name)
                response.reels?.get(options.name)
            }
            StoryViewerOptions.Type.USER -> {
                val response = service.getUserStories(options.id.toString())
                response.reel
            }
            // should not reach beyond this point
            StoryViewerOptions.Type.LOCATION -> {
                val response = service.getStories("locations", options.id.toString())
                response.story
            }
            StoryViewerOptions.Type.HASHTAG -> {
                val response = service.getStories("tags", options.name)
                response.story
            }
            else -> null
        }
    }

    private suspend fun respondToSticker(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: Long,
        stickerId: Long,
        action: String,
        arg1: String,
        arg2: String,
    ): StoryStickerResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "mutation_token" to UUID.randomUUID().toString(),
            "client_context" to UUID.randomUUID().toString(),
            "radio_type" to "wifi-none",
            arg1 to arg2,
        )
        val signedForm = Utils.sign(form)
        return service.respondToSticker(storyId, stickerId, action, signedForm)
    }

    suspend fun respondToQuestion(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: Long,
        stickerId: Long,
        answer: String,
    ): StoryStickerResponse = respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_question_response", "response", answer)

    suspend fun respondToQuiz(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: Long,
        stickerId: Long,
        answer: Int,
    ): StoryStickerResponse {
        return respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_quiz_answer", "answer", answer.toString())
    }

    suspend fun respondToPoll(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: Long,
        stickerId: Long,
        answer: Int,
    ): StoryStickerResponse = respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_poll_vote", "vote", answer.toString())

    suspend fun respondToSlider(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: Long,
        stickerId: Long,
        answer: Double,
    ): StoryStickerResponse = respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_slider_vote", "vote", answer.toString())

    suspend fun seen(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyMediaId: String,
        takenAt: Long,
        seenAt: Long,
    ): String {
        val reelsForm = mapOf(storyMediaId to listOf(takenAt.toString() + "_" + seenAt))
        val form = mutableMapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "container_module" to "feed_timeline",
            "reels" to reelsForm,
        )
        val signedForm = Utils.sign(form)
        val queryMap = mapOf(
            "reel" to "1",
            "live_vod" to "0",
        )
        return service.seen(queryMap, signedForm)
    }

    private fun sort(list: List<Story>): List<Story> {
        val listCopy = ArrayList(list)
        listCopy.sortWith { o1, o2 ->
            if (o1.latestReelMedia == null || o2.latestReelMedia == null) return@sortWith 0
            else when (Utils.settingsHelper.getString(PreferenceKeys.STORY_SORT)) {
                "1" -> return@sortWith o2.latestReelMedia.compareTo(o1.latestReelMedia)
                "2" -> return@sortWith o1.latestReelMedia.compareTo(o2.latestReelMedia)
                else -> return@sortWith 0
            }
        }
        return listCopy
    }

    companion object {
        @Volatile
        private var INSTANCE: StoriesRepository? = null

        fun getInstance(): StoriesRepository {
            return INSTANCE ?: synchronized(this) {
                val service: StoriesService = retrofit.create(StoriesService::class.java)
                StoriesRepository(service).also { INSTANCE = it }
            }
        }
    }
}