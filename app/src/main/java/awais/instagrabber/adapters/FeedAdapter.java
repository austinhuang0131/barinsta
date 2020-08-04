package awais.instagrabber.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONObject;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.activities.CommentsViewer;
import awais.instagrabber.activities.PostViewer;
import awais.instagrabber.adapters.viewholder.FeedItemViewHolder;
import awais.instagrabber.customviews.CommentMentionClickSpan;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class FeedAdapter extends RecyclerView.Adapter<FeedItemViewHolder> {
    private final static String ellipsize = "… more";
    private final Activity activity;
    private final LayoutInflater layoutInflater;
    private final ArrayList<FeedModel> feedModels;
    private final MentionClickListener mentionClickListener;
    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(@NonNull final View v) {
            final Object tag = v.getTag();

            if (tag instanceof FeedModel) {
                final FeedModel feedModel = (FeedModel) tag;

                if (v instanceof RamboTextView) {
                    if (feedModel.isMentionClicked())
                        feedModel.toggleCaption();
                    feedModel.setMentionClicked(false);
                    if (!expandCollapseTextView((RamboTextView) v, feedModel))
                        feedModel.toggleCaption();

                } else {
                    final int id = v.getId();
                    switch (id) {
                        case R.id.btnComments:
                            activity.startActivityForResult(new Intent(activity, CommentsViewer.class)
                                    .putExtra(Constants.EXTRAS_SHORTCODE, feedModel.getShortCode())
                                    .putExtra(Constants.EXTRAS_POST, feedModel.getPostId())
                                    .putExtra(Constants.EXTRAS_USER, feedModel.getProfileModel().getId()), 6969);
                            break;

                        case R.id.viewStoryPost:
                            activity.startActivity(new Intent(activity, PostViewer.class)
                                    .putExtra(Constants.EXTRAS_INDEX, feedModel.getPosition())
                                    .putExtra(Constants.EXTRAS_POST, new PostModel(feedModel.getShortCode(), false))
                                    .putExtra(Constants.EXTRAS_TYPE, ItemGetType.FEED_ITEMS));
                            break;

                        case R.id.btnDownload:
                            final Context context = v.getContext();
                            ProfileModel profileModel = feedModel.getProfileModel();
                            final String username = profileModel != null ? profileModel.getUsername() : null;

                            final ViewerPostModel[] sliderItems = feedModel.getSliderItems();

                            if (feedModel.getItemType() != MediaItemType.MEDIA_TYPE_SLIDER || sliderItems == null || sliderItems.length == 1)
                                Utils.batchDownload(context, username, DownloadMethod.DOWNLOAD_FEED, Collections.singletonList(feedModel));
                            else {
                                final ArrayList<BasePostModel> postModels = new ArrayList<>();
                                final DialogInterface.OnClickListener clickListener = (dialog, which) -> {
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
                                    if (breakWhenFoundSelected && postModels.size() == 0)
                                        postModels.add(sliderItems[0]);

                                    if (postModels.size() > 0)
                                        Utils.batchDownload(context, username, DownloadMethod.DOWNLOAD_FEED, postModels);
                                };

                                new AlertDialog.Builder(context).setTitle(R.string.post_viewer_download_dialog_title)
                                        .setPositiveButton(R.string.post_viewer_download_current, clickListener)
                                        .setNegativeButton(R.string.post_viewer_download_album, clickListener).show();
                            }
                            break;

                        case R.id.ivProfilePic:
                            if (mentionClickListener != null) {
                                profileModel = feedModel.getProfileModel();
                                if (profileModel != null)
                                    mentionClickListener.onClick(null, profileModel.getUsername(), false);
                            }
                            break;
                    }
                }
            }
        }
    };
    private final View.OnLongClickListener longClickListener = v -> {
        final Object tag;
        if (v instanceof RamboTextView && (tag = v.getTag()) instanceof FeedModel)
            Utils.copyText(v.getContext(), ((FeedModel) tag).getPostCaption());
        return true;
    };
    public SimpleExoPlayer pagerPlayer;
    private final PlayerChangeListener playerChangeListener = (childPos, player) -> {
        // todo
        pagerPlayer = player;
    };

    public FeedAdapter(final Activity activity, final ArrayList<FeedModel> FeedModels, final MentionClickListener mentionClickListener) {
        this.activity = activity;
        this.feedModels = FeedModels;
        this.mentionClickListener = mentionClickListener;
        this.layoutInflater = LayoutInflater.from(activity);
    }

    @NonNull
    @Override
    public FeedItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view;
        if (viewType == MediaItemType.MEDIA_TYPE_VIDEO.ordinal())
            view = layoutInflater.inflate(R.layout.item_feed_video, parent, false);
        else if (viewType == MediaItemType.MEDIA_TYPE_SLIDER.ordinal())
            view = layoutInflater.inflate(R.layout.item_feed_slider, parent, false);
        else
            view = layoutInflater.inflate(R.layout.item_feed, parent, false);
        return new FeedItemViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final FeedItemViewHolder viewHolder, final int position) {
        final FeedModel feedModel = feedModels.get(position);
        if (feedModel != null) {
            final RequestManager glideRequestManager = Glide.with(viewHolder.itemView);

            feedModel.setPosition(position);

            viewHolder.viewPost.setTag(feedModel);
            viewHolder.profilePic.setTag(feedModel);
            viewHolder.btnDownload.setTag(feedModel);
            viewHolder.viewerCaption.setTag(feedModel);

            final ProfileModel profileModel = feedModel.getProfileModel();
            if (profileModel != null) {
                glideRequestManager.load(profileModel.getSdProfilePic()).into(viewHolder.profilePic);
                viewHolder.username.setText("@"+profileModel.getUsername());
            }

            viewHolder.viewPost.setOnClickListener(clickListener);
            viewHolder.profilePic.setOnClickListener(clickListener);
            viewHolder.btnDownload.setOnClickListener(clickListener);

            viewHolder.tvPostDate.setText(feedModel.getPostDate());

            final long commentsCount = feedModel.getCommentsCount();
            viewHolder.commentsCount.setText(String.valueOf(commentsCount));

            viewHolder.btnComments.setTag(feedModel);
            viewHolder.btnComments.setOnClickListener(clickListener);
            viewHolder.btnComments.setEnabled(true);

            final JSONObject location = feedModel.getLocation();

            if (location == null) {
                viewHolder.location.setVisibility(View.GONE);
                viewHolder.username.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
                ));
            }
            else {
                viewHolder.location.setVisibility(View.VISIBLE);
                viewHolder.location.setText(location.optString("name"));
                viewHolder.username.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
                ));
                viewHolder.location.setOnClickListener(v ->
                        new AlertDialog.Builder(v.getContext()).setTitle(location.optString("name"))
                                .setMessage(R.string.comment_view_mention_location_search)
                                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok,
                                (dialog, which) -> mentionClickListener.onClick(null, location.optString("id")+"/"+location.optString("slug"), false)).show()
                );
            }

            final String thumbnailUrl = feedModel.getThumbnailUrl();
            final String displayUrl = feedModel.getDisplayUrl();
            CharSequence postCaption = feedModel.getPostCaption();

            final boolean captionEmpty = Utils.isEmpty(postCaption);

            viewHolder.viewerCaption.setOnClickListener(clickListener);
            viewHolder.viewerCaption.setOnLongClickListener(longClickListener);
            viewHolder.viewerCaption.setVisibility(captionEmpty ? View.GONE : View.VISIBLE);

            if (!captionEmpty && Utils.hasMentions(postCaption)) {
                postCaption = Utils.getMentionText(postCaption);
                feedModel.setPostCaption(postCaption);
                viewHolder.viewerCaption.setText(postCaption, TextView.BufferType.SPANNABLE);
                viewHolder.viewerCaption.setMentionClickListener(mentionClickListener);
            } else {
                viewHolder.viewerCaption.setText(postCaption);
            }

            expandCollapseTextView(viewHolder.viewerCaption, feedModel);

            final MediaItemType itemType = feedModel.getItemType();
            final View viewToChangeHeight;

            if (itemType == MediaItemType.MEDIA_TYPE_VIDEO) {
                viewToChangeHeight = viewHolder.playerView;

                viewHolder.videoViewsParent.setVisibility(View.VISIBLE);
                viewHolder.videoViews.setText(String.valueOf(feedModel.getViewCount()));
            } else {
                viewHolder.videoViewsParent.setVisibility(View.GONE);
                viewHolder.btnMute.setVisibility(View.GONE);

                if (itemType == MediaItemType.MEDIA_TYPE_SLIDER) {
                    viewToChangeHeight = viewHolder.mediaList;

                    final ViewerPostModel[] sliderItems = feedModel.getSliderItems();
                    final int sliderItemLen = sliderItems != null ? sliderItems.length : 0;

                    if (sliderItemLen > 0) {
                        viewHolder.mediaCounter.setText("1/" + sliderItemLen);
                        viewHolder.mediaList.setOffscreenPageLimit(Math.min(5, sliderItemLen));

                        final ViewPager.SimpleOnPageChangeListener simpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
                            private int prevPos = 0;

                            @Override
                            public void onPageSelected(final int position) {
                                ViewerPostModel sliderItem = sliderItems[prevPos];
                                if (sliderItem != null) sliderItem.setSelected(false);
                                sliderItem = sliderItems[position];
                                if (sliderItem != null) sliderItem.setSelected(true);

                                View childAt = viewHolder.mediaList.getChildAt(prevPos);
                                if (childAt instanceof PlayerView) {
                                    pagerPlayer = (SimpleExoPlayer) ((PlayerView) childAt).getPlayer();
                                    if (pagerPlayer != null) pagerPlayer.setPlayWhenReady(false);
                                }
                                childAt = viewHolder.mediaList.getChildAt(position);
                                if (childAt instanceof PlayerView) {
                                    pagerPlayer = (SimpleExoPlayer) ((PlayerView) childAt).getPlayer();
                                    if (pagerPlayer != null) pagerPlayer.setPlayWhenReady(true);
                                }
                                prevPos = position;
                                viewHolder.mediaCounter.setText((position + 1) + "/" + sliderItemLen);
                            }
                        };

                        //noinspection deprecation
                        viewHolder.mediaList.setOnPageChangeListener(simpleOnPageChangeListener); // cause add listeners might add to recycled holders

                        final View.OnClickListener muteClickListener = v -> {
                            Player player = null;
                            if (v instanceof PlayerView) player = ((PlayerView) v).getPlayer();
                            else if (v instanceof ImageView || v == viewHolder.btnMute) {
                                final int currentItem = viewHolder.mediaList.getCurrentItem();
                                if (currentItem < viewHolder.mediaList.getChildCount()) {
                                    final View childAt = viewHolder.mediaList.getChildAt(currentItem);
                                    if (childAt instanceof PlayerView) player = ((PlayerView) childAt).getPlayer();
                                }

                            } else {
                                final Object tag = v.getTag();
                                if (tag instanceof Player) player = (Player) tag;
                            }

                            if (player instanceof SimpleExoPlayer) {
                                final SimpleExoPlayer exoPlayer = (SimpleExoPlayer) player;
                                final float intVol = exoPlayer.getVolume() == 0f ? 1f : 0f;
                                exoPlayer.setVolume(intVol);
                                viewHolder.btnMute.setImageResource(intVol == 0f ? R.drawable.mute : R.drawable.vol);
                                Utils.sessionVolumeFull = intVol == 1f;
                            }
                        };

                        viewHolder.btnMute.setOnClickListener(muteClickListener);
                        viewHolder.mediaList.setAdapter(new ChildMediaItemsAdapter(sliderItems, viewHolder.btnMute, playerChangeListener));
                    }
                } else {
                    viewToChangeHeight = viewHolder.imageView;
                    String url = displayUrl;
                    if (Utils.isEmpty(url)) url = thumbnailUrl;
                    glideRequestManager.load(url).into(viewHolder.imageView);
                }
            }

            if (viewToChangeHeight != null) {
                final ViewGroup.LayoutParams layoutParams = viewToChangeHeight.getLayoutParams();
                layoutParams.height = Utils.displayMetrics.widthPixels + 1;
                viewToChangeHeight.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public int getItemCount() {
        return feedModels == null ? 0 : feedModels.size();
    }

    @Override
    public int getItemViewType(final int position) {
        if (feedModels != null) return feedModels.get(position).getItemType().ordinal();
        return MediaItemType.MEDIA_TYPE_IMAGE.ordinal();
    }

    /**
     * expands or collapses {@link RamboTextView} [stg idek why i wrote this documentation]
     *
     * @param textView  the {@link RamboTextView} view, to expand and collapse
     * @param feedModel the {@link FeedModel} model to check wether model is collapsed to expanded
     *
     * @return true if expanded/collapsed, false if empty or text size is <= 255 chars
     */
    public static boolean expandCollapseTextView(@NonNull final RamboTextView textView, @NonNull final FeedModel feedModel) {
        final CharSequence caption = feedModel.getPostCaption();
        if (Utils.isEmpty(caption)) return false;

        final TextView.BufferType bufferType = caption instanceof Spanned ? TextView.BufferType.SPANNABLE : TextView.BufferType.NORMAL;

        if (!feedModel.isCaptionExpanded()) {
            int i = Utils.indexOfChar(caption, '\r', 0);
            if (i == -1) i = Utils.indexOfChar(caption, '\n', 0);
            if (i == -1) i = 255;

            final int captionLen = caption.length();
            final int minTrim = Math.min(255, i);
            if (captionLen <= minTrim) return false;

            if (Utils.hasMentions(caption))
                textView.setText(Utils.getMentionText(caption), TextView.BufferType.SPANNABLE);
            textView.setCaptionIsExpandable(true);
            textView.setCaptionIsExpanded(true);
        } else {
            textView.setText(caption, bufferType);
            textView.setCaptionIsExpanded(false);
        }
        return true;
    }

    private interface PlayerChangeListener {
        void playerChanged(final int childPos, final SimpleExoPlayer player);
    }

    private static final class ChildMediaItemsAdapter extends PagerAdapter {
        private final PlayerChangeListener playerChangeListener;
        private final ViewerPostModel[] sliderItems;
        private final View btnMute;
        private SimpleExoPlayer player;

        private ChildMediaItemsAdapter(final ViewerPostModel[] sliderItems, final View btnMute,
                                       final PlayerChangeListener playerChangeListener) {
            this.sliderItems = sliderItems;
            this.btnMute = btnMute;
            if (BuildConfig.DEBUG) this.playerChangeListener = playerChangeListener;
            else this.playerChangeListener = null;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
            if (BuildConfig.DEBUG) container.setBackgroundColor(0xFF_0a_c0_09); // todo remove

            final Context context = container.getContext();
            final ViewerPostModel sliderItem = sliderItems[position];

            if (sliderItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                if (btnMute != null) btnMute.setVisibility(View.VISIBLE);
                final PlayerView playerView = new PlayerView(context);

                player = new SimpleExoPlayer.Builder(context).build();
                playerView.setPlayer(player);

                float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
                if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;
                player.setVolume(vol);
                player.setPlayWhenReady(Utils.settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));

                final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(context, "instagram"))
                        .createMediaSource(Uri.parse(sliderItem.getDisplayUrl()));

                player.setRepeatMode(Player.REPEAT_MODE_ALL);
                player.prepare(mediaSource);
                player.setVolume(vol);

                playerView.setTag(player);

                if (playerChangeListener != null) {
                    //todo
                    // playerChangeListener.playerChanged(position, player);
                    Log.d("AWAISKING_APP", "playerChangeListener: " + playerChangeListener);
                }

                container.addView(playerView);
                return playerView;
            } else {
                if (btnMute != null) btnMute.setVisibility(View.GONE);

                final PhotoView photoView = new PhotoView(context);
                photoView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                Glide.with(context).load(sliderItem.getDisplayUrl()).into(photoView);
                container.addView(photoView);
                return photoView;
            }
        }

        @Override
        public void destroyItem(@NonNull final ViewGroup container, final int position, @NonNull final Object object) {
            final Player player = object instanceof PlayerView ? ((PlayerView) object).getPlayer() : this.player;

            if (player == this.player && this.player != null) {
                this.player.stop(true);
                this.player.release();
            } else if (player != null) {
                player.stop(true);
                player.release();
            }

            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return sliderItems != null ? sliderItems.length : 0;
        }

        @Override
        public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
            return view == object;
        }
    }
}