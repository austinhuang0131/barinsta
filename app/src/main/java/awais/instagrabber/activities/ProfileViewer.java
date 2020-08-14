package awais.instagrabber.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.HighlightsAdapter;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.HashtagFetcher;
import awais.instagrabber.asyncs.HighlightsFetcher;
import awais.instagrabber.asyncs.LocationFetcher;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
import awais.instagrabber.customviews.MouseDrawer;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.customviews.helpers.VideoAwareRecyclerScroller;
import awais.instagrabber.databinding.ActivityProfileBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.HashtagModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
import static awais.instagrabber.utils.Constants.BOTTOM_TOOLBAR;
import static awais.instagrabber.utils.Utils.logCollector;

public final class ProfileViewer extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private final ArrayList<PostModel> allItems = new ArrayList<>(), selectedItems = new ArrayList<>();
    private static AsyncTask<?, ?, ?> currentlyExecuting;
    private final boolean autoloadPosts = Utils.settingsHelper.getBoolean(AUTOLOAD_POSTS);
    private boolean hasNextPage = false;
    private View collapsingToolbar;
    private String endCursor = null;
    private ProfileModel profileModel;
    private HashtagModel hashtagModel;
    private LocationModel locationModel;
    private StoryModel[] storyModels;
    private MenuItem downloadAction;
    private final FetchListener<PostModel[]> postsFetchListener = new FetchListener<PostModel[]>() {
        @Override
        public void onResult(final PostModel[] result) {
            if (result != null) {
                final int oldSize = allItems.size();
                allItems.addAll(Arrays.asList(result));

                postsAdapter.notifyItemRangeInserted(oldSize, result.length);

                profileBinding.profileView.mainPosts.post(() -> {
                    profileBinding.profileView.mainPosts.setNestedScrollingEnabled(true);
                    profileBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
                });

                if (isHashtag)
                    profileBinding.toolbar.toolbar.setTitle(userQuery);
                else if (isLocation)
                    profileBinding.toolbar.toolbar.setTitle(locationModel.getName());
                else profileBinding.toolbar.toolbar.setTitle("@"+profileModel.getUsername());

                final PostModel model = result[result.length - 1];
                if (model != null) {
                    endCursor = model.getEndCursor();
                    hasNextPage = model.hasNextPage();
                    if (autoloadPosts && hasNextPage)
                        currentlyExecuting = new PostsFetcher(
                                profileModel != null ? profileModel.getId()
                                    : (hashtagModel != null ? ("#"+hashtagModel.getName()) : locationModel.getId()), endCursor, this)
                                .setUsername((isLocation || isHashtag) ? null : profileModel.getUsername())
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else {
                        profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    }
                    model.setPageCursor(false, null);
                }
            }
            else {
                profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                profileBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
                profileBinding.profileView.privatePage2.setText(R.string.empty_acc);
                profileBinding.profileView.privatePage.setVisibility(View.VISIBLE);
            }
        }
    };
    private final MentionClickListener mentionClickListener = new MentionClickListener() {
        @Override
        public void onClick(final RamboTextView view, final String text, final boolean isHashtag) {
            startActivity(new Intent(getApplicationContext(), ProfileViewer.class).putExtra(Constants.EXTRAS_USERNAME, text));
        }
    };
    public final HighlightsAdapter highlightsAdapter = new HighlightsAdapter(null, new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Object tag = v.getTag();
            if (tag instanceof HighlightModel) {
                final HighlightModel highlightModel = (HighlightModel) tag;
                new iStoryStatusFetcher(highlightModel.getId(), null, false, false,
                        (!isLoggedIn && Utils.settingsHelper.getBoolean(Constants.STORIESIG)), true, result -> {
                    if (result != null && result.length > 0)
                        startActivity(new Intent(ProfileViewer.this, StoryViewer.class)
                                //.putExtra(Constants.EXTRAS_USERNAME, userQuery.replace("@", ""))
                                .putExtra(Constants.EXTRAS_HIGHLIGHT, highlightModel.getTitle())
                                .putExtra(Constants.EXTRAS_STORIES, result)
                        );
                    else Toast.makeText(ProfileViewer.this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    });
    private Resources resources;
    private RecyclerLazyLoader lazyLoader;
    private boolean isHashtag, isUser, isLocation;
    private PostsAdapter postsAdapter;
    private String cookie = Utils.settingsHelper.getString(Constants.COOKIE), userQuery;
    public boolean isLoggedIn = !Utils.isEmpty(cookie);
    private ActivityProfileBinding profileBinding;

    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        stopCurrentExecutor();
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_USERNAME)
                || Utils.isEmpty((userQuery = intent.getStringExtra(Constants.EXTRAS_USERNAME)))) {
            Utils.errorFinish(this);
            return;
        }

        userQuery = (userQuery.contains("/") || userQuery.startsWith("#") || userQuery.startsWith("@")) ? userQuery : ("@"+userQuery);

        profileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(profileBinding.getRoot());

        resources = getResources();

        profileBinding.profileView.swipeRefreshLayout.setOnRefreshListener(this);
        profileBinding.profileView.mainUrl.setMovementMethod(new LinkMovementMethod());

        isLoggedIn = !Utils.isEmpty(cookie);

        collapsingToolbar = profileBinding.profileView.appBarLayout.getChildAt(0);

        profileBinding.profileView.mainPosts.setNestedScrollingEnabled(false);
        profileBinding.profileView.highlightsList.setLayoutManager(
                new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        profileBinding.profileView.highlightsList.setAdapter(highlightsAdapter);

        setSupportActionBar(profileBinding.toolbar.toolbar);

        // change the next number to adjust grid
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(ProfileViewer.this, Utils.convertDpToPx(110));
        profileBinding.profileView.mainPosts.setLayoutManager(layoutManager);
        profileBinding.profileView.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        profileBinding.profileView.mainPosts.setAdapter(postsAdapter = new PostsAdapter(allItems, v -> {
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                final PostModel postModel = (PostModel) tag;

                if (postsAdapter.isSelecting) toggleSelection(postModel);
                else startActivity(new Intent(ProfileViewer.this, PostViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, postModel.getPosition())
                        .putExtra(Constants.EXTRAS_POST, postModel)
                        .putExtra(Constants.EXTRAS_USER, userQuery)
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
                profileBinding.profileView.swipeRefreshLayout.setRefreshing(true);
                stopCurrentExecutor();
                currentlyExecuting = new PostsFetcher(profileModel != null ? profileModel.getId()
                        : (hashtagModel != null ? ("#"+hashtagModel.getName()) : locationModel.getId()), endCursor, postsFetchListener)
                        .setUsername((isHashtag || isLocation) ? null : profileModel.getUsername())
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                endCursor = null;
            }
        });
        profileBinding.profileView.mainPosts.addOnScrollListener(lazyLoader);

        this.onRefresh();
    }

    @Override
    public void onRefresh() {
        if (lazyLoader != null) lazyLoader.resetState();
        stopCurrentExecutor();
        allItems.clear();
        selectedItems.clear();
        if (postsAdapter != null) {
            postsAdapter.isSelecting = false;
            postsAdapter.notifyDataSetChanged();
        }
        profileBinding.profileView.appBarLayout.setExpanded(true, true);
        profileBinding.profileView.privatePage.setVisibility(View.GONE);
        profileBinding.profileView.privatePage2.setTextSize(28);
        profileBinding.profileView.mainProfileImage.setImageBitmap(null);
        profileBinding.profileView.mainHashtagImage.setImageBitmap(null);
        profileBinding.profileView.mainLocationImage.setImageBitmap(null);
        profileBinding.profileView.mainUrl.setText(null);
        profileBinding.profileView.locationUrl.setText(null);
        profileBinding.profileView.mainFullName.setText(null);
        profileBinding.profileView.locationFullName.setText(null);
        profileBinding.profileView.mainPostCount.setText(null);
        profileBinding.profileView.mainLocPostCount.setText(null);
        profileBinding.profileView.mainTagPostCount.setText(null);
        profileBinding.profileView.mainFollowers.setText(null);
        profileBinding.profileView.mainFollowing.setText(null);
        profileBinding.profileView.mainBiography.setText(null);
        profileBinding.profileView.locationBiography.setText(null);
        profileBinding.profileView.mainBiography.setEnabled(false);
        profileBinding.profileView.locationBiography.setEnabled(false);
        profileBinding.profileView.mainProfileImage.setEnabled(false);
        profileBinding.profileView.mainLocationImage.setEnabled(false);
        profileBinding.profileView.mainHashtagImage.setEnabled(false);
        profileBinding.profileView.mainBiography.setMentionClickListener(null);
        profileBinding.profileView.locationBiography.setMentionClickListener(null);
        profileBinding.profileView.mainUrl.setVisibility(View.GONE);
        profileBinding.profileView.locationUrl.setVisibility(View.GONE);
        profileBinding.profileView.isVerified.setVisibility(View.GONE);
        profileBinding.profileView.btnFollow.setVisibility(View.GONE);
        profileBinding.profileView.btnRestrict.setVisibility(View.GONE);
        profileBinding.profileView.btnBlock.setVisibility(View.GONE);
        profileBinding.profileView.btnSaved.setVisibility(View.GONE);
        profileBinding.profileView.btnLiked.setVisibility(View.GONE);
        profileBinding.profileView.btnTagged.setVisibility(View.GONE);
        profileBinding.profileView.btnMap.setVisibility(View.GONE);

        profileBinding.profileView.btnFollow.setOnClickListener(profileActionListener);
        profileBinding.profileView.btnRestrict.setOnClickListener(profileActionListener);
        profileBinding.profileView.btnBlock.setOnClickListener(profileActionListener);
        profileBinding.profileView.btnSaved.setOnClickListener(profileActionListener);
        profileBinding.profileView.btnLiked.setOnClickListener(profileActionListener);
        profileBinding.profileView.btnTagged.setOnClickListener(profileActionListener);
        profileBinding.profileView.btnFollowTag.setOnClickListener(profileActionListener);

        profileBinding.profileView.infoContainer.setVisibility(View.GONE);
        profileBinding.profileView.tagInfoContainer.setVisibility(View.GONE);
        profileBinding.profileView.locInfoContainer.setVisibility(View.GONE);

        profileBinding.profileView.mainPosts.setNestedScrollingEnabled(false);
        profileBinding.profileView.highlightsList.setVisibility(View.GONE);
        collapsingToolbar.setVisibility(View.GONE);
        highlightsAdapter.setData(null);

        profileBinding.profileView.swipeRefreshLayout.setRefreshing(userQuery != null);
        if (userQuery == null) {
            profileBinding.toolbar.toolbar.setTitle(R.string.app_name);
            return;
        }

        isHashtag = userQuery.charAt(0) == '#';
        isUser = userQuery.charAt(0) == '@';
        isLocation = userQuery.contains("/");
        collapsingToolbar.setVisibility(isUser ? View.VISIBLE : View.GONE);

        if (isHashtag) {
            profileModel = null;
            locationModel = null;
            profileBinding.toolbar.toolbar.setTitle(userQuery);
            profileBinding.profileView.tagInfoContainer.setVisibility(View.VISIBLE);
            profileBinding.profileView.btnFollowTag.setVisibility(View.GONE);

            currentlyExecuting = new HashtagFetcher(userQuery.substring(1), result -> {
                hashtagModel = result;

                if (hashtagModel == null) {
                    profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ProfileViewer.this, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                    profileBinding.toolbar.toolbar.setTitle(R.string.app_name);
                    return;
                }

                currentlyExecuting = new PostsFetcher(userQuery, postsFetchListener)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                profileBinding.profileView.btnFollowTag.setVisibility(View.VISIBLE);

                if (isLoggedIn) {
                    new iStoryStatusFetcher(hashtagModel.getName(), null, false, true, false, false, stories -> {
                        storyModels = stories;
                        if (stories != null && stories.length > 0) profileBinding.profileView.mainHashtagImage.setStoriesBorder();
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    if (hashtagModel.getFollowing() == true) {
                        profileBinding.profileView.btnFollowTag.setText(R.string.unfollow);
                        profileBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_purple_background)));
                    }
                    else {
                        profileBinding.profileView.btnFollowTag.setText(R.string.follow);
                        profileBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_pink_background)));
                    }
                } else {
                    if (Utils.dataBox.getFavorite(userQuery) != null) {
                        profileBinding.profileView.btnFollowTag.setText(R.string.unfavorite_short);
                        profileBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_purple_background)));
                    }
                    else {
                        profileBinding.profileView.btnFollowTag.setText(R.string.favorite_short);
                        profileBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_pink_background)));
                    }
                }

                profileBinding.profileView.mainHashtagImage.setEnabled(false);
                new MyTask().execute();
                profileBinding.profileView.mainHashtagImage.setEnabled(true);

                final String postCount = String.valueOf(hashtagModel.getPostCount());

                SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
                profileBinding.profileView.mainTagPostCount.setText(span);
                profileBinding.profileView.mainTagPostCount.setVisibility(View.VISIBLE);
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (isUser) {
            hashtagModel = null;
            locationModel = null;
            profileBinding.toolbar.toolbar.setTitle(userQuery);
            profileBinding.profileView.infoContainer.setVisibility(View.VISIBLE);
            profileBinding.profileView.btnFollowTag.setVisibility(View.GONE);

            currentlyExecuting = new ProfileFetcher(userQuery.substring(1), result -> {
                profileModel = result;

                if (profileModel == null) {
                    profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ProfileViewer.this, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                    profileBinding.toolbar.toolbar.setTitle(R.string.app_name);
                    return;
                }

                profileBinding.profileView.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
                final String profileId = profileModel.getId();

                if (isLoggedIn || Utils.settingsHelper.getBoolean(Constants.STORIESIG)) {
                  new iStoryStatusFetcher(profileId, profileModel.getUsername(), false, false,
                          (!isLoggedIn && Utils.settingsHelper.getBoolean(Constants.STORIESIG)), false,
                          stories -> {
                      storyModels = stories;
                      if (stories != null && stories.length > 0) profileBinding.profileView.mainProfileImage.setStoriesBorder();
                  }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                  new HighlightsFetcher(profileId, (!isLoggedIn && Utils.settingsHelper.getBoolean(Constants.STORIESIG)), hls -> {
                      if (hls != null && hls.length > 0) {
                          profileBinding.profileView.highlightsList.setVisibility(View.VISIBLE);
                          highlightsAdapter.setData(hls);
                      }
                      else profileBinding.profileView.highlightsList.setVisibility(View.GONE);
                  }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                if (isLoggedIn) {
                    final String myId = Utils.getUserIdFromCookie(cookie);
                    if (!profileId.equals(myId)) {
                        profileBinding.profileView.btnTagged.setVisibility(View.GONE);
                        profileBinding.profileView.btnSaved.setVisibility(View.GONE);
                        profileBinding.profileView.btnLiked.setVisibility(View.GONE);
                        profileBinding.profileView.btnFollow.setVisibility(View.VISIBLE);
                        if (profileModel.getFollowing() == true) {
                            profileBinding.profileView.btnFollow.setText(R.string.unfollow);
                            profileBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    ProfileViewer.this, R.color.btn_purple_background)));
                        }
                        else if (profileModel.getRequested() == true) {
                            profileBinding.profileView.btnFollow.setText(R.string.cancel);
                            profileBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    ProfileViewer.this, R.color.btn_purple_background)));
                        }
                        else {
                            profileBinding.profileView.btnFollow.setText(R.string.follow);
                            profileBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    ProfileViewer.this, R.color.btn_pink_background)));
                        }
                        profileBinding.profileView.btnRestrict.setVisibility(View.VISIBLE);
                        if (profileModel.getRestricted() == true) {
                            profileBinding.profileView.btnRestrict.setText(R.string.unrestrict);
                            profileBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    ProfileViewer.this, R.color.btn_green_background)));
                        }
                        else {
                            profileBinding.profileView.btnRestrict.setText(R.string.restrict);
                            profileBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                    ProfileViewer.this, R.color.btn_orange_background)));
                        }
                        if (profileModel.isReallyPrivate()) {
                            profileBinding.profileView.btnBlock.setVisibility(View.VISIBLE);
                            profileBinding.profileView.btnTagged.setVisibility(View.GONE);
                            if (profileModel.getBlocked() == true) {
                                profileBinding.profileView.btnBlock.setText(R.string.unblock);
                                profileBinding.profileView.btnBlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        ProfileViewer.this, R.color.btn_green_background)));
                            } else {
                                profileBinding.profileView.btnBlock.setText(R.string.block);
                                profileBinding.profileView.btnBlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        ProfileViewer.this, R.color.btn_red_background)));
                            }
                        } else {
                            profileBinding.profileView.btnBlock.setVisibility(View.GONE);
                            profileBinding.profileView.btnSaved.setVisibility(View.VISIBLE);
                            profileBinding.profileView.btnTagged.setVisibility(View.VISIBLE);
                            if (profileModel.getBlocked() == true) {
                                profileBinding.profileView.btnSaved.setText(R.string.unblock);
                                profileBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        ProfileViewer.this, R.color.btn_green_background)));
                            } else {
                                profileBinding.profileView.btnSaved.setText(R.string.block);
                                profileBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                        ProfileViewer.this, R.color.btn_red_background)));
                            }
                        }
                    }
                    else {
                        profileBinding.profileView.btnTagged.setVisibility(View.VISIBLE);
                        profileBinding.profileView.btnSaved.setVisibility(View.VISIBLE);
                        profileBinding.profileView.btnLiked.setVisibility(View.VISIBLE);
                        profileBinding.profileView.btnSaved.setText(R.string.saved);
                        profileBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_orange_background)));
                    }
                } else {
                    if (Utils.dataBox.getFavorite(userQuery) != null) {
                        profileBinding.profileView.btnFollow.setText(R.string.unfavorite_short);
                        profileBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_purple_background)));
                    }
                    else {
                        profileBinding.profileView.btnFollow.setText(R.string.favorite_short);
                        profileBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_pink_background)));
                    }
                    profileBinding.profileView.btnFollow.setVisibility(View.VISIBLE);
                    if (!profileModel.isReallyPrivate()) {
                        profileBinding.profileView.btnRestrict.setVisibility(View.VISIBLE);
                        profileBinding.profileView.btnRestrict.setText(R.string.tagged);
                        profileBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                                ProfileViewer.this, R.color.btn_blue_background)));
                    }
                }

                profileBinding.profileView.mainProfileImage.setEnabled(false);
                new MyTask().execute();
                profileBinding.profileView.mainProfileImage.setEnabled(true);

                final long followersCount = profileModel.getFollowersCount();
                final long followingCount = profileModel.getFollowingCount();

                final String postCount = String.valueOf(profileModel.getPostCount());

                SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
                profileBinding.profileView.mainPostCount.setText(span);

                final String followersCountStr = String.valueOf(followersCount);
                final int followersCountStrLen = followersCountStr.length();
                span = new SpannableStringBuilder(resources.getString(R.string.main_posts_followers, followersCountStr));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, followersCountStrLen, 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, followersCountStrLen, 0);
                profileBinding.profileView.mainFollowers.setText(span);

                final String followingCountStr = String.valueOf(followingCount);
                final int followingCountStrLen = followingCountStr.length();
                span = new SpannableStringBuilder(resources.getString(R.string.main_posts_following, followingCountStr));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, followingCountStrLen, 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, followingCountStrLen, 0);
                profileBinding.profileView.mainFollowing.setText(span);

                profileBinding.profileView.mainFullName.setText(Utils.isEmpty(profileModel.getName()) ? profileModel.getUsername() : profileModel.getName());

                CharSequence biography = profileModel.getBiography();
                profileBinding.profileView.mainBiography.setCaptionIsExpandable(true);
                profileBinding.profileView.mainBiography.setCaptionIsExpanded(true);
                if (Utils.hasMentions(biography)) {
                    biography = Utils.getMentionText(biography);
                    profileBinding.profileView.mainBiography.setText(biography, TextView.BufferType.SPANNABLE);
                    profileBinding.profileView.mainBiography.setMentionClickListener(mentionClickListener);
                } else {
                    profileBinding.profileView.mainBiography.setText(biography);
                    profileBinding.profileView.mainBiography.setMentionClickListener(null);
                }

                final String url = profileModel.getUrl();
                if (Utils.isEmpty(url)) {
                    profileBinding.profileView.mainUrl.setVisibility(View.GONE);
                } else {
                    profileBinding.profileView.mainUrl.setVisibility(View.VISIBLE);
                    profileBinding.profileView.mainUrl.setText(Utils.getSpannableUrl(url));
                }

                profileBinding.profileView.mainFullName.setSelected(true);
                profileBinding.profileView.mainBiography.setEnabled(true);

                if (!profileModel.isReallyPrivate()) {
                    profileBinding.profileView.mainFollowing.setClickable(true);
                    profileBinding.profileView.mainFollowers.setClickable(true);

                    if (isLoggedIn) {
                        final View.OnClickListener followClickListener = v -> startActivity(new Intent(ProfileViewer.this, FollowViewer.class)
                                .putExtra(Constants.EXTRAS_FOLLOWERS, v == profileBinding.profileView.mainFollowers)
                                .putExtra(Constants.EXTRAS_NAME, profileModel.getUsername())
                                .putExtra(Constants.EXTRAS_ID, profileId));

                        profileBinding.profileView.mainFollowers.setOnClickListener(followersCount > 0 ? followClickListener : null);
                        profileBinding.profileView.mainFollowing.setOnClickListener(followingCount > 0 ? followClickListener : null);
                    }

                    if (profileModel.getPostCount() == 0) {
                        profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                        profileBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
                        profileBinding.profileView.privatePage2.setText(R.string.empty_acc);
                        profileBinding.profileView.privatePage.setVisibility(View.VISIBLE);
                    }
                    else {
                        profileBinding.profileView.swipeRefreshLayout.setRefreshing(true);
                        profileBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
                        currentlyExecuting = new PostsFetcher(profileId, postsFetchListener).setUsername(profileModel.getUsername())
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else {
                    profileBinding.profileView.mainFollowers.setClickable(false);
                    profileBinding.profileView.mainFollowing.setClickable(false);
                    profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    // error
                    profileBinding.profileView.privatePage1.setImageResource(R.drawable.lock);
                    profileBinding.profileView.privatePage2.setText(R.string.priv_acc);
                    profileBinding.profileView.privatePage.setVisibility(View.VISIBLE);
                    profileBinding.profileView.mainPosts.setVisibility(View.GONE);
                }
            }
            ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else if (isLocation) {
            profileModel = null;
            hashtagModel = null;
            profileBinding.toolbar.toolbar.setTitle(userQuery);
            profileBinding.profileView.locInfoContainer.setVisibility(View.VISIBLE);

            currentlyExecuting = new LocationFetcher(userQuery.split("/")[0], result -> {
                locationModel = result;

                if (locationModel == null) {
                    profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ProfileViewer.this, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                    profileBinding.toolbar.toolbar.setTitle(R.string.app_name);
                    return;
                }
                profileBinding.toolbar.toolbar.setTitle(locationModel.getName());

                final String profileId = locationModel.getId();

                if (isLoggedIn) {
                    new iStoryStatusFetcher(profileId.split("/")[0], null, true, false, false, false, stories -> {
                        storyModels = stories;
                        if (stories != null && stories.length > 0) profileBinding.profileView.mainLocationImage.setStoriesBorder();
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                profileBinding.profileView.mainLocationImage.setEnabled(false);
                new MyTask().execute();
                profileBinding.profileView.mainLocationImage.setEnabled(true);

                final String postCount = String.valueOf(locationModel.getPostCount());

                SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
                span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
                profileBinding.profileView.mainLocPostCount.setText(span);

                profileBinding.profileView.locationFullName.setText(locationModel.getName());

                CharSequence biography = locationModel.getBio();
                profileBinding.profileView.locationBiography.setCaptionIsExpandable(true);
                profileBinding.profileView.locationBiography.setCaptionIsExpanded(true);

                if (Utils.isEmpty(biography)) {
                    profileBinding.profileView.locationBiography.setVisibility(View.GONE);
                }
                else if (Utils.hasMentions(biography)) {
                    profileBinding.profileView.locationBiography.setVisibility(View.VISIBLE);
                    biography = Utils.getMentionText(biography);
                    profileBinding.profileView.locationBiography.setText(biography, TextView.BufferType.SPANNABLE);
                    profileBinding.profileView.locationBiography.setMentionClickListener(mentionClickListener);
                } else {
                    profileBinding.profileView.locationBiography.setVisibility(View.VISIBLE);
                    profileBinding.profileView.locationBiography.setText(biography);
                    profileBinding.profileView.locationBiography.setMentionClickListener(null);
                }

                if (!locationModel.getGeo().startsWith("geo:0.0,0.0?z=17")) {
                    profileBinding.profileView.btnMap.setVisibility(View.VISIBLE);
                    profileBinding.profileView.btnMap.setOnClickListener(v -> {
                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(locationModel.getGeo()));
                        startActivity(intent);
                    });
                }
                else {
                    profileBinding.profileView.btnMap.setVisibility(View.GONE);
                    profileBinding.profileView.btnMap.setOnClickListener(null);
                }

                final String url = locationModel.getUrl();
                if (Utils.isEmpty(url)) {
                    profileBinding.profileView.locationUrl.setVisibility(View.GONE);
                } else if (!url.startsWith("http")) {
                    profileBinding.profileView.locationUrl.setVisibility(View.VISIBLE);
                    profileBinding.profileView.locationUrl.setText(Utils.getSpannableUrl("http://"+url));
                } else {
                    profileBinding.profileView.locationUrl.setVisibility(View.VISIBLE);
                    profileBinding.profileView.locationUrl.setText(Utils.getSpannableUrl(url));
                }

                profileBinding.profileView.locationFullName.setSelected(true);
                profileBinding.profileView.locationBiography.setEnabled(true);

                if (locationModel.getPostCount() == 0) {
                    profileBinding.profileView.swipeRefreshLayout.setRefreshing(false);
                    profileBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
                    profileBinding.profileView.privatePage2.setText(R.string.empty_acc);
                    profileBinding.profileView.privatePage.setVisibility(View.VISIBLE);
                }
                else {
                    profileBinding.profileView.swipeRefreshLayout.setRefreshing(true);
                    profileBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
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

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.saved, menu);

        downloadAction = menu.findItem(R.id.downloadAction);
        downloadAction.setVisible(false);

        downloadAction.setOnMenuItemClickListener(item -> {
            if (selectedItems.size() > 0) {
                Utils.batchDownload(this, userQuery, DownloadMethod.DOWNLOAD_MAIN, selectedItems);
            }
            return true;
        });
        return true;
    }

    private void toggleSelection(final PostModel postModel) {
        if (postModel != null && postsAdapter != null) {
            if (postModel.isSelected()) selectedItems.remove(postModel);
            else selectedItems.add(postModel);
            postModel.setSelected(!postModel.isSelected());
            notifyAdapter(postModel);
        }
    }

    private void notifyAdapter(final PostModel postModel) {
        if (selectedItems.size() < 1) postsAdapter.isSelecting = false;
        if (postModel.getPosition() < 0) postsAdapter.notifyDataSetChanged();
        else postsAdapter.notifyItemChanged(postModel.getPosition(), postModel);

        if (downloadAction != null) downloadAction.setVisible(postsAdapter.isSelecting);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED && selectedItems.size() > 0)
            Utils.batchDownload(this, userQuery, DownloadMethod.DOWNLOAD_MAIN, selectedItems);
    }

    public void deselectSelection(final BasePostModel postModel) {
        if (postModel instanceof PostModel) {
            selectedItems.remove(postModel);
            postModel.setSelected(false);
            if (postsAdapter != null) notifyAdapter((PostModel) postModel);
        }
    }

    class MyTask extends AsyncTask<Void, Bitmap, Void> {
        private Bitmap mIcon_val;

        protected Void doInBackground(Void... voids) {
            try {
                mIcon_val = BitmapFactory.decodeStream((InputStream) new URL(
                        (hashtagModel != null) ? hashtagModel.getSdProfilePic() : (
                                (locationModel != null) ? locationModel.getSdProfilePic() :
                                        profileModel.getSdProfilePic())
                ).getContent());
            } catch (Throwable ex) {
                Log.e("austin_debug", "bitmap: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (hashtagModel != null) profileBinding.profileView.mainHashtagImage.setImageBitmap(mIcon_val);
            else if (locationModel != null) profileBinding.profileView.mainLocationImage.setImageBitmap(mIcon_val);
            else profileBinding.profileView.mainProfileImage.setImageBitmap(mIcon_val);
        }
    }

    private final View.OnClickListener profileActionListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final boolean iamme = (isLoggedIn && profileModel != null)
                    ? Utils.getUserIdFromCookie(cookie).equals(profileModel.getId())
                    : false;
            if (!isLoggedIn && Utils.dataBox.getFavorite(userQuery) != null && v == profileBinding.profileView.btnFollow) {
                Utils.dataBox.delFavorite(new DataBox.FavoriteModel(userQuery,
                        Long.parseLong(Utils.dataBox.getFavorite(userQuery).split("/")[1]),
                        locationModel != null ? locationModel.getName() : userQuery.replaceAll("^@", "")));
                onRefresh();
            } else if (!isLoggedIn && (v == profileBinding.profileView.btnFollow || v == profileBinding.profileView.btnFollowTag)) {
                Utils.dataBox.addFavorite(new DataBox.FavoriteModel(userQuery, System.currentTimeMillis(),
                        locationModel != null ? locationModel.getName() : userQuery.replaceAll("^@", "")));
                onRefresh();
            } else if (v == profileBinding.profileView.btnFollow) {
                new ProfileAction().execute("follow");
            } else if (v == profileBinding.profileView.btnRestrict && isLoggedIn) {
                new ProfileAction().execute("restrict");
            } else if (v == profileBinding.profileView.btnSaved && !iamme) {
                new ProfileAction().execute("block");
            } else if (v == profileBinding.profileView.btnFollowTag) {
                new ProfileAction().execute("followtag");
            } else if (v == profileBinding.profileView.btnTagged || (v == profileBinding.profileView.btnRestrict && !isLoggedIn)) {
                startActivity(new Intent(ProfileViewer.this, SavedViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, "%"+profileModel.getId())
                        .putExtra(Constants.EXTRAS_USER, "@"+profileModel.getUsername())
                );
            } else if (v == profileBinding.profileView.btnSaved) {
                startActivity(new Intent(ProfileViewer.this, SavedViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, "$"+profileModel.getId())
                        .putExtra(Constants.EXTRAS_USER, "@"+profileModel.getUsername())
                );
            } else if (v == profileBinding.profileView.btnLiked) {
                startActivity(new Intent(ProfileViewer.this, SavedViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, "^"+profileModel.getId())
                        .putExtra(Constants.EXTRAS_USER, "@"+profileModel.getUsername())
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
                    ((action == "followtag" && hashtagModel != null) ? ("tags/"+
                            (hashtagModel.getFollowing() == true ? "unfollow/" : "follow/")+hashtagModel.getName()+"/") : (
                    ((action == "restrict" && profileModel != null) ? "restrict_action" : ("friendships/"+profileModel.getId()))+"/"+
                    ((action == "follow" && profileModel != null) ?
                    ((profileModel.getFollowing() == true ||
                            (profileModel.getFollowing() == false && profileModel.getRequested() == true))
                            ? "unfollow/" : "follow/") :
                    ((action == "restrict" && profileModel != null) ?
                            (profileModel.getRestricted() == true ? "unrestrict/" : "restrict/") :
                            (profileModel.getBlocked() == true ? "unblock/" : "block/")))));
            try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
                if (action == "restrict") {
                    final String urlParameters = "target_user_id="+profileModel.getId();
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
                else Toast.makeText(ProfileViewer.this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
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