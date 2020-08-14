package awais.instagrabber.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.activities.Main;
import awais.instagrabber.activities.ProfileViewer;
import awais.instagrabber.activities.SavedViewer;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.customviews.CommentMentionClickSpan;
import awais.instagrabber.databinding.DialogImportExportBinding;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemRavenMediaModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.InboxReadState;
import awais.instagrabber.models.enums.IntentModelType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.models.enums.RavenExpiringMediaType;
import awais.instagrabber.models.enums.RavenMediaViewType;
import awaisomereport.LogCollector;

import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemActionLogModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemAnimatedMediaModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemLinkContext;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemLinkModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemMediaModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemReelShareModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemVideoCallEventModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemVoiceMediaModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.RavenExpiringMediaActionSummaryModel;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;

public final class Utils {
    public static LogCollector logCollector;
    public static SettingsHelper settingsHelper;
    public static DataBox dataBox;
    public static boolean sessionVolumeFull = false;
    @SuppressLint("StaticFieldLeak")
    public static NotificationManagerCompat notificationManager;
    public static final CookieManager COOKIE_MANAGER = CookieManager.getInstance();
    public static final String[] PERMS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final java.net.CookieManager NET_COOKIE_MANAGER = new java.net.CookieManager(null, CookiePolicy.ACCEPT_ALL);
    public static final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public static final String CHANNEL_ID = "InstaGrabber", CHANNEL_NAME = "Instagrabber",
            NOTIF_GROUP_NAME = "awais.instagrabber.InstaNotif";
    public static boolean isChannelCreated = false;
    public static String telegramPackage;
    public static ClipboardManager clipboardManager;
    public static DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    public static SimpleDateFormat datetimeParser;

