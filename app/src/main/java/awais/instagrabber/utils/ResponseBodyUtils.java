package awais.instagrabber.utils;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.InboxReadState;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.RavenExpiringMediaType;
import awais.instagrabber.models.enums.RavenMediaViewType;
import awaisomereport.LogCollector;

public final class ResponseBodyUtils {
    private static final String TAG = "ResponseBodyUtils";

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
                        } else if (currRes < lastResMain && low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        }
                    } else {
                        if (currRes > lastResBase && !low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        } else if (currRes < lastResBase && low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        }
                    }
                }
            }

            if (lastIndexMain >= 0) return sources[lastIndexMain];
            else if (lastIndexBase >= 0) return sources[lastIndexBase];
        } catch (final Exception e) {
            if (Utils.logCollector != null)
                Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "getHighQualityPost",
                                                   new Pair<>("resourcesNull", resources == null),
                                                   new Pair<>("isVideo", isVideo));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return null;
    }

    public static String getHighQualityImage(final JSONObject resources) {
        String src = null;
        try {
            if (resources.has("display_resources"))
                src = getHighQualityPost(resources.getJSONArray("display_resources"), false, false, false);
            else if (resources.has("image_versions2"))
                src = getHighQualityPost(resources.getJSONObject("image_versions2").getJSONArray("candidates"), false, true, false);
            if (src == null) return resources.getString("display_url");
        } catch (final Exception e) {
            if (Utils.logCollector != null)
                Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "getHighQualityImage",
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
            if (Utils.logCollector != null)
                Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "getLowQualityImage",
                                                   new Pair<>("resourcesNull", resources == null));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return src;
    }

    public static ThumbnailDetails getItemThumbnail(@NonNull final JSONArray jsonArray) {
        final ThumbnailDetails thumbnailDetails = new ThumbnailDetails();
        final int imageResLen = jsonArray.length();
        for (int i = 0; i < imageResLen; ++i) {
            final JSONObject imageResource = jsonArray.optJSONObject(i);
            try {
                // final float ratio = (float) height / width;
                // if (ratio >= 0.95f && ratio <= 1.0f) {
                thumbnailDetails.height = imageResource.getInt("height");
                thumbnailDetails.width = imageResource.getInt("width");
                thumbnailDetails.url = imageResource.getString("url");
                break;
                // }
            } catch (final Exception e) {
                if (Utils.logCollector != null)
                    Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "getItemThumbnail");
                if (BuildConfig.DEBUG) Log.e(TAG, "", e);
            }
        }
        // if (TextUtils.isEmpty(thumbnail)) thumbnail = jsonArray.optJSONObject(0).optString("url");
        return thumbnailDetails;
    }

    public static class ThumbnailDetails {
        int width;
        int height;
        public String url;
    }

    @Nullable
    public static ThumbnailDetails getThumbnailUrl(@NonNull final JSONObject mediaObj, final MediaItemType mediaType) throws Exception {
        ThumbnailDetails thumbnail = null;
        if (mediaType == MediaItemType.MEDIA_TYPE_IMAGE || mediaType == MediaItemType.MEDIA_TYPE_VIDEO) {
            final JSONObject imageVersions = mediaObj.optJSONObject("image_versions2");
            if (imageVersions != null)
                thumbnail = getItemThumbnail(imageVersions.getJSONArray("candidates"));

        } else if (mediaType == MediaItemType.MEDIA_TYPE_SLIDER) {
            final JSONArray carouselMedia = mediaObj.optJSONArray("carousel_media");
            if (carouselMedia != null)
                thumbnail = getItemThumbnail(carouselMedia.getJSONObject(0)
                                                          .getJSONObject("image_versions2")
                                                          .getJSONArray("candidates"));
        }
        return thumbnail;
    }

    public static String getVideoUrl(@NonNull final JSONObject mediaObj) {
        String thumbnail = null;
        final JSONArray imageVersions = mediaObj.optJSONArray("video_versions");
        if (imageVersions != null) {
            thumbnail = getItemThumbnail(imageVersions).url;
        }
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

    public static DirectItemModel.DirectItemMediaModel getDirectMediaModel(final JSONObject mediaObj) throws Exception {
        final DirectItemModel.DirectItemMediaModel mediaModel;
        if (mediaObj == null) mediaModel = null;
        else {
            final JSONObject userObj = mediaObj.optJSONObject("user");

            ProfileModel user = null;
            if (userObj != null) {
                user = new ProfileModel(
                        userObj.getBoolean("is_private"),
                        false,
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
            if (TextUtils.isEmpty(id)) id = null;

            final ThumbnailDetails thumbnailDetails = getThumbnailUrl(mediaObj, mediaType);
            mediaModel = new DirectItemModel.DirectItemMediaModel(
                    mediaType,
                    mediaObj.optLong("expiring_at"),
                    mediaObj.optLong("pk"),
                    id,
                    thumbnailDetails != null ? thumbnailDetails.url : null,
                    mediaType == MediaItemType.MEDIA_TYPE_VIDEO ? getVideoUrl(mediaObj) : null,
                    user,
                    mediaObj.optString("code"),
                    thumbnailDetails != null ? thumbnailDetails.height : 0,
                    thumbnailDetails != null ? thumbnailDetails.width : 0);
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
        if ("clip".equals(itemType)) return DirectItemType.CLIP;
        return DirectItemType.TEXT;
    }

    @NonNull
    public static InboxThreadModel createInboxThreadModel(@NonNull final JSONObject data, final boolean inThreadView) throws Exception {
        final InboxReadState readState = data.getInt("read_state") == 0 ? InboxReadState.STATE_READ : InboxReadState.STATE_UNREAD;
        final String threadType = data.getString("thread_type"); // they're all "private", group is identified by boolean "is_group"

        final String threadId = data.getString("thread_id");
        final String threadV2Id = data.getString("thread_v2_id");
        final String threadTitle = data.getString("thread_title");

        final String threadNewestCursor = data.optString("newest_cursor");
        final String threadOldestCursor = data.optString("oldest_cursor");
        final String threadNextCursor = data.has("next_cursor") ? data.getString("next_cursor") : null;
        final String threadPrevCursor = data.has("prev_cursor") ? data.getString("prev_cursor") : null;

        final boolean threadHasOlder = data.getBoolean("has_older");
        final long unreadCount = data.optLong("read_state", 0);

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
        final JSONArray admins = data.getJSONArray("admin_user_ids");
        final int adminsLen = admins.length();

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

        final Long[] adminIDs = new Long[adminsLen];
        for (int j = 0; j < adminsLen; ++j) {
            adminIDs[j] = admins.getLong(j);
        }

        final JSONArray items = data.getJSONArray("items");
        final int itemsLen = items.length();

        final ArrayList<DirectItemModel> itemModels = new ArrayList<>(itemsLen);
        for (int i = 0; i < itemsLen; ++i) {
            final JSONObject itemObject = items.getJSONObject(i);

            CharSequence text = null;
            ProfileModel profileModel = null;
            DirectItemModel.DirectItemLinkModel linkModel = null;
            DirectItemModel.DirectItemMediaModel directMedia = null;
            DirectItemModel.DirectItemReelShareModel reelShareModel = null;
            DirectItemModel.DirectItemActionLogModel actionLogModel = null;
            DirectItemModel.DirectItemAnimatedMediaModel animatedMediaModel = null;
            DirectItemModel.DirectItemVoiceMediaModel voiceMediaModel = null;
            DirectItemModel.DirectItemRavenMediaModel ravenMediaModel = null;
            DirectItemModel.DirectItemVideoCallEventModel videoCallEventModel = null;

            final DirectItemType itemType = getDirectItemType(itemObject.getString("item_type"));
            switch (itemType) {
                case ANIMATED_MEDIA: {
                    final JSONObject animatedMedia = itemObject.getJSONObject("animated_media");
                    final JSONObject stickerImage = animatedMedia.getJSONObject("images").getJSONObject("fixed_height");

                    animatedMediaModel = new DirectItemModel.DirectItemAnimatedMediaModel(
                            animatedMedia.getBoolean("is_random"),
                            animatedMedia.getBoolean("is_sticker"),
                            animatedMedia.getString("id"),
                            stickerImage.getString("url"),
                            stickerImage.optString("webp"),
                            stickerImage.optString("mp4"),
                            stickerImage.getInt("height"),
                            stickerImage.getInt("width"));
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

                    voiceMediaModel = new DirectItemModel.DirectItemVoiceMediaModel(
                            voiceMedia.getString("id"),
                            audio.getString("audio_src"),
                            audio.getLong("duration"),
                            waveformData);
                }
                break;

                case LINK: {
                    final JSONObject linkObj = itemObject.getJSONObject("link");

                    DirectItemModel.DirectItemLinkContext itemLinkContext = null;
                    final JSONObject linkContext = linkObj.optJSONObject("link_context");
                    if (linkContext != null) {
                        itemLinkContext = new DirectItemModel.DirectItemLinkContext(
                                linkContext.getString("link_url"),
                                linkContext.optString("link_title"),
                                linkContext.optString("link_summary"),
                                linkContext.optString("link_image_url")
                        );
                    }

                    linkModel = new DirectItemModel.DirectItemLinkModel(
                            linkObj.getString("text"),
                            linkObj.getString("client_context"),
                            linkObj.optString("mutation_token"),
                            itemLinkContext);
                }
                break;

                case REEL_SHARE: {
                    final JSONObject reelShare = itemObject.getJSONObject("reel_share");
                    reelShareModel = new DirectItemModel.DirectItemReelShareModel(
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
                    for (int j = 0; j < seenUsersLen; j++)
                        seenUserIds[j] = seenUserIdsArray.getString(j);

                    DirectItemModel.RavenExpiringMediaActionSummaryModel expiringSummaryModel = null;
                    final JSONObject actionSummary = visualMedia.optJSONObject("expiring_media_action_summary");
                    if (actionSummary != null)
                        expiringSummaryModel = new DirectItemModel.RavenExpiringMediaActionSummaryModel(
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

                    ravenMediaModel = new DirectItemModel.DirectItemRavenMediaModel(
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
                    videoCallEventModel = new DirectItemModel.DirectItemVideoCallEventModel(videoCallEvent.getLong("vc_id"),
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
                    for (int q = 0; q < bold.length(); ++q) {
                        JSONObject boldItem = bold.getJSONObject(q);
                        desc = desc.substring(0, boldItem.getInt("start") + q * 7) + "<b>"
                                + desc.substring(boldItem.getInt("start") + q * 7, boldItem.getInt("end") + q * 7)
                                + "</b>" + desc.substring(boldItem.getInt("end") + q * 7);
                    }
                    actionLogModel = new DirectItemModel.DirectItemActionLogModel(desc);
                    break;

                case MEDIA_SHARE:
                    directMedia = getDirectMediaModel(itemObject.getJSONObject("media_share"));
                    break;

                case CLIP:
                    directMedia = getDirectMediaModel(itemObject.getJSONObject("clip").getJSONObject("clip"));
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
                        reelShareModel = new DirectItemModel.DirectItemReelShareModel(
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

            String[] liked = null;
            if (!itemObject.isNull("reactions") && !itemObject.getJSONObject("reactions").isNull("likes")) {
                JSONArray rawLiked = itemObject.getJSONObject("reactions").getJSONArray("likes");
                liked = new String[rawLiked.length()];
                for (int l = 0; l < rawLiked.length(); ++l) {
                    liked[l] = String.valueOf(rawLiked.getJSONObject(l).getLong("sender_id"));
                }
            }

            itemModels.add(new DirectItemModel(
                    itemObject.getLong("user_id"),
                    itemObject.getLong("timestamp"),
                    itemObject.getString("item_id"),
                    liked,
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
                                    userModels, leftuserModels, adminIDs,
                                    itemModels.toArray(new DirectItemModel[0]),
                                    muted, isPin, named, canonical,
                                    pending, threadHasOlder, unreadCount, isSpam, isGroup, archived, lastActivityAt);
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
}
