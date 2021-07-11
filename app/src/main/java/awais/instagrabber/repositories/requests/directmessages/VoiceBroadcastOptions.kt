package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType
import org.json.JSONArray

class VoiceBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val uploadId: String,
    val waveform: List<Float>,
    val waveformSamplingFrequencyHz: Int
) : BroadcastOptions(clientContext, threadIdsOrUserIds, BroadcastItemType.VOICE) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "waveform" to JSONArray(waveform).toString(),
            "upload_id" to uploadId,
            "waveform_sampling_frequency_hz" to waveformSamplingFrequencyHz.toString()
        )
}