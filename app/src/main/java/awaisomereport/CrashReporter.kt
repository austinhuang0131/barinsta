package awaisomereport

import android.app.Application

class CrashReporter private constructor(application: Application) : Thread.UncaughtExceptionHandler {

    private val crashHandler: CrashHandler?
    private var startAttempted = false
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    init {
        crashHandler = CrashHandler(application)
    }

    fun start() {
        if (startAttempted) return
        startAttempted = true
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, exception: Throwable) {
        if (crashHandler == null) {
            defaultExceptionHandler?.uncaughtException(t, exception)
            return
        }
        crashHandler.uncaughtException(t, exception, defaultExceptionHandler ?: return)
    }

    companion object {
        @Volatile
        private var INSTANCE: CrashReporter? = null

        fun getInstance(application: Application): CrashReporter {
            return INSTANCE ?: synchronized(this) {
                CrashReporter(application).also { INSTANCE = it }
            }
        }
    }
}