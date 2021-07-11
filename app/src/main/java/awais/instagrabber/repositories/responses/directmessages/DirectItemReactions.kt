package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemReactions(
    var emojis: List<DirectItemEmojiReaction>? = null,
    var likes: List<DirectItemEmojiReaction>? = null,
) : Cloneable, Serializable {
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }
}