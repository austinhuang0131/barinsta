package awais.instagrabber.models;

import java.io.Serializable;

public final class FeedStoryModel implements Serializable {
    private final String storyMediaId;
    private final ProfileModel profileModel;
    private StoryModel[] storyModels;
    private boolean fullyRead;

    public FeedStoryModel(final String storyMediaId, final ProfileModel profileModel, final boolean fullyRead) {
        this.storyMediaId = storyMediaId;
        this.profileModel = profileModel;
        this.fullyRead = fullyRead;
    }

    public String getStoryMediaId() {
        return storyMediaId;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public void setStoryModels(final StoryModel[] storyModels) {
        this.storyModels = storyModels;
    }

    public StoryModel[] getStoryModels() {
        return storyModels;
    }

    public boolean getFullyRead() {
        return fullyRead;
    }
}