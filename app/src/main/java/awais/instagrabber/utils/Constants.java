package awais.instagrabber.utils;

public final class Constants {
    // string prefs
    public static final String FOLDER_PATH = "custom_path";
    public static final String DATE_TIME_FORMAT = "date_time_format";
    public static final String DATE_TIME_SELECTION = "date_time_selection";
    public static final String CUSTOM_DATE_TIME_FORMAT = "date_time_custom_format";
    // int prefs
    public static final String APP_THEME = "app_theme";
    public static final String APP_LANGUAGE = "app_language";
    public static final String PREV_INSTALL_VERSION = "prevVersion";
    // boolean prefs
    public static final String DOWNLOAD_USER_FOLDER = "download_user_folder";
    public static final String BOTTOM_TOOLBAR = "bottom_toolbar";
    public static final String FOLDER_SAVE_TO = "saved_to";
    public static final String AUTOPLAY_VIDEOS = "autoplay_videos";
    public static final String MUTED_VIDEOS = "muted_videos";
    public static final String AUTOLOAD_POSTS = "autoload_posts";
    public static final String SHOW_FEED = "show_feed";
    public static final String CUSTOM_DATE_TIME_FORMAT_ENABLED = "data_time_custom_enabled";
    // never Export
    public static final String COOKIE = "cookie";
    public static final String SHOW_QUICK_ACCESS_DIALOG = "show_quick_dlg";
    public static final String DEVICE_UUID = "device_uuid";
    //////////////////////// EXTRAS ////////////////////////
    public static final String EXTRAS_USER = "user";
    public static final String EXTRAS_HASHTAG = "hashtag";
    public static final String EXTRAS_LOCATION = "location";
    public static final String EXTRAS_USERNAME = "username";
    public static final String EXTRAS_ID = "id";
    public static final String EXTRAS_POST = "post";
    public static final String EXTRAS_PROFILE = "profile";
    public static final String EXTRAS_TYPE = "type";
    public static final String EXTRAS_NAME = "name";
    public static final String EXTRAS_STORIES = "stories";
    public static final String EXTRAS_HIGHLIGHT = "highlight";
    public static final String EXTRAS_INDEX = "index";
    public static final String EXTRAS_THREAD_MODEL = "threadModel";
    public static final String EXTRAS_FOLLOWERS = "followers";
    public static final String EXTRAS_SHORTCODE = "shortcode";
    public static final String EXTRAS_END_CURSOR = "endCursor";
    public static final String FEED = "feed";
    public static final String FEED_ORDER = "feedOrder";
    // spoof
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 8.1.0; motorola one Build/OPKS28.63-18-3; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/70.0.3538.80 Mobile Safari/537.36 " +
            "Instagram 152.0.0.25.117 Android (27/8.1.0; 320dpi; 720x1362; motorola; motorola one; deen_sprout; qcom; pt_BR; 234847224)";
    public static final String I_USER_AGENT =
            "Instagram 152.0.0.25.117 Android (27/8.1.0; 320dpi; 720x1362; motorola; motorola one; deen_sprout; qcom; pt_BR; 234847224)";
    // see https://github.com/dilame/instagram-private-api/blob/master/src/core/constants.ts
    public static final String SUPPORTED_CAPABILITIES = "[ { \"name\": \"SUPPORTED_SDK_VERSIONS\", \"value\":" +
            " \"13.0,14.0,15.0,16.0,17.0,18.0,19.0,20.0,21.0,22.0,23.0,24.0,25.0,26.0,27.0,28.0,29.0,30.0,31.0," +
            "32.0,33.0,34.0,35.0,36.0,37.0,38.0,39.0,40.0,41.0,42.0,43.0,44.0,45.0,46.0,47.0,48.0,49.0,50.0,51.0," +
            "52.0,53.0,54.0,55.0,56.0,57.0,58.0,59.0,60.0,61.0,62.0,63.0,64.0,65.0,66.0\" }, { \"name\": \"FACE_TRACKER_VERSION\", " +
            "\"value\": 12 }, { \"name\": \"segmentation\", \"value\": \"segmentation_enabled\" }, { \"name\": \"COMPRESSION\", " +
            "\"value\": \"ETC2_COMPRESSION\" }, { \"name\": \"world_tracker\", \"value\": \"world_tracker_enabled\" }, { \"name\": " +
            "\"gyroscope\", \"value\": \"gyroscope_enabled\" } ]";
    public static final String SIGNATURE_VERSION = "4";
    public static final String SIGNATURE_KEY = "9193488027538fd3450b83b7d05286d4ca9599a0f7eeed90d8c85925698a05dc";
}