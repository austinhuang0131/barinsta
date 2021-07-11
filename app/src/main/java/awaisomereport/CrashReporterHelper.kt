package awaisomereport

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import awais.instagrabber.BuildConfig
import awais.instagrabber.R
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.extensions.TAG
import java.io.*
import java.time.LocalDateTime

object CrashReporterHelper {
    private val shortBorder = "=".repeat(14)
    private val longBorder = "=".repeat(21)
    private const val prefix = "stack-"
    private const val suffix = ".stacktrace"

    fun startErrorReporterActivity(
        application: Application,
        exception: Throwable
    ) {
        try {
            application.openFileOutput(
                "$prefix${System.currentTimeMillis()}$suffix",
                Context.MODE_PRIVATE
            ).use { it.write(getReportContent(exception).toByteArray()) }
        } catch (ex: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "", ex)
        }
        application.startActivity(Intent(application, ErrorReporterActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun getReportContent(exception: Throwable): String {
        var reportContent =
            """
                IMPORTANT: If sending by email, your email address and the entire content will be made public at
                IMPORTANT: https://github.com/austinhuang0131/barinsta/issues
                IMPORTANT: When possible, please describe the steps leading to this crash. Thank you for your cooperation.

                Error report collected on: ${LocalDateTime.now()}

                Information:
                $shortBorder
                VERSION		    : ${BuildConfig.VERSION_NAME}
                VERSION_CODE	: ${BuildConfig.VERSION_CODE}
                PHONE-MODEL	    : ${Build.MODEL}
                ANDROID_VERS	: ${Build.VERSION.RELEASE}
                ANDROID_REL	    : ${Build.VERSION.SDK_INT}
                BRAND			: ${Build.BRAND}
                MANUFACTURER	: ${Build.MANUFACTURER}
                BOARD			: ${Build.BOARD}
                DEVICE			: ${Build.DEVICE}
                PRODUCT		    : ${Build.PRODUCT}
                HOST			: ${Build.HOST}
                TAGS			: ${Build.TAGS}

                Stack:
                $shortBorder
            """.trimIndent()
        reportContent = "$reportContent${getStackStrace(exception)}\n\n*** End of current Report ***"
        return reportContent.replace("\n", "\r\n")
    }

    private fun getStackStrace(exception: Throwable): String {
        val writer = StringWriter()
        return PrintWriter(writer).use {
            val reportBuilder = StringBuilder("\n")
            exception.printStackTrace(it)
            reportBuilder.append(writer.toString())
            var cause = exception.cause
            if (cause != null) reportBuilder.append("\nCause:\n$shortBorder")
            while (cause != null) {
                cause.printStackTrace(it)
                reportBuilder.append(writer.toString())
                cause = cause.cause
            }
            return@use reportBuilder.toString()
        }
    }

    @JvmStatic
    fun startCrashEmailIntent(context: Context) {
        try {
            val filePath = context.filesDir.absolutePath
            val errorFileList: Array<String>? = try {
                val dir = File(filePath)
                if (dir.exists() && !dir.isDirectory) {
                    dir.delete()
                }
                dir.mkdirs()
                dir.list { _: File?, name: String -> name.endsWith(suffix) }
            } catch (e: Exception) {
                null
            }
            if (errorFileList == null || errorFileList.isEmpty()) {
                return
            }
            val errorStringBuilder: StringBuilder = StringBuilder("\n\n")
            val maxSendMail = 5
            for ((curIndex, curString) in errorFileList.withIndex()) {
                val file = File("$filePath/$curString")
                if (curIndex <= maxSendMail) {
                    errorStringBuilder.append("New Trace collected:\n$longBorder\n")
                    BufferedReader(FileReader(file)).use { input ->
                        var line: String?
                        while (input.readLine().also { line = it } != null) errorStringBuilder.append(line).append("\n")
                    }
                }
                file.delete()
            }
            errorStringBuilder.append("\n\n")
            val resources = context.resources
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.CRASH_REPORT_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.crash_report_subject))
                        putExtra(Intent.EXTRA_TEXT, errorStringBuilder.toString().replace("\n", "\r\n"))
                    },
                    context.resources.getString(R.string.crash_report_title)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
    }

    @JvmStatic
    fun deleteAllStacktraceFiles(context: Context) {
        val filePath = context.filesDir.absolutePath
        val errorFileList: Array<File>? = try {
            val dir = File(filePath)
            if (dir.exists() && !dir.isDirectory) {
                dir.delete()
            }
            dir.mkdirs()
            dir.listFiles { _: File?, name: String -> name.endsWith(suffix) }
        } catch (e: Exception) {
            null
        }
        errorFileList?.forEach { it.delete() }
    }
}