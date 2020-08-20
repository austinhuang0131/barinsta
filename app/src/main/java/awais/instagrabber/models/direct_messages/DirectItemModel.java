package awais.instagrabber.models.direct_messages;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.RavenExpiringMediaType;
import awais.instagrabber.models.enums.RavenMediaViewType;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Constants.COOKIE;

public final class DirectItemModel implements Serializable, Comparable<DirectItemModel> {
    private final long userId, timestamp;
    private final DirectItemType itemType;
    private final String itemId;
    private String[] likes;
    private boolean liked;
    private final CharSequence text;
    private final DirectItemLinkModel linkModel;
    private final DirectItemMediaModel mediaModel;
    private final ProfileModel profileModel;
    private final DirectItemReelShareModel reelShare;
    private final DirectItemActionLogModel actionLogModel;
    private final DirectItemVoiceMediaModel voiceMediaModel;
    private final DirectItemRavenMediaModel ravenMediaModel;
    private final DirectItemAnimatedMediaModel animatedMediaModel;
    private final DirectItemVideoCallEventModel videoCallEventModel;

    private final String myId = Utils.getUserIdFromCookie(Utils.settingsHelper.getString(COOKIE));

    public DirectItemModel(final long userId, final long timestamp, final String itemId, final String[] likes,
                           final DirectItemType itemType, final CharSequence text, final DirectItemLinkModel linkModel,
                           final ProfileModel profileModel, final DirectItemReelShareModel reelShare, final DirectItemMediaModel mediaModel,
                           final DirectItemActionLogModel actionLogModel, final DirectItemVoiceMediaModel voiceMediaModel,
                           final DirectItemRavenMediaModel ravenMediaModel, final DirectItemVideoCallEventModel videoCallEventModel,
                           final DirectItemAnimatedMediaModel animatedMediaModel) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.itemType = itemType;
        this.itemId = itemId;
        this.likes = likes;
        this.liked = likes != null ? Arrays.asList(likes).contains(myId) : false;
        this.text = text;
        this.linkModel = linkModel;
        this.profileModel = profileModel;
        this.reelShare = reelShare;
        this.mediaModel = mediaModel;
        this.actionLogModel = actionLogModel;
        this.voiceMediaModel = voiceMediaModel;
        this.ravenMediaModel = ravenMediaModel;
        this.videoCallEventModel = videoCallEventModel;
        this.animatedMediaModel = animatedMediaModel;
    }

    public DirectItemType getItemType() {
        return itemType;
    }

    public CharSequence getText() {
        return text;
    }

    public String getItemId() {
        return itemId;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked() {
        this.liked = !liked;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getDateTime() {
        return Utils.datetimeParser.format(new Date(timestamp / 1000L));
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public DirectItemLinkModel getLinkModel() {
        return linkModel;
    }

    public DirectItemMediaModel getMediaModel() {
        return mediaModel;
    }

    public DirectItemReelShareModel getReelShare() {
        return reelShare;
    }

    public DirectItemActionLogModel getActionLogModel() {
        return actionLogModel;
    }

    public DirectItemVoiceMediaModel getVoiceMediaModel() {
        return voiceMediaModel;
    }

    public DirectItemRavenMediaModel getRavenMediaModel() {
        return ravenMediaModel;
    }

    public DirectItemAnimatedMediaModel getAnimatedMediaModel() {
        return animatedMediaModel;
    }

    public DirectItemVideoCallEventModel getVideoCallEventModel() {
        return videoCallEventModel;
    }

    @Override
    public int compareTo(@NonNull final DirectItemModel o) {
        return Long.compare(timestamp, o.timestamp);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public final static class DirectItemAnimatedMediaModel implements Serializable {
        private final boolean isRandom, isSticker;
        private final String id;
        private final String gifUrl, webpUrl, mp4Url;
        private final int height, width;

        public DirectItemAnimatedMediaModel(final boolean isRandom, final boolean isSticker, final String id, final String gifUrl,
                                            final String webpUrl, final String mp4Url, final int height, final int width) {
            this.isRandom = isRandom;
            this.isSticker = isSticker;
            this.id = id;
            this.gifUrl = gifUrl;
            this.webpUrl = webpUrl;
            this.mp4Url = mp4Url;
            this.height = height;
            this.width = width;
        }

        public boolean isRandom() {
            return isRandom;
        }

        public boolean isSticker() {
            return isSticker;
        }

        public String getId() {
            return id;
        }

        public String getGifUrl() {
            return gifUrl;
        }

        public String getWebpUrl() {
            return webpUrl;
        }

        public String getMp4Url() {
            return mp4Url;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }

    public final static class DirectItemVoiceMediaModel implements Serializable {
        private final String id, audioUrl;
        private final long durationMs;
        private final int[] waveformData;
        private int progress;
        private boolean isPlaying = false;

        public DirectItemVoiceMediaModel(final String id, final String audioUrl, final long durationMs, final int[] waveformData) {
            this.id = id;
            this.audioUrl = audioUrl;
            this.durationMs = durationMs;
            this.waveformData = waveformData;
        }

        public String getId() {
            return id;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public int[] getWaveformData() {
            return waveformData;
        }

        public void setProgress(final int progress) {
            this.progress = progress;
        }

        public int getProgress() {
            return progress;
        }

        public boolean isPlaying() {
            return isPlaying;
        }

        public void setPlaying(final boolean playing) {
            isPlaying = playing;
        }
    }

    public final static class DirectItemLinkModel implements Serializable {
        private final String text;
        private final String clientContext;
        private final String mutationToken;
        private final DirectItemLinkContext linkContext;

        public DirectItemLinkModel(final String text, final String clientContext, final String mutationToken,
                                   final DirectItemLinkContext linkContext) {
            this.text = text;
            this.clientContext = clientContext;
            this.mutationToken = mutationToken;
            this.linkContext = linkContext;
        }

        public String getText() {
            return text;
        }

        public String getClientContext() {
            return clientContext;
        }

        public String getMutationToken() {
            return mutationToken;
        }

        public DirectItemLinkContext getLinkContext() {
            return linkContext;
        }
    }

    public final static class DirectItemLinkContext implements Serializable {
        private final String linkUrl;
        private final String linkTitle;
        private final String linkSummary;
        private final String linkImageUrl;

        public DirectItemLinkContext(final String linkUrl, final String linkTitle, final String linkSummary, final String linkImageUrl) {
            this.linkUrl = linkUrl;
            this.linkTitle = linkTitle;
            this.linkSummary = linkSummary;
            this.linkImageUrl = linkImageUrl;
        }

        public String getLinkUrl() {
            return linkUrl;
        }

        public String getLinkTitle() {
            return linkTitle;
        }

        public String getLinkSummary() {
            return linkSummary;
        }

        public String getLinkImageUrl() {
            return linkImageUrl;
        }
    }

    public final static class DirectItemActionLogModel implements Serializable {
        private final String description;

        public DirectItemActionLogModel(final String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public final static class DirectItemReelShareModel implements Serializable {
        private final boolean isReelPersisted;
        private final long reelOwnerId;
        private final String reelOwnerName;
        private final String text;
        private final String type;
        private final String reelType;
        private final String reelName;
        private final String reelId;
        private final DirectItemMediaModel media;

        public DirectItemReelShareModel(final boolean isReelPersisted, final long reelOwnerId, final String reelOwnerName,
                                        final String text, final String type, final String reelType, final String reelName,
                                        final String reelId, final DirectItemMediaModel media) {
            this.isReelPersisted = isReelPersisted;
            this.reelOwnerId = reelOwnerId;
            this.reelOwnerName = reelOwnerName;
            this.text = text;
            this.type = type;
            this.reelType = reelType;
            this.reelName = reelName;
            this.reelId = reelId;
            this.media = media;
        }

        public boolean isReelPersisted() {
            return isReelPersisted;
        }

        public long getReelOwnerId() {
            return reelOwnerId;
        }

        public String getReelOwnerName() { return reelOwnerName; }

        public String getText() {
            return text;
        }

        public String getType() {
            return type;
        }

        public String getReelType() {
            return reelType;
        }

        public String getReelName() {
            return reelName;
        }

        public String getReelId() {
            return reelId;
        }

        public DirectItemMediaModel getMedia() {
            return media;
        }
    }

    public final static class DirectItemMediaModel implements Serializable {
        private final MediaItemType mediaType;
        private final long expiringAt, pk;
        private final String id, thumbUrl, videoUrl, code;
        private final ProfileModel user;

        public DirectItemMediaModel(final MediaItemType mediaType, final long expiringAt, final long pk, final String id,
                                    final String thumbUrl, final String videoUrl, final ProfileModel user, final String code) {
            this.mediaType = mediaType;
            this.expiringAt = expiringAt;
            this.pk = pk;
            this.id = id;
            this.thumbUrl = thumbUrl;
            this.videoUrl = videoUrl;
            this.user = user;
            this.code = code;
        }

        public MediaItemType getMediaType() {
            return mediaType;
        }

        public long getExpiringAt() {
            return expiringAt;
        }

        public long getPk() {
            return pk;
        }

        public String getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public ProfileModel getUser() {
            return user;
        }

        public String getThumbUrl() {
            return thumbUrl;
        }

        public String getVideoUrl() {

            if (mediaType == MediaItemType.MEDIA_TYPE_VIDEO) return videoUrl;
            else return thumbUrl;

        }
    }

    public final static class DirectItemRavenMediaModel implements Serializable {
        private final long expireAtSecs;
        private final int playbackDurationSecs;
        private final int seenCount;
        private final String[] seenUserIds;
        private final RavenMediaViewType viewType;
        private final DirectItemMediaModel media;
        private final RavenExpiringMediaActionSummaryModel expiringMediaActionSummary;

        public DirectItemRavenMediaModel(final long expireAtSecs, final int playbackDurationSecs, final int seenCount,
                                         final String[] seenUserIds, final RavenMediaViewType viewType, final DirectItemMediaModel media,
                                         final RavenExpiringMediaActionSummaryModel expiringMediaActionSummary) {
            this.expireAtSecs = expireAtSecs;
            this.playbackDurationSecs = playbackDurationSecs;
            this.seenCount = seenCount;
            this.seenUserIds = seenUserIds;
            this.viewType = viewType;
            this.media = media;
            this.expiringMediaActionSummary = expiringMediaActionSummary;
        }

        public long getExpireAtSecs() {
            return expireAtSecs;
        }

        public int getPlaybackDurationSecs() {
            return playbackDurationSecs;
        }

        public int getSeenCount() {
            return seenCount;
        }

        public String[] getSeenUserIds() {
            return seenUserIds;
        }

        public RavenMediaViewType getViewType() {
            return viewType;
        }

        public DirectItemMediaModel getMedia() {
            return media;
        }

        public RavenExpiringMediaActionSummaryModel getExpiringMediaActionSummary() {
            return expiringMediaActionSummary;
        }
    }

    public final static class DirectItemVideoCallEventModel implements Serializable {
        private final long videoCallId;
        private final boolean hasAudioOnlyCall;
        private final String action;
        private final String description;

        public DirectItemVideoCallEventModel(final long videoCallId, final boolean hasAudioOnlyCall, final String action, final String description) {
            this.videoCallId = videoCallId;
            this.hasAudioOnlyCall = hasAudioOnlyCall;
            this.action = action;
            this.description = description;
        }

        public long getVideoCallId() {
            return videoCallId;
        }

        public boolean isHasAudioOnlyCall() {
            return hasAudioOnlyCall;
        }

        public String getAction() {
            return action;
        }

        public String getDescription() {
            return description;
        }
    }

    public final static class RavenExpiringMediaActionSummaryModel implements Serializable {
        private final long timestamp;
        private final int count;
        private final RavenExpiringMediaType type;

        public RavenExpiringMediaActionSummaryModel(final long timestamp, final int count, final RavenExpiringMediaType type) {
            this.timestamp = timestamp;
            this.count = count;
            this.type = type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getCount() {
            return count;
        }

        public RavenExpiringMediaType getType() {
            return type;
        }
    }
}