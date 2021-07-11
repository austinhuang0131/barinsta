package awais.instagrabber.repositories.requests

data class UploadFinishOptions(
    val uploadId: String,
    val sourceType: String,
    val videoOptions: VideoOptions? = null
)

data class VideoOptions(
    val length: Float = 0f,
    var clips: List<Clip> = emptyList(),
    val posterFrameIndex: Int = 0,
    val isAudioMuted: Boolean = false
) {
    val map: Map<String, Any>
        get() = mapOf(
            "length" to length,
            "clips" to clips,
            "poster_frame_index" to posterFrameIndex,
            "audio_muted" to isAudioMuted
        )
}

data class Clip(
    val length: Float = 0f,
    val sourceType: String
)