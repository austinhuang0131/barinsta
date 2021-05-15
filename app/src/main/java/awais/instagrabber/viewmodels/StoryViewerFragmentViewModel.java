package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesService;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class StoryViewerFragmentViewModel extends ViewModel {
    private static final String TAG = StoryViewerFragmentViewModel.class.getSimpleName();
    /**
     * Tracks the current story index in {@link StoryViewerFragmentViewModel#stories}
     */
    private final MutableLiveData<Integer> currentStoryIndex = new MutableLiveData<>(0);

    private final MutableLiveData<List<StoryModel>> currentStoryItems = new MutableLiveData<>();
    private final MutableLiveData<StoryModel> activeStoryItem = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeStoryItemIndex = new MutableLiveData<>(0);

    private final StoriesService storiesService;

    private StoryViewerOptions options;
    @Nullable
    private List<?> stories;

    public StoryViewerFragmentViewModel() {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (csrfToken == null) {
            throw new IllegalStateException("No csrf token in cookie");
        }
        final long userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceId = settingsHelper.getString(Constants.DEVICE_UUID);
        storiesService = StoriesService.getInstance(csrfToken, userIdFromCookie, deviceId);
    }

    public LiveData<List<StoryModel>> getCurrentStoryItems() {
        return currentStoryItems;
    }

    public LiveData<StoryModel> getActiveStoryItem() {
        return activeStoryItem;
    }

    public LiveData<Integer> getActiveStoryItemIndex() {
        return activeStoryItemIndex;
    }

    public void setActiveStoryItemIndex(final int activeStoryItemIndex) {
        final int index = Math.max(activeStoryItemIndex, 0);
        this.activeStoryItemIndex.postValue(index);
        try {
            final List<StoryModel> currentStoryItemsValue = currentStoryItems.getValue();
            if (currentStoryItemsValue == null || currentStoryItemsValue.isEmpty()) return;
            activeStoryItem.postValue(currentStoryItemsValue.get(index));
        } catch (Exception e) {
            Log.e(TAG, "onSuccess: ", e);
            activeStoryItem.postValue(null);
        }

    }

    public void setOptions(@NonNull final StoryViewerOptions options) {
        this.options = options;
    }

    public void setStories(@Nullable final List<?> stories) {
        this.stories = stories;
    }
    // to be called at first init only

    public void init() {
        final int storyIndex = getCurrentStoryIndexFromOptions(options);
        currentStoryIndex.postValue(storyIndex);
        setCurrentStoryItems(storyIndex);
    }

    private int getCurrentStoryIndexFromOptions(@NonNull final StoryViewerOptions options) {
        int storyIndex = 0;
        switch (options.getType()) {
            case HASHTAG:
            case LOCATION:
            case USER:
            case STORY:
            case STORY_ARCHIVE:
                break;
            case HIGHLIGHT:
            case FEED_STORY:
                storyIndex = options.getStoryIndex();
                break;
        }
        return storyIndex;
    }

    private void setCurrentStoryItems(final int storyIndex) {
        switch (options.getType()) {
            case HASHTAG:
            case LOCATION:
                setStoryItemsFromOptions();
                break;
            case STORY:
                setDirectStoryItems();
                break;
            case USER:
                setUserStoryItems();
                break;
            case HIGHLIGHT: {
                if (stories == null) return;
                final Object story = stories.get(storyIndex);
                if (!(story instanceof HighlightModel)) return;
                setHighlightStoryItems((HighlightModel) story);
                break;
            }
            case FEED_STORY: {
                if (stories == null) return;
                final Object story = stories.get(storyIndex);
                if (!(story instanceof FeedStoryModel)) return;
                setFeedStoryItems((FeedStoryModel) story);
                break;
            }
            case STORY_ARCHIVE: {
                if (stories == null) return;
                final Object story = stories.get(storyIndex);
                if (!(story instanceof HighlightModel)) return;
                setArchiveStoryItems((HighlightModel) story);
                break;
            }
        }
    }

    private void setFeedStoryItems(@NonNull final FeedStoryModel story) {
        final String storyMediaId = story.getStoryMediaId();
        final String username = story.getProfileModel().getUsername();
        final StoryViewerOptions fetchOptions = StoryViewerOptions.forUser(Long.parseLong(storyMediaId), username);
        // if (story.isLive()) {
        //     live = story.getFirstStoryModel();
        // }
        fetchUserStory(fetchOptions);
    }

    private void setHighlightStoryItems(@NonNull final HighlightModel story) {
        final StoryViewerOptions fetchOptions = StoryViewerOptions.forHighlight(story.getId());
        fetchUserStory(fetchOptions);
    }

    private void setUserStoryItems() {
        final String username = options.getName();
        final StoryViewerOptions fetchOptions = StoryViewerOptions.forUser(options.getId(), username);
        fetchUserStory(fetchOptions);
    }

    private void setDirectStoryItems() {
        fetchStoryItem(options.getId());
    }

    private void setStoryItemsFromOptions() {
        fetchUserStory(options);
    }

    private void setArchiveStoryItems(@NonNull final HighlightModel story) {
        final String storyMediaId = parseStoryMediaId(story.getId());
        // currentStoryUsername = model.getTitle();
        fetchUserStory(StoryViewerOptions.forStoryArchive(storyMediaId));
    }

    /**
     * Parses the Story's media ID. For user stories this is a number, but for archive stories
     * this is "archiveDay:" plus a number.
     */
    private static String parseStoryMediaId(String rawId) {
        final String regex = "(?:archiveDay:)?(.+)";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(rawId);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            return matcher.group(1);
        }
        return rawId;
    }

    private void fetchUserStory(@NonNull final StoryViewerOptions fetchOptions) {
        final int tempActiveStoryItemIndex = 0;
        storiesService.getUserStory(fetchOptions, new ServiceCallback<List<StoryModel>>() {
            @Override
            public void onSuccess(final List<StoryModel> result) {
                currentStoryItems.postValue(result);
                activeStoryItemIndex.postValue(tempActiveStoryItemIndex);
                if (result == null || result.isEmpty()) {
                    activeStoryItem.postValue(null);
                    return;
                }
                try {
                    activeStoryItem.postValue(result.get(tempActiveStoryItemIndex));
                } catch (Exception e) {
                    Log.e(TAG, "onSuccess: ", e);
                    activeStoryItem.postValue(null);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    private void fetchStoryItem(final long mediaId) {
        final int tempActiveStoryItemIndex = 0;
        storiesService.fetch(mediaId, new ServiceCallback<StoryModel>() {
            @Override
            public void onSuccess(final StoryModel result) {
                currentStoryItems.postValue(Collections.singletonList(result));
                activeStoryItemIndex.postValue(tempActiveStoryItemIndex);
                if (result == null) {
                    activeStoryItem.postValue(null);
                    return;
                }
                activeStoryItem.postValue(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }
}