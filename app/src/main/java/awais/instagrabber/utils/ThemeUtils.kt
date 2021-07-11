@file:JvmName("ThemeUtils")

package awais.instagrabber.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import awais.instagrabber.R

object ThemeUtils {
    fun changeTheme(context: Context) {
        var themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // this is fallback / default
        if (Utils.settingsHelper != null) themeCode = Utils.settingsHelper.getThemeCode(false)
        if (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && Build.VERSION.SDK_INT < 29) {
            themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
        val isNight = isNight(context, themeCode)
        val themeResName =
            if (isNight) Utils.settingsHelper.getString(Constants.PREF_DARK_THEME) else Utils.settingsHelper.getString(
                Constants.PREF_LIGHT_THEME
            )
        val themeResId = context.resources.getIdentifier(themeResName, "style", context.packageName)
        val finalThemeResId: Int
        finalThemeResId = if (themeResId <= 0) {
            // Nothing set in settings
            if (isNight) R.style.AppTheme_Dark_Black else R.style.AppTheme_Light_White
        } else themeResId
        // Log.d(TAG, "changeTheme: finalThemeResId: " + finalThemeResId);
        context.setTheme(finalThemeResId)
    }

    fun isNight(context: Context, themeCode: Int): Boolean {
        // check if setting is set to 'Dark'
        var isNight = themeCode == AppCompatDelegate.MODE_NIGHT_YES
        // if not dark check if themeCode is MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_AUTO_BATTERY
        if (!isNight && (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || themeCode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)) {
            // check if resulting theme would be NIGHT
            val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            isNight = uiMode == Configuration.UI_MODE_NIGHT_YES
        }
        return isNight
    }
}