package awais.instagrabber

import android.app.Application
import android.content.ClipboardManager
import android.util.Log
import awais.instagrabber.fragments.settings.PreferenceKeys.CUSTOM_DATE_TIME_FORMAT
import awais.instagrabber.fragments.settings.PreferenceKeys.CUSTOM_DATE_TIME_FORMAT_ENABLED
import awais.instagrabber.fragments.settings.PreferenceKeys.DATE_TIME_FORMAT
import awais.instagrabber.utils.*
import awais.instagrabber.utils.LocaleUtils.currentLocale
import awais.instagrabber.utils.Utils.settingsHelper
import awais.instagrabber.utils.extensions.TAG
import awaisomereport.CrashReporter
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import java.net.CookieHandler
import java.time.format.DateTimeFormatter
import java.util.*

@Suppress("unused")
class InstaGrabberApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CookieHandler.setDefault(NET_COOKIE_MANAGER)
        settingsHelper = SettingsHelper(this)
        setupCrashReporter()
        setupCloseGuard()
        setupFresco()
        Utils.cacheDir = cacheDir.absolutePath
        Utils.clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        LocaleUtils.setLocale(baseContext)
        val pattern = if (settingsHelper.getBoolean(CUSTOM_DATE_TIME_FORMAT_ENABLED)) {
            settingsHelper.getString(CUSTOM_DATE_TIME_FORMAT)
        } else {
            settingsHelper.getString(DATE_TIME_FORMAT)
        }
        TextUtils.setFormatter(DateTimeFormatter.ofPattern(pattern, currentLocale))
        if (TextUtils.isEmpty(settingsHelper.getString(Constants.DEVICE_UUID))) {
            settingsHelper.putString(Constants.DEVICE_UUID, UUID.randomUUID().toString())
        }
    }

    private fun setupCrashReporter() {
        if (BuildConfig.DEBUG) return
        CrashReporter.getInstance(this).start()
    }

    private fun setupCloseGuard() {
        if (!BuildConfig.DEBUG) return
        try {
            Class.forName("dalvik.system.CloseGuard")
                .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
    }

    private fun setupFresco() {
        // final Set<RequestListener> requestListeners = new HashSet<>();
        // requestListeners.add(new RequestLoggingListener());
        val imagePipelineConfig = ImagePipelineConfig
            .newBuilder(this) // .setMainDiskCacheConfig(diskCacheConfig)
            // .setRequestListeners(requestListeners)
            .setDownsampleEnabled(true)
            .build()
        Fresco.initialize(this, imagePipelineConfig)
        // FLog.setMinimumLoggingLevel(FLog.VERBOSE);
    }
}