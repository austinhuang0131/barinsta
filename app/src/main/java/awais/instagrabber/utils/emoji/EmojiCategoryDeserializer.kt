package awais.instagrabber.utils.emoji

import android.util.Log
import awais.instagrabber.customviews.emoji.Emoji
import awais.instagrabber.customviews.emoji.EmojiCategory
import awais.instagrabber.customviews.emoji.EmojiCategoryType
import awais.instagrabber.utils.extensions.TAG
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class EmojiCategoryDeserializer : JsonDeserializer<EmojiCategory> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): EmojiCategory {
        val jsonObject = json.asJsonObject
        val typeElement = jsonObject["type"]
        val emojisObject = jsonObject.getAsJsonObject("emojis")
        if (typeElement == null || emojisObject == null) {
            throw JsonParseException("Invalid json for EmojiCategory")
        }
        val typeString = typeElement.asString
        val type: EmojiCategoryType = try {
            EmojiCategoryType.valueOf(typeString)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "deserialize: ", e)
            EmojiCategoryType.OTHERS
        }
        val emojis: MutableMap<String, Emoji> = linkedMapOf()
        for ((unicode, value) in emojisObject.entrySet()) {
            if (unicode == null || value == null) {
                throw JsonParseException("Invalid json for EmojiCategory")
            }
            emojis[unicode] = context.deserialize(value, Emoji::class.java)
        }
        return EmojiCategory(type, emojis)
    }
}