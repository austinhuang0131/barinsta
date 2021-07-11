package awais.instagrabber.utils

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import awais.instagrabber.fragments.settings.PreferenceKeys
import java.util.*

// taken from my app TESV Console Codes
object LocaleUtils {
    private lateinit var defaultLocale: Locale

    @JvmStatic
    lateinit var currentLocale: Locale
        private set

    @JvmStatic
    fun setLocale(baseContext: Context) {
        var baseContext1 = baseContext
        defaultLocale = Locale.getDefault()
        if (baseContext1 is ContextThemeWrapper) baseContext1 = baseContext1.baseContext
        if (Utils.settingsHelper == null) Utils.settingsHelper = SettingsHelper(baseContext1)
        val appLanguageSettings = Utils.settingsHelper.getString(PreferenceKeys.APP_LANGUAGE)
        val lang = getCorrespondingLanguageCode(appLanguageSettings)
        currentLocale = when {
            TextUtils.isEmpty(lang) -> defaultLocale
            lang!!.contains("_") -> {
                val split = lang.split("_")
                Locale(split[0], split[1])
            }
            else -> Locale(lang)
        }
        currentLocale.let {
            Locale.setDefault(it)
            val res = baseContext1.resources
            val config = res.configuration
            // config.locale = currentLocale
            config.setLocale(it)
            config.setLayoutDirection(it)
            res.updateConfiguration(config, res.displayMetrics)
        }
    }

    @JvmStatic
    fun updateConfig(wrapper: ContextThemeWrapper) {
        if (!this::currentLocale.isInitialized) return
        val configuration = Configuration()
        // configuration.locale = currentLocale
        configuration.setLocale(currentLocale)
        wrapper.applyOverrideConfiguration(configuration)
    }

    fun getCorrespondingLanguageCode(appLanguageSettings: String): String? {
        if (TextUtils.isEmpty(appLanguageSettings)) return null
        when (appLanguageSettings.toInt()) {
            1 -> return "en"
            2 -> return "fr"
            3 -> return "es"
            4 -> return "zh_CN"
            5 -> return "in"
            6 -> return "it"
            7 -> return "de"
            8 -> return "pl"
            9 -> return "tr"
            10 -> return "pt"
            11 -> return "fa"
            12 -> return "mk"
            13 -> return "vi"
            14 -> return "zh_TW"
            15 -> return "ca"
            16 -> return "ru"
            17 -> return "hi"
            18 -> return "nl"
            19 -> return "sk"
            20 -> return "ja"
            21 -> return "el"
            22 -> return "eu"
            23 -> return "sv"
            24 -> return "ko"
            25 -> return "ar"
        }
        return null
    }
}