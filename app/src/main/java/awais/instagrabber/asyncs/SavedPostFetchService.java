package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ProfileService;
import awais.instagrabber.webservices.ServiceCallback;
import kotlinx.coroutines.Dispatchers;

public class SavedPostFetchService implements PostFetcher.PostFetchService {
    private final ProfileService profileService;
    private final GraphQLRepository graphQLRepository;
    private final long profileId;
    private final PostItemType type;
    private final boolean isLoggedIn;

    private String nextMaxId;
    private final String collectionId;
    private boolean moreAvailable;

    public SavedPostFetchService(final long profileId, final PostItemType type, final boolean isLoggedIn, final String collectionId) {
        this.profileId = profileId;
        this.type = type;
        this.isLoggedIn = isLoggedIn;
        this.collectionId = collectionId;
        graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        profileService = isLoggedIn ? ProfileService.getInstance() : null;
    }

    @Override
    public void fetch(final FetchListener<List<Media>> fetchListener) {
        final ServiceCallback<PostsFetchResponse> callback = new ServiceCallback<PostsFetchResponse>() {
            @Override
            public void onSuccess(final PostsFetchResponse result) {
                if (result == null) return;
                nextMaxId = result.getNextCursor();
                moreAvailable = result.getHasNextPage();
                if (fetchListener != null) {
                    fetchListener.onResult(result.getFeedModels());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                // Log.e(TAG, "onFailure: ", t);
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
            }
        };
        switch (type) {
            case LIKED:
                profileService.fetchLiked(nextMaxId, callback);
                break;
            case TAGGED:
                if (isLoggedIn) profileService.fetchTagged(profileId, nextMaxId, callback);
                else graphQLRepository.fetchTaggedPosts(
                        profileId,
                        30,
                        nextMaxId,
                        CoroutineUtilsKt.getContinuation((postsFetchResponse, throwable) -> {
                            if (throwable != null) {
                                callback.onFailure(throwable);
                                return;
                            }
                            callback.onSuccess(postsFetchResponse);
                        }, Dispatchers.getIO())
                );
                break;
            case COLLECTION:
            case SAVED:
                profileService.fetchSaved(nextMaxId, collectionId, callback);
                break;
            default:
                callback.onFailure(null);
        }
    }

    @Override
    public void reset() {
        nextMaxId = null;
    }

    @Override
    public boolean hasNextPage() {
        return moreAvailable;
    }
}
