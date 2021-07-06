package awais.instagrabber.utils

import android.content.Context
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import awais.instagrabber.R
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.stories.StoryMedia
import awais.instagrabber.utils.AppExecutors.tasksThread
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.workers.DownloadWorker
import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs


object DownloadUtils {
    private val TAG = DownloadUtils::class.java.simpleName

    // private static final String DIR_BARINSTA = "Barinsta";
    private const val DIR_DOWNLOADS = "Downloads"
    private const val DIR_CAMERA = "Camera"
    private const val DIR_EDIT = "Edit"
    private const val DIR_RECORDINGS = "Sent Recordings"
    private const val DIR_TEMP = "Temp"
    private const val DIR_BACKUPS = "Backups"
    private const val MIME_DIR = DocumentsContract.Document.MIME_TYPE_DIR
    private val dirMap: MutableMap<String, DocumentFile?> = mutableMapOf()
    private var root: DocumentFile? = null
    @JvmStatic
    @Throws(ReselectDocumentTreeException::class)
    fun init(
        context: Context,
        barinstaDirUri: String?
    ) {
        if (isEmpty(barinstaDirUri)) {
            throw ReselectDocumentTreeException("folder path is null or empty")
        }
        val uri = Uri.parse(barinstaDirUri)
        if (!barinstaDirUri!!.startsWith("content://com.android.externalstorage.documents")) {
            throw ReselectDocumentTreeException(uri)
        }
        val existingPermissions = context.contentResolver.persistedUriPermissions
        if (existingPermissions.isEmpty()) {
            throw ReselectDocumentTreeException(uri)
        }
        val anyMatch = existingPermissions.stream()
            .anyMatch { uriPermission: UriPermission -> uriPermission.uri == uri }
        if (!anyMatch) {
            throw ReselectDocumentTreeException(uri)
        }
        root = DocumentFile.fromTreeUri(context, uri)
        if (root == null || !root!!.exists() || root!!.lastModified() == 0L) {
            root = null
            throw ReselectDocumentTreeException(uri)
        }
        Utils.settingsHelper.putString(PreferenceKeys.PREF_BARINSTA_DIR_URI, uri.toString())
        // set up directories
        val dirKeys = mapOf(
            DIR_DOWNLOADS to MIME_DIR,
            DIR_CAMERA to MIME_DIR,
            DIR_EDIT to MIME_DIR,
            DIR_RECORDINGS to MIME_DIR,
            DIR_TEMP to MIME_DIR,
            DIR_BACKUPS to MIME_DIR
        )
        dirMap.putAll(checkFiles(context, root, dirKeys, true))
    }

    fun destroy() {
        root = null
        dirMap.clear()
    }

    fun checkFiles(context: Context,
                   parent: DocumentFile?,
                   queries: Map<String, String>, // <file name, mime type>
                   create: Boolean
    ): Map<String, DocumentFile?> {
        // first we'll find existing ones
        val result: MutableMap<String, DocumentFile?> = mutableMapOf()
        if (root == null || parent == null || !parent.isDirectory) return result.toMap()
        val docId = DocumentsContract.getDocumentId(parent.uri)
        val docUri = DocumentsContract.buildChildDocumentsUriUsingTree(root!!.uri, docId)
        val docCursor = context.contentResolver.query(
            docUri, arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null
        )
        if (docCursor == null) return result.toMap()
        while (docCursor.moveToNext()) {
            val q = queries.get(docCursor.getString(0))
            if (q == null || !docCursor.getString(2).equals(q)) continue
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(parent.uri, docCursor.getString(1))
            val dir = if (q.equals(MIME_DIR)) DocumentFile.fromTreeUri(context, fileUri)
                      else DocumentFile.fromSingleUri(context, fileUri)
            result.put(docCursor.getString(0), dir)
            if (result.size >= queries.size) break
        }
        docCursor.close()
        // next we'll create inexistent ones, if necessary
        if (create) {
            for (k in queries) {
                if (result.get(k.key) == null) {
                    result.put(k.key, if (MIME_DIR.equals(k.value)) parent.createDirectory(k.key)
                                      else parent.createFile(k.value, k.key))
                }
            }
        }
        return result.toMap()
    }

    fun getRootDir(dir: String): DocumentFile? {
        if (root == null) return null
        return dirMap.get(dir)
    }

    @JvmStatic
    val downloadDir: DocumentFile?
        get() = getRootDir(DIR_DOWNLOADS)

    @JvmStatic
    val cameraDir: DocumentFile?
        get() = getRootDir(DIR_CAMERA)

