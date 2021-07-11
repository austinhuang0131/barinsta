package awais.instagrabber.repositories.responses.directmessages

data class DirectInbox(
    var threads: List<DirectThread>? = emptyList(),
    val hasOlder: Boolean = false,
    val unseenCount: Int = 0,
    val unseenCountTs: String? = null,
    val oldestCursor: String? = null,
    val blendedInboxEnabled: Boolean
) : Cloneable {
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }
}