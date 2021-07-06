package awais.instagrabber.workers

import android.app.Notification
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import awais.instagrabber.BuildConfig
import awais.instagrabber.R
import awais.instagrabber.services.DeleteImageIntentService
import awais.instagrabber.utils.BitmapUtils
import awais.instagrabber.utils.Constants.DOWNLOAD_CHANNEL_ID
import awais.instagrabber.utils.Constants.NOTIF_GROUP_NAME
import awais.instagrabber.utils.DownloadUtils
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter
import java.io.BufferedInputStream
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import kotlin.collections.Map
import kotlin.math.abs

class DownloadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    override suspend fun doWork(): Result {
        val downloadRequestFilePath = inputData.getString(KEY_DOWNLOAD_REQUEST_JSON)
        if (downloadRequestFilePath.isNullOrBlank()) {
            return Result.failure(Data.Builder()
                .putString("error", "downloadRequest is empty or null")
                .build())
        }
        val downloadRequestString: String
        val requestFile = Uri.parse(downloadRequestFilePath)
        val context = applicationContext
        val contentResolver = context.contentResolver ?: return Result.failure(Data.Builder()
                .putString("error", "contentResolver is null")
                .build())
        try {
            val scanner = Scanner(contentResolver.openInputStream(requestFile))
            downloadRequestString = scanner.useDelimiter("\\A").next()
        } catch (e: Exception) {
            Log.e(TAG, "doWork: ", e)
            return Result.failure(Data.Builder()
                .putString("error", e.localizedMessage)
                .build())
        }
        if (downloadRequestString.isBlank()) {
            return Result.failure(Data.Builder()
                .putString("error", "downloadRequest is empty")
                .build())
        }
        val downloadRequest: DownloadRequest = try {
            Gson().fromJson(downloadRequestString, DownloadRequest::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "doWork", e)
            return Result.failure(Data.Builder()
                .putString("error", e.localizedMessage)
                .build())
        } ?: return Result.failure(Data.Builder()
            .putString("error", "downloadRequest is null")
            .build())
        val urlToFilePathMap = downloadRequest.urlToFilePathMap
        download(urlToFilePathMap)
        Handler(Looper.getMainLooper()).postDelayed({ showSummary(urlToFilePathMap) }, 500)
        val deleted = DocumentFile.fromSingleUri(context, requestFile)!!.delete()
        if (!deleted) {
            Log.w(TAG, "doWork: requestFile not deleted!")
        }
        return Result.success()
    }

    private suspend fun download(urlToFilePathMap: Map<String, String>) {
        val notificationId = notificationId
        val entries = urlToFilePathMap.entries
        var count = 1
        val total = urlToFilePathMap.size
        for ((url, uriString) in entries) {
            updateDownloadProgress(notificationId, count, total, 0f)
            withContext(Dispatchers.IO) {
                val file = DocumentFile.fromSingleUri(applicationContext, Uri.parse(uriString))
                download(notificationId, count, total, url, file!!)
            }
            count++
        }
    }

    private val notificationId: Int
        get() = abs(id.hashCode())

    private fun download(
        notificationId: Int,
        position: Int,
        total: Int,
        url: String,
        filePath: DocumentFile,
    ) {
        val context = applicationContext.let { it }
        val contentResolver = context.contentResolver?.let { it } ?: return
        val filePathType = filePath.type?.let { it } ?: return
        val isJpg = filePathType.startsWith("image")
        // using temp file approach to remove IPTC so that download progress can be reported
        val outFile = if (isJpg) DownloadUtils.getTempFile(null, "jpg") else filePath
        try {
            val urlConnection = URL(url).openConnection()
            val fileSize = if (Build.VERSION.SDK_INT >= 24) urlConnection.contentLengthLong else urlConnection.contentLength.toLong()
            var totalRead = 0f
            try {
                BufferedInputStream(urlConnection.getInputStream()).use { bis ->
                    contentResolver.openOutputStream(outFile!!.uri).use { fos ->
                        val buffer = ByteArray(0x2000)
                        var count: Int
                        while (bis.read(buffer, 0, 0x2000).also { count = it } != -1) {
                            totalRead += count
                            fos!!.write(buffer, 0, count)
                            setProgressAsync(Data.Builder().putString(URL, url)
                                .putFloat(PROGRESS, totalRead * 100f / fileSize)
                                .build())
                            updateDownloadProgress(notificationId, position, total, totalRead * 100f / fileSize)
                        }
                        fos!!.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error while writing data from url: " + url + " to file: " + outFile!!.name, e)
            }
            if (isJpg) {
                try {
                    contentResolver.openInputStream(outFile!!.uri).use { fis ->
                        contentResolver.openOutputStream(filePath.uri).use { fos ->
                            val jpegIptcRewriter = JpegIptcRewriter()
                            jpegIptcRewriter.removeIPTC(fis, fos)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error while removing iptc: url: " + url
                               + ", tempFile: " + outFile!!.name
                               + ", finalFile: " + filePath.name, e)
                }
                val deleted = outFile!!.delete()
                if (!deleted) {
                    Log.w(TAG, "download: tempFile not deleted!")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while downloading: $url", e)
        }
        setProgressAsync(Data.Builder().putString(URL, url)
            .putFloat(PROGRESS, 100f)
            .build())
        updateDownloadProgress(notificationId, position, total, 100f)
    }

    private fun updateDownloadProgress(
        notificationId: Int,
        position: Int,
        total: Int,
        percent: Float,
    ) {
        val notification = createProgressNotification(position, total, percent)
        try {
            if (notification == null) {
                notificationManager.cancel(notificationId)
                return
            }
            setForegroundAsync(ForegroundInfo(notificationId, notification)).get()
        } catch (e: ExecutionException) {
            Log.e(TAG, "updateDownloadProgress", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "updateDownloadProgress", e)
        }
    }

    private fun createProgressNotification(position: Int, total: Int, percent: Float): Notification? {
        val context = applicationContext
        var ongoing = true
        val totalPercent: Int
        if (position == total && percent == 100f) {
            ongoing = false
            totalPercent = 100
        } else {
            totalPercent = (100f * (position - 1) / total + 1f / total * percent).toInt()
        }
        if (totalPercent == 100) {
            return null
        }
        // Log.d(TAG, "createProgressNotification: position: " + position
        //         + ", total: " + total
        //         + ", percent: " + percent
        //         + ", totalPercent: " + totalPercent);
        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(ongoing)
            .setProgress(100, totalPercent, totalPercent < 0)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentTitle(context.getString(R.string.downloader_downloading_post))
        if (total > 1) {
            builder.setContentText(context.getString(R.string.downloader_downloading_child, position, total))
        }
        return builder.build()
    }

    private fun showSummary(urlToFilePathMap: Map<String, String>) {
        val context = applicationContext
        val filePaths = urlToFilePathMap.mapNotNull { DocumentFile.fromSingleUri(context, Uri.parse(it.value)) }
        val notifications: MutableList<NotificationCompat.Builder> = LinkedList()
        val notificationIds: MutableList<Int> = LinkedList()
        var count = 1
        for (filePath: DocumentFile in filePaths) {
            // final File file = new File(filePath);
            // context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, filePath.getUri()));
            // Utils.scanDocumentFile(context, filePath, (path, uri) -> {});
            // final Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
            val contentResolver = context.contentResolver
            var bitmap: Bitmap? = null
            val mimeType = filePath.type // Utils.getMimeType(uri, contentResolver);
            if (!isEmpty(mimeType)) {
                if (mimeType!!.startsWith("image")) {
                    try {
                        contentResolver.openInputStream(filePath.uri).use { inputStream ->
                            bitmap = BitmapFactory.decodeStream(inputStream)
                        }
                    } catch (e: java.lang.Exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "", e)
                    }
                } else if (mimeType.startsWith("video")) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        try {
                            retriever.setDataSource(context, filePath.uri)
                        } catch (e: java.lang.Exception) {
                            // retriever.setDataSource(file.getAbsolutePath());
                            Log.e(TAG, "showSummary: ", e)
                        }
                        bitmap = retriever.frameAtTime
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) try {
                            retriever.close()
                        } catch (e: java.lang.Exception) {
                            Log.e(TAG, "showSummary: ", e)
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, "", e)
                    }
                }
            }
            val downloadComplete = context.getString(R.string.downloader_complete)
            val intent = Intent(Intent.ACTION_VIEW, filePath.uri)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_FROM_BACKGROUND
                            or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                .putExtra(Intent.EXTRA_STREAM, filePath.uri)
            val pendingIntent = PendingIntent.getActivity(
                context,
                DOWNLOAD_NOTIFICATION_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
            val notificationId: Int = notificationId + count
            notificationIds.add(notificationId)
            count++
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentText(null)
                    .setContentTitle(downloadComplete)
                    .setWhen(System.currentTimeMillis())
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setGroup(NOTIF_GROUP_NAME + "_" + id)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setContentIntent(pendingIntent)
                    .addAction(
                        R.drawable.ic_delete,
                        context.getString(R.string.delete),
                        DeleteImageIntentService.pendingIntent(context, filePath, notificationId)
                    )
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null)
                    )
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            }
            notifications.add(builder)
        }
        var summaryNotification: Notification? = null
        if (urlToFilePathMap.size != 1) {
            val text = "Downloaded " + urlToFilePathMap.size + " items"
            summaryNotification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setContentTitle("Downloaded")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_download)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText(text))
                .setGroup(NOTIF_GROUP_NAME + "_" + id)
                .setGroupSummary(true)
                .build()
        }
        for (i in notifications.indices) {
            val builder = notifications[i]
            // only make sound and vibrate for the last notification
            if (i != notifications.size - 1) {
                builder.setSound(null)
                    .setVibrate(null)
            }
            notificationManager.notify(notificationIds[i], builder.build())
        }
        if (summaryNotification != null) {
            notificationManager.notify(notificationId + count, summaryNotification)
        }
    }

    private fun getThumbnail(
        context: Context,
        file: File,
        uri: Uri,
        contentResolver: ContentResolver,
    ): Bitmap? {
        val mimeType = Utils.getMimeType(uri, contentResolver)
        if (isEmpty(mimeType)) return null
        var bitmap: Bitmap? = null
        if (mimeType.startsWith("image")) {
            try {
                val bitmapResult = BitmapUtils.getBitmapResult(
                    context.contentResolver,
                    uri,
                    BitmapUtils.THUMBNAIL_SIZE,
                    BitmapUtils.THUMBNAIL_SIZE,
                    -1f,
                    true
                ) ?: return null
                bitmap = bitmapResult.bitmap
            } catch (e: Exception) {
                Log.e(TAG, "", e)
            }
            return bitmap
        }
        if (mimeType.startsWith("video")) {
            try {
                val retriever = MediaMetadataRetriever()
                bitmap = try {
                    try {
                        retriever.setDataSource(context, uri)
                    } catch (e: Exception) {
                        retriever.setDataSource(file.absolutePath)
                    }
                    retriever.frameAtTime
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "getThumbnail: ", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "", e)
            }
        }
        return bitmap
    }

    class DownloadRequest private constructor(val urlToFilePathMap: Map<String, String>) {

        class Builder {
            private var urlToFilePathMap: MutableMap<String, String> = mutableMapOf()
            fun setUrlToFilePathMap(urlToFilePathMap: Map<String, DocumentFile?>): Builder {
                this.urlToFilePathMap = urlToFilePathMap
                    .filter{ it.value != null }
                    .mapValues { it.value!!.uri.toString() }
                    .toMutableMap()
                return this
            }

            fun addUrl(url: String, filePath: DocumentFile): Builder {
                urlToFilePathMap[url] = filePath.uri.toString()
                return this
            }

            fun build(): DownloadRequest {
                return DownloadRequest(urlToFilePathMap)
            }
        }

        companion object {
            @JvmStatic
            fun builder(): Builder {
                return Builder()
            }
        }
    }

    companion object {
        const val PROGRESS = "PROGRESS"
        const val URL = "URL"
        const val KEY_DOWNLOAD_REQUEST_JSON = "download_request_json"
        private const val DOWNLOAD_GROUP = "DOWNLOAD_GROUP"
        private const val DOWNLOAD_NOTIFICATION_INTENT_REQUEST_CODE = 2020
        private const val DELETE_IMAGE_REQUEST_CODE = 2030
    }

}