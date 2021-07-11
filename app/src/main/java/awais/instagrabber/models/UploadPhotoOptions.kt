package awais.instagrabber.models

data class UploadPhotoOptions(
    val uploadId: String? = null,
    val name: String,
    val byteLength: Long = 0,
    val isSideCar: Boolean = false,
    val waterfallId: String? = null,
)