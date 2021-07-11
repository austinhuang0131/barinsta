package awais.instagrabber.utils.emoji

import awais.instagrabber.customviews.emoji.Emoji
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class EmojiDeserializer : JsonDeserializer<Emoji> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Emoji {
        val jsonObject = json.asJsonObject
        val unicodeElement = jsonObject["unicode"]
        val nameElement = jsonObject["name"]
        if (unicodeElement == null || nameElement == null) {
            throw JsonParseException("Invalid json for Emoji class")
        }
        val variantsElement = jsonObject["variants"]
        val variants: MutableList<Emoji> = mutableListOf()
        if (variantsElement != null) {
            val variantsArray = variantsElement.asJsonArray
            for (variantElement in variantsArray) {
                val variant = context.deserialize<Emoji>(variantElement, Emoji::class.java)
                if (variant != null) {
                    variants.add(variant)
                }
            }
        }
        return Emoji(
            unicodeElement.asString,
            nameElement.asString,
            variants
        )
    }
}