    public static void setupCookies(final String cookieRaw) {
        final CookieStore cookieStore = NET_COOKIE_MANAGER.getCookieStore();
        if (cookieRaw == "LOGOUT") {
            cookieStore.removeAll();
        }
        else if (cookieRaw != null && !isEmpty(cookieRaw)) {
            try {
                final URI uri1 = new URI("https://instagram.com");
                final URI uri2 = new URI("https://instagram.com/");
                final URI uri3 = new URI("https://i.instagram.com/");
                for (final String cookie : cookieRaw.split(";")) {
                    final String[] strings = cookie.split("=", 2);
                    final HttpCookie httpCookie = new HttpCookie(strings[0].trim(), strings[1].trim());
                    httpCookie.setDomain("instagram.com");
                    httpCookie.setPath("/");
                    httpCookie.setVersion(0);
                    cookieStore.add(uri1, httpCookie);
                    cookieStore.add(uri2, httpCookie);
                    cookieStore.add(uri3, httpCookie);
                }
            } catch (final URISyntaxException e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.UTILS, "setupCookies");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
    }

    @Nullable
    public static String getUserIdFromCookie(final String cookie) {
        if (!isEmpty(cookie)) {
            final int uidIndex = cookie.indexOf("ds_user_id");
            if (uidIndex > 0) {
                final int uidEndIndex = cookie.indexOf(';', uidIndex + 10);
                if (uidEndIndex > 0) {
                    final String uid = cookie.substring(uidIndex + 11, uidEndIndex);
                    return !isEmpty(uid) ? uid : null;
                }
            }
        }
        return null;
    }

    @Nullable
    public static IntentModel stripString(@NonNull String clipString) {
        final int wwwDel = clipString.contains("www.") ? 4 : 0;
        final boolean isHttps = clipString.startsWith("https");

        IntentModelType type = IntentModelType.UNKNOWN;
        if (clipString.contains("instagram.com/")) {
            clipString = clipString.substring((isHttps ? 22 : 21) + wwwDel);

            final char firstChar = clipString.charAt(0);
            if (clipString.startsWith("p/") || clipString.startsWith("reel/")) {
                clipString = clipString.substring(clipString.startsWith("p/") ? 2 : 5);
                type = IntentModelType.POST;
            } else if (clipString.startsWith("explore/tags/")) {
                clipString = clipString.substring(13);
                type = IntentModelType.HASHTAG;
            } else if (clipString.startsWith("explore/locations/")) {
                clipString = clipString.substring(18);
                type = IntentModelType.LOCATION;
            } else if (clipString.startsWith("_u/")) { // usually exists in embeds
                clipString = clipString.substring(3);
                type = IntentModelType.USERNAME;
            }

            clipString = cleanString(clipString);
        } else if (clipString.contains("ig.me/u/")) {
            clipString = clipString.substring((isHttps ? 16 : 15) + wwwDel);
            clipString = cleanString(clipString);
            type = IntentModelType.USERNAME;

        } else return null;

        final int clipLen = clipString.length() - 1;
        if (clipString.charAt(clipLen) == '/')
            clipString = clipString.substring(0, clipLen);

        if (!clipString.contains("/")) return new IntentModel(type, clipString);
        else return null;
    }

    @NonNull
    public static String cleanString(@NonNull final String clipString) {
        final int queryIndex = clipString.indexOf('?');
        final int paramIndex = clipString.indexOf('#');
        int startIndex = -1;
        if (queryIndex > 0 && paramIndex > 0) {
            if (queryIndex < paramIndex) startIndex = queryIndex;
            else if (paramIndex < queryIndex) startIndex = paramIndex;
        } else if (queryIndex == -1 && paramIndex > 0) startIndex = paramIndex;
        else if (paramIndex == -1 && queryIndex > 0) startIndex = queryIndex;
        return startIndex != -1 ? clipString.substring(0, startIndex) : clipString;
    }

    @NonNull
    public static CharSequence getMentionText(@NonNull final CharSequence text) {
        final int commentLength = text.length();
        final SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text, 0, commentLength);

        for (int i = 0; i < commentLength; ++i) {
            char currChar = text.charAt(i);

            if (currChar == '@' || currChar == '#') {
                final int startLen = i;

                do {
                    if (++i == commentLength) break;
                    currChar = text.charAt(i);

                    if (currChar == '.' && i + 1 < commentLength) {
                        final char nextChar = text.charAt(i + 1);
                        if (nextChar == '.' || nextChar == ' ' || nextChar == '#' || nextChar == '@' || nextChar == '/'
                                || nextChar == '\r' || nextChar == '\n') {
                            break;
                        }
                    }
                    else if (currChar == '.')
                        break;

                    // for merged hashtags
                    if (currChar == '#') {
                        --i;
                        break;
                    }
                } while (currChar != ' ' && currChar != '\r' && currChar != '\n' && currChar != '>' && currChar != '<'
                        && currChar != ':' && currChar != ';' && currChar != '\'' && currChar != '"' && currChar != '['
                        && currChar != ']' && currChar != '\\' && currChar != '=' && currChar != '-' && currChar != '!'
                        && currChar != '$' && currChar != '%' && currChar != '^' && currChar != '&' && currChar != '*'
                        && currChar != '(' && currChar != ')' && currChar != '{' && currChar != '}' && currChar != '/'
                        && currChar != '|' && currChar != '?' && currChar != '`' && currChar != '~'
                );

                final int endLen = currChar != '#' ? i : i + 1; // for merged hashtags
                stringBuilder.setSpan(new CommentMentionClickSpan(), startLen,
                        Math.min(commentLength, endLen), // fixed - crash when end index is greater than comment length ( @kernoeb )
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }

        return stringBuilder;
    }

    // isI: true if the content was requested from i.instagram.com instead of graphql
    @Nullable
    public static String getHighQualityPost(final JSONArray resources, final boolean isVideo, final boolean isI, final boolean low) {
        try {
            final int resourcesLen = resources.length();

            final String[] sources = new String[resourcesLen];
            int lastResMain = low ? 1000000 : 0, lastIndexMain = -1;
            int lastResBase = low ? 1000000 : 0, lastIndexBase = -1;
            for (int i = 0; i < resourcesLen; ++i) {
                final JSONObject item = resources.getJSONObject(i);
                if (item != null && (!isVideo || item.has(Constants.EXTRAS_PROFILE) || isI)) {
                    sources[i] = item.getString(isI ? "url" : "src");
                    final int currRes = item.getInt(isI ? "width" : "config_width") * item.getInt(isI ? "height" : "config_height");

                    final String profile = isVideo ? item.optString(Constants.EXTRAS_PROFILE) : null;

                    if (!isVideo || "MAIN".equals(profile)) {
                        if (currRes > lastResMain && !low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        }
                        else if (currRes < lastResMain && low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        }
                    } else {
                        if (currRes > lastResBase && !low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        }
                        else if (currRes < lastResBase && low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        }
                    }
                }
            }

            if (lastIndexMain >= 0) return sources[lastIndexMain];
            else if (lastIndexBase >= 0) return sources[lastIndexBase];
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.UTILS, "getHighQualityPost",
                        new Pair<>("resourcesNull", resources == null),
                        new Pair<>("isVideo", isVideo));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return null;
    }

    public static String getHighQualityImage(final JSONObject resources) {
        String src = null;
        try {
            if (resources.has("display_resources")) src = getHighQualityPost(resources.getJSONArray("display_resources"), false, false, false);
            else if (resources.has("image_versions2"))
                src = getHighQualityPost(resources.getJSONObject("image_versions2").getJSONArray("candidates"), false, true, false);
            if (src == null) return resources.getString("display_url");
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.UTILS, "getHighQualityImage",
                        new Pair<>("resourcesNull", resources == null));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return src;
    }

    public static String getLowQualityImage(final JSONObject resources) {
        String src = null;
        try {
            src = getHighQualityPost(resources.getJSONObject("image_versions2").getJSONArray("candidates"), false, true, true);
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.UTILS, "getLowQualityImage",
                        new Pair<>("resourcesNull", resources == null));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return src;
    }

    public static String getItemThumbnail(@NonNull final JSONArray jsonArray) {
        String thumbnail = null;
        final int imageResLen = jsonArray.length();

        for (int i = 0; i < imageResLen; ++i) {
            final JSONObject imageResource = jsonArray.optJSONObject(i);
            try {
                final int width = imageResource.getInt("width");
                final int height = imageResource.getInt("height");
                final float ratio = Float.parseFloat(String.format(Locale.ENGLISH, "%.2f", (float) height / width));
                if (ratio >= 0.95f && ratio <= 1.0f) {
                    thumbnail = imageResource.getString("url");
                    break;
                }
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.UTILS, "getItemThumbnail");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                thumbnail = null;
            }
        }

        if (Utils.isEmpty(thumbnail)) thumbnail = jsonArray.optJSONObject(0).optString("url");

        return thumbnail;
    }

    @Nullable
    public static String getThumbnailUrl(@NonNull final JSONObject mediaObj, final MediaItemType mediaType) throws Exception {
        String thumbnail = null;

        if (mediaType == MediaItemType.MEDIA_TYPE_IMAGE || mediaType == MediaItemType.MEDIA_TYPE_VIDEO) {
            final JSONObject imageVersions = mediaObj.optJSONObject("image_versions2");
            if (imageVersions != null)
                thumbnail = Utils.getItemThumbnail(imageVersions.getJSONArray("candidates"));

        } else if (mediaType == MediaItemType.MEDIA_TYPE_SLIDER) {
            final JSONArray carouselMedia = mediaObj.optJSONArray("carousel_media");
            if (carouselMedia != null) thumbnail = Utils.getItemThumbnail(carouselMedia.getJSONObject(0)
                    .getJSONObject("image_versions2").getJSONArray("candidates"));
        }

        return thumbnail;
    }

    public static String getVideoUrl(@NonNull final JSONObject mediaObj) throws Exception {
        String thumbnail = null;

        final JSONArray imageVersions = mediaObj.optJSONArray("video_versions");
        if (imageVersions != null)
            thumbnail = Utils.getItemThumbnail(imageVersions);

        return thumbnail;
    }

    @Nullable
    public static MediaItemType getMediaItemType(final int mediaType) {
        if (mediaType == 1) return MediaItemType.MEDIA_TYPE_IMAGE;
        if (mediaType == 2) return MediaItemType.MEDIA_TYPE_VIDEO;
        if (mediaType == 8) return MediaItemType.MEDIA_TYPE_SLIDER;
        if (mediaType == 11) return MediaItemType.MEDIA_TYPE_VOICE;
        return null;
    }

    public static DirectItemMediaModel getDirectMediaModel(final JSONObject mediaObj) throws Exception {
        final DirectItemMediaModel mediaModel;
        if (mediaObj == null) mediaModel = null;
        else {
            final JSONObject userObj = mediaObj.optJSONObject("user");

            ProfileModel user = null;
            if (userObj != null) {
                user = new ProfileModel(
                        userObj.getBoolean("is_private"),
                        false, // temporary
                        userObj.optBoolean("is_verified"),
                        String.valueOf(userObj.get("pk")),
                        userObj.getString("username"),
                        userObj.getString("full_name"),
                        null, null,
                        userObj.getString("profile_pic_url"),
                        null, 0, 0, 0, false, false, false, false);
            }

            final MediaItemType mediaType = getMediaItemType(mediaObj.optInt("media_type", -1));

            String id = mediaObj.optString("id");
            if (Utils.isEmpty(id)) id = null;

            mediaModel = new DirectItemMediaModel(mediaType,
                    mediaObj.optLong("expiring_at"),
                    mediaObj.optLong("pk"),
                    id,
                    getThumbnailUrl(mediaObj, mediaType),
                    mediaType == MediaItemType.MEDIA_TYPE_VIDEO ? getVideoUrl(mediaObj) : null,
                    user,
                    mediaObj.optString("code"));
        }
        return mediaModel;
    }

    private static DirectItemType getDirectItemType(final String itemType) {
        if ("placeholder".equals(itemType)) return DirectItemType.PLACEHOLDER;
        if ("media".equals(itemType)) return DirectItemType.MEDIA;
        if ("link".equals(itemType)) return DirectItemType.LINK;
        if ("like".equals(itemType)) return DirectItemType.LIKE;
        if ("reel_share".equals(itemType)) return DirectItemType.REEL_SHARE;
        if ("media_share".equals(itemType)) return DirectItemType.MEDIA_SHARE;
        if ("action_log".equals(itemType)) return DirectItemType.ACTION_LOG;
        if ("raven_media".equals(itemType)) return DirectItemType.RAVEN_MEDIA;
        if ("profile".equals(itemType)) return DirectItemType.PROFILE;
        if ("video_call_event".equals(itemType)) return DirectItemType.VIDEO_CALL_EVENT;
        if ("animated_media".equals(itemType)) return DirectItemType.ANIMATED_MEDIA;
        if ("voice_media".equals(itemType)) return DirectItemType.VOICE_MEDIA;
        if ("story_share".equals(itemType)) return DirectItemType.STORY_SHARE;
        return DirectItemType.TEXT;
    }

    @NonNull
    public static InboxThreadModel createInboxThreadModel(@NonNull final JSONObject data, final boolean inThreadView) throws Exception {
        final InboxReadState readState = data.getInt("read_state") == 0 ? InboxReadState.STATE_READ : InboxReadState.STATE_UNREAD;
        final String threadType = data.getString("thread_type");// private = dms, [??] = group

        final String threadId = data.getString("thread_id");
        final String threadV2Id = data.getString("thread_v2_id");
        final String threadTitle = data.getString("thread_title");

        final String threadNewestCursor = data.getString("newest_cursor");
        final String threadOldestCursor = data.getString("oldest_cursor");
        final String threadNextCursor = data.has("next_cursor") ? data.getString("next_cursor") : null;
        final String threadPrevCursor = data.has("prev_cursor") ? data.getString("prev_cursor") : null;

        final boolean threadHasOlder = data.getBoolean("has_older");
        final boolean threadHasNewer = data.getBoolean("has_newer");

        final long lastActivityAt = data.optLong("last_activity_at");
        final boolean named = data.optBoolean("named");
        final boolean muted = data.optBoolean("muted");
        final boolean isPin = data.optBoolean("is_pin");
        final boolean isSpam = data.optBoolean("is_spam");
        final boolean isGroup = data.optBoolean("is_group");
        final boolean pending = data.optBoolean("pending");
        final boolean archived = data.optBoolean("archived");
        final boolean canonical = data.optBoolean("canonical");

        final JSONArray users = data.getJSONArray("users");
        final int usersLen = users.length();
        final JSONArray leftusers = data.getJSONArray("left_users");
        final int leftusersLen = leftusers.length();

        final ProfileModel[] userModels = new ProfileModel[usersLen];
        for (int j = 0; j < usersLen; ++j) {
            final JSONObject userObject = users.getJSONObject(j);
            userModels[j] = new ProfileModel(userObject.getBoolean("is_private"),
                    false,
                    userObject.optBoolean("is_verified"),
                    String.valueOf(userObject.get("pk")),
                    userObject.getString("username"),
                    userObject.getString("full_name"),
                    null, null,
                    userObject.getString("profile_pic_url"),
                    null, 0, 0, 0, false, false, false, false);
        }

        final ProfileModel[] leftuserModels = new ProfileModel[leftusersLen];
        for (int j = 0; j < leftusersLen; ++j) {
            final JSONObject userObject = leftusers.getJSONObject(j);
            leftuserModels[j] = new ProfileModel(userObject.getBoolean("is_private"),
                    false,
                    userObject.optBoolean("is_verified"),
                    String.valueOf(userObject.get("pk")),
                    userObject.getString("username"),
                    userObject.getString("full_name"),
                    null, null,
                    userObject.getString("profile_pic_url"),
                    null, 0, 0, 0, false, false, false, false);
        }

        final JSONArray items = data.getJSONArray("items");
        final int itemsLen = items.length();

        final ArrayList<DirectItemModel> itemModels = new ArrayList<>(itemsLen);
        for (int i = 0; i < itemsLen; ++i) {
            final JSONObject itemObject = items.getJSONObject(i);

            CharSequence text = null;
            ProfileModel profileModel = null;
            DirectItemLinkModel linkModel = null;
            DirectItemMediaModel directMedia = null;
            DirectItemReelShareModel reelShareModel = null;
            DirectItemActionLogModel actionLogModel = null;
            DirectItemAnimatedMediaModel animatedMediaModel = null;
            DirectItemVoiceMediaModel voiceMediaModel = null;
            DirectItemRavenMediaModel ravenMediaModel = null;
            DirectItemVideoCallEventModel videoCallEventModel = null;

            final DirectItemType itemType = getDirectItemType(itemObject.getString("item_type"));
            switch (itemType) {
                case ANIMATED_MEDIA: {
                    final JSONObject animatedMedia = itemObject.getJSONObject("animated_media");
                    final JSONObject stickerImage = animatedMedia.getJSONObject("images").getJSONObject("fixed_height");

                    animatedMediaModel = new DirectItemAnimatedMediaModel(animatedMedia.getBoolean("is_random"),
                            animatedMedia.getBoolean("is_sticker"), animatedMedia.getString("id"),
                            stickerImage.getString("url"), stickerImage.optString("webp"), stickerImage.optString("mp4"),
                            stickerImage.getInt("height"), stickerImage.getInt("width"));
                }
                break;

                case VOICE_MEDIA: {
                    final JSONObject voiceMedia = itemObject.getJSONObject("voice_media").getJSONObject("media");
                    final JSONObject audio = voiceMedia.getJSONObject("audio");

                    int[] waveformData = null;
                    final JSONArray waveformDataArray = audio.optJSONArray("waveform_data");
                    if (waveformDataArray != null) {
                        final int waveformDataLen = waveformDataArray.length();
                        waveformData = new int[waveformDataLen];
                        // 0.011775206
                        for (int j = 0; j < waveformDataLen; ++j) {
                            waveformData[j] = (int) (waveformDataArray.optDouble(j) * 10);
                        }
                    }

                    voiceMediaModel = new DirectItemVoiceMediaModel(voiceMedia.getString("id"),
                            audio.getString("audio_src"), audio.getLong("duration"),
                            waveformData);
                }
                break;

                case LINK: {
                    final JSONObject linkObj = itemObject.getJSONObject("link");

                    DirectItemLinkContext itemLinkContext = null;
                    final JSONObject linkContext = linkObj.optJSONObject("link_context");
                    if (linkContext != null) {
                        itemLinkContext = new DirectItemLinkContext(
                                linkContext.getString("link_url"),
                                linkContext.optString("link_title"),
                                linkContext.optString("link_summary"),
                                linkContext.optString("link_image_url")
                        );
                    }

                    linkModel = new DirectItemLinkModel(linkObj.getString("text"),
                            linkObj.getString("client_context"),
                            linkObj.optString("mutation_token"),
                            itemLinkContext);
                }
                break;

                case REEL_SHARE: {
                    final JSONObject reelShare = itemObject.getJSONObject("reel_share");
                    reelShareModel = new DirectItemReelShareModel(
                            reelShare.optBoolean("is_reel_persisted"),
                            reelShare.getLong("reel_owner_id"),
                            reelShare.getJSONObject("media").getJSONObject("user").getString("username"),
                            reelShare.getString("text"),
                            reelShare.getString("type"),
                            reelShare.getString("reel_type"),
                            reelShare.optString("reel_name"),
                            reelShare.optString("reel_id"),
                            getDirectMediaModel(reelShare.optJSONObject("media")));
                }
                break;

                case RAVEN_MEDIA: {
                    final JSONObject visualMedia = itemObject.getJSONObject("visual_media");

                    final JSONArray seenUserIdsArray = visualMedia.getJSONArray("seen_user_ids");
                    final int seenUsersLen = seenUserIdsArray.length();
                    final String[] seenUserIds = new String[seenUsersLen];
                    for (int j = 0; j < seenUsersLen; j++) seenUserIds[j] = seenUserIdsArray.getString(j);

                    RavenExpiringMediaActionSummaryModel expiringSummaryModel = null;
                    final JSONObject actionSummary = visualMedia.optJSONObject("expiring_media_action_summary");
                    if (actionSummary != null) expiringSummaryModel = new RavenExpiringMediaActionSummaryModel(
                            actionSummary.getLong("timestamp"), actionSummary.getInt("count"),
                            getExpiringMediaType(actionSummary.getString("type")));

                    final RavenMediaViewType viewType;
                    final String viewMode = visualMedia.getString("view_mode");
                    switch (viewMode) {
                        case "replayable":
                            viewType = RavenMediaViewType.REPLAYABLE;
                            break;
                        case "permanent":
                            viewType = RavenMediaViewType.PERMANENT;
                            break;
                        case "once":
                        default:
                            viewType = RavenMediaViewType.ONCE;
                    }

                    ravenMediaModel = new DirectItemRavenMediaModel(
                            visualMedia.optLong(viewType == RavenMediaViewType.PERMANENT ? "url_expire_at_secs" : "replay_expiring_at_us"),
                            visualMedia.optInt("playback_duration_secs"),
                            visualMedia.getInt("seen_count"),
                            seenUserIds,
                            viewType,
                            getDirectMediaModel(visualMedia.optJSONObject("media")),
                            expiringSummaryModel);

                }
                break;

                case VIDEO_CALL_EVENT: {
                    final JSONObject videoCallEvent = itemObject.getJSONObject("video_call_event");
                    videoCallEventModel = new DirectItemVideoCallEventModel(videoCallEvent.getLong("vc_id"),
                            videoCallEvent.optBoolean("thread_has_audio_only_call"),
                            videoCallEvent.getString("action"),
                            videoCallEvent.getString("description"));
                }
                break;

                case PROFILE: {
                    final JSONObject profile = itemObject.getJSONObject("profile");
                    profileModel = new ProfileModel(profile.getBoolean("is_private"),
                            false,
                            profile.getBoolean("is_verified"),
                            Long.toString(profile.getLong("pk")),
                            profile.getString("username"),
                            profile.getString("full_name"),
                            null, null,
                            profile.getString("profile_pic_url"),
                            null, 0, 0, 0, false, false, false, false);
                }
                break;

                case PLACEHOLDER:
                    final JSONObject placeholder = itemObject.getJSONObject("placeholder");
                    text = placeholder.getString("title") + "<br><small>" + placeholder.getString("message") + "</small>";
                    break;

                case ACTION_LOG:
                    if (inThreadView && itemObject.optInt("hide_in_thread", 0) != 0)
                        continue;
                    final JSONObject actionLog = itemObject.getJSONObject("action_log");
                    String desc = actionLog.getString("description");
                    JSONArray bold = actionLog.getJSONArray("bold");
                    for (int q=0; q < bold.length(); ++q) {
                        JSONObject boldItem = bold.getJSONObject(q);
                        desc = desc.substring(0, boldItem.getInt("start") + q*7) + "<b>"
                                + desc.substring(boldItem.getInt("start") + q*7, boldItem.getInt("end") + q*7)
                                + "</b>" + desc.substring(boldItem.getInt("end") + q*7, desc.length());
                    }
                    actionLogModel = new DirectItemActionLogModel(desc);
                    break;

                case MEDIA_SHARE:
                    directMedia = getDirectMediaModel(itemObject.getJSONObject("media_share"));
                    break;

                case MEDIA:
                    directMedia = getDirectMediaModel(itemObject.optJSONObject("media"));
                    break;

                case LIKE:
                    text = itemObject.getString("like");
                    break;

                case STORY_SHARE:
                    final JSONObject storyShare = itemObject.getJSONObject("story_share");
                    if (!storyShare.has("media"))
                        text = "<small>" + storyShare.optString("message") + "</small>";
                    else {
                        reelShareModel = new DirectItemReelShareModel(
                                storyShare.optBoolean("is_reel_persisted"),
                                storyShare.getJSONObject("media").getJSONObject("user").getLong("pk"),
                                storyShare.getJSONObject("media").getJSONObject("user").getString("username"),
                                storyShare.getString("text"),
                                storyShare.getString("story_share_type"),
                                storyShare.getString("reel_type"),
                                storyShare.optString("reel_name"),
                                storyShare.optString("reel_id"),
                                getDirectMediaModel(storyShare.optJSONObject("media")));
                    }
                    break;

                case TEXT:
                    if (!itemObject.has("text"))
                        Log.d("AWAISKING_APP", "itemObject: " + itemObject); // todo
                    text = itemObject.optString("text");
                    break;
            }

            itemModels.add(new DirectItemModel(
                    itemObject.getLong("user_id"),
                    itemObject.getLong("timestamp"),
                    itemObject.getString("item_id"),
                    itemType,
                    text,
                    linkModel,
                    profileModel,
                    reelShareModel,
                    directMedia,
                    actionLogModel,
                    voiceMediaModel,
                    ravenMediaModel,
                    videoCallEventModel,
                    animatedMediaModel));
        }

        itemModels.trimToSize();

        return new InboxThreadModel(readState, threadId, threadV2Id, threadType, threadTitle,
                threadNewestCursor, threadOldestCursor, threadNextCursor, threadPrevCursor,
                null, // todo
                userModels, leftuserModels,
                itemModels.toArray(new DirectItemModel[0]),
                muted, isPin, named, canonical,
                pending, threadHasOlder, threadHasNewer, isSpam, isGroup, archived, lastActivityAt);
    }

    private static RavenExpiringMediaType getExpiringMediaType(final String type) {
        if ("raven_sent".equals(type)) return RavenExpiringMediaType.RAVEN_SENT;
        if ("raven_opened".equals(type)) return RavenExpiringMediaType.RAVEN_OPENED;
        if ("raven_blocked".equals(type)) return RavenExpiringMediaType.RAVEN_BLOCKED;
        if ("raven_sending".equals(type)) return RavenExpiringMediaType.RAVEN_SENDING;
        if ("raven_replayed".equals(type)) return RavenExpiringMediaType.RAVEN_REPLAYED;
        if ("raven_delivered".equals(type)) return RavenExpiringMediaType.RAVEN_DELIVERED;
        if ("raven_suggested".equals(type)) return RavenExpiringMediaType.RAVEN_SUGGESTED;
        if ("raven_screenshot".equals(type)) return RavenExpiringMediaType.RAVEN_SCREENSHOT;
        if ("raven_cannot_deliver".equals(type)) return RavenExpiringMediaType.RAVEN_CANNOT_DELIVER;
        //if ("raven_unknown".equals(type)) [default?]
        return RavenExpiringMediaType.RAVEN_UNKNOWN;
    }

    public static NotificationType getNotifType(final String itemType) {
        if ("GraphLikeAggregatedStory".equals(itemType)) return NotificationType.LIKE;
        if ("GraphFollowAggregatedStory".equals(itemType)) return NotificationType.FOLLOW;
        if ("GraphCommentMediaStory".equals(itemType)) return NotificationType.COMMENT;
        if ("GraphMentionStory".equals(itemType)) return NotificationType.MENTION;
        return null;
    }

    public static int convertDpToPx(final float dp) {
        if (displayMetrics == null)
            displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round((dp * displayMetrics.densityDpi) / 160.0f);
    }

    public static void changeTheme(final Context context) {
        int themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // this is fallback / default

        if (settingsHelper != null) themeCode = settingsHelper.getThemeCode(false);

        if (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && Build.VERSION.SDK_INT < 29)
            themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;

        boolean isAmoledEnabled = false;
        if (settingsHelper != null) {
            isAmoledEnabled = settingsHelper.getBoolean(Constants.AMOLED_THEME);
        }
        // use amoled theme only if enabled in settings
        if (isAmoledEnabled) {
            // check if setting is set to 'Dark'
            boolean isNight = themeCode == AppCompatDelegate.MODE_NIGHT_YES;
            // if not dark check if themeCode is MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_AUTO_BATTERY
            if (!isNight && (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || themeCode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)) {
                // check if resulting theme would be NIGHT
                final int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                isNight = uiMode == Configuration.UI_MODE_NIGHT_YES;
            }
            if (isNight) {
                // set amoled theme
                Log.d("InstaGrabber", "settings amoled theme");
                context.setTheme(R.style.Theme_Amoled);
                return;
            }
        }
        AppCompatDelegate.setDefaultNightMode(themeCode);
    }

    public static void setTooltipText(final View view, @StringRes final int tooltipTextRes) {
        if (view != null && tooltipTextRes != 0 && tooltipTextRes != -1) {
            final Context context = view.getContext();
            final String tooltipText = context.getResources().getString(tooltipTextRes);

            if (Build.VERSION.SDK_INT >= 26) view.setTooltipText(tooltipText);
            else view.setOnLongClickListener(v -> {
                Toast.makeText(context, tooltipText, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    @NonNull
    public static String millisToString(final long timeMs) {
        final long totalSeconds = timeMs / 1000;

        final long seconds = totalSeconds % 60;
        final long minutes = totalSeconds / 60 % 60;
        final long hours = totalSeconds / 3600;

        final String strSec = Long.toString(seconds);
        final String strMin = Long.toString(minutes);

        final String strRetSec = strSec.length() > 1 ? strSec : "0" + seconds;
        final String strRetMin = strMin.length() > 1 ? strMin : "0" + minutes;

        final String retMinSec = strRetMin + ':' + strRetSec;

        if (hours > 0)
            return Long.toString(hours) + ':' + retMinSec;
        return retMinSec;
    }

    // extracted from String class
    public static int indexOfChar(@NonNull final CharSequence sequence, final int ch, final int startIndex) {
        final int max = sequence.length();
        if (startIndex < max) {
            if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                for (int i = startIndex; i < max; i++) if (sequence.charAt(i) == ch) return i;
            } else if (Character.isValidCodePoint(ch)) {
                final char hi = (char) ((ch >>> 10) + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
                final char lo = (char) ((ch & 0x3ff) + Character.MIN_LOW_SURROGATE);
                for (int i = startIndex; i < max; i++)
                    if (sequence.charAt(i) == hi && sequence.charAt(i + 1) == lo) return i;
            }
        }
        return -1;
    }

    public static boolean hasMentions(final CharSequence text) {
        if (isEmpty(text)) return false;
        return Utils.indexOfChar(text, '@', 0) != -1 || Utils.indexOfChar(text, '#', 0) != -1;
    }

    public static void copyText(final Context context, final CharSequence string) {
        final boolean ctxNotNull = context != null;
        if (ctxNotNull && clipboardManager == null)
            clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        int toastMessage = R.string.clipboard_error;
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(Utils.CHANNEL_NAME, string));
            toastMessage = R.string.clipboard_copied;
        }
        if (ctxNotNull) Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    public static String readFromConnection(@NonNull final HttpURLConnection conn) throws Exception {
        final StringBuilder sb = new StringBuilder();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public static void batchDownload(@NonNull final Context context, @Nullable String username, final DownloadMethod method,
                                     final List<? extends BasePostModel> itemsToDownload) {
        if (settingsHelper == null) settingsHelper = new SettingsHelper(context);

        if (itemsToDownload == null || itemsToDownload.size() < 1) return;

        if (username != null && username.charAt(0) == '@') username = username.substring(1);

        if (ContextCompat.checkSelfPermission(context, Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED)
            batchDownloadImpl(context, username, method, itemsToDownload);
        else if (context instanceof Activity)
            ActivityCompat.requestPermissions((Activity) context, Utils.PERMS, 8020);
    }

    private static void batchDownloadImpl(@NonNull final Context context, @Nullable final String username,
                                          final DownloadMethod method, final List<? extends BasePostModel> itemsToDownload) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");

        if (settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
            final String customPath = settingsHelper.getString(FOLDER_PATH);
            if (!Utils.isEmpty(customPath)) dir = new File(customPath);
        }

        if (settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !isEmpty(username))
            dir = new File(dir, username);

        if (dir.exists() || dir.mkdirs()) {
            final Main main = method != DownloadMethod.DOWNLOAD_FEED && context instanceof Main ? (Main) context : null;
            final ProfileViewer pv = method == DownloadMethod.DOWNLOAD_MAIN && context instanceof ProfileViewer ? (ProfileViewer) context : null;
            final SavedViewer saved = method == DownloadMethod.DOWNLOAD_SAVED && context instanceof SavedViewer ? (SavedViewer) context : null;

            final int itemsToDownloadSize = itemsToDownload.size();

            final File finalDir = dir;
            for (int i = itemsToDownloadSize - 1; i >= 0; i--) {
                final BasePostModel selectedItem = itemsToDownload.get(i);

                if (main == null && saved == null && pv == null) {
                    new DownloadAsync(context,
                            selectedItem.getDisplayUrl(),
                            getDownloadSaveFile(finalDir, selectedItem, ""),
                            null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                } else {
                    new PostFetcher(selectedItem.getShortCode(), result -> {
                        if (result != null) {
                            final int resultsSize = result.length;
                            final boolean multiResult = resultsSize > 1;

                            for (int j = 0; j < resultsSize; j++) {
                                final BasePostModel model = result[j];
                                final File saveFile = getDownloadSaveFile(finalDir, model, multiResult ? "_slide_" + (j + 1) : "");

                                new DownloadAsync(context,
                                        model.getDisplayUrl(),
                                        saveFile,
                                        file -> {
                                            model.setDownloaded(true);
                                            if (saved != null) saved.deselectSelection(selectedItem);
                                            else if (main != null) main.mainHelper.deselectSelection(selectedItem);
                                            else if (pv != null) pv.deselectSelection(selectedItem);
                                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        } else {
                            if (saved != null) saved.deselectSelection(selectedItem);
                            else if (main != null) main.mainHelper.deselectSelection(selectedItem);
                            else if (pv != null) pv.deselectSelection(selectedItem);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        } else
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
    }

    public static void dmDownload(@NonNull final Context context, @Nullable final String username, final DownloadMethod method,
                                     final List<? extends DirectItemMediaModel> itemsToDownload) {
        if (settingsHelper == null) settingsHelper = new SettingsHelper(context);

        if (itemsToDownload == null || itemsToDownload.size() < 1) return;

        if (ContextCompat.checkSelfPermission(context, Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED)
            dmDownloadImpl(context, username, method, itemsToDownload);
        else if (context instanceof Activity)
            ActivityCompat.requestPermissions((Activity) context, Utils.PERMS, 8020);
    }

    private static void dmDownloadImpl(@NonNull final Context context, @Nullable final String username,
                                          final DownloadMethod method, final List<? extends DirectItemMediaModel> itemsToDownload) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");

        if (settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
            final String customPath = settingsHelper.getString(FOLDER_PATH);
            if (!Utils.isEmpty(customPath)) dir = new File(customPath);
        }

        if (settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !isEmpty(username))
            dir = new File(dir, username);

        if (dir.exists() || dir.mkdirs()) {
            final Main main = method != DownloadMethod.DOWNLOAD_FEED && context instanceof Main ? (Main) context : null;

            final int itemsToDownloadSize = itemsToDownload.size();

            final File finalDir = dir;
            for (int i = itemsToDownloadSize - 1; i >= 0; i--) {
                final DirectItemMediaModel selectedItem = itemsToDownload.get(i);

                if (main == null) {
                    new DownloadAsync(context,
                            selectedItem.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO ? selectedItem.getVideoUrl() : selectedItem.getThumbUrl(),
                            getDownloadSaveFileDm(finalDir, selectedItem, ""),
                            null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                }
            }
        } else
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private static File getDownloadSaveFile(final File finalDir, @NonNull final BasePostModel model, final String sliderPrefix) {
        final String displayUrl = model.getDisplayUrl();
        return new File(finalDir, model.getPostId() + '_' + model.getPosition() + sliderPrefix +
                getExtensionFromModel(displayUrl, model));
    }

    @NonNull
    private static File getDownloadSaveFileDm(final File finalDir, @NonNull final DirectItemMediaModel model, final String sliderPrefix) {
        final String displayUrl = model.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO ? model.getVideoUrl() : model.getThumbUrl();
        return new File(finalDir, model.getId() + sliderPrefix +
                getExtensionFromModel(displayUrl, model));
    }

    @NonNull
    public static String getExtensionFromModel(@NonNull final String url, final Object model) {
        final String extension;
        final int index = url.indexOf('?');

        if (index != -1) extension = url.substring(index - 4, index);
        else {
            final boolean isVideo;
            if (model instanceof StoryModel)
                isVideo = ((StoryModel) model).getItemType() == MediaItemType.MEDIA_TYPE_VIDEO;
            else if (model instanceof BasePostModel)
                isVideo = ((BasePostModel) model).getItemType() == MediaItemType.MEDIA_TYPE_VIDEO;
            else
                isVideo = false;
            extension = isVideo || url.contains(".mp4") ? ".mp4" : ".jpg";
        }

        return extension;
    }

    public static void checkExistence(final File downloadDir, final File customDir, final boolean isSlider,
                                      @NonNull final BasePostModel model) {
        boolean exists = false;

        try {
            final String displayUrl = model.getDisplayUrl();
            final int index = displayUrl.indexOf('?');

            final String fileName = model.getPostId() + '_';
            final String extension = displayUrl.substring(index - 4, index);

            final String fileWithoutPrefix = fileName + '0' + extension;
            exists = new File(downloadDir, fileWithoutPrefix).exists();
            if (!exists) {
                final String fileWithPrefix = fileName + "[\\d]+(|_slide_[\\d]+)(\\.mp4|\\" + extension + ")";
                final FilenameFilter filenameFilter = (dir, name) -> Pattern.matches(fileWithPrefix, name);

                File[] files = downloadDir.listFiles(filenameFilter);
                if ((files == null || files.length < 1) && customDir != null)
                    files = customDir.listFiles(filenameFilter);

                if (files != null && files.length >= 1) exists = true;
            }
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.UTILS, "checkExistence",
                        new Pair<>("isSlider", isSlider),
                        new Pair<>("model", model));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        model.setDownloaded(exists);
    }

    public static boolean hasKey(final String key, final String username, final String name) {
        if (!Utils.isEmpty(key)) {
            final boolean hasUserName = username != null && username.toLowerCase().contains(key);
            if (!hasUserName && name != null) return name.toLowerCase().contains(key);
        }
        return true;
    }

    public static void showImportExportDialog(final Context context) {
        final DialogImportExportBinding importExportBinding = DialogImportExportBinding.inflate(LayoutInflater.from(context));

        final View passwordParent = (View) importExportBinding.cbPassword.getParent();
        final View exportLoginsParent = (View) importExportBinding.cbExportLogins.getParent();
        final View exportFavoritesParent = (View) importExportBinding.cbExportFavorites.getParent();
        final View exportSettingsParent = (View) importExportBinding.cbExportSettings.getParent();
        final View importLoginsParent = (View) importExportBinding.cbImportLogins.getParent();
        final View importFavoritesParent = (View) importExportBinding.cbImportFavorites.getParent();
        final View importSettingsParent = (View) importExportBinding.cbImportSettings.getParent();

        importExportBinding.cbPassword.setOnCheckedChangeListener((buttonView, isChecked) ->
                importExportBinding.etPassword.etPassword.setEnabled(isChecked));

        final AlertDialog[] dialog = new AlertDialog[1];
        final View.OnClickListener onClickListener = v -> {
            if (v == passwordParent) importExportBinding.cbPassword.performClick();

            else if (v == exportLoginsParent) importExportBinding.cbExportLogins.performClick();
            else if (v == exportFavoritesParent) importExportBinding.cbExportFavorites.performClick();

            else if (v == importLoginsParent) importExportBinding.cbImportLogins.performClick();
            else if (v == importFavoritesParent) importExportBinding.cbImportFavorites.performClick();

            else if (v == exportSettingsParent) importExportBinding.cbExportSettings.performClick();
            else if (v == importSettingsParent) importExportBinding.cbImportSettings.performClick();

            else if (context instanceof AppCompatActivity) {
                final FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
                final String folderPath = settingsHelper.getString(FOLDER_PATH);

                if (v == importExportBinding.btnSaveTo) {
                    final Editable text = importExportBinding.etPassword.etPassword.getText();
                    final boolean passwordChecked = importExportBinding.cbPassword.isChecked();
                    if (passwordChecked && isEmpty(text))
                        Toast.makeText(context, R.string.dialog_export_err_password_empty, Toast.LENGTH_SHORT).show();
                    else {
                        new DirectoryChooser().setInitialDirectory(folderPath).setInteractionListener(path -> {
                            final File file = new File(path, "InstaGrabber_Settings_" + System.currentTimeMillis() + ".zaai");
                            final String password = passwordChecked ? text.toString() : null;
                            int flags = 0;
                            if (importExportBinding.cbExportFavorites.isChecked()) flags |= ExportImportUtils.FLAG_FAVORITES;
                            if (importExportBinding.cbExportSettings.isChecked()) flags |= ExportImportUtils.FLAG_SETTINGS;
                            if (importExportBinding.cbExportLogins.isChecked()) flags |= ExportImportUtils.FLAG_COOKIES;

                            ExportImportUtils.Export(password, flags, file, result -> {
                                Toast.makeText(context, result ? R.string.dialog_export_success : R.string.dialog_export_failed, Toast.LENGTH_SHORT).show();
                                if (dialog[0] != null && dialog[0].isShowing()) dialog[0].dismiss();
                            });

                        }).show(fragmentManager, null);
                    }

                } else if (v == importExportBinding.btnImport) {
                    new DirectoryChooser().setInitialDirectory(folderPath).setShowZaAiConfigFiles(true).setInteractionListener(path -> {
                        int flags = 0;
                        if (importExportBinding.cbImportFavorites.isChecked()) flags |= ExportImportUtils.FLAG_FAVORITES;
                        if (importExportBinding.cbImportSettings.isChecked()) flags |= ExportImportUtils.FLAG_SETTINGS;
                        if (importExportBinding.cbImportLogins.isChecked()) flags |= ExportImportUtils.FLAG_COOKIES;

                        ExportImportUtils.Import(context, flags, new File(path), result -> {
                            ((AppCompatActivity) context).recreate();
                            Toast.makeText(context, result ? R.string.dialog_import_success : R.string.dialog_import_failed, Toast.LENGTH_SHORT).show();
                            if (dialog[0] != null && dialog[0].isShowing()) dialog[0].dismiss();
                        });

                    }).show(fragmentManager, null);
                }
            }
        };

        passwordParent.setOnClickListener(onClickListener);
        exportLoginsParent.setOnClickListener(onClickListener);
        exportSettingsParent.setOnClickListener(onClickListener);
        exportFavoritesParent.setOnClickListener(onClickListener);
        importLoginsParent.setOnClickListener(onClickListener);
        importSettingsParent.setOnClickListener(onClickListener);
        importFavoritesParent.setOnClickListener(onClickListener);
        importExportBinding.btnSaveTo.setOnClickListener(onClickListener);
        importExportBinding.btnImport.setOnClickListener(onClickListener);

        dialog[0] = new AlertDialog.Builder(context).setView(importExportBinding.getRoot()).show();
    }

    public static String sign(final String message) {
        try {
            Mac hasher = Mac.getInstance("HmacSHA256");
            hasher.init(new SecretKeySpec(Constants.SIGNATURE_KEY.getBytes(), "HmacSHA256"));
            byte[] hash = hasher.doFinal(message.getBytes());
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "ig_sig_key_version="+Constants.SIGNATURE_VERSION+"&signed_body=" + hexString.toString() + "." + message;
        }
        catch (Throwable e) {
            Log.e("austin_debug", "sign: ", e);
            return null;
        }
    }

    public static CharSequence getSpannableUrl(final String url) {
        if (Utils.isEmpty(url)) return url;
        final int httpIndex = url.indexOf("http:");
        final int httpsIndex = url.indexOf("https:");
        if (httpIndex == -1 && httpsIndex == -1) return url;

        final int length = url.length();

        final int startIndex = httpIndex != -1 ? httpIndex : httpsIndex;
        final int spaceIndex = url.indexOf(' ', startIndex + 1);

        final int endIndex = (spaceIndex != -1 ? spaceIndex : length);

        final String extractUrl = url.substring(startIndex, Math.min(length, endIndex) - 1);

        final SpannableString spannableString = new SpannableString(url);
        spannableString.setSpan(new URLSpan(extractUrl), startIndex, endIndex, 0);

        return spannableString;
    }

    public static boolean isEmpty(final CharSequence charSequence) {
        if (charSequence == null || charSequence.length() < 1) return true;
        if (charSequence instanceof String) {
            String str = (String) charSequence;
            if ("".equals(str) || "null".equals(str) || str.isEmpty()) return true;
            str = str.trim();
            return "".equals(str) || "null".equals(str) || str.isEmpty();
        }
        return "null".contentEquals(charSequence) || "".contentEquals(charSequence) || charSequence.length() < 1;
    }

    public static boolean isImage(final Uri itemUri, final ContentResolver contentResolver) {
        String mimeType;
        if (itemUri == null) return false;
        final String scheme = itemUri.getScheme();
        if (isEmpty(scheme))
            mimeType = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(itemUri.toString()).toLowerCase());
        else mimeType = scheme.equals(ContentResolver.SCHEME_CONTENT) ? contentResolver.getType(itemUri)
                : mimeTypeMap.getMimeTypeFromExtension
                (MimeTypeMap.getFileExtensionFromUrl(itemUri.toString()).toLowerCase());

        if (isEmpty(mimeType)) return true;
        mimeType = mimeType.toLowerCase();
        return mimeType.startsWith("image");
    }

    @Nullable
    public static String getCookie(@Nullable final String webViewUrl) {
        int lastLongestCookieLength = 0;
        String mainCookie = null;

        String cookie;
        if (!Utils.isEmpty(webViewUrl)) {
            cookie = Utils.COOKIE_MANAGER.getCookie(webViewUrl);
            if (cookie != null) {
                final int cookieLen = cookie.length();
                if (cookieLen > lastLongestCookieLength) {
                    mainCookie = cookie;
                    lastLongestCookieLength = cookieLen;
                }
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("https://instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("https://instagram.com/");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("http://instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("http://instagram.com/");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("https://www.instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("https://www.instagram.com/");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("http://www.instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = Utils.COOKIE_MANAGER.getCookie("http://www.instagram.com/");
        if (cookie != null && cookie.length() > lastLongestCookieLength) mainCookie = cookie;

        return mainCookie;
    }

    public static void errorFinish(@NonNull final Activity activity) {
        Toast.makeText(activity, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    @Nullable
    public static String getInstalledTelegramPackage(@NonNull final Context context) {
        final String[] packages = {
                "org.telegram.messenger",
                "org.thunderdog.challegram",
                "ir.ilmili.telegraph",
                "org.telegram.BifToGram",
                "org.vidogram.messenger",
                "com.xplus.messenger",
                "com.ellipi.messenger",
                "org.telegram.plus",
                "com.iMe.android",
                "org.viento.colibri",
                "org.viento.colibrix",
                "ml.parsgram",
                "com.ringtoon.app.tl",
        };

        final PackageManager packageManager = context.getPackageManager();
        for (final String pkg : packages) {
            try {
                final PackageInfo packageInfo = packageManager.getPackageInfo(pkg, 0);
                if (packageInfo.applicationInfo.enabled) return pkg;
            } catch (final Exception e) {
                try {
                    if (packageManager.getApplicationInfo(pkg, 0).enabled) return pkg;
                } catch (final Exception e1) {
                    // meh
                }
            }
        }

        return null;
    }
}