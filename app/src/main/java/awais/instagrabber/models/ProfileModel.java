package awais.instagrabber.models;

import java.io.Serializable;

public final class ProfileModel implements Serializable {
    private final boolean isPrivate, reallyPrivate, isVerified, following, restricted, blocked, requested;
    private final long postCount, followersCount, followingCount;
    private final String id, username, name, biography, url, sdProfilePic, hdProfilePic;

    public ProfileModel(final boolean isPrivate, final boolean reallyPrivate,
                        final boolean isVerified, final String id, final String username, final String name, final String biography,
                        final String url, final String sdProfilePic, final String hdProfilePic, final long postCount,
                        final long followersCount, final long followingCount, final boolean following, final boolean restricted,
                        final boolean blocked, final boolean requested) {
        this.isPrivate = isPrivate;
        this.reallyPrivate = reallyPrivate;
        this.isVerified = isVerified;
        this.id = id;
        this.url = url;
        this.name = name;
        this.username = username;
        this.biography = biography;
        this.sdProfilePic = sdProfilePic;
        this.hdProfilePic = hdProfilePic;
        this.postCount = postCount;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.following = following;
        this.restricted = restricted;
        this.blocked = blocked;
        this.requested = requested;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isReallyPrivate() {
        return reallyPrivate;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getBiography() {
        return biography;
    }

    public String getUrl() {
        return url;
    }

    public String getSdProfilePic() {
        return sdProfilePic;
    }

    public String getHdProfilePic() {
        return hdProfilePic;
    }

    public long getPostCount() { return postCount; }

    public long getFollowersCount() { return followersCount; }

    public long getFollowingCount() {
        return followingCount;
    }

    public boolean getFollowing() { return following; }

    public boolean getRestricted() { return restricted; }

    public boolean getBlocked() {
        return blocked;
    }

    public boolean getRequested() {
        return requested;
    }
}