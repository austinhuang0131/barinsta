package awais.instagrabber.repositories.responses.directmessages;

import java.io.Serializable;
import java.util.Objects;

import awais.instagrabber.models.ProfileModel;

public class DirectUser implements Serializable {
    private final long pk;
    private final String username;
    private final String fullName;
    private final boolean isPrivate;
    private final String profilePicUrl;
    private final String profilePicId;
    private final DirectUserFriendshipStatus friendshipStatus;
    private final boolean isVerified;
    private final boolean hasAnonymousProfilePicture;
    private final boolean isDirectappInstalled;
    private final String reelAutoArchive;
    private final String allowedCommenterType;

    public DirectUser(final long pk,
                      final String username,
                      final String fullName,
                      final boolean isPrivate,
                      final String profilePicUrl,
                      final String profilePicId,
                      final DirectUserFriendshipStatus friendshipStatus,
                      final boolean isVerified,
                      final boolean hasAnonymousProfilePicture,
                      final boolean isDirectappInstalled,
                      final String reelAutoArchive,
                      final String allowedCommenterType) {
        this.pk = pk;
        this.username = username;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
        this.profilePicUrl = profilePicUrl;
        this.profilePicId = profilePicId;
        this.friendshipStatus = friendshipStatus;
        this.isVerified = isVerified;
        this.hasAnonymousProfilePicture = hasAnonymousProfilePicture;
        this.isDirectappInstalled = isDirectappInstalled;
        this.reelAutoArchive = reelAutoArchive;
        this.allowedCommenterType = allowedCommenterType;
    }

    public long getPk() {
        return pk;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public String getProfilePicId() {
        return profilePicId;
    }

    public DirectUserFriendshipStatus getFriendshipStatus() {
        return friendshipStatus;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean isHasAnonymousProfilePicture() {
        return hasAnonymousProfilePicture;
    }

    public boolean isDirectappInstalled() {
        return isDirectappInstalled;
    }

    public String getReelAutoArchive() {
        return reelAutoArchive;
    }

    public String getAllowedCommenterType() {
        return allowedCommenterType;
    }

    public static DirectUser fromProfileModel(final ProfileModel profileModel) {
        return new DirectUser(
                Long.parseLong(profileModel.getId()),
                profileModel.getUsername(),
                profileModel.getName(),
                profileModel.isPrivate(),
                profileModel.getSdProfilePic(),
                null,
                new DirectUserFriendshipStatus(
                        profileModel.isFollowing(),
                        profileModel.isBlocked(),
                        profileModel.isPrivate(),
                        false,
                        profileModel.isRequested(),
                        false,
                        profileModel.isRestricted()),
                profileModel.isVerified(),
                false,
                false,
                null,
                null
        );
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DirectUser that = (DirectUser) o;
        return pk == that.pk &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, username);
    }
}