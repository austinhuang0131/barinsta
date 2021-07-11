package awaisomereport

import android.app.Application

class CrashHandler(private val application: Application) : ICrashHandler {
    override fun uncaughtException(
        t: Thread,
        exception: Throwable,
        defaultExceptionHandler: Thread.UncaughtExceptionHandler
    ) {
        CrashReporterHelper.startErrorReporterActivity(application, exception)
        defaultExceptionHandler.uncaughtException(t, exception)
    }
}