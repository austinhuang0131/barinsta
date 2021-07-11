package awais.instagrabber.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringDef
import androidx.appcompat.app.AppCompatDelegate
import awais.instagrabber.fragments.settings.PreferenceKeys
import java.util.*

class SettingsHelper(context: Context) {
    private val sharedPreferences: SharedPreferences? = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getString(@StringSettings key: String): String {
        val stringDefault = getStringDefault(key)
        return sharedPreferences?.getString(
            key,
            stringDefault
        ) ?: stringDefault
    }

    fun getStringSet(@StringSetSettings key: String?): Set<String> {
        val stringSetDefault: Set<String> = HashSet()
        return sharedPreferences?.getStringSet(
            key,
            stringSetDefault
        ) ?: stringSetDefault
    }

    fun getInteger(@IntegerSettings key: String): Int {
        val integerDefault = getIntegerDefault(key)
        return sharedPreferences?.getInt(key, integerDefault) ?: integerDefault
    }

    fun getBoolean(@BooleanSettings key: String?): Boolean {
        return sharedPreferences?.getBoolean(key, false) ?: false
    }

    private fun getStringDefault(@StringSettings key: String): String {
        if (PreferenceKeys.DATE_TIME_FORMAT == key) {
            return Constants.defaultDateTimeFormat
        }
        return if (PreferenceKeys.DATE_TIME_SELECTION == key) "0;3;0" else ""
    }

    private fun getIntegerDefault(@IntegerSettings key: String): Int {
        if (PreferenceKeys.APP_THEME == key) return getThemeCode(true)
        return if (Constants.PREV_INSTALL_VERSION == key || Constants.APP_UA_CODE == key || Constants.BROWSER_UA_CODE == key) -1 else 0
    }

    fun getThemeCode(fromHelper: Boolean): Int {
        var themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        if (!fromHelper && sharedPreferences != null) {
            themeCode = sharedPreferences.getString(PreferenceKeys.APP_THEME, themeCode.toString())?.toInt() ?: 0
            when (themeCode) {
                1 -> themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                3 -> themeCode = AppCompatDelegate.MODE_NIGHT_NO
                0 -> themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
        if (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && Build.VERSION.SDK_INT < 29) {
            themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
        return themeCode
    }

    fun putString(@StringSettings key: String?, `val`: String?) {
        sharedPreferences?.edit()?.putString(key, `val`)?.apply()
    }

    fun putStringSet(@StringSetSettings key: String?, `val`: Set<String?>?) {
        sharedPreferences?.edit()?.putStringSet(key, `val`)?.apply()
    }

    fun putInteger(@IntegerSettings key: String?, `val`: Int) {
        sharedPreferences?.edit()?.putInt(key, `val`)?.apply()
    }

    fun putBoolean(@BooleanSettings key: String?, `val`: Boolean) {
        sharedPreferences?.edit()?.putBoolean(key, `val`)?.apply()
    }

    fun hasPreference(key: String?): Boolean {
        return sharedPreferences?.contains(key) ?: false
    }

    @StringDef(
        PreferenceKeys.APP_LANGUAGE,
        PreferenceKeys.APP_THEME,
        Constants.APP_UA,
        Constants.BROWSER_UA,
        Constants.COOKIE,
        PreferenceKeys.FOLDER_PATH,
        PreferenceKeys.DATE_TIME_FORMAT,
        PreferenceKeys.DATE_TIME_SELECTION,
        PreferenceKeys.CUSTOM_DATE_TIME_FORMAT,
        Constants.DEVICE_UUID,
        Constants.SKIPPED_VERSION,
        Constants.DEFAULT_TAB,
        Constants.PREF_DARK_THEME,
        Constants.PREF_LIGHT_THEME,
        Constants.PREF_POSTS_LAYOUT,
        Constants.PREF_PROFILE_POSTS_LAYOUT,
        Constants.PREF_TOPIC_POSTS_LAYOUT,
        Constants.PREF_HASHTAG_POSTS_LAYOUT,
        Constants.PREF_LOCATION_POSTS_LAYOUT,
        Constants.PREF_LIKED_POSTS_LAYOUT,
        Constants.PREF_TAGGED_POSTS_LAYOUT,
        Constants.PREF_SAVED_POSTS_LAYOUT,
        PreferenceKeys.STORY_SORT,
        Constants.PREF_EMOJI_VARIANTS,
        Constants.PREF_REACTIONS,
        PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH_FREQ_UNIT,
        PreferenceKeys.PREF_TAB_ORDER,
        PreferenceKeys.PREF_BARINSTA_DIR_URI
    )
    annotation class StringSettings

    @StringDef(
        PreferenceKeys.DOWNLOAD_USER_FOLDER,
        PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME,
        PreferenceKeys.AUTOPLAY_VIDEOS_STORIES,
        PreferenceKeys.MUTED_VIDEOS,
//        PreferenceKeys.SHOW_CAPTIONS,
        PreferenceKeys.CUSTOM_DATE_TIME_FORMAT_ENABLED,
        PreferenceKeys.MARK_AS_SEEN,
        PreferenceKeys.DM_MARK_AS_SEEN,
        PreferenceKeys.CHECK_ACTIVITY,
        PreferenceKeys.CHECK_UPDATES,
        PreferenceKeys.SWAP_DATE_TIME_FORMAT_ENABLED,
        PreferenceKeys.PREF_ENABLE_DM_NOTIFICATIONS,
        PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH,
        PreferenceKeys.FLAG_SECURE,
        PreferenceKeys.TOGGLE_KEYWORD_FILTER,
        PreferenceKeys.PREF_ENABLE_SENTRY,
        PreferenceKeys.HIDE_MUTED_REELS,
        PreferenceKeys.PLAY_IN_BACKGROUND,
        PreferenceKeys.PREF_SHOWN_COUNT_TOOLTIP,
        PreferenceKeys.PREF_SEARCH_FOCUS_KEYBOARD,
        PreferenceKeys.PREF_STORY_SHOW_LIST,
        PreferenceKeys.PREF_AUTO_BACKUP_ENABLED
    )
    annotation class BooleanSettings

    @StringDef(
        Constants.PREV_INSTALL_VERSION,
        Constants.BROWSER_UA_CODE,
        Constants.APP_UA_CODE,
        PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH_FREQ_NUMBER
    )
    annotation class IntegerSettings

    @StringDef(PreferenceKeys.KEYWORD_FILTERS)
    annotation class StringSetSettings

}