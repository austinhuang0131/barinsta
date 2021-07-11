package awais.instagrabber.models.enums

import com.google.gson.annotations.SerializedName

enum class RavenMediaViewMode {
    @SerializedName("permanent")
    PERMANENT,
    @SerializedName("replayable")
    REPLAYABLE,
    @SerializedName("once")
    ONCE,
}