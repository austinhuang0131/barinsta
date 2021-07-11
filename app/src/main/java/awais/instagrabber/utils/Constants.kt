package awais.instagrabber.utils

object Constants {
    const val CRASH_REPORT_EMAIL = "barinsta@austinhuang.me"

    // int prefs, do not export
    const val PREV_INSTALL_VERSION = "prevVersion"
    const val BROWSER_UA_CODE = "browser_ua_code"
    const val APP_UA_CODE = "app_ua_code"

    // never Export
    const val COOKIE = "cookie"

    // deprecated: public static final String SHOW_QUICK_ACCESS_DIALOG = "show_quick_dlg";
    const val DEVICE_UUID = "device_uuid"
    const val BROWSER_UA = "browser_ua"
    const val APP_UA = "app_ua"

    //////////////////////// EXTRAS ////////////////////////
    const val EXTRAS_USER = "user"
    const val EXTRAS_HASHTAG = "hashtag"
    const val EXTRAS_LOCATION = "location"
    const val EXTRAS_USERNAME = "username"
    const val EXTRAS_ID = "id"
    const val EXTRAS_POST = "post"
    const val EXTRAS_PROFILE = "profile"
    const val EXTRAS_TYPE = "type"
    const val EXTRAS_NAME = "name"
    const val EXTRAS_STORIES = "stories"
    const val EXTRAS_HIGHLIGHT = "highlight"
    const val EXTRAS_INDEX = "index"
    const val EXTRAS_THREAD_MODEL = "threadModel"
    const val EXTRAS_FOLLOWERS = "followers"
    const val EXTRAS_SHORTCODE = "shortcode"
    const val EXTRAS_END_CURSOR = "endCursor"
    const val FEED = "feed"
    const val FEED_ORDER = "feedOrder"

    // Notification ids
    const val ACTIVITY_NOTIFICATION_ID = 10
    const val DM_UNREAD_PARENT_NOTIFICATION_ID = 20
    const val DM_CHECK_NOTIFICATION_ID = 11

    // see https://github.com/dilame/instagram-private-api/blob/master/src/core/constants.ts
    //    public static final String SUPPORTED_CAPABILITIES = "[ { \"name\": \"SUPPORTED_SDK_VERSIONS\", \"value\":" +
    //            " \"13.0,14.0,15.0,16.0,17.0,18.0,19.0,20.0,21.0,22.0,23.0,24.0,25.0,26.0,27.0,28.0,29.0,30.0,31.0," +
    //            "32.0,33.0,34.0,35.0,36.0,37.0,38.0,39.0,40.0,41.0,42.0,43.0,44.0,45.0,46.0,47.0,48.0,49.0,50.0,51.0," +
    //            "52.0,53.0,54.0,55.0,56.0,57.0,58.0,59.0,60.0,61.0,62.0,63.0,64.0,65.0,66.0\" }, { \"name\": \"FACE_TRACKER_VERSION\", " +
    //            "\"value\": 12 }, { \"name\": \"segmentation\", \"value\": \"segmentation_enabled\" }, { \"name\": \"COMPRESSION\", " +
    //            "\"value\": \"ETC2_COMPRESSION\" }, { \"name\": \"world_tracker\", \"value\": \"world_tracker_enabled\" }, { \"name\": " +
    //            "\"gyroscope\", \"value\": \"gyroscope_enabled\" } ]";
    //    public static final String SIGNATURE_VERSION = "4";
    //    public static final String SIGNATURE_KEY = "9193488027538fd3450b83b7d05286d4ca9599a0f7eeed90d8c85925698a05dc";
    const val BREADCRUMB_KEY = "iN4\$aGr0m"
    const val LOGIN_RESULT_CODE = 5000
    const val SKIPPED_VERSION = "skipped_version"
    const val DEFAULT_TAB = "default_tab"
    const val PREF_DARK_THEME = "dark_theme"
    const val PREF_LIGHT_THEME = "light_theme"
    const val DEFAULT_HASH_TAG_PIC = "https://www.instagram.com/static/images/hashtag/search-hashtag-default-avatar.png/1d8417c9a4f5.png"
    const val SHARED_PREFERENCES_NAME = "settings"
    const val PREF_POSTS_LAYOUT = "posts_layout"
    const val PREF_PROFILE_POSTS_LAYOUT = "profile_posts_layout"
    const val PREF_TOPIC_POSTS_LAYOUT = "topic_posts_layout"
    const val PREF_HASHTAG_POSTS_LAYOUT = "hashtag_posts_layout"
    const val PREF_LOCATION_POSTS_LAYOUT = "location_posts_layout"
    const val PREF_LIKED_POSTS_LAYOUT = "liked_posts_layout"
    const val PREF_TAGGED_POSTS_LAYOUT = "tagged_posts_layout"
    const val PREF_SAVED_POSTS_LAYOUT = "saved_posts_layout"
    const val PREF_EMOJI_VARIANTS = "emoji_variants"
    const val PREF_REACTIONS = "reactions"
    const val ACTIVITY_CHANNEL_ID = "activity"
    const val ACTIVITY_CHANNEL_NAME = "Activity"
    const val DOWNLOAD_CHANNEL_ID = "download"
    const val DOWNLOAD_CHANNEL_NAME = "Downloads"
    const val DM_UNREAD_CHANNEL_ID = "dmUnread"
    const val DM_UNREAD_CHANNEL_NAME = "Messages"
    const val SILENT_NOTIFICATIONS_CHANNEL_ID = "silentNotifications"
    const val SILENT_NOTIFICATIONS_CHANNEL_NAME = "Silent notifications"
    const val NOTIF_GROUP_NAME = "awais.instagrabber.InstaNotif"
    const val GROUP_KEY_DM = "awais.instagrabber.MESSAGES"
    const val GROUP_KEY_SILENT_NOTIFICATIONS = "awais.instagrabber.SILENT_NOTIFICATIONS"
    const val SHOW_ACTIVITY_REQUEST_CODE = 1738
    const val SHOW_DM_THREAD = 2000
    const val DM_SYNC_SERVICE_REQUEST_CODE = 3000
    const val GLOBAL_NETWORK_ERROR_DIALOG_REQUEST_CODE = 7777
    const val ACTION_SHOW_ACTIVITY = "show_activity"
    const val ACTION_SHOW_DM_THREAD = "show_dm_thread"
    const val DM_THREAD_ACTION_EXTRA_THREAD_ID = "thread_id"
    const val DM_THREAD_ACTION_EXTRA_THREAD_TITLE = "thread_title"
    const val X_IG_APP_ID = "936619743392459"
    const val EXTRA_INITIAL_URI = "initial_uri"
    const val defaultDateTimeFormat = "hh:mm:ss a 'on' dd-MM-yyyy"
}