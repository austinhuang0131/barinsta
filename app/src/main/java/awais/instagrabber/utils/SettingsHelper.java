package awais.instagrabber.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import androidx.appcompat.app.AppCompatDelegate;

import static awais.instagrabber.utils.Constants.APP_LANGUAGE;
import static awais.instagrabber.utils.Constants.APP_THEME;
import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
import static awais.instagrabber.utils.Constants.AUTOPLAY_VIDEOS;
import static awais.instagrabber.utils.Constants.BOTTOM_TOOLBAR;
import static awais.instagrabber.utils.Constants.COOKIE;
import static awais.instagrabber.utils.Constants.CUSTOM_DATE_TIME_FORMAT;
import static awais.instagrabber.utils.Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED;
import static awais.instagrabber.utils.Constants.DATE_TIME_FORMAT;
import static awais.instagrabber.utils.Constants.DATE_TIME_SELECTION;
import static awais.instagrabber.utils.Constants.DEVICE_UUID;
import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Constants.MARK_AS_SEEN;
import static awais.instagrabber.utils.Constants.MUTED_VIDEOS;
import static awais.instagrabber.utils.Constants.PREV_INSTALL_VERSION;
import static awais.instagrabber.utils.Constants.SHOW_QUICK_ACCESS_DIALOG;

public final class SettingsHelper {
    private final SharedPreferences sharedPreferences;

    public SettingsHelper(@NonNull final Context context) {
        this.sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @NonNull
    public String getString(@StringSettings final String key) {
        final String stringDefault = getStringDefault(key);
        if (sharedPreferences != null) return sharedPreferences.getString(key, stringDefault);
        return stringDefault;
    }

    public int getInteger(@IntegerSettings final String key) {
        final int integerDefault = getIntegerDefault(key);
        if (sharedPreferences != null) return sharedPreferences.getInt(key, integerDefault);
        return integerDefault;
    }

    public boolean getBoolean(@BooleanSettings final String key) {
        final boolean booleanDefault = getBooleanDefault(key);
        if (sharedPreferences != null) return sharedPreferences.getBoolean(key, booleanDefault);
        return booleanDefault;
    }

    @NonNull
    private String getStringDefault(@StringSettings final String key) {
        if (DATE_TIME_FORMAT.equals(key))
            return "hh:mm:ss a 'on' dd-MM-yyyy";
        if (DATE_TIME_SELECTION.equals(key))
            return "0;3;0";
        return "";
    }

    private int getIntegerDefault(@IntegerSettings final String key) {
        if (APP_THEME.equals(key)) return getThemeCode(true);
        if (PREV_INSTALL_VERSION.equals(key)) return -1;
        return 0;
    }

    private boolean getBooleanDefault(@BooleanSettings final String key) {
        return BOTTOM_TOOLBAR.equals(key) ||
                AUTOPLAY_VIDEOS.equals(key) ||
                SHOW_QUICK_ACCESS_DIALOG.equals(key) ||
                MUTED_VIDEOS.equals(key);
    }

    public int getThemeCode(final boolean fromHelper) {
        int themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

        if (!fromHelper && sharedPreferences != null) {
            themeCode = sharedPreferences.getInt(APP_THEME, themeCode);
            if (themeCode == 1) themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
            else if (themeCode == 3) themeCode = AppCompatDelegate.MODE_NIGHT_NO;
            else if (themeCode == 0) themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

        if (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && Build.VERSION.SDK_INT < 29)
            themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;

        return themeCode;
    }

    public void putString(@StringSettings final String key, final String val) {
        if (sharedPreferences != null) sharedPreferences.edit().putString(key, val).apply();
    }

    public void putInteger(@IntegerSettings final String key, final int val) {
        if (sharedPreferences != null) sharedPreferences.edit().putInt(key, val).apply();
    }

    public void putBoolean(@BooleanSettings final String key, final boolean val) {
        if (sharedPreferences != null) sharedPreferences.edit().putBoolean(key, val).apply();
    }

    @StringDef({COOKIE, FOLDER_PATH, DATE_TIME_FORMAT, DATE_TIME_SELECTION, CUSTOM_DATE_TIME_FORMAT, DEVICE_UUID})
    public @interface StringSettings {}

    @StringDef({DOWNLOAD_USER_FOLDER, BOTTOM_TOOLBAR, FOLDER_SAVE_TO, AUTOPLAY_VIDEOS, SHOW_QUICK_ACCESS_DIALOG, MUTED_VIDEOS,
            AUTOLOAD_POSTS, CUSTOM_DATE_TIME_FORMAT_ENABLED, MARK_AS_SEEN})
    public @interface BooleanSettings {}

    @StringDef({APP_THEME, APP_LANGUAGE, PREV_INSTALL_VERSION})
    public @interface IntegerSettings {}
}