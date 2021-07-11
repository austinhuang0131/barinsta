package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.TagsService;
import kotlinx.coroutines.Dispatchers;

public class HashtagPostFetchService implements PostFetcher.PostFetchService {
    private final TagsService tagsService;
    private final GraphQLRepository graphQLRepository;
    private final Hashtag hashtagModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public HashtagPostFetchService(final Hashtag hashtagModel, final boolean isLoggedIn) {
        this.hashtagModel = hashtagModel;
        this.isLoggedIn = isLoggedIn;
        tagsService = isLoggedIn ? TagsService.getInstance() : null;
        graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<Media>> fetchListener) {
        final ServiceCallback<PostsFetchResponse> cb = new ServiceCallback<PostsFetchResponse>() {
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
        if (isLoggedIn) tagsService.fetchPosts(hashtagModel.getName().toLowerCase(), nextMaxId, cb);
        else graphQLRepository.fetchHashtagPosts(
                hashtagModel.getName().toLowerCase(),
                nextMaxId,
                CoroutineUtilsKt.getContinuation((postsFetchResponse, throwable) -> {
                    if (throwable != null) {
                        cb.onFailure(throwable);
                        return;
                    }
                    cb.onSuccess(postsFetchResponse);
                }, Dispatchers.getIO())
        );
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
