package awais.instagrabber;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.appbar.AppBarLayout;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import awais.instagrabber.activities.FollowViewer;
import awais.instagrabber.activities.Main;
import awais.instagrabber.activities.PostViewer;
import awais.instagrabber.activities.SavedViewer;
import awais.instagrabber.activities.StoryViewer;
import awais.instagrabber.adapters.DiscoverAdapter;
import awais.instagrabber.adapters.FeedAdapter;
import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.DiscoverFetcher;
import awais.instagrabber.asyncs.FeedFetcher;
import awais.instagrabber.asyncs.FeedStoriesFetcher;
import awais.instagrabber.asyncs.HashtagFetcher;
import awais.instagrabber.asyncs.HighlightsFetcher;
import awais.instagrabber.asyncs.LocationFetcher;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
import awais.instagrabber.asyncs.i.iTopicFetcher;
import awais.instagrabber.customviews.MouseDrawer;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.customviews.helpers.VideoAwareRecyclerScroller;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.DiscoverItemModel;
import awais.instagrabber.models.DiscoverTopicModel;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.IntentModelType;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
import static awais.instagrabber.utils.Constants.BOTTOM_TOOLBAR;
import static awais.instagrabber.utils.Utils.logCollector;

public final class MainHelper implements SwipeRefreshLayout.OnRefreshListener {
    private static AsyncTask<?, ?, ?> currentlyExecuting;
    private AsyncTask<Void, Void, FeedStoryModel[]> prevStoriesFetcher;
    private final boolean autoloadPosts;
    private FeedStoryModel[] stories;
    private boolean hasNextPage = false, feedHasNextPage = false, discoverHasMore = false;
    private String endCursor = null, feedEndCursor = null, discoverEndMaxId = null, topic = null, rankToken = null;
    private String[] topicIds = null;
    private final FetchListener<PostModel[]> postsFetchListener = new FetchListener<PostModel[]>() {
        @Override
        public void onResult(final PostModel[] result) {
            if (result != null) {
                final int oldSize = main.allItems.size();
                main.allItems.addAll(Arrays.asList(result));

                postsAdapter.notifyItemRangeInserted(oldSize, result.length);

                main.mainBinding.profileView.mainPosts.post(() -> {
                    main.mainBinding.profileView.mainPosts.setNestedScrollingEnabled(true);
                    main.mainBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
                });

                if (isHashtag)
                    main.mainBinding.toolbar.toolbar.setTitle(main.userQuery);
                else if (isLocation)
                    main.mainBinding.toolbar.toolbar.setTitle(main.locationModel.getName());
                else main.mainBinding.toolbar.toolbar.setTitle("@"+main.profileModel.getUsername());

                final PostModel model = result[result.length - 1];
                if (model != null) {
                    endCursor = model.getEndCursor();
                    hasNextPage = model.hasNextPage();
                    if (autoloadPosts && hasNextPage)
                        currentlyExecuting = new PostsFetcher(
                                main.profileModel != null ? main.profileModel.getId()
                                    : (main.hashtagModel != null ? main.userQuery : main.locationModel.getId()), endCursor, this)
                                .setUsername((isLocation || isHashtag) ? null : main.profileModel.getUsername())
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else {
                        main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    }
                    model.setPageCursor(false, null);
                }
            }
            else {
                main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                main.mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
                main.mainBinding.profileView.privatePage2.setText(R.string.empty_acc);
                main.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
            }
        }
    };
    private final FetchListener<FeedModel[]> feedFetchListener = new FetchListener<FeedModel[]>() {
        @Override
        public void doBefore() {
            main.mainBinding.feedView.feedSwipeRefreshLayout.post(() -> main.mainBinding.feedView.feedSwipeRefreshLayout.setRefreshing(true));
        }

        @Override
        public void onResult(final FeedModel[] result) {
            if (result != null) {
                final int oldSize = main.feedItems.size();
                main.feedItems.addAll(Arrays.asList(result));
                feedAdapter.notifyItemRangeInserted(oldSize, result.length);

                main.mainBinding.feedView.feedPosts.post(() -> main.mainBinding.feedView.feedPosts.setNestedScrollingEnabled(true));

                final PostModel feedPostModel = result[result.length - 1];
                if (feedPostModel != null) {
                    feedEndCursor = feedPostModel.getEndCursor();
                    feedHasNextPage = feedPostModel.hasNextPage();
                    feedPostModel.setPageCursor(false, null);
                }
            }

            main.mainBinding.feedView.feedSwipeRefreshLayout.setRefreshing(false);
        }
    };
    private final FetchListener<DiscoverItemModel[]> discoverFetchListener = new FetchListener<DiscoverItemModel[]>() {
        @Override
        public void doBefore() {
            main.mainBinding.discoverSwipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final DiscoverItemModel[] result) {
            if (result == null || result.length == 0) {
                Toast.makeText(main, R.string.discover_empty, Toast.LENGTH_SHORT).show();
            }
            else if (result != null) {
                final int oldSize = main.discoverItems.size();
                main.discoverItems.addAll(Arrays.asList(result));
                discoverAdapter.notifyItemRangeInserted(oldSize, result.length);

                final DiscoverItemModel discoverItemModel = result[result.length - 1];
                if (discoverItemModel != null) {
                    discoverEndMaxId = discoverItemModel.getNextMaxId();
                    discoverHasMore = discoverItemModel.hasMore();
                    discoverItemModel.setMore(false, null);
                }
            }

            main.mainBinding.discoverSwipeRefreshLayout.setRefreshing(false);
        }
    };
    private final FetchListener<DiscoverTopicModel> topicFetchListener = new FetchListener<DiscoverTopicModel>() {
        @Override
        public void doBefore() {}

        @Override
        public void onResult(final DiscoverTopicModel result) {
            if (result != null) {
                topicIds = result.getIds();
                rankToken = result.getToken();
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                        main, android.R.layout.simple_spinner_dropdown_item, result.getNames() );
                main.mainBinding.discoverType.setAdapter(spinnerArrayAdapter);
            }
        }
    };
    private final FetchListener<FeedStoryModel[]> feedStoriesListener = new FetchListener<FeedStoryModel[]>() {
        @Override
        public void doBefore() {
            main.mainBinding.feedView.feedStories.setVisibility(View.GONE);
        }

        @Override
        public void onResult(final FeedStoryModel[] result) {
            feedStoriesAdapter.setData(result);
            if (result != null && result.length > 0) {
                main.mainBinding.feedView.feedStories.setVisibility(View.VISIBLE);
                stories = result;
            }
        }
    };
    private final MentionClickListener mentionClickListener = new MentionClickListener() {
        @Override
        public void onClick(final RamboTextView view, final String text, final boolean isHashtag) {
            new AlertDialog.Builder(main).setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
                    .setTitle(text).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok, (dialog, which) -> {
                if (Main.scanHack != null) Main.scanHack.onResult(text);
            }).show();
        }
    };
    private final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter(null, new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Object tag = v.getTag();
            if (tag instanceof FeedStoryModel) {
                final FeedStoryModel feedStoryModel = (FeedStoryModel) tag;
                final int index = indexOfIntArray(stories, feedStoryModel);
                new iStoryStatusFetcher(feedStoryModel.getStoryMediaId(), null, false, false, false, false, result -> {
                    if (result != null && result.length > 0)
                        main.startActivity(new Intent(main, StoryViewer.class)
                            .putExtra(Constants.EXTRAS_STORIES, result)
                            .putExtra(Constants.EXTRAS_USERNAME, feedStoryModel.getProfileModel().getUsername())
                            .putExtra(Constants.FEED, stories)
                            .putExtra(Constants.FEED_ORDER, index)
                        );
                    else Toast.makeText(main, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    });
    @NonNull
    private final Main main;
    private Resources resources;
    private final View collapsingToolbar;
    private final RecyclerLazyLoader lazyLoader;
    private boolean isHashtag, isUser, isLocation;
    private PostsAdapter postsAdapter;
    private FeedAdapter feedAdapter;
    private RecyclerLazyLoader feedLazyLoader, discoverLazyLoader;
    private DiscoverAdapter discoverAdapter;
    public SimpleExoPlayer currentFeedPlayer; // hack for remix drawer layout
    private String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    public boolean isLoggedIn = !Utils.isEmpty(cookie);

    public MainHelper(@NonNull final Main main) {
        stopCurrentExecutor();

        this.main = main;
        this.resources = main.getResources();
        this.autoloadPosts = Utils.settingsHelper.getBoolean(AUTOLOAD_POSTS);

        main.mainBinding.profileView.swipeRefreshLayout.setOnRefreshListener(this);
        main.mainBinding.profileView.mainUrl.setMovementMethod(new LinkMovementMethod());

        final LinearLayout iconSlider = main.findViewById(R.id.iconSlider);
        final ImageView iconFeed = (ImageView) iconSlider.getChildAt(0);
        final ImageView iconProfile = (ImageView) iconSlider.getChildAt(1);
        final ImageView iconDiscover = (ImageView) iconSlider.getChildAt(2);

        final boolean isBottomToolbar = Utils.settingsHelper.getBoolean(BOTTOM_TOOLBAR);
        isLoggedIn = !Utils.isEmpty(cookie);
        if (!isLoggedIn) {
            main.mainBinding.drawerLayout.removeView(main.mainBinding.feedView.feedLayout);
            main.mainBinding.drawerLayout.removeView(main.mainBinding.discoverLayout);
            iconFeed.setAlpha(0.4f);
            iconDiscover.setAlpha(0.4f);
        } else {
            iconFeed.setAlpha(1f);
            iconDiscover.setAlpha(1f);

            setupExplore();

            setupFeed();

            final TypedValue resolvedAttr = new TypedValue();
            main.getTheme().resolveAttribute(android.R.attr.textColorPrimary, resolvedAttr, true);

            final int selectedItem = ContextCompat.getColor(main, resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data);
            final ColorStateList colorStateList = ColorStateList.valueOf(selectedItem);

            main.mainBinding.toolbar.toolbar.measure(0, -1);
            final int toolbarMeasuredHeight = main.mainBinding.toolbar.toolbar.getMeasuredHeight();

            final ViewGroup.LayoutParams layoutParams = main.mainBinding.toolbar.toolbar.getLayoutParams();
            final MouseDrawer.DrawerListener simpleDrawerListener = new MouseDrawer.DrawerListener() {
                private final String titleDiscover = resources.getString(R.string.title_discover);

                @Override
                public void onDrawerSlide(final View drawerView, @MouseDrawer.EdgeGravity final int gravity, final float slideOffset) {
                    final int currentIconAlpha = (int) Math.max(100, 255 - 255 * slideOffset);
                    final int otherIconAlpha = (int) Math.max(100, 255 * slideOffset);

                    ImageViewCompat.setImageTintList(iconProfile, colorStateList.withAlpha(currentIconAlpha));

                    final boolean drawerOpening = slideOffset > 0.0f;
                    final int alpha;
                    final ColorStateList imageTintList;

                    if (gravity == GravityCompat.START) {
                        // this helps hide the toolbar when opening feed

                        final int roundedToolbarHeight;
                        final float toolbarHeight;

                        if (isBottomToolbar) {
                            toolbarHeight = toolbarMeasuredHeight * slideOffset;
                            roundedToolbarHeight = -Math.round(toolbarHeight);
                        } else {
                            toolbarHeight = -toolbarMeasuredHeight * slideOffset;
                            roundedToolbarHeight = Math.round(toolbarHeight);
                        }

                        layoutParams.height = Math.max(0, Math.min(toolbarMeasuredHeight, toolbarMeasuredHeight + roundedToolbarHeight));
                        main.mainBinding.toolbar.toolbar.setLayoutParams(layoutParams);
                        main.mainBinding.toolbar.toolbar.setTranslationY(toolbarHeight);

                        imageTintList = ImageViewCompat.getImageTintList(iconDiscover);
                        alpha = imageTintList != null ? (imageTintList.getDefaultColor() & 0xFF_000000) >> 24 : 0;

                        if (drawerOpening && alpha > 100)
                            ImageViewCompat.setImageTintList(iconDiscover, colorStateList.withAlpha(currentIconAlpha));

                        ImageViewCompat.setImageTintList(iconFeed, colorStateList.withAlpha(otherIconAlpha));
                    } else {
                        // this changes toolbar title
                        main.mainBinding.toolbar.toolbar.setTitle(slideOffset >= 0.466 ? titleDiscover : main.userQuery);

                        imageTintList = ImageViewCompat.getImageTintList(iconFeed);
                        alpha = imageTintList != null ? (imageTintList.getDefaultColor() & 0xFF_000000) >> 24 : 0;

                        if (drawerOpening && alpha > 100)
                            ImageViewCompat.setImageTintList(iconFeed, colorStateList.withAlpha(currentIconAlpha));

                        ImageViewCompat.setImageTintList(iconDiscover, colorStateList.withAlpha(otherIconAlpha));
                    }
                }

                @Override
                public void onDrawerOpened(@NonNull final View drawerView, @MouseDrawer.EdgeGravity final int gravity) {
                    if (gravity == GravityCompat.START || drawerView == main.mainBinding.feedView.feedLayout) {
                        if (currentFeedPlayer != null) {
                            currentFeedPlayer.setPlayWhenReady(true);
                            currentFeedPlayer.getPlaybackState();
                        }
                    } else {
                        // clear selection
                        isSelectionCleared();
                    }
                }

                @Override
                public void onDrawerClosed(@NonNull final View drawerView, @MouseDrawer.EdgeGravity final int gravity) {
                    if (gravity == GravityCompat.START || drawerView == main.mainBinding.feedView.feedLayout) {
                        if (currentFeedPlayer != null) {
                            currentFeedPlayer.setPlayWhenReady(false);
                            currentFeedPlayer.getPlaybackState();
                        }
                    } else {
                        // clear selection
                        isSelectionCleared();
                    }
                }
            };

            ImageViewCompat.setImageTintList(iconFeed, colorStateList.withAlpha(100)); // to change colors when created
            ImageViewCompat.setImageTintList(iconProfile, colorStateList.withAlpha(255)); // to change colors when created
            ImageViewCompat.setImageTintList(iconDiscover, colorStateList.withAlpha(100)); // to change colors when created

            main.mainBinding.drawerLayout.addDrawerListener(simpleDrawerListener);
        }

        collapsingToolbar = main.mainBinding.profileView.appBarLayout.getChildAt(0);

        main.mainBinding.profileView.mainPosts.setNestedScrollingEnabled(false);
        main.mainBinding.profileView.highlightsList.setLayoutManager(new LinearLayoutManager(main, LinearLayoutManager.HORIZONTAL, false));
        main.mainBinding.profileView.highlightsList.setAdapter(main.highlightsAdapter);

        // int color = -1;
        // final Drawable background = main.mainBinding.profileView.appBarLayout.getBackground();
        // if (background instanceof MaterialShapeDrawable) {
        //     final MaterialShapeDrawable drawable = (MaterialShapeDrawable) background;
        //     final ColorStateList fillColor = drawable.getFillColor();
        //     if (fillColor != null) color = fillColor.getDefaultColor();
        // } else {
        //     final Bitmap bitmap = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);
        //     final Canvas canvas = new Canvas();
        //     canvas.setBitmap(bitmap);
        //     background.draw(canvas);
        //     color = bitmap.getPixel(4, 4);
        //     if (!bitmap.isRecycled()) bitmap.recycle();
        // }
        // if (color == -1 || color == 0) color = resources.getBoolean(R.bool.isNight) ? 0xff212121 : 0xfff5f5f5;
        // main.mainBinding.profileView.profileInfo.setBackgroundColor(color);
        // if (!isBottomToolbar) main.mainBinding.toolbar.toolbar.setBackgroundColor(color);

        // main.mainBinding.profileView.appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
        //     private int height;
        //
        //     @Override
        //     public void onOffsetChanged(final AppBarLayout appBarLayout, final int verticalOffset) {
        //         if (height == 0) {
        //             height = main.mainBinding.profileView.profileInfo.getHeight();
        //             collapsingToolbar.setMinimumHeight(height);
        //         }
        //         main.mainBinding.profileView.profileInfo.setTranslationY(-Math.min(0, verticalOffset));
        //     }
        // });

        main.setSupportActionBar(main.mainBinding.toolbar.toolbar);
        if (isBottomToolbar) {
            final LinearLayout linearLayout = (LinearLayout) main.mainBinding.toolbar.toolbar.getParent();
            linearLayout.removeView(main.mainBinding.toolbar.toolbar);
            linearLayout.addView(main.mainBinding.toolbar.toolbar, linearLayout.getChildCount());
        }

        // change the next number to adjust grid
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(main, Utils.convertDpToPx(110));
        main.mainBinding.profileView.mainPosts.setLayoutManager(layoutManager);
        main.mainBinding.profileView.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        main.mainBinding.profileView.mainPosts.setAdapter(postsAdapter = new PostsAdapter(main.allItems, v -> {
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                final PostModel postModel = (PostModel) tag;

                if (postsAdapter.isSelecting) toggleSelection(postModel);
                else main.startActivity(new Intent(main, PostViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, postModel.getPosition())
                        .putExtra(Constants.EXTRAS_POST, postModel)
                        .putExtra(Constants.EXTRAS_USER, main.userQuery)
                        .putExtra(Constants.EXTRAS_TYPE, ItemGetType.MAIN_ITEMS));
            }
        }, v -> { // long click listener
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                postsAdapter.isSelecting = true;
                toggleSelection((PostModel) tag);
            }
            return true;
        }));

        this.lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if ((!autoloadPosts || isHashtag) && hasNextPage) {
                main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(true);
                stopCurrentExecutor();
                currentlyExecuting = new PostsFetcher(main.profileModel != null ? main.profileModel.getId()
                        : (main.hashtagModel != null ? main.userQuery : main.locationModel.getId()), endCursor, postsFetchListener)
                        .setUsername((isHashtag || isLocation) ? null : main.profileModel.getUsername())
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                endCursor = null;
            }
        });
        main.mainBinding.profileView.mainPosts.addOnScrollListener(lazyLoader);
    }

    private void setupFeed() {
        main.mainBinding.feedView.feedStories.setLayoutManager(new LinearLayoutManager(main, LinearLayoutManager.HORIZONTAL, false));
        main.mainBinding.feedView.feedStories.setAdapter(feedStoriesAdapter);
        refreshFeedStories();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(main);
        main.mainBinding.feedView.feedPosts.setLayoutManager(layoutManager);
        main.mainBinding.feedView.feedPosts.setAdapter(feedAdapter = new FeedAdapter(main, main.feedItems, (view, text, isHashtag) ->
                new AlertDialog.Builder(main).setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
                        .setTitle(text).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (Main.scanHack != null) {
                        main.mainBinding.drawerLayout.closeDrawers();
                        Main.scanHack.onResult(text);
                    }
                }).show()));

        main.mainBinding.feedView.feedSwipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFeedStories();

            if (feedLazyLoader != null) feedLazyLoader.resetState();
            main.feedItems.clear();
            if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
            new FeedFetcher(feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });

        main.mainBinding.feedView.feedPosts.addOnScrollListener(feedLazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (feedHasNextPage) {
                main.mainBinding.feedView.feedSwipeRefreshLayout.setRefreshing(true);
                new FeedFetcher(feedEndCursor, feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                feedEndCursor = null;
            }
        }));

        main.mainBinding.feedView.feedPosts.addOnScrollListener(new VideoAwareRecyclerScroller(main, main.feedItems,
                (itemPos, player) -> currentFeedPlayer = player));

        new FeedFetcher(feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void refreshFeedStories() {
        // todo setup feed stories
        if (prevStoriesFetcher != null) {
            try {
                prevStoriesFetcher.cancel(true);
            } catch (final Exception e) {
                // ignore
            }
        }
        prevStoriesFetcher = new FeedStoriesFetcher(feedStoriesListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setupExplore() {
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(main, Utils.convertDpToPx(110));
        main.mainBinding.discoverPosts.setLayoutManager(layoutManager);
        main.mainBinding.discoverPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));

        new iTopicFetcher(topicFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        main.mainBinding.discoverType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (topicIds != null) {
                    topic = topicIds[pos];
                    main.mainBinding.discoverSwipeRefreshLayout.setRefreshing(true);
                    if (discoverLazyLoader != null) discoverLazyLoader.resetState();
                    main.discoverItems.clear();
                    if (discoverAdapter != null) discoverAdapter.notifyDataSetChanged();
                    new DiscoverFetcher(topic, null, rankToken, discoverFetchListener, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        main.mainBinding.discoverSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (discoverLazyLoader != null) discoverLazyLoader.resetState();
            main.discoverItems.clear();
            if (discoverAdapter != null) discoverAdapter.notifyDataSetChanged();
            new DiscoverFetcher(topic, null, rankToken, discoverFetchListener, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });

        main.mainBinding.discoverPosts.setAdapter(discoverAdapter = new DiscoverAdapter(main.discoverItems, v -> {
            final Object tag = v.getTag();
            if (tag instanceof DiscoverItemModel) {
                final DiscoverItemModel itemModel = (DiscoverItemModel) tag;

                if (discoverAdapter.isSelecting) toggleDiscoverSelection(itemModel);
                else main.startActivity(new Intent(main, PostViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, itemModel.getPosition())
                        .putExtra(Constants.EXTRAS_TYPE, ItemGetType.DISCOVER_ITEMS)
                        .putExtra(Constants.EXTRAS_POST, new PostModel(itemModel.getShortCode(), false)));
            }
        }, v -> {
            final Object tag = v.getTag();
            if (tag instanceof DiscoverItemModel) {
                discoverAdapter.isSelecting = true;
                toggleDiscoverSelection((DiscoverItemModel) tag);
            }
            return true;
        }));

        main.mainBinding.discoverPosts.addOnScrollListener(discoverLazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (discoverHasMore) {
                main.mainBinding.discoverSwipeRefreshLayout.setRefreshing(true);
                new DiscoverFetcher(topic, discoverEndMaxId, rankToken, discoverFetchListener, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                discoverEndMaxId = null;
            }
        }));
    }

    public void onIntent(final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (!Utils.isEmpty(action) && !Intent.ACTION_MAIN.equals(action)) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                boolean error = true;

                String data = null;
                final Bundle extras = intent.getExtras();
                if (extras != null) {
                    final Object extraData = extras.get(Intent.EXTRA_TEXT);
                    if (extraData != null) {
                        error = false;
                        data = extraData.toString();
                    }
                }

                if (error) {
                    final Uri intentData = intent.getData();
                    if (intentData != null) data = intentData.toString();
                }

                if (data != null && !Utils.isEmpty(data)) {
                    if (data.indexOf('\n') > 0) data = data.substring(data.lastIndexOf('\n') + 1);

                    final IntentModel model = Utils.stripString(data);
                    if (model != null) {
                        final String modelText = model.getText();
                        final IntentModelType modelType = model.getType();

                        if (modelType == IntentModelType.POST) {
                            main.startActivityForResult(new Intent(main, PostViewer.class)
                                    .putExtra(Constants.EXTRAS_USER, main.userQuery)
                                    .putExtra(Constants.EXTRAS_POST, new PostModel(modelText, false)), 9629);
                        } else {
                            main.addToStack();
                            main.userQuery = modelType == IntentModelType.HASHTAG ? ('#' + modelText) :
                                    (modelType == IntentModelType.LOCATION ? modelText : ('@'+modelText));
                            onRefresh();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        main.mainBinding.drawerLayout.closeDrawers();
        if (lazyLoader != null) lazyLoader.resetState();
        stopCurrentExecutor();
        main.allItems.clear();
        main.selectedItems.clear();
        if (postsAdapter != null) {
            postsAdapter.isSelecting = false;
            postsAdapter.notifyDataSetChanged();
        }
        main.mainBinding.profileView.appBarLayout.setExpanded(true, true);
        main.mainBinding.profileView.privatePage.setVisibility(View.GONE);
        main.mainBinding.profileView.privatePage2.setTextSize(28);
        main.mainBinding.profileView.mainProfileImage.setImageBitmap(null);
        main.mainBinding.profileView.mainHashtagImage.setImageBitmap(null);
        main.mainBinding.profileView.mainLocationImage.setImageBitmap(null);
        main.mainBinding.profileView.mainUrl.setText(null);
        main.mainBinding.profileView.locationUrl.setText(null);
        main.mainBinding.profileView.mainFullName.setText(null);
        main.mainBinding.profileView.locationFullName.setText(null);
        main.mainBinding.profileView.mainPostCount.setText(null);
        main.mainBinding.profileView.mainLocPostCount.setText(null);
        main.mainBinding.profileView.mainTagPostCount.setText(null);
        main.mainBinding.profileView.mainFollowers.setText(null);
        main.mainBinding.profileView.mainFollowing.setText(null);
        main.mainBinding.profileView.mainBiography.setText(null);
        main.mainBinding.profileView.locationBiography.setText(null);
        main.mainBinding.profileView.mainBiography.setEnabled(false);
        main.mainBinding.profileView.locationBiography.setEnabled(false);
        main.mainBinding.profileView.mainProfileImage.setEnabled(false);
        main.mainBinding.profileView.mainLocationImage.setEnabled(false);
        main.mainBinding.profileView.mainHashtagImage.setEnabled(false);
        main.mainBinding.profileView.mainBiography.setMentionClickListener(null);
        main.mainBinding.profileView.locationBiography.setMentionClickListener(null);
        main.mainBinding.profileView.mainUrl.setVisibility(View.GONE);
        main.mainBinding.profileView.locationUrl.setVisibility(View.GONE);
        main.mainBinding.profileView.isVerified.setVisibility(View.GONE);
        main.mainBinding.profileView.btnFollow.setVisibility(View.GONE);
        main.mainBinding.profileView.btnRestrict.setVisibility(View.GONE);
        main.mainBinding.profileView.btnBlock.setVisibility(View.GONE);
        main.mainBinding.profileView.btnSaved.setVisibility(View.GONE);
        main.mainBinding.profileView.btnLiked.setVisibility(View.GONE);
        main.mainBinding.profileView.btnTagged.setVisibility(View.GONE);
        main.mainBinding.profileView.btnMap.setVisibility(View.GONE);

        main.mainBinding.profileView.btnFollow.setOnClickListener(profileActionListener);
        main.mainBinding.profileView.btnRestrict.setOnClickListener(profileActionListener);
        main.mainBinding.profileView.btnBlock.setOnClickListener(profileActionListener);
        main.mainBinding.profileView.btnSaved.setOnClickListener(profileActionListener);
        main.mainBinding.profileView.btnLiked.setOnClickListener(profileActionListener);
        main.mainBinding.profileView.btnTagged.setOnClickListener(profileActionListener);
        main.mainBinding.profileView.btnFollowTag.setOnClickListener(profileActionListener);

        main.mainBinding.profileView.infoContainer.setVisibility(View.GONE);
        main.mainBinding.profileView.tagInfoContainer.setVisibility(View.GONE);
        main.mainBinding.profileView.locInfoContainer.setVisibility(View.GONE);

        main.mainBinding.profileView.mainPosts.setNestedScrollingEnabled(false);
        main.mainBinding.profileView.highlightsList.setVisibility(View.GONE);
        collapsingToolbar.setVisibility(View.GONE);
        main.highlightsAdapter.setData(null);

        main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(main.userQuery != null);
        if (main.userQuery == null) {
            main.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
            return;
        }

        isHashtag = main.userQuery.charAt(0) == '#';
        isUser = main.userQuery.charAt(0) == '@';
        isLocation = main.userQuery.contains("/");
        collapsingToolbar.setVisibility(isUser ? View.VISIBLE : View.GONE);

        if (isHashtag) {
            main.profileModel = null;
            main.locationModel = null;
            main.mainBinding.toolbar.toolbar.setTitle(main.userQuery);
            main.mainBinding.profileView.tagInfoContainer.setVisibility(View.VISIBLE);
            main.mainBinding.profileView.btnFollowTag.setVisibility(View.GONE);

            currentlyExecuting = new HashtagFetcher(main.userQuery.substring(1), hashtagModel -> {
                main.hashtagModel = hashtagModel;

                if (hashtagModel == null) {
                    main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(main, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                    main.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
                    return;
                }

                currentlyExecuting = new PostsFetcher(main.userQuery, postsFetchListener)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                main.mainBinding.profileView.btnFollowTag.setVisibility(View.VISIBLE);

                if (isLoggedIn) {
                    new iStoryStatusFetcher(hashtagModel.getName(), null, false, true, false, false, result -> {
                        main.storyModels = result;
                        if (result != null && result.length > 0) main.mainBinding.profileView.mainHashtagImage.setStoriesBorder();
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    if (hashtagModel.getFollowing() == true) {
                        main.mainBinding.profileView.btnFollowTag.setText(R.string.unfollow);
                        main.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_purple_background)));
                    }
                    else {
                        main.mainBinding.profileView.btnFollowTag.setText(R.string.follow);
                        main.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_pink_background)));
                    }
                } else {
                    if (Utils.dataBox.getFavorite(main.userQuery) != null) {
                        main.mainBinding.profileView.btnFollowTag.setText(R.string.unfavorite_short);
                        main.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_purple_background)));
                    }
                    else {
                        main.mainBinding.profileView.btnFollowTag.setText(R.string.favorite_short);
                        main.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_pink_background)));
                    }
                }

                main.mainBinding.profileView.mainHashtagImage.setEnabled(false);
                new MyTask().execute();
                main.mainBinding.profileView.mainHashtagImage.setEnabled(true);

                final String postCount = String.valueOf(hashtagModel.getPostCount());

                SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
                main.mainBinding.profileView.mainTagPostCount.setText(span);
                main.mainBinding.profileView.mainTagPostCount.setVisibility(View.VISIBLE);
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (isUser) {
            main.hashtagModel = null;
            main.locationModel = null;
            main.mainBinding.toolbar.toolbar.setTitle(main.userQuery);
            main.mainBinding.profileView.infoContainer.setVisibility(View.VISIBLE);
            main.mainBinding.profileView.btnFollowTag.setVisibility(View.GONE);

            currentlyExecuting = new ProfileFetcher(main.userQuery.substring(1), profileModel -> {
                main.profileModel = profileModel;

                if (profileModel == null) {
                    main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(main, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                    main.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
                    return;
                }

                main.mainBinding.profileView.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
                final String profileId = profileModel.getId();

                if (isLoggedIn || Utils.settingsHelper.getBoolean(Constants.STORIESIG)) {
                  new iStoryStatusFetcher(profileId, profileModel.getUsername(), false, false,
                          (!isLoggedIn && Utils.settingsHelper.getBoolean(Constants.STORIESIG)), false,
                          result -> {
                      main.storyModels = result;
                      if (result != null && result.length > 0) main.mainBinding.profileView.mainProfileImage.setStoriesBorder();
                  }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                  new HighlightsFetcher(profileId, (!isLoggedIn && Utils.settingsHelper.getBoolean(Constants.STORIESIG)), result -> {
                      if (result != null && result.length > 0) {
                          main.mainBinding.profileView.highlightsList.setVisibility(View.VISIBLE);
                          main.highlightsAdapter.setData(result);
                      }
                      else main.mainBinding.profileView.highlightsList.setVisibility(View.GONE);
                  }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                if (isLoggedIn) {
                    final String myId = Utils.getUserIdFromCookie(cookie);
                    if (!profileId.equals(myId)) {
                        main.mainBinding.profileView.btnTagged.setVisibility(View.GONE);
                        main.mainBinding.profileView.btnSaved.setVisibility(View.GONE);
                        main.mainBinding.profileView.btnLiked.setVisibility(View.GONE);
                        main.mainBinding.profileView.btnFollow.setVisibility(View.VISIBLE);
                        if (profileModel.getFollowing() == true) {
                            main.mainBinding.profileView.btnFollow.setText(R.string.unfollow);
                            main.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    main, R.color.btn_purple_background)));
                        }
                        else if (profileModel.getRequested() == true) {
                            main.mainBinding.profileView.btnFollow.setText(R.string.cancel);
                            main.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    main, R.color.btn_purple_background)));
                        }
                        else {
                            main.mainBinding.profileView.btnFollow.setText(R.string.follow);
                            main.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    main, R.color.btn_pink_background)));
                        }
                        main.mainBinding.profileView.btnRestrict.setVisibility(View.VISIBLE);
                        if (profileModel.getRestricted() == true) {
                            main.mainBinding.profileView.btnRestrict.setText(R.string.unrestrict);
                            main.mainBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    main, R.color.btn_green_background)));
                        }
                        else {
                            main.mainBinding.profileView.btnRestrict.setText(R.string.restrict);
                            main.mainBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    main, R.color.btn_orange_background)));
                        }
                        if (profileModel.isReallyPrivate()) {
                            main.mainBinding.profileView.btnBlock.setVisibility(View.VISIBLE);
                            main.mainBinding.profileView.btnTagged.setVisibility(View.GONE);
                            if (profileModel.getBlocked() == true) {
                                main.mainBinding.profileView.btnBlock.setText(R.string.unblock);
                                main.mainBinding.profileView.btnBlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        main, R.color.btn_green_background)));
                            } else {
                                main.mainBinding.profileView.btnBlock.setText(R.string.block);
                                main.mainBinding.profileView.btnBlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        main, R.color.btn_red_background)));
                            }
                        } else {
                            main.mainBinding.profileView.btnBlock.setVisibility(View.GONE);
                            main.mainBinding.profileView.btnSaved.setVisibility(View.VISIBLE);
                            main.mainBinding.profileView.btnTagged.setVisibility(View.VISIBLE);
                            if (profileModel.getBlocked() == true) {
                                main.mainBinding.profileView.btnSaved.setText(R.string.unblock);
                                main.mainBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        main, R.color.btn_green_background)));
                            } else {
                                main.mainBinding.profileView.btnSaved.setText(R.string.block);
                                main.mainBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        main, R.color.btn_red_background)));
                            }
                        }
                    }
                    else {
                        main.mainBinding.profileView.btnTagged.setVisibility(View.VISIBLE);
                        main.mainBinding.profileView.btnSaved.setVisibility(View.VISIBLE);
                        main.mainBinding.profileView.btnLiked.setVisibility(View.VISIBLE);
                        main.mainBinding.profileView.btnSaved.setText(R.string.saved);
                        main.mainBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_orange_background)));
                    }
                } else {
                    if (Utils.dataBox.getFavorite(main.userQuery) != null) {
                        main.mainBinding.profileView.btnFollow.setText(R.string.unfavorite_short);
                        main.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_purple_background)));
                    }
                    else {
                        main.mainBinding.profileView.btnFollow.setText(R.string.favorite_short);
                        main.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_pink_background)));
                    }
                    main.mainBinding.profileView.btnFollow.setVisibility(View.VISIBLE);
                    if (!profileModel.isReallyPrivate()) {
                        main.mainBinding.profileView.btnRestrict.setVisibility(View.VISIBLE);
                        main.mainBinding.profileView.btnRestrict.setText(R.string.tagged);
                        main.mainBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                main, R.color.btn_blue_background)));
                    }
                }

                main.mainBinding.profileView.mainProfileImage.setEnabled(false);
                new MyTask().execute();
                main.mainBinding.profileView.mainProfileImage.setEnabled(true);

                final long followersCount = profileModel.getFollowersCount();
                final long followingCount = profileModel.getFollowingCount();

                final String postCount = String.valueOf(profileModel.getPostCount());

                SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
                main.mainBinding.profileView.mainPostCount.setText(span);

                final String followersCountStr = String.valueOf(followersCount);
                final int followersCountStrLen = followersCountStr.length();
                span = new SpannableStringBuilder(resources.getString(R.string.main_posts_followers, followersCountStr));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, followersCountStrLen, 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, followersCountStrLen, 0);
                main.mainBinding.profileView.mainFollowers.setText(span);

                final String followingCountStr = String.valueOf(followingCount);
                final int followingCountStrLen = followingCountStr.length();
                span = new SpannableStringBuilder(resources.getString(R.string.main_posts_following, followingCountStr));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, followingCountStrLen, 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, followingCountStrLen, 0);
                main.mainBinding.profileView.mainFollowing.setText(span);

                main.mainBinding.profileView.mainFullName.setText(Utils.isEmpty(profileModel.getName()) ? profileModel.getUsername() : profileModel.getName());

                CharSequence biography = profileModel.getBiography();
                main.mainBinding.profileView.mainBiography.setCaptionIsExpandable(true);
                main.mainBinding.profileView.mainBiography.setCaptionIsExpanded(true);
                if (Utils.hasMentions(biography)) {
                    biography = Utils.getMentionText(biography);
                    main.mainBinding.profileView.mainBiography.setText(biography, TextView.BufferType.SPANNABLE);
                    main.mainBinding.profileView.mainBiography.setMentionClickListener(mentionClickListener);
                } else {
                    main.mainBinding.profileView.mainBiography.setText(biography);
                    main.mainBinding.profileView.mainBiography.setMentionClickListener(null);
                }

                final String url = profileModel.getUrl();
                if (Utils.isEmpty(url)) {
                    main.mainBinding.profileView.mainUrl.setVisibility(View.GONE);
                } else {
                    main.mainBinding.profileView.mainUrl.setVisibility(View.VISIBLE);
                    main.mainBinding.profileView.mainUrl.setText(Utils.getSpannableUrl(url));
                }

                main.mainBinding.profileView.mainFullName.setSelected(true);
                main.mainBinding.profileView.mainBiography.setEnabled(true);

                if (!profileModel.isReallyPrivate()) {
                    main.mainBinding.profileView.mainFollowing.setClickable(true);
                    main.mainBinding.profileView.mainFollowers.setClickable(true);

                    if (isLoggedIn) {
                        final View.OnClickListener followClickListener = v -> main.startActivity(new Intent(main, FollowViewer.class)
                                .putExtra(Constants.EXTRAS_FOLLOWERS, v == main.mainBinding.profileView.mainFollowers)
                                .putExtra(Constants.EXTRAS_NAME, profileModel.getUsername())
                                .putExtra(Constants.EXTRAS_ID, profileId));

                        main.mainBinding.profileView.mainFollowers.setOnClickListener(followersCount > 0 ? followClickListener : null);
                        main.mainBinding.profileView.mainFollowing.setOnClickListener(followingCount > 0 ? followClickListener : null);
                    }

                    if (profileModel.getPostCount() == 0) {
                        main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                        main.mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
                        main.mainBinding.profileView.privatePage2.setText(R.string.empty_acc);
                        main.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
                    }
                    else {
                        main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(true);
                        main.mainBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
                        currentlyExecuting = new PostsFetcher(profileId, postsFetchListener).setUsername(profileModel.getUsername())
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else {
                    main.mainBinding.profileView.mainFollowers.setClickable(false);
                    main.mainBinding.profileView.mainFollowing.setClickable(false);
                    main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    // error
                    main.mainBinding.profileView.privatePage1.setImageResource(R.drawable.lock);
                    main.mainBinding.profileView.privatePage2.setText(R.string.priv_acc);
                    main.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
                    main.mainBinding.profileView.mainPosts.setVisibility(View.GONE);
                }
            }
            ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else if (isLocation) {
            main.profileModel = null;
            main.hashtagModel = null;
            main.mainBinding.toolbar.toolbar.setTitle(main.userQuery);
            main.mainBinding.profileView.locInfoContainer.setVisibility(View.VISIBLE);

            currentlyExecuting = new LocationFetcher(main.userQuery.split("/")[0], locationModel -> {
                main.locationModel = locationModel;

                if (locationModel == null) {
                    main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(main, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                    main.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
                    return;
                }
                main.mainBinding.toolbar.toolbar.setTitle(locationModel.getName());

                final String profileId = locationModel.getId();

                if (isLoggedIn) {
                    new iStoryStatusFetcher(profileId.split("/")[0], null, true, false, false, false, result -> {
                        main.storyModels = result;
                        if (result != null && result.length > 0) main.mainBinding.profileView.mainLocationImage.setStoriesBorder();
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                main.mainBinding.profileView.mainLocationImage.setEnabled(false);
                new MyTask().execute();
                main.mainBinding.profileView.mainLocationImage.setEnabled(true);

                final String postCount = String.valueOf(locationModel.getPostCount());

                SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
                main.mainBinding.profileView.mainLocPostCount.setText(span);

                main.mainBinding.profileView.locationFullName.setText(locationModel.getName());

                CharSequence biography = locationModel.getBio();
                main.mainBinding.profileView.locationBiography.setCaptionIsExpandable(true);
                main.mainBinding.profileView.locationBiography.setCaptionIsExpanded(true);

                if (Utils.isEmpty(biography)) {
                    main.mainBinding.profileView.locationBiography.setVisibility(View.GONE);
                }
                else if (Utils.hasMentions(biography)) {
                    main.mainBinding.profileView.locationBiography.setVisibility(View.VISIBLE);
                    biography = Utils.getMentionText(biography);
                    main.mainBinding.profileView.locationBiography.setText(biography, TextView.BufferType.SPANNABLE);
                    main.mainBinding.profileView.locationBiography.setMentionClickListener(mentionClickListener);
                } else {
                    main.mainBinding.profileView.locationBiography.setVisibility(View.VISIBLE);
                    main.mainBinding.profileView.locationBiography.setText(biography);
                    main.mainBinding.profileView.locationBiography.setMentionClickListener(null);
                }

                if (!locationModel.getGeo().startsWith("geo:0.0,0.0?z=17")) {
                    main.mainBinding.profileView.btnMap.setVisibility(View.VISIBLE);
                    main.mainBinding.profileView.btnMap.setOnClickListener(v -> {
                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(locationModel.getGeo()));
                        main.startActivity(intent);
                    });
                }
                else {
                    main.mainBinding.profileView.btnMap.setVisibility(View.GONE);
                    main.mainBinding.profileView.btnMap.setOnClickListener(null);
                }

                final String url = locationModel.getUrl();
                if (Utils.isEmpty(url)) {
                    main.mainBinding.profileView.locationUrl.setVisibility(View.GONE);
                } else if (!url.startsWith("http")) {
                    main.mainBinding.profileView.locationUrl.setVisibility(View.VISIBLE);
                    main.mainBinding.profileView.locationUrl.setText(Utils.getSpannableUrl("http://"+url));
                } else {
                    main.mainBinding.profileView.locationUrl.setVisibility(View.VISIBLE);
                    main.mainBinding.profileView.locationUrl.setText(Utils.getSpannableUrl(url));
                }

                main.mainBinding.profileView.locationFullName.setSelected(true);
                main.mainBinding.profileView.locationBiography.setEnabled(true);

                if (locationModel.getPostCount() == 0) {
                    main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    main.mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
                    main.mainBinding.profileView.privatePage2.setText(R.string.empty_acc);
                    main.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
                }
                else {
                    main.mainBinding.profileView.swipeRefreshLayout.setRefreshing(true);
                    main.mainBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
                    currentlyExecuting = new PostsFetcher(profileId, postsFetchListener)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
            ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static void stopCurrentExecutor() {
        if (currentlyExecuting != null) {
            try {
                currentlyExecuting.cancel(true);
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
    }

    private void toggleSelection(final PostModel postModel) {
        if (postModel != null && postsAdapter != null) {
            if (postModel.isSelected()) main.selectedItems.remove(postModel);
            else main.selectedItems.add(postModel);
            postModel.setSelected(!postModel.isSelected());
            notifyAdapter(postModel);
        }
    }

    private void notifyAdapter(final PostModel postModel) {
        if (main.selectedItems.size() < 1) postsAdapter.isSelecting = false;
        if (postModel.getPosition() < 0) postsAdapter.notifyDataSetChanged();
        else postsAdapter.notifyItemChanged(postModel.getPosition(), postModel);

        if (main.downloadAction != null) main.downloadAction.setVisible(postsAdapter.isSelecting);
    }

    ///////////////////////////////////////////////////
    private void toggleDiscoverSelection(final DiscoverItemModel itemModel) {
        if (itemModel != null && discoverAdapter != null) {
            if (itemModel.isSelected()) main.selectedDiscoverItems.remove(itemModel);
            else main.selectedDiscoverItems.add(itemModel);
            itemModel.setSelected(!itemModel.isSelected());
            notifyDiscoverAdapter(itemModel);
        }
    }

    private void notifyDiscoverAdapter(final DiscoverItemModel itemModel) {
        if (main.selectedDiscoverItems.size() < 1) discoverAdapter.isSelecting = false;
        if (itemModel.getPosition() < 0) discoverAdapter.notifyDataSetChanged();
        else discoverAdapter.notifyItemChanged(itemModel.getPosition(), itemModel);

        if (main.downloadAction != null) main.downloadAction.setVisible(discoverAdapter.isSelecting);
    }

    public boolean isSelectionCleared() {
        if (postsAdapter != null && postsAdapter.isSelecting) {
            for (final PostModel postModel : main.selectedItems) postModel.setSelected(false);
            main.selectedItems.clear();
            postsAdapter.isSelecting = false;
            postsAdapter.notifyDataSetChanged();
            if (main.downloadAction != null) main.downloadAction.setVisible(false);
            return false;
        } else if (discoverAdapter != null && discoverAdapter.isSelecting) {
            for (final DiscoverItemModel itemModel : main.selectedDiscoverItems) itemModel.setSelected(false);
            main.selectedDiscoverItems.clear();
            discoverAdapter.isSelecting = false;
            discoverAdapter.notifyDataSetChanged();
            if (main.downloadAction != null) main.downloadAction.setVisible(false);
            return false;
        }
        return true;
    }

    public void deselectSelection(final BasePostModel postModel) {
        if (postModel instanceof PostModel) {
            main.selectedItems.remove(postModel);
            postModel.setSelected(false);
            if (postsAdapter != null) notifyAdapter((PostModel) postModel);
        } else if (postModel instanceof DiscoverItemModel) {
            main.selectedDiscoverItems.remove(postModel);
            postModel.setSelected(false);
            if (discoverAdapter != null) notifyDiscoverAdapter((DiscoverItemModel) postModel);
        }
    }

    public void onPause() {
        if (currentFeedPlayer != null) {
            currentFeedPlayer.setPlayWhenReady(false);
            currentFeedPlayer.getPlaybackState();
        }
    }

    public void onResume() {
        if (currentFeedPlayer != null) {
            currentFeedPlayer.setPlayWhenReady(true);
            currentFeedPlayer.getPlaybackState();
        }
    }

    public static int indexOfIntArray(Object[] array, Object key) {
        int returnvalue = -1;
        for (int i = 0; i < array.length; ++i) {
            if (key == array[i]) {
                returnvalue = i;
                break;
            }
        }
        return returnvalue;
    }

    class MyTask extends AsyncTask<Void, Bitmap, Void> {
        private Bitmap mIcon_val;

        protected Void doInBackground(Void... voids) {
            try {
                mIcon_val = BitmapFactory.decodeStream((InputStream) new URL(
                        (main.hashtagModel != null) ? main.hashtagModel.getSdProfilePic() : (
                                (main.locationModel != null) ? main.locationModel.getSdProfilePic() :
                                        main.profileModel.getSdProfilePic())
                ).getContent());
            } catch (Throwable ex) {
                Log.e("austin_debug", "bitmap: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (main.hashtagModel != null) main.mainBinding.profileView.mainHashtagImage.setImageBitmap(mIcon_val);
            else if (main.locationModel != null) main.mainBinding.profileView.mainLocationImage.setImageBitmap(mIcon_val);
            else main.mainBinding.profileView.mainProfileImage.setImageBitmap(mIcon_val);
        }
    }

    private final View.OnClickListener profileActionListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final boolean iamme = (isLoggedIn && main.profileModel != null)
                    ? Utils.getUserIdFromCookie(cookie).equals(main.profileModel.getId())
                    : false;
            if (!isLoggedIn && Utils.dataBox.getFavorite(main.userQuery) != null && v == main.mainBinding.profileView.btnFollow) {
                Utils.dataBox.delFavorite(new DataBox.FavoriteModel(main.userQuery,
                        Long.parseLong(Utils.dataBox.getFavorite(main.userQuery).split("/")[1]),
                        main.locationModel != null ? main.locationModel.getName() : main.userQuery.replaceAll("^@", "")));
                onRefresh();
            } else if (!isLoggedIn && (v == main.mainBinding.profileView.btnFollow || v == main.mainBinding.profileView.btnFollowTag)) {
                Utils.dataBox.addFavorite(new DataBox.FavoriteModel(main.userQuery, System.currentTimeMillis(),
                        main.locationModel != null ? main.locationModel.getName() : main.userQuery.replaceAll("^@", "")));
                onRefresh();
            } else if (v == main.mainBinding.profileView.btnFollow) {
                new ProfileAction().execute("follow");
            } else if (v == main.mainBinding.profileView.btnRestrict && isLoggedIn) {
                new ProfileAction().execute("restrict");
            } else if (v == main.mainBinding.profileView.btnSaved && !iamme) {
                new ProfileAction().execute("block");
            } else if (v == main.mainBinding.profileView.btnFollowTag) {
                new ProfileAction().execute("followtag");
            } else if (v == main.mainBinding.profileView.btnTagged || (v == main.mainBinding.profileView.btnRestrict && !isLoggedIn)) {
                main.startActivity(new Intent(main, SavedViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, "%"+main.profileModel.getId())
                        .putExtra(Constants.EXTRAS_USER, "@"+main.profileModel.getUsername())
                );
            } else if (v == main.mainBinding.profileView.btnSaved) {
                main.startActivity(new Intent(main, SavedViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, "$"+main.profileModel.getId())
                        .putExtra(Constants.EXTRAS_USER, "@"+main.profileModel.getUsername())
                );
            } else if (v == main.mainBinding.profileView.btnLiked) {
                main.startActivity(new Intent(main, SavedViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, "^"+main.profileModel.getId())
                        .putExtra(Constants.EXTRAS_USER, "@"+main.profileModel.getUsername())
                );
            }
        }
    };

    class ProfileAction extends AsyncTask<String, Void, Void> {
        boolean ok = false;
        String action;

        protected Void doInBackground(String... rawAction) {
            action = rawAction[0];
            final String url = "https://www.instagram.com/web/"+
                    ((action == "followtag" && main.hashtagModel != null) ? ("tags/"+
                            (main.hashtagModel.getFollowing() == true ? "unfollow/" : "follow/")+main.hashtagModel.getName()+"/") : (
                    ((action == "restrict" && main.profileModel != null) ? "restrict_action" : ("friendships/"+main.profileModel.getId()))+"/"+
                    ((action == "follow" && main.profileModel != null) ?
                    ((main.profileModel.getFollowing() == true ||
                            (main.profileModel.getFollowing() == false && main.profileModel.getRequested() == true))
                            ? "unfollow/" : "follow/") :
                    ((action == "restrict" && main.profileModel != null) ?
                            (main.profileModel.getRestricted() == true ? "unrestrict/" : "restrict/") :
                            (main.profileModel.getBlocked() == true ? "unblock/" : "block/")))));
            try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
                if (action == "restrict") {
                    final String urlParameters = "target_user_id="+main.profileModel.getId();
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Content-Length", "" +
                            Integer.toString(urlParameters.getBytes().length));
                    urlConnection.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();
                }
                else urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = true;
                }
                else Toast.makeText(main, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", action+": " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok == true) {
                onRefresh();
            }
        }
    }
}