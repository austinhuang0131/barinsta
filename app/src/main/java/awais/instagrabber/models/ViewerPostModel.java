package awais.instagrabber.models;

import org.json.JSONObject;
import awais.instagrabber.models.enums.MediaItemType;

public final class ViewerPostModel extends BasePostModel {
    protected final String username;
    protected final JSONObject location;
    protected final long videoViews;
    protected String sliderDisplayUrl;
    protected long commentsCount, likes;
    private boolean isCurrentSlide = false;

    public ViewerPostModel(final MediaItemType itemType, final String postId, final String displayUrl, final String shortCode,
                           final String postCaption, final String username, final long videoViews, final long timestamp,
                           boolean liked, boolean bookmarked, long likes, final JSONObject location) {
        this.itemType = itemType;
        this.postId = postId;
        this.displayUrl = displayUrl;
        this.postCaption = postCaption;
        this.username = username;
        this.shortCode = shortCode;
        this.videoViews = videoViews;
        this.timestamp = timestamp;
        this.liked = liked;
        this.likes = likes;
        this.bookmarked = bookmarked;
        this.location = location;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public String getSliderDisplayUrl() {
        return sliderDisplayUrl;
    }

    public String getUsername() {
        return username;
    }

    public JSONObject getLocation() {
        return location;
    }

    public final long getVideoViews() {
        return videoViews;
    }

    public long getLikes() {
        return likes;
    }

    // setManualLike means user liked from InstaGrabber
    public boolean setManualLike(final boolean like) {
        liked = like;
        likes = (like) ? (likes + 1) : (likes - 1);
        return liked;
    }

    public void setSliderDisplayUrl(final String sliderDisplayUrl) {
        this.sliderDisplayUrl = sliderDisplayUrl;
    }

    public void setCommentsCount(final long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public void setCurrentSlide(final boolean currentSlide) {
        this.isCurrentSlide = currentSlide;
    }

    public boolean isCurrentSlide() {
        return isCurrentSlide;
    }
}