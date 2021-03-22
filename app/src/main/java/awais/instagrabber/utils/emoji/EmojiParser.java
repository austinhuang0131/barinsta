package awais.instagrabber.utils.emoji;

import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.customviews.emoji.EmojiCategory;
import awais.instagrabber.customviews.emoji.EmojiCategoryType;
import awais.instagrabber.utils.NetworkUtils;

public final class EmojiParser {
    private static final String TAG = EmojiParser.class.getSimpleName();
    private static final Object LOCK = new Object();

    private static EmojiParser instance;

    private Map<String, Emoji> allEmojis = Collections.emptyMap();
    private Map<EmojiCategoryType, EmojiCategory> categoryMap = Collections.emptyMap();
    private ImmutableList<EmojiCategory> categories;

    public static EmojiParser getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new EmojiParser();
                }
            }
        }
        return instance;
    }

    private EmojiParser() {
        final String file = "res/raw/emojis.json";
        final ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            Log.e(TAG, "Emoji: classLoader is null");
            return;
        }
        try (final InputStream in = classLoader.getResourceAsStream(file)) {
            final String json = NetworkUtils.readFromInputStream(in);
            final Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(EmojiCategory.class, new EmojiCategoryDeserializer())
                    .registerTypeAdapter(Emoji.class, new EmojiDeserializer())
                    .setLenient()
                    .create();
            final Type type = new TypeToken<Map<EmojiCategoryType, EmojiCategory>>() {}.getType();
            categoryMap = gson.fromJson(json, type);
            // Log.d(TAG, "EmojiParser: " + categoryMap);
            allEmojis = categoryMap.values()
                                   .stream()
                                   .flatMap((Function<EmojiCategory, Stream<Emoji>>) emojiCategory -> {
                                       final Map<String, Emoji> emojis = emojiCategory.getEmojis();
                                       return emojis.values().stream();
                                   })
                                   .flatMap(emoji -> ImmutableList.<Emoji>builder()
                                           .add(emoji)
                                           .addAll(emoji.getVariants())
                                           .build()
                                           .stream())
                                   .collect(Collectors.toMap(Emoji::getUnicode, Function.identity()));
        } catch (Exception e) {
            Log.e(TAG, "EmojiParser: ", e);
        }
    }

    public Map<EmojiCategoryType, EmojiCategory> getCategoryMap() {
        return categoryMap;
    }

    public List<EmojiCategory> getEmojiCategories() {
        if (categories == null) {
            final Collection<EmojiCategory> categoryCollection = categoryMap.values();
            categories = ImmutableList.copyOf(categoryCollection);
        }
        return categories;
    }

    public Map<String, Emoji> getAllEmojis() {
        return allEmojis;
    }

    public Emoji getEmoji(final String emoji) {
        if (emoji == null) {
            return null;
        }
        return allEmojis.get(emoji);
    }
}