    @JvmStatic
    fun getImageEditDir(sessionId: String?, context: Context): DocumentFile? {
        val editRoot = getRootDir(DIR_EDIT)
        if (sessionId == null) return editRoot
        return checkFiles(context,
                          editRoot,
                          mapOf(sessionId to MIME_DIR),
                          true).get(sessionId)
    }

    @JvmStatic
    val recordingsDir: DocumentFile?
        get() = getRootDir(DIR_RECORDINGS)

    @JvmStatic
    val backupsDir: DocumentFile?
        get() = getRootDir(DIR_BACKUPS)

    private fun getDownloadDir(
        context: Context,
        username: String?,
        shouldCreate: Boolean
    ): DocumentFile? {
        if (!Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_USER_FOLDER) || username.isNullOrEmpty())
            return downloadDir
        return checkFiles(context,
                          downloadDir,
                          mapOf(username to MIME_DIR),
                          shouldCreate).get(username)
    }

    private val tempDir: DocumentFile?
        get() = getRootDir(DIR_TEMP)

    private fun getDownloadSavePaths(
        postId: String?,
        displayUrl: String?
    ): Pair<String, String> {
        return getDownloadFileName(postId, "", displayUrl, "")
    }

    private fun getDownloadSavePaths(
        postId: String?,
        displayUrl: String,
        username: String
    ): Pair<String, String> {
        return getDownloadFileName(postId, "", displayUrl, username)
    }

    private fun getDownloadChildSavePaths(
        postId: String?,
        childPosition: Int,
        url: String?,
        username: String
    ): Pair<String, String> {
        val sliderPostfix = "_slide_$childPosition"
        return getDownloadFileName(postId, sliderPostfix, url, username)
    }

    private fun getDownloadFileName(
        postId: String?,
        sliderPostfix: String,
        displayUrl: String?,
        username: String
    ): Pair<String, String> {
        val extension = getFileExtensionFromUrl(displayUrl)
        val usernamePrepend = if (isEmpty(username)) "" else username + "_"
        val fileName = usernamePrepend + postId + sliderPostfix + extension
        val mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(extension)
        return Pair(fileName, mimeType!!)
    }

    fun getTempFile(fileName: String?, extension: String): DocumentFile? {
        val dir = tempDir
        var name = fileName
        if (isEmpty(name)) {
            name = UUID.randomUUID().toString()
        }
        var mimeType: String? = "application/octet-stream"
        if (!isEmpty(extension)) {
            name += ".$extension"
            val mimeType1 = Utils.mimeTypeMap.getMimeTypeFromExtension(extension)
            if (mimeType1 != null) {
                mimeType = mimeType1
            }
        }
        var file = dir!!.findFile(name!!)
        if (file == null) {
            file = dir.createFile(mimeType!!, name)
        }
        return file
    }

    /**
     * Copied from [MimeTypeMap.getFileExtensionFromUrl])
     *
     *
     * Returns the file extension or an empty string if there is no
     * extension. This method is a convenience method for obtaining the
     * extension of a url and has undefined results for other Strings.
     *
     * @param url URL
     * @return The file extension of the given url.
     */
    @JvmStatic
    fun getFileExtensionFromUrl(url: String?): String {
        var url = url
        if (!isEmpty(url)) {
            val fragment = url!!.lastIndexOf('#')
            if (fragment > 0) {
                url = url.substring(0, fragment)
            }
            val query = url.lastIndexOf('?')
            if (query > 0) {
                url = url.substring(0, query)
            }
            val filenamePos = url.lastIndexOf('/')
            val filename = if (0 <= filenamePos) url.substring(filenamePos + 1) else url

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() &&
                Pattern.matches("[a-zA-Z_0-9.\\-()%]+", filename)
            ) {
                val dotPos = filename.lastIndexOf('.')
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1)
                }
            }
        }
        return ""
    }

    @JvmStatic
    fun checkDownloaded(media: Media, context: Context): List<Boolean> {
        val checkList: MutableList<Boolean> = LinkedList()
        val user = media.user
        var username = "username"
        if (user != null) {
            username = user.username
        }
        val userFolder = getDownloadDir(context, username, false)
        if (userFolder == null) return checkList
        when (media.type) {
            MediaItemType.MEDIA_TYPE_IMAGE, MediaItemType.MEDIA_TYPE_VIDEO -> {
                val url =
                    if (media.type == MediaItemType.MEDIA_TYPE_VIDEO) ResponseBodyUtils.getVideoUrl(
                        media
                    ) else ResponseBodyUtils.getImageUrl(media)
                val fileName = getDownloadSavePaths(media.code, url)
                val fileNameWithUser = getDownloadSavePaths(media.code, url, username)
                val files = checkFiles(context, userFolder, mapOf(fileName, fileNameWithUser), false)
                checkList.add(files.size > 0)
            }
            MediaItemType.MEDIA_TYPE_SLIDER -> {
                val sliderItems = media.carouselMedia
                val fileNames: MutableMap<String, String> = mutableMapOf()
                val filePairs: MutableMap<String, String> = mutableMapOf()
                var i = 0
                while (i < sliderItems!!.size) {
                    val child = sliderItems[i]
                    val url =
                        if (child.type == MediaItemType.MEDIA_TYPE_VIDEO) ResponseBodyUtils.getVideoUrl(
                            child
                        ) else ResponseBodyUtils.getImageUrl(child)
                    val fileName = getDownloadChildSavePaths(media.code, i+1, url, "")
                    val fileNameWithUser = getDownloadChildSavePaths(media.code, i+1, url, username)
                    fileNames.put(fileName.first, fileName.second)
                    fileNames.put(fileNameWithUser.first, fileNameWithUser.second)
                    filePairs.put(fileName.first, fileNameWithUser.first)
                    i++
                }
                val files = checkFiles(context, userFolder, fileNames, false)
                for (p in filePairs) {
                    checkList.add(files.get(p.key) != null || files.get(p.value) != null)
                }
            }
            else -> {
            }
        }
        return checkList
    }

    @JvmStatic
    fun showDownloadDialog(
        context: Context,
        feedModel: Media,
        childPosition: Int,
        popupLocation: View?
    ) {
        if (childPosition == -1 || popupLocation == null) {
            download(context, feedModel)
            return
        }
        val themeWrapper = ContextThemeWrapper(context, R.style.popupMenuStyle)
        val popupMenu = PopupMenu(themeWrapper, popupLocation)
        val menu = popupMenu.menu
        menu.add(0, R.id.download_current, 0, R.string.post_viewer_download_current)
        menu.add(0, R.id.download_all, 1, R.string.post_viewer_download_album)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            val itemId = item.itemId
            if (itemId == R.id.download_current) {
                download(context, feedModel, childPosition)
            } else if (itemId == R.id.download_all) {
                download(context, feedModel)
            }
            false
        }
        popupMenu.show()
    }

    @JvmStatic
    fun download(
        context: Context,
        storyModel: StoryMedia
    ) {
        val downloadDir = getDownloadDir(context, storyModel.user?.username, true) ?: return
        val url =
            if (storyModel.type == MediaItemType.MEDIA_TYPE_VIDEO) ResponseBodyUtils.getVideoUrl(storyModel)
            else ResponseBodyUtils.getImageUrl(storyModel)
        val extension = getFileExtensionFromUrl(url)
        val mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(extension)
        val baseFileName = storyModel.id + "_" + storyModel.takenAt + extension
        val usernamePrepend =
            if (Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME)
                && storyModel.user?.username != null
            ) storyModel.user.username + "_" else ""
        val fileName = usernamePrepend + baseFileName
        var saveFile = checkFiles(context, downloadDir, mapOf(fileName to mimeType!!), true).get(fileName)
        download(context, url, saveFile)
    }

    @JvmOverloads
    @JvmStatic
    fun download(
        context: Context,
        feedModel: Media,
        position: Int = -1
    ) {
        download(context, listOf(feedModel), position)
    }

    // this must be used for bulk download, but ONLY bulk download
    @JvmStatic
    fun download(
        context: Context,
        feedModels: List<Media>
    ) {
        val builder = NotificationCompat.Builder(context, Constants.DOWNLOAD_CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setProgress(1, 0, true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentTitle(context.getString(R.string.downloader_preparing))
        val notification = builder.build()
        val nid = abs(UUID.randomUUID().hashCode())
        val nManager = NotificationManagerCompat.from(context.applicationContext)
        nManager.notify(nid, notification)
        tasksThread.execute {
            download(context, feedModels, -1)
            nManager.cancel(nid)
        }
    }

    private fun download(
        context: Context,
        feedModels: List<Media>,
        childPositionIfSingle: Int
    ) {
        val map: MutableMap<String, Pair<String, String>> = HashMap()
        val fileMap: MutableMap<String, DocumentFile?> = HashMap()
        for (media in feedModels) {
            val mediaUser = media.user
            val username = mediaUser?.username ?: ""
            val dir = getDownloadDir(context, username, true)
            when (media.type) {
                MediaItemType.MEDIA_TYPE_IMAGE, MediaItemType.MEDIA_TYPE_VIDEO -> {
                    val url = getUrlOfType(media)
                    var fileName = media.id
                    if (mediaUser != null && isEmpty(media.code)) {
                        fileName = mediaUser.username + "_" + fileName
                    }
                    if (!isEmpty(media.code)) {
                        fileName = media.code
                        if (Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME) && mediaUser != null) {
                            fileName = mediaUser.username + "_" + fileName
                        }
                    }
                    val pair = getDownloadSavePaths(fileName, url)
                    map[url!!] = pair
                }
                MediaItemType.MEDIA_TYPE_VOICE -> {
                    val url = getUrlOfType(media)
                    var fileName = media.id
                    if (mediaUser != null) {
                        fileName = mediaUser.username + "_" + fileName
                    }
                    val pair = getDownloadSavePaths(fileName, url)
                    map[url!!] = pair
                }
                MediaItemType.MEDIA_TYPE_SLIDER -> {
                    val sliderItems = media.carouselMedia
                    var i = 0
                    while (i < sliderItems!!.size) {
                        if (childPositionIfSingle >= 0 && feedModels.size == 1 && i != childPositionIfSingle) {
                            i++
                            continue
                        }
                        val child = sliderItems[i]
                        val url = getUrlOfType(child)
                        val usernamePrepend =
                            if (Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME) && mediaUser != null) mediaUser.username else ""
                        val pair = getDownloadChildSavePaths(media.code, i + 1, url, usernamePrepend)
                        map[url!!] = pair
                        i++
                    }
                }
            }
            fileMap.putAll(checkFiles(context, dir, map.values.toMap(), true))
        }
        if (map.isEmpty() || fileMap.isEmpty()) return
        val resultMap: MutableMap<String, DocumentFile?> = mutableMapOf()
        map.mapValuesTo(resultMap) { fileMap.get(it.value.first) }
        download(context, resultMap)
    }

    private fun getUrlOfType(media: Media): String? {
        when (media.type) {
            MediaItemType.MEDIA_TYPE_IMAGE -> {
                return ResponseBodyUtils.getImageUrl(media)
            }
            MediaItemType.MEDIA_TYPE_VIDEO -> {
                val videoVersions = media.videoVersions
                var url: String? = null
                if (videoVersions != null && !videoVersions.isEmpty()) {
                    url = videoVersions[0].url
                }
                return url
            }
            MediaItemType.MEDIA_TYPE_VOICE -> {
                val audio = media.audio
                var url: String? = null
                if (audio != null) {
                    url = audio.audioSrc
                }
                return url
            }
        }
        return null
    }

    @JvmStatic
    fun download(
        context: Context?,
        url: String?,
        filePath: DocumentFile?
    ) {
        if (context == null || filePath == null) return
        download(context, Collections.singletonMap(url!!, filePath))
    }

    private fun download(context: Context?, urlFilePathMap: Map<String, DocumentFile?>) {
        if (context == null) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = DownloadWorker.DownloadRequest.builder()
            .setUrlToFilePathMap(urlFilePathMap)
            .build()
        val requestJson = Gson().toJson(request)
        val tempFile = getTempFile(null, "json")
        if (tempFile == null) {
            Log.e(TAG, "download: temp file is null")
            return
        }
        val uri = tempFile.uri
        val contentResolver = context.contentResolver ?: return
        try {
            BufferedWriter(OutputStreamWriter(contentResolver.openOutputStream(uri))).use { writer ->
                writer.write(
                    requestJson
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "download: Error writing request to file", e)
            tempFile.delete()
            return
        }
        val downloadWorkRequest: WorkRequest =
            OneTimeWorkRequest.Builder(DownloadWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(
                            DownloadWorker.KEY_DOWNLOAD_REQUEST_JSON,
                            tempFile.uri.toString()
                        )
                        .build()
                )
                .setConstraints(constraints)
                .addTag("download")
                .build()
        WorkManager.getInstance(context)
            .enqueue(downloadWorkRequest)
    }

    @JvmStatic
    fun getRootDirUri(): Uri? {
        return if (root != null) root!!.uri else null
    }

    class ReselectDocumentTreeException : Exception {
        val initialUri: Uri?

        constructor() {
            initialUri = null
        }

        constructor(message: String?) : super(message) {
            initialUri = null
        }

        constructor(initialUri: Uri?) {
            this.initialUri = initialUri
        }
    }
}