package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemXma(
    val previewUrlInfo: XmaUrlInfo? = null,
    val playableUrlInfo: XmaUrlInfo? = null,
) : Serializable

data class XmaUrlInfo(
    val url: String? = null,
    val urlExpirationTimestampUs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
) : Serializable