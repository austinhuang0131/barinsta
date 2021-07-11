package awais.instagrabber.repositories.responses.stories

import java.io.Serializable
import awais.instagrabber.repositories.responses.Hashtag
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.User

data class StorySticker(
    // only ONE object should exist
    val user: User?, // reel_mentions
    val hashtag: Hashtag?, // story_hashtags
    val location: Location?, // story_locations
    val mediaId: String?, // story_feed_media
    val pollSticker: PollSticker?, // story_polls
    val questionSticker: QuestionSticker?, // story_questions
    val quizSticker: QuizSticker?, // story_quizs
    val links: List<StoryCta?>?, // story_cta, requires link_text from the story
    val sliderSticker: SliderSticker? // story_sliders
) : Serializable