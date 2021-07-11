package awais.instagrabber.repositories.responses

import java.io.Serializable

data class Audio(
    val audioSrc: String?,
    val duration: Long,
    val waveformData: List<Float>?,
    val waveformSamplingFrequencyHz: Int,
    val audioSrcExpirationTimestampUs: Long
) : Serializable