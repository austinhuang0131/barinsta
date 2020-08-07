package awais.instagrabber.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Utils;

public abstract class BasePostModel implements Serializable {
    protected String postId;
    protected String displayUrl;
    protected String shortCode;
    protected CharSequence postCaption;
    protected MediaItemType itemType;
    protected boolean isSelected;
    protected boolean isDownloaded;
    protected long timestamp;
    protected int position;
    boolean liked, bookmarked;

    public boolean getLike() {
        return liked;
    }

    public boolean getBookmark() {
        return bookmarked;
    }

    public MediaItemType getItemType() {
        return itemType;
    }

    public final String getPostId() {
        return postId;
    }

    public final String getDisplayUrl() {
        return displayUrl;
    }

    public final CharSequence getPostCaption() {
        return postCaption;
    }

    public final String getShortCode() {
        return shortCode;
    }

    public final long getTimestamp() {
        return timestamp;
    }

    public int getPosition() {
        return this.position;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setItemType(final MediaItemType itemType) {
        this.itemType = itemType;
    }

    public void setPostId(final String postId) {
        this.postId = postId;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public void setSelected(final boolean selected) {
        this.isSelected = selected;
    }

    public void setDownloaded(final boolean downloaded) {
        isDownloaded = downloaded;
    }

    @NonNull
    public final String getPostDate() {
        return Utils.datetimeParser.format(new Date(timestamp * 1000L));
    }
}