package awais.instagrabber.models.enums

enum class BroadcastItemType(val value: String) {
    TEXT("text"),
    REACTION("reaction"),
    REELSHARE("reel_share"),
    IMAGE("configure_photo"),
    LINK("link"),
    VIDEO("configure_video"),
    VOICE("share_voice"),
    ANIMATED_MEDIA("animated_media"),
    MEDIA_SHARE("media_share"),
    PROFILE("profile"),
    STORY("story_share"), // not reply
}