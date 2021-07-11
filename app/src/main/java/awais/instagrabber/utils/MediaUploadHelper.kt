@file:JvmName("MediaUploadHelper")

package awais.instagrabber.utils

import awais.instagrabber.models.UploadPhotoOptions
import awais.instagrabber.models.UploadVideoOptions
import awais.instagrabber.models.enums.MediaItemType
import org.json.JSONObject
import java.time.Instant
import java.util.*
import kotlin.random.Random


private const val LOWER = 1000000000L
private const val UPPER = 9999999999L

private fun createPhotoRuploadParams(options: UploadPhotoOptions): Map<String, String> {
    val imageCompression = mapOf(
        "lib_name" to "moz",
        "lib_version" to "3.1.m",
        "quality" to "80",
    )
    return listOfNotNull(
        "retry_context" to retryContextString,
        "media_type" to "1",
        "upload_id" to (options.uploadId ?: ""),
        "xsharing_user_ids" to "[]",
        "image_compression" to JSONObject(imageCompression).toString(),
        if (options.isSideCar) "is_sidecar" to "1" else null,
    ).toMap()
}

private fun createVideoRuploadParams(options: UploadVideoOptions): Map<String, String> = listOfNotNull(
    "retry_context" to retryContextString,
    "media_type" to "2",
    "xsharing_user_ids" to "[]",
    "upload_id" to options.uploadId,
    "upload_media_width" to options.width.toString(),
    "upload_media_height" to options.height.toString(),
    "upload_media_duration_ms" to options.duration.toString(),
    if (options.isSideCar) "is_sidecar" to "1" else null,
    if (options.forAlbum) "for_album" to "1" else null,
    if (options.isDirect) "direct_v2" to "1" else null,
    *(if (options.isForDirectStory) arrayOf(
        "for_direct_story" to "1",
        "content_tags" to ""
    ) else emptyArray()),
    if (options.isIgtvVideo) "is_igtv_video" to "1" else null,
    if (options.isDirectVoice) "is_direct_voice" to "1" else null,
).toMap()

val retryContextString: String
    get() {
        return JSONObject(
            mapOf(
                "num_step_auto_retry" to 0,
                "num_reupload" to 0,
                "num_step_manual_retry" to 0,
            )
        ).toString()
    }

fun createUploadPhotoOptions(byteLength: Long): UploadPhotoOptions {
    val uploadId = generateUploadId()
    return UploadPhotoOptions(
        uploadId,
        generateName(uploadId),
        byteLength,
    )
}

fun createUploadDmVideoOptions(
    byteLength: Long,
    duration: Long,
    width: Int,
    height: Int
): UploadVideoOptions {
    val uploadId = generateUploadId()
    return UploadVideoOptions(
        uploadId,
        generateName(uploadId),
        byteLength,
        duration,
        width,
        height,
        isDirect = true,
        mediaType = MediaItemType.MEDIA_TYPE_VIDEO,
    )
}

fun createUploadDmVoiceOptions(
    byteLength: Long,
    duration: Long
): UploadVideoOptions {
    val uploadId = generateUploadId()
    return UploadVideoOptions(
        uploadId,
        generateName(uploadId),
        byteLength,
        duration,
        isDirectVoice = true,
        mediaType = MediaItemType.MEDIA_TYPE_VOICE,
    )
}

fun generateUploadId(): String {
    return Instant.now().epochSecond.toString()
}

fun generateName(uploadId: String): String {
    val random = Random.nextLong(LOWER, UPPER + 1)
    return "${uploadId}_0_$random"
}

fun getUploadPhotoHeaders(options: UploadPhotoOptions): Map<String, String> {
    val waterfallId = options.waterfallId ?: UUID.randomUUID().toString()
    val contentLength = options.byteLength.toString()
    return mapOf(
        "X_FB_PHOTO_WATERFALL_ID" to waterfallId,
        "X-Entity-Type" to "image/jpeg",
        "Offset" to "0",
        "X-Instagram-Rupload-Params" to JSONObject(createPhotoRuploadParams(options)).toString(),
        "X-Entity-Name" to options.name,
        "X-Entity-Length" to contentLength,
        "Content-Type" to "application/octet-stream",
        "Content-Length" to contentLength,
        "Accept-Encoding" to "gzip",
    )
}

fun getUploadVideoHeaders(options: UploadVideoOptions): Map<String, String> {
    val ruploadParams = createVideoRuploadParams(options)
    val waterfallId = options.waterfallId ?: UUID.randomUUID().toString()
    val contentLength = options.byteLength.toString()
    return getBaseUploadVideoHeaders(ruploadParams) + mapOf(
        "X_FB_PHOTO_WATERFALL_ID" to waterfallId,
        "X-Entity-Type" to "video/mp4",
        "Offset" to (if (options.offset > 0) options.offset else 0).toString(),
        "X-Entity-Name" to options.name,
        "X-Entity-Length" to contentLength,
        "Content-Type" to "application/octet-stream",
        "Content-Length" to contentLength,
    )
}

private fun getBaseUploadVideoHeaders(ruploadParams: Map<String, String>): Map<String, String> {
    return mapOf(
        "X-IG-Connection-Type" to "WIFI",
        "X-IG-Capabilities" to "3brTvwE=",
        "Accept-Encoding" to "gzip",
        "X-Instagram-Rupload-Params" to JSONObject(ruploadParams).toString()
    )
}