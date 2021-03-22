package awais.instagrabber.repositories.responses;

import java.io.Serializable;

import awais.instagrabber.models.enums.FollowingType;

public final class Hashtag implements Serializable {
    private final FollowingType following; // 0 false 1 true
    private final long mediaCount;
    private final String id;
    private final String name;
    private final String profilePicUrl; // on app API this is always null (property exists)

    public Hashtag(final String id,
                   final String name,
                   final String profilePicUrl,
                   final long mediaCount,
                   final FollowingType following) {
        this.id = id;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
        this.mediaCount = mediaCount;
        this.following = following;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public Long getMediaCount() {
        return mediaCount;
    }

    public FollowingType getFollowing() {
        return following;
    }
}