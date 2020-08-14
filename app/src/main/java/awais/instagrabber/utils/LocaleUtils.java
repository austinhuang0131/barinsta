package awais.instagrabber.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.ContextThemeWrapper;

import androidx.annotation.Nullable;

import java.util.Locale;

// taken from my app TESV Console Codes
public final class LocaleUtils {
    private static Locale defaultLocale, currentLocale;

    public static void setLocale(Context baseContext) {
        if (defaultLocale == null) defaultLocale = Locale.getDefault();

        if (baseContext instanceof ContextThemeWrapper)
            baseContext = ((ContextThemeWrapper) baseContext).getBaseContext();

        final String lang = LocaleUtils.getCorrespondingLanguageCode(baseContext);

        currentLocale = Utils.isEmpty(lang) ? defaultLocale : new Locale(lang);
        Locale.setDefault(currentLocale);

        final Resources res = baseContext.getResources();
        final Configuration config = res.getConfiguration();

        config.locale = currentLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(currentLocale);
            config.setLayoutDirection(currentLocale);
        }

        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void updateConfig(final ContextThemeWrapper wrapper) {
        if (currentLocale != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final Configuration configuration = new Configuration();
            configuration.locale = currentLocale;
            configuration.setLocale(currentLocale);
            wrapper.applyOverrideConfiguration(configuration);
        }
    }

    @Nullable
    private static String getCorrespondingLanguageCode(final Context baseContext) {
        if (Utils.settingsHelper == null)
            Utils.settingsHelper = new SettingsHelper(baseContext);

        final int appLanguageIndex = Utils.settingsHelper.getInteger(Constants.APP_LANGUAGE);

        if (appLanguageIndex == 1) return "en";
        if (appLanguageIndex == 2) return "fr";
        if (appLanguageIndex == 3) return "es";
        if (appLanguageIndex == 4) return "zh";
        if (appLanguageIndex == 5) return "in";
        if (appLanguageIndex == 6) return "it";
        if (appLanguageIndex == 7) return "de";
        if (appLanguageIndex == 8) return "pl";
        if (appLanguageIndex == 9) return "tr";
        if (appLanguageIndex == 10) return "pt";

        return null;
    }
}