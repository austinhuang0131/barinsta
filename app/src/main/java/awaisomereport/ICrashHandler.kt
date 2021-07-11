package awaisomereport

interface ICrashHandler {
    fun uncaughtException(
        t: Thread,
        exception: Throwable,
        defaultExceptionHandler: Thread.UncaughtExceptionHandler
    )
}