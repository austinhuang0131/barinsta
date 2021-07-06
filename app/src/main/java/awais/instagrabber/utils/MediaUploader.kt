package awais.instagrabber.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import awais.instagrabber.models.UploadVideoOptions
import awais.instagrabber.webservices.interceptors.AddCookiesInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.io.IOException
import java.io.InputStream

object MediaUploader {
    private const val HOST = "https://i.instagram.com"
    private val octetStreamMediaType: MediaType = requireNotNull(MediaType.parse("application/octet-stream")) {
        "No media type found for application/octet-stream"
    }

    suspend fun uploadPhoto(
        uri: Uri,
        contentResolver: ContentResolver,
    ): MediaUploadResponse = withContext(Dispatchers.IO) {
        val bitmapResult = BitmapUtils.loadBitmap(contentResolver, uri, 1000f, false)
        val bitmap = bitmapResult?.bitmap ?: throw IOException("bitmap is null")
        uploadPhoto(contentResolver, bitmap)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun uploadPhoto(
        contentResolver: ContentResolver,
        bitmap: Bitmap,
    ): MediaUploadResponse = withContext(Dispatchers.IO) {
        val file: DocumentFile? = BitmapUtils.convertToJpegAndSaveToFile(contentResolver, bitmap, null)
        val byteLength: Long = file!!.length()
        val options = createUploadPhotoOptions(byteLength)
        val headers = getUploadPhotoHeaders(options)
        val url = HOST + "/rupload_igphoto/" + options.name + "/"
        try {
            contentResolver.openInputStream(file.uri).use { input ->
                upload(input!!, url, headers)
            }
        } finally {
            file.delete()
        }
    }

    @JvmStatic
    @Suppress("BlockingMethodInNonBlockingContext") // See https://youtrack.jetbrains.com/issue/KTIJ-838
    suspend fun uploadVideo(
        uri: Uri,
        contentResolver: ContentResolver,
        options: UploadVideoOptions,
    ): MediaUploadResponse = withContext(Dispatchers.IO) {
        val headers = getUploadVideoHeaders(options)
        val url = HOST + "/rupload_igvideo/" + options.name + "/"
        contentResolver.openInputStream(uri).use { input ->
            if (input == null) {
                // listener.onFailure(RuntimeException("InputStream was null"))
                throw IllegalStateException("InputStream was null")
            }
            upload(input, url, headers)
        }
    }

    @Throws(IOException::class)
    private suspend fun upload(
        input: InputStream,
        url: String,
        headers: Map<String, String>,
    ): MediaUploadResponse {
        try {
            val client = OkHttpClient.Builder()
                // .addInterceptor(new LoggingInterceptor())
                .addInterceptor(AddCookiesInterceptor())
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
            val request = Request.Builder()
                .headers(Headers.of(headers))
                .url(url)
                .post(create(octetStreamMediaType, input))
                .build()
            return withContext(Dispatchers.IO) {
                val response = client.newCall(request).await()
                val body = response.body()
                @Suppress("BlockingMethodInNonBlockingContext") // Blocked by https://github.com/square/okio/issues/501
                MediaUploadResponse(response.code(), if (body != null) JSONObject(body.string()) else null)
            }
        } catch (e: Exception) {
            // rethrow for proper stacktrace. See https://github.com/gildor/kotlin-coroutines-okhttp/tree/master#wrap-exception-manually
            throw IOException(e)
        }
    }

    private fun create(mediaType: MediaType, inputStream: InputStream): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType {
            return mediaType
        }

        override fun contentLength(): Long {
            return try {
                inputStream.available().toLong()
            } catch (e: IOException) {
                0
            }
        }

        @Throws(IOException::class)
        @Suppress("DEPRECATION_ERROR")
        override fun writeTo(sink: BufferedSink) {
            Okio.source(inputStream).use { sink.writeAll(it) }
        }
    }

    data class MediaUploadResponse(val responseCode: Int, val response: JSONObject?)
}