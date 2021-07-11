package awais.instagrabber.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.core.util.Pair
import androidx.documentfile.provider.DocumentFile
import awais.instagrabber.utils.extensions.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object BitmapUtils {
    private val bitmapMemoryCache: LruCache<String, Bitmap>
    const val THUMBNAIL_SIZE = 200f

    @JvmStatic
    fun addBitmapToMemoryCache(key: String, bitmap: Bitmap, force: Boolean) {
        if (force || getBitmapFromMemCache(key) == null) {
            bitmapMemoryCache.put(key, bitmap)
        }
    }

    @JvmStatic
    fun getBitmapFromMemCache(key: String): Bitmap? {
        return bitmapMemoryCache[key]
    }

    @JvmStatic
    suspend fun getThumbnail(context: Context, uri: Uri): BitmapResult? {
        val key = uri.toString()
        val cachedBitmap = getBitmapFromMemCache(key)
        if (cachedBitmap != null) {
            return BitmapResult(cachedBitmap, -1, -1)
        }
        return loadBitmap(context.contentResolver, uri, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)
    }

    /**
     * Loads bitmap from given Uri
     *
     * @param contentResolver [ContentResolver] to resolve the uri
     * @param uri             Uri from where Bitmap will be loaded
     * @param reqWidth        Required width
     * @param reqHeight       Required height
     * @param addToCache      true if the loaded bitmap should be added to the mem cache
     */
    suspend fun loadBitmap(
        contentResolver: ContentResolver?,
        uri: Uri?,
        reqWidth: Float,
        reqHeight: Float,
        addToCache: Boolean,
    ): BitmapResult? = loadBitmap(contentResolver, uri, reqWidth, reqHeight, -1f, addToCache)

    /**
     * Loads bitmap from given Uri
     *
     * @param contentResolver [ContentResolver] to resolve the uri
     * @param uri             Uri from where Bitmap will be loaded
     * @param maxDimenSize    Max size of the largest side of the image
     * @param addToCache      true if the loaded bitmap should be added to the mem cache
     */
    suspend fun loadBitmap(
        contentResolver: ContentResolver?,
        uri: Uri?,
        maxDimenSize: Float,
        addToCache: Boolean,
    ): BitmapResult? = loadBitmap(contentResolver, uri, -1f, -1f, maxDimenSize, addToCache)

    /**
     * Loads bitmap from given Uri
     *
     * @param contentResolver [ContentResolver] to resolve the uri
     * @param uri             Uri from where [Bitmap] will be loaded
     * @param reqWidth        Required width (set to -1 if maxDimenSize provided)
     * @param reqHeight       Required height (set to -1 if maxDimenSize provided)
     * @param maxDimenSize    Max size of the largest side of the image (set to -1 if setting reqWidth and reqHeight)
     * @param addToCache      true if the loaded bitmap should be added to the mem cache
     */
    private suspend fun loadBitmap(
        contentResolver: ContentResolver?,
        uri: Uri?,
        reqWidth: Float,
        reqHeight: Float,
        maxDimenSize: Float,
        addToCache: Boolean,
    ): BitmapResult? =
        if (contentResolver == null || uri == null) null else withContext(Dispatchers.IO) {
            getBitmapResult(contentResolver,
                uri,
                reqWidth,
                reqHeight,
                maxDimenSize,
                addToCache)
        }

    fun getBitmapResult(
        contentResolver: ContentResolver,
        uri: Uri,
        reqWidth: Float,
        reqHeight: Float,
        maxDimenSize: Float,
        addToCache: Boolean,
    ): BitmapResult? {
        var bitmapOptions: BitmapFactory.Options
        var actualReqWidth = reqWidth
        var actualReqHeight = reqHeight
        try {
            contentResolver.openInputStream(uri).use { input ->
                val outBounds = BitmapFactory.Options()
                outBounds.inJustDecodeBounds = true
                outBounds.inPreferredConfig = Bitmap.Config.ARGB_8888
                BitmapFactory.decodeStream(input, null, outBounds)
                if (outBounds.outWidth == -1 || outBounds.outHeight == -1) return null
                bitmapOptions = BitmapFactory.Options()
                if (maxDimenSize > 0) {
                    // Raw height and width of image
                    val height = outBounds.outHeight
                    val width = outBounds.outWidth
                    val ratio = width.toFloat() / height
                    if (height > width) {
                        actualReqHeight = maxDimenSize
                        actualReqWidth = actualReqHeight * ratio
                    } else {
                        actualReqWidth = maxDimenSize
                        actualReqHeight = actualReqWidth / ratio
                    }
                }
                bitmapOptions.inSampleSize = calculateInSampleSize(outBounds, actualReqWidth, actualReqHeight)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadBitmap: ", e)
            return null
        }
        try {
            contentResolver.openInputStream(uri).use { input ->
                bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
                if (addToCache && bitmap != null) {
                    addBitmapToMemoryCache(uri.toString(), bitmap, true)
                }
                return BitmapResult(bitmap, actualReqWidth.toInt(), actualReqHeight.toInt())
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadBitmap: ", e)
        }
        return null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Float, reqHeight: Float): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2f
            val halfWidth = width / 2f
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                   && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Decodes the bounds of an image from its Uri and returns a pair of the dimensions
     *
     * @param uri the Uri of the image
     * @return dimensions of the image
     */
    @Throws(IOException::class)
    fun decodeDimensions(
        contentResolver: ContentResolver,
        uri: Uri,
    ): Pair<Int, Int>? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
            return if (options.outWidth == -1 || options.outHeight == -1) null else Pair(options.outWidth, options.outHeight)
        }
    }

    @Throws(IOException::class)
    fun convertToJpegAndSaveToFile(contentResolver: ContentResolver, bitmap: Bitmap, file: DocumentFile?): DocumentFile? {
        val tempFile = file ?: DownloadUtils.getTempFile(null, "jpg")
        contentResolver.openOutputStream(tempFile!!.uri).use { output ->
            val compressResult = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            if (!compressResult) {
                throw RuntimeException("Compression failed!")
            }
        }
        return tempFile
    }

    @JvmStatic
    @Throws(Exception::class)
    fun convertToJpegAndSaveToUri(
        context: Context,
        bitmap: Bitmap,
        uri: Uri,
    ) {
        context.contentResolver.openOutputStream(uri).use { output ->
            val compressResult = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            if (!compressResult) {
                throw RuntimeException("Compression failed!")
            }
        }
    }

    class BitmapResult(var bitmap: Bitmap?, var width: Int, var height: Int)

    init {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        // Use 1/8th of the available memory for this memory cache.
        val cacheSize: Int = maxMemory / 8
        bitmapMemoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.byteCount / 1024
            }
        }
    }
}