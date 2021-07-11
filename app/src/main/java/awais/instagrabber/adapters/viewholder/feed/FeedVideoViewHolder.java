package awais.instagrabber.adapters.viewholder.feed;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.databinding.ItemFeedVideoBinding;
import awais.instagrabber.databinding.LayoutPostViewBottomBinding;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.MediaCandidate;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedVideoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedVideoViewHolder";

    private final ItemFeedVideoBinding binding;
    private final FeedAdapterV2.FeedItemCallback feedItemCallback;
    private final Handler handler;
    private final DefaultDataSourceFactory dataSourceFactory;

    private final LayoutPostViewBottomBinding bottom;
    private CacheDataSourceFactory cacheDataSourceFactory;
    private Media media;

    // private final Runnable loadRunnable = new Runnable() {
    //     @Override
    //     public void run() {
    //         // loadPlayer(feedModel);
    //     }
    // };

    public FeedVideoViewHolder(@NonNull final ItemFeedVideoBinding binding,
                               final FeedAdapterV2.FeedItemCallback feedItemCallback) {
        super(binding.getRoot(), feedItemCallback);
        bottom = LayoutPostViewBottomBinding.bind(binding.getRoot());
        this.binding = binding;
        this.feedItemCallback = feedItemCallback;
        bottom.viewsCount.setVisibility(View.VISIBLE);
        handler = new Handler(Looper.getMainLooper());
        final Context context = binding.getRoot().getContext();
        dataSourceFactory = new DefaultDataSourceFactory(context, "instagram");
        final SimpleCache simpleCache = Utils.getSimpleCacheInstance(context);
        if (simpleCache != null) {
            cacheDataSourceFactory = new CacheDataSourceFactory(simpleCache, dataSourceFactory);
        }
    }

    @Override
    public void bindItem(final Media media) {
        // Log.d(TAG, "Binding post: " + feedModel.getPostId());
        this.media = media;
        final String viewCount = itemView.getResources().getQuantityString(R.plurals.views_count, (int) media.getViewCount(), media.getViewCount());
        bottom.viewsCount.setText(viewCount);
        final LayoutVideoPlayerWithThumbnailBinding videoPost =
                LayoutVideoPlayerWithThumbnailBinding.inflate(LayoutInflater.from(itemView.getContext()), binding.getRoot(), false);
        final ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) videoPost.getRoot().getLayoutParams();
        final NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                media.getOriginalWidth(),
                (int) (Utils.displayMetrics.heightPixels * 0.8),
                Utils.displayMetrics.widthPixels);
        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
        layoutParams.height = widthHeight.second;
        final View postView = videoPost.getRoot();
        binding.postContainer.addView(postView);
        final float vol = settingsHelper.getBoolean(PreferenceKeys.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {

            @Override
            public void onThumbnailClick() {
                feedItemCallback.onPostClick(media);
            }

            @Override
            public void onPlayerViewLoaded() {
                final ViewGroup.LayoutParams layoutParams = videoPost.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils.getResultingHeight(requiredWidth, media.getOriginalHeight(), media.getOriginalWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                videoPost.playerView.requestLayout();
            }
        };
        final float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
        String videoUrl = null;
        final List<MediaCandidate> videoVersions = media.getVideoVersions();
        if (videoVersions != null && !videoVersions.isEmpty()) {
            final MediaCandidate videoVersion = videoVersions.get(0);
            videoUrl = videoVersion.getUrl();
        }
        final VideoPlayerViewHelper videoPlayerViewHelper = new VideoPlayerViewHelper(binding.getRoot().getContext(),
                                                                                      videoPost,
                                                                                      videoUrl,
                                                                                      vol,
                                                                                      aspectRatio,
                                                                                      ResponseBodyUtils.getThumbUrl(media),
                                                                                      false,
                                                                                      // null,
                                                                                      videoPlayerCallback);
        videoPost.thumbnail.post(() -> {
            if (media.getOriginalHeight() > 0.8 * Utils.displayMetrics.heightPixels) {
                final ViewGroup.LayoutParams tLayoutParams = videoPost.thumbnail.getLayoutParams();
                tLayoutParams.height = (int) (0.8 * Utils.displayMetrics.heightPixels);
                videoPost.thumbnail.requestLayout();
            }
        });
    }

    public Media getCurrentFeedModel() {
        return media;
    }

    // public void stopPlaying() {
    //     // Log.d(TAG, "Stopping post: " + feedModel.getPostId() + ", player: " + player + ", player.isPlaying: " + (player != null && player.isPlaying()));
    //     handler.removeCallbacks(loadRunnable);
    //     if (player != null) {
    //         player.release();
    //     }
    //     if (videoPost.root.getDisplayedChild() == 1) {
    //         videoPost.root.showPrevious();
    //     }
    // }
    //
    // public void startPlaying() {
    //     handler.removeCallbacks(loadRunnable);
    //     handler.postDelayed(loadRunnable, 800);
    // }
}
