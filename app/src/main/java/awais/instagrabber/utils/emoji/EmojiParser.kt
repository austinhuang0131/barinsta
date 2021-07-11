package awais.instagrabber.utils.emoji

import android.content.Context
import android.util.Log
import awais.instagrabber.R
import awais.instagrabber.customviews.emoji.Emoji
import awais.instagrabber.customviews.emoji.EmojiCategory
import awais.instagrabber.customviews.emoji.EmojiCategoryType
import awais.instagrabber.utils.NetworkUtils
import awais.instagrabber.utils.SingletonHolder
import awais.instagrabber.utils.extensions.TAG
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class EmojiParser private constructor(context: Context) {
    var allEmojis: Map<String, Emoji> = emptyMap()
    var categoryMap: Map<EmojiCategoryType, EmojiCategory> = emptyMap()
    val emojiCategories: List<EmojiCategory> by lazy {
        categoryMap.values.toList()
    }

    fun getEmoji(emoji: String): Emoji? {
        return allEmojis[emoji]
    }

    init {
        try {
            context.applicationContext.resources.openRawResource(R.raw.emojis).use { `in` ->
                val json = NetworkUtils.readFromInputStream(`in`)
                val gson = GsonBuilder().apply {
                    setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    registerTypeAdapter(EmojiCategory::class.java, EmojiCategoryDeserializer())
                    registerTypeAdapter(Emoji::class.java, EmojiDeserializer())
                    setLenient()
                }.create()
                val type = object : TypeToken<Map<EmojiCategoryType, EmojiCategory>>() {}.type
                categoryMap = gson.fromJson(json, type)
                // Log.d(TAG, "EmojiParser: " + categoryMap);
                allEmojis = categoryMap
                    .flatMap { (_, emojiCategory) -> emojiCategory.emojis.values }
                    .flatMap { listOf(it) + it.variants }
                    .filterNotNull()
                    .map { it.unicode to it }
                    .toMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "EmojiParser: ", e)
        }
    }

    companion object : SingletonHolder<EmojiParser, Context>(::EmojiParser)
}