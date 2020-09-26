package awais.instagrabber.fragments.main;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapter;
import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.adapters.viewholder.feed.FeedItemViewHolder;
import awais.instagrabber.asyncs.FeedFetcher;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.customviews.helpers.VideoAwareRecyclerScroller;
import awais.instagrabber.databinding.FragmentFeedBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.viewmodels.FeedViewModel;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesService;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FeedFragment";
    private static final double MAX_VIDEO_HEIGHT = 0.9 * Utils.displayMetrics.heightPixels;
    private static final int RESIZED_VIDEO_HEIGHT = (int) (0.8 * Utils.displayMetrics.heightPixels);

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentFeedBinding binding;
    private StoriesService storiesService;
    private boolean feedHasNextPage = false;
    private String feedEndCursor = null;
    private FeedViewModel feedViewModel;
    private VideoAwareRecyclerScroller videoAwareRecyclerScroller;
    private boolean shouldRefresh = true;
    private boolean isPullToRefresh;

    private final boolean shouldAutoPlay = settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS);
    private final FetchListener<FeedModel[]> feedFetchListener = new FetchListener<FeedModel[]>() {
        @Override
        public void doBefore() {
            binding.feedSwipeRefreshLayout.post(() -> binding.feedSwipeRefreshLayout.setRefreshing(true));
        }

        @Override
        public void onResult(final FeedModel[] result) {
            if (result == null || result.length <= 0) {
                binding.feedSwipeRefreshLayout.setRefreshing(false);
                return;
            }
            final List<FeedModel> currentFeedModelList = feedViewModel.getList().getValue();
            final Map<String, FeedModel> thumbToFeedMap = new HashMap<>();
            for (final FeedModel feedModel : result) {
                thumbToFeedMap.put(feedModel.getThumbnailUrl(), feedModel);
            }
            final BaseDataSubscriber<Void> subscriber = new BaseDataSubscriber<Void>() {
                int success = 0;
                int failed = 0;

                @Override
                protected void onNewResultImpl(@NonNull final DataSource<Void> dataSource) {
                    final Map<String, Object> extras = dataSource.getExtras();
                    if (extras == null) return;
                    final Uri thumbUri = (Uri) extras.get("uri_source");
                    if (thumbUri == null) return;
                    final Integer encodedWidth = (Integer) extras.get("encoded_width");
                    final Integer encodedHeight = (Integer) extras.get("encoded_height");
                    if (encodedWidth == null || encodedHeight == null) return;
                    final FeedModel feedModel = thumbToFeedMap.get(thumbUri.toString());
                    if (feedModel == null) return;
                    int requiredWidth = Utils.displayMetrics.widthPixels;
                    int resultingHeight = NumberUtils
                            .getResultingHeight(requiredWidth, encodedHeight, encodedWidth);
                    if (feedModel
                            .getItemType() == MediaItemType.MEDIA_TYPE_VIDEO && resultingHeight >= MAX_VIDEO_HEIGHT) {
                        // If its a video and the height is too large, need to reduce the height,
                        // so that entire video fits on screen
                        resultingHeight = RESIZED_VIDEO_HEIGHT;
                        requiredWidth = NumberUtils.getResultingWidth(RESIZED_VIDEO_HEIGHT,
                                                                      resultingHeight,
                                                                      requiredWidth);
                    }
                    feedModel.setImageWidth(requiredWidth);
                    feedModel.setImageHeight(resultingHeight);
                    success++;
                    updateAdapter();
                }

                @Override
                protected void onFailureImpl(@NonNull final DataSource<Void> dataSource) {
                    failed++;
                    updateAdapter();
                }

                public void updateAdapter() {
                    if (failed + success != result.length) return;
                    List<FeedModel> finalList = currentFeedModelList == null || currentFeedModelList.isEmpty()
                                                ? new ArrayList<>()
                                                : new ArrayList<>(currentFeedModelList);
                    final List<FeedModel> resultList = Arrays.asList(result);
                    if (isPullToRefresh) {
                        finalList = resultList;
                        isPullToRefresh = false;
                    } else {
                        finalList.addAll(resultList);
                    }
                    feedViewModel.getList().postValue(finalList);
                    final PostModel feedPostModel = result[result.length - 1];
                    if (feedPostModel != null) {
                        feedEndCursor = feedPostModel.getEndCursor();
                        feedHasNextPage = feedPostModel.hasNextPage();
                        feedPostModel.setPageCursor(false, null);
                    }
                    binding.feedSwipeRefreshLayout.setRefreshing(false);
                }
            };

            for (final FeedModel feedModel : result) {
                final DataSource<Void> ds = Fresco.getImagePipeline()
                                                  .prefetchToBitmapCache(ImageRequest.fromUri(feedModel.getThumbnailUrl()), null);
                ds.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
            }
        }
    };
    private final MentionClickListener mentionClickListener = (view, text, isHashtag, isLocation) -> {
        if (isHashtag) {
            final NavDirections action = FeedFragmentDirections.actionGlobalHashTagFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        if (isLocation) {
            final NavDirections action = FeedFragmentDirections.actionGlobalLocationFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        final NavDirections action = FeedFragmentDirections.actionGlobalProfileFragment("@" + text);
        NavHostFragment.findNavController(this).navigate(action);
    };
    private final View.OnClickListener postViewClickListener = v -> {
        final Object tag = v.getTag();
        if (!(tag instanceof FeedModel)) return;

        final FeedModel feedModel = (FeedModel) tag;
        if (v instanceof RamboTextView) {
            if (feedModel.isMentionClicked()) feedModel.toggleCaption();
            feedModel.setMentionClicked(false);
            if (!FeedItemViewHolder.expandCollapseTextView((RamboTextView) v, feedModel.getPostCaption()))
                feedModel.toggleCaption();
            return;
        }

        final int id = v.getId();
        switch (id) {
            case R.id.btnComments:
                final NavDirections commentsAction = FeedFragmentDirections.actionGlobalCommentsViewerFragment(
                        feedModel.getShortCode(),
                        feedModel.getPostId(),
                        feedModel.getProfileModel().getId()
                );
                NavHostFragment.findNavController(this).navigate(commentsAction);
                break;
            case R.id.viewStoryPost:
                final List<FeedModel> feedModels = feedViewModel.getList().getValue();
                if (feedModels == null || feedModels.size() == 0) return;
                if (feedModels.get(0) == null) return;
                final String postId = feedModels.get(0).getPostId();
                final boolean isId = postId != null;
                final String[] idsOrShortCodes = new String[feedModels.size()];
                for (int i = 0; i < feedModels.size(); i++) {
                    idsOrShortCodes[i] = isId ? feedModels.get(i).getPostId()
                                              : feedModels.get(i).getShortCode();
                }
                final NavDirections action = FeedFragmentDirections.actionGlobalPostViewFragment(
                        feedModel.getPosition(),
                        idsOrShortCodes,
                        isId);
                NavHostFragment.findNavController(this).navigate(action);
                break;

            case R.id.btnDownload:
                ProfileModel profileModel = feedModel.getProfileModel();
                final String username = profileModel != null ? profileModel.getUsername() : null;

                final ViewerPostModel[] sliderItems = feedModel.getSliderItems();

                final Context context = getContext();
                if (context == null) return;
                if (feedModel
                        .getItemType() != MediaItemType.MEDIA_TYPE_SLIDER || sliderItems == null || sliderItems.length == 1)
                    DownloadUtils.batchDownload(context,
                                                username,
                                                DownloadMethod.DOWNLOAD_FEED,
                                                Collections.singletonList(feedModel));
                else {
                    final ArrayList<BasePostModel> postModels = new ArrayList<>();
                    final DialogInterface.OnClickListener clickListener1 = (dialog, which) -> {
                        postModels.clear();

                        final boolean breakWhenFoundSelected = which == DialogInterface.BUTTON_POSITIVE;

                        for (final ViewerPostModel sliderItem : sliderItems) {
                            if (sliderItem != null) {
                                if (!breakWhenFoundSelected) postModels.add(sliderItem);
                                else if (sliderItem.isSelected()) {
                                    postModels.add(sliderItem);
                                    break;
                                }
                            }
                        }

                        // shows 0 items on first item of viewpager cause onPageSelected hasn't been called yet
                        if (breakWhenFoundSelected && postModels.size() == 0) {
                            postModels.add(sliderItems[0]);
                        }
                        if (postModels.size() > 0) {
                            DownloadUtils.batchDownload(context,
                                                        username,
                                                        DownloadMethod.DOWNLOAD_FEED,
                                                        postModels);
                        }
                    };

                    new AlertDialog.Builder(context)
                            .setTitle(R.string.post_viewer_download_dialog_title).setPositiveButton(
                            R.string.post_viewer_download_current,
                            clickListener1)
                            .setNegativeButton(R.string.post_viewer_download_album, clickListener1)
                            .show();
                }
                break;

            case R.id.ivProfilePic:
                profileModel = feedModel.getProfileModel();
                if (profileModel != null) mentionClickListener.onClick(null, profileModel.getUsername(), false, false);
                break;
        }
    };
    private FeedStoriesViewModel feedStoriesViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        storiesService = StoriesService.getInstance();
        // feedService = FeedService.getInstance();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        binding.feedSwipeRefreshLayout.setOnRefreshListener(this);
        setupFeedStories();
        setupFeed();
        shouldRefresh = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoAwareRecyclerScroller != null) {
            videoAwareRecyclerScroller.stopPlaying();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.feedSwipeRefreshLayout.setRefreshing(false);
        if (videoAwareRecyclerScroller != null && shouldAutoPlay) {
            videoAwareRecyclerScroller.startPlaying();
        }
    }

    @Override
    public void onRefresh() {
        isPullToRefresh = true;
        feedEndCursor = null;
        fetchFeed();
        fetchStories();
    }

    private void setupFeed() {
        feedViewModel = new ViewModelProvider(fragmentActivity).get(FeedViewModel.class);
        final Context context = getContext();
        if (context == null) return;
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        binding.feedRecyclerView.setLayoutManager(layoutManager);
        binding.feedRecyclerView.setHasFixedSize(true);
        final FeedAdapter feedAdapter = new FeedAdapter(postViewClickListener, mentionClickListener);
        feedAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        binding.feedRecyclerView.setAdapter(feedAdapter);
        feedViewModel.getList().observe(fragmentActivity, feedAdapter::submitList);
        final RecyclerLazyLoader lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (feedHasNextPage) {
                fetchFeed();
            }
        });
        if (shouldAutoPlay) {
            videoAwareRecyclerScroller = new VideoAwareRecyclerScroller();
            binding.feedRecyclerView.addOnScrollListener(videoAwareRecyclerScroller);
        }
        binding.feedRecyclerView.addOnScrollListener(lazyLoader);
        fetchFeed();
    }

    private void fetchFeed() {
        binding.feedSwipeRefreshLayout.setRefreshing(true);
        new FeedFetcher(feedEndCursor, feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setupFeedStories() {
        feedStoriesViewModel = new ViewModelProvider(fragmentActivity).get(FeedStoriesViewModel.class);
        final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter((model, position) -> {
            final NavDirections action = FeedFragmentDirections.actionFeedFragmentToStoryViewerFragment(position, null, false, false, null, null);
            NavHostFragment.findNavController(this).navigate(action);
        });
        final Context context = getContext();
        if (context == null) return;
        binding.feedStoriesRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        binding.feedStoriesRecyclerView.setAdapter(feedStoriesAdapter);
        feedStoriesViewModel.getList().observe(fragmentActivity, feedStoriesAdapter::submitList);
        fetchStories();
    }

    private void fetchStories() {
        storiesService.getFeedStories(new ServiceCallback<List<FeedStoryModel>>() {
            @Override
            public void onSuccess(final List<FeedStoryModel> result) {
                feedStoriesViewModel.getList().postValue(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "failed", t);
            }
        });
    }
}
