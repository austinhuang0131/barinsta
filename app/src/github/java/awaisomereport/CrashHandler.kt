package awaisomereport

import android.app.Application
import awais.instagrabber.BuildConfig
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.utils.Utils
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions.BeforeSendCallback
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions

class CrashHandler(private val application: Application) : ICrashHandler {
    private var enabled = false

    init {
        enabled = if (!Utils.settingsHelper.hasPreference(PreferenceKeys.PREF_ENABLE_SENTRY)) {
            // disabled by default (change to true if we need enabled by default)
            false
        } else {
            Utils.settingsHelper.getBoolean(PreferenceKeys.PREF_ENABLE_SENTRY)
        }
        if (enabled) {
            SentryAndroid.init(application) { options: SentryAndroidOptions ->
                options.dsn = BuildConfig.dsn
                options.setDiagnosticLevel(SentryLevel.ERROR)
                options.beforeSend = BeforeSendCallback { event: SentryEvent, _: Any? ->
                    // Removing unneeded info from event
                    event.contexts.device?.apply {
                        name = null
                        timezone = null
                        isCharging = null
                        bootTime = null
                        freeStorage = null
                        batteryTemperature = null
                    }
                    event
                }
            }
        }
    }

    override fun uncaughtException(
        t: Thread,
        exception: Throwable,
        defaultExceptionHandler: Thread.UncaughtExceptionHandler
    ) {
        // When enabled, Sentry auto captures unhandled exceptions
        if (!enabled) {
            CrashReporterHelper.startErrorReporterActivity(application, exception)
        }
        defaultExceptionHandler.uncaughtException(t, exception)
    }
}