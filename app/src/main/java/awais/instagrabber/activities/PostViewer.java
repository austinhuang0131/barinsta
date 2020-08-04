package awais.instagrabber.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import awais.instagrabber.R;
import awais.instagrabber.adapters.PostsMediaAdapter;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.asyncs.i.iPostFetcher;
import awais.instagrabber.customviews.CommentMentionClickSpan;
import awais.instagrabber.customviews.helpers.SwipeGestureListener;
import awais.instagrabber.databinding.ActivityViewerBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.SwipeEvent;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class PostViewer extends BaseLanguageActivity {
    private ActivityViewerBinding viewerBinding;
    private String url, prevUsername, commentsEndCursor;
    private ProfileModel profileModel;
    private BasePostModel postModel;
    private ViewerPostModel viewerPostModel;
    private SimpleExoPlayer player;
    private ArrayAdapter<String> profileDialogAdapter;
    private View viewsContainer, viewerCaptionParent;
    private GestureDetectorCompat gestureDetector;
    private SwipeEvent swipeEvent;
    private CharSequence postCaption = null, postShortCode, postUserId;
    private Resources resources;
    private boolean session = false, isFromShare;
    private int slidePos = 0, lastSlidePos = 0;
    private ItemGetType itemGetType;
    @SuppressLint("ClickableViewAccessibility")
    final View.OnTouchListener gestureTouchListener = new View.OnTouchListener() {
        private float startX;
        private float startY;

        @Override
        public boolean onTouch(final View v, final MotionEvent event) {
            if (v == viewerCaptionParent) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;

                    case MotionEvent.ACTION_UP:
                        if (!(Utils.isEmpty(postCaption) ||
                                Math.abs(startX - event.getX()) > 50 || Math.abs(startY - event.getY()) > 50)) {
                            Utils.copyText(PostViewer.this, postCaption);
                            return false;
                        }
                }
            }
            return gestureDetector.onTouchEvent(event);
        }
    };
    private final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
        final String username = viewerPostModel.getUsername();

        if (which == 0) {
            searchUsername(username);
        } else if (profileModel != null && which == 1) {
            startActivity(new Intent(this, ProfileViewer.class)
                    .putExtra(Constants.EXTRAS_PROFILE, profileModel));
        }
    };
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (v == viewerBinding.topPanel.ivProfilePic) {
                new AlertDialog.Builder(PostViewer.this).setAdapter(profileDialogAdapter, profileDialogListener)
                        .setNeutralButton(R.string.cancel, null).setTitle(viewerPostModel.getUsername()).show();

            } else if (v == viewerBinding.ivToggleFullScreen) {
                toggleFullscreen();

                final LinearLayout topPanelRoot = viewerBinding.topPanel.getRoot();
                final int iconRes;

                if (containerLayoutParams.weight != 3.3f) {
                    containerLayoutParams.weight = 3.3f;
                    iconRes = R.drawable.ic_fullscreen_exit;
                    topPanelRoot.setVisibility(View.GONE);
                    viewerBinding.btnDownload.setVisibility(View.VISIBLE);
                    viewerBinding.bottomPanel.tvPostDate.setVisibility(View.GONE);
                } else {
                    containerLayoutParams.weight = (viewerBinding.mediaList.getVisibility() == View.VISIBLE) ? 1.35f : 1.9f;
                    containerLayoutParams.weight += (Utils.isEmpty(settingsHelper.getString(Constants.COOKIE))) ? 0.3f : 0;
                    iconRes = R.drawable.ic_fullscreen;
                    topPanelRoot.setVisibility(View.VISIBLE);
                    viewerBinding.btnDownload.setVisibility(View.GONE);
                    viewerBinding.bottomPanel.tvPostDate.setVisibility(View.VISIBLE);
                }

                viewerBinding.ivToggleFullScreen.setImageResource(iconRes);
                viewerBinding.container.setLayoutParams(containerLayoutParams);

            } else if (v == viewerBinding.bottomPanel.btnMute) {
                if (player != null) {
                    final float intVol = player.getVolume() == 0f ? 1f : 0f;
                    player.setVolume(intVol);
                    viewerBinding.bottomPanel.btnMute.setImageResource(intVol == 0f ? R.drawable.mute : R.drawable.vol);
                    Utils.sessionVolumeFull = intVol == 1f;
                }
            } else if (v == viewerBinding.btnLike) {
                new PostAction().execute("likes");
            } else if (v == viewerBinding.btnBookmark) {
                new PostAction().execute("save");
            } else {
                final Object tag = v.getTag();
                if (tag instanceof ViewerPostModel) {
                    viewerPostModel = (ViewerPostModel) tag;
                    slidePos = Math.max(0, viewerPostModel.getPosition());
                    refreshPost();
                }
            }
        }
    };
    private final View.OnClickListener downloadClickListener = v -> {
        if (ContextCompat.checkSelfPermission(this, Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED)
            showDownloadDialog();
        else
            ActivityCompat.requestPermissions(this, Utils.PERMS, 8020);
    };
    private final PostsMediaAdapter mediaAdapter = new PostsMediaAdapter(null, onClickListener);
    private RequestManager glideRequestManager;
    private LinearLayout.LayoutParams containerLayoutParams;
    private final FetchListener<ViewerPostModel[]> pfl = result -> {
        if (result == null || result.length < 1) {
            Toast.makeText(this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            return;
        }

        viewerPostModel = result[0];

        mediaAdapter.setData(result);
        if (result.length > 1) {
            viewerBinding.mediaList.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.55f
            ));
            containerLayoutParams.weight = 1.35f;
            containerLayoutParams.weight += (Utils.isEmpty(settingsHelper.getString(Constants.COOKIE))) ? 0.3f : 0;
            viewerBinding.container.setLayoutParams(containerLayoutParams);
            viewerBinding.mediaList.setVisibility(View.VISIBLE);
        }

        final View viewStoryPost = findViewById(R.id.viewStoryPost);
        if (viewStoryPost != null) {
            viewStoryPost.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (profileModel.isPrivate())
                        Toast.makeText(getApplicationContext(), R.string.share_private_post, Toast.LENGTH_LONG).show();
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/"+postModel.getShortCode());
                    startActivity(Intent.createChooser(sharingIntent,
                            (profileModel.isPrivate()) ? getString(R.string.share_private_post) : getString(R.string.share_public_post)));
                }
            });
        }

        viewerCaptionParent.setOnTouchListener(gestureTouchListener);
        viewerBinding.playerView.setOnTouchListener(gestureTouchListener);
        viewerBinding.imageViewer.setOnSingleFlingListener((e1, e2, velocityX, velocityY) -> {
            final float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && Math.abs(diffX) > SwipeGestureListener.SWIPE_THRESHOLD
                    && Math.abs(velocityX) > SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD) {
                swipeEvent.onSwipe(diffX > 0);
                return true;
            }
            return false;
        });

        final long commentsCount = viewerPostModel.getCommentsCount();
        viewerBinding.bottomPanel.commentsCount.setText(String.valueOf(commentsCount));
        viewerBinding.bottomPanel.btnComments.setVisibility(View.VISIBLE);

        viewerBinding.bottomPanel.btnComments.setOnClickListener(v ->
                startActivityForResult(new Intent(this, CommentsViewer.class)
                        .putExtra(Constants.EXTRAS_SHORTCODE, postShortCode)
                        .putExtra(Constants.EXTRAS_POST, viewerPostModel.getPostId())
                        .putExtra(Constants.EXTRAS_USER, postUserId), 6969));
        viewerBinding.bottomPanel.btnComments.setClickable(true);
        viewerBinding.bottomPanel.btnComments.setEnabled(true);

        if (postModel instanceof PostModel) {
            final PostModel postModel = (PostModel) this.postModel;
            postModel.setPostId(viewerPostModel.getPostId());
            postModel.setTimestamp(viewerPostModel.getTimestamp());
            postModel.setPostCaption(viewerPostModel.getPostCaption());
            postModel.setLike(viewerPostModel.getLike());
            postModel.setBookmark(viewerPostModel.getBookmark());
        }

        setupPostInfoBar("@"+viewerPostModel.getUsername(), viewerPostModel.getItemType(),
                viewerPostModel.getLocation() == null ? null : viewerPostModel.getLocation());

        postCaption = postModel.getPostCaption();
        viewerCaptionParent.setVisibility(View.VISIBLE);

        viewerBinding.bottomPanel.btnDownload.setOnClickListener(downloadClickListener);

        refreshPost();
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewerBinding = ActivityViewerBinding.inflate(getLayoutInflater());
        setContentView(viewerBinding.getRoot());

        glideRequestManager = Glide.with(this);

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_POST)
                || (postModel = (PostModel) intent.getSerializableExtra(Constants.EXTRAS_POST)) == null) {
            Utils.errorFinish(this);
            return;
        }

        containerLayoutParams = (LinearLayout.LayoutParams) viewerBinding.container.getLayoutParams();

        if (intent.hasExtra(Constants.EXTRAS_TYPE))
            itemGetType = (ItemGetType) intent.getSerializableExtra(Constants.EXTRAS_TYPE);

        resources = getResources();

        final View viewStoryPost = findViewById(R.id.viewStoryPost);
        if (viewStoryPost != null) {
            viewStoryPost.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (profileModel.isPrivate())
                        Toast.makeText(getApplicationContext(), R.string.share_private_post, Toast.LENGTH_LONG).show();
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/"+postShortCode);
                    startActivity(Intent.createChooser(sharingIntent,
                            (profileModel.isPrivate()) ? getString(R.string.share_private_post) : getString(R.string.share_public_post)));
                }
            });
        }

        viewerBinding.topPanel.title.setMovementMethod(new LinkMovementMethod());
        viewerBinding.topPanel.title.setMentionClickListener((view, text, isHashtag) ->
                onClickListener.onClick(viewerBinding.topPanel.ivProfilePic));
        viewerBinding.topPanel.ivProfilePic.setOnClickListener(onClickListener);

        viewerBinding.ivToggleFullScreen.setOnClickListener(onClickListener);
        if (Utils.isEmpty(settingsHelper.getString(Constants.COOKIE))) {
            viewerBinding.btnLike.setVisibility(View.GONE);
            viewerBinding.btnBookmark.setVisibility(View.GONE);
            viewerBinding.postActions.setVisibility(View.GONE);
            viewerBinding.postActions.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 0
            ));
            containerLayoutParams.weight = (containerLayoutParams.weight == 3.3f) ? 3.3f : 2.2f;
            viewerBinding.container.setLayoutParams(containerLayoutParams);
        }
        else {
            viewerBinding.btnLike.setOnClickListener(onClickListener);
            viewerBinding.btnBookmark.setOnClickListener(onClickListener);
        }
        viewerBinding.btnDownload.setOnClickListener(downloadClickListener);

        profileDialogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new String[]{resources.getString(R.string.open_profile), resources.getString(R.string.view_pfp)});

        postModel.setPosition(intent.getIntExtra(Constants.EXTRAS_INDEX, -1));
        postShortCode = postModel.getShortCode();

        final boolean postIdNull = postModel.getPostId() == null;
        if (!postIdNull)
            setupPostInfoBar(intent.getStringExtra(Constants.EXTRAS_USER), postModel.getItemType(), null);

        isFromShare = postModel.getPosition() == -1 || postIdNull;

        viewerCaptionParent = (View) viewerBinding.bottomPanel.viewerCaption.getParent();
        viewsContainer = (View) viewerBinding.bottomPanel.tvVideoViews.getParent();

        viewerBinding.mediaList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        viewerBinding.mediaList.setAdapter(mediaAdapter);
        viewerBinding.mediaList.setVisibility(View.GONE);

        swipeEvent = isRight -> {
            final List<? extends BasePostModel> itemGetterItems;
            final boolean isMainSwipe;

            if (itemGetType == ItemGetType.SAVED_ITEMS && SavedViewer.itemGetter != null) {
                itemGetterItems = SavedViewer.itemGetter.get(itemGetType);
                isMainSwipe = !(itemGetterItems.size() < 1 || itemGetType == ItemGetType.SAVED_ITEMS && isFromShare);
            }
            else if (itemGetType != null && Main.itemGetter != null) {
                itemGetterItems = Main.itemGetter.get(itemGetType);
                isMainSwipe = !(itemGetterItems.size() < 1 || itemGetType == ItemGetType.MAIN_ITEMS && isFromShare);
            } else {
                itemGetterItems = null;
                isMainSwipe = false;
            }

            final BasePostModel[] basePostModels = mediaAdapter.getPostModels();
            final int slides = basePostModels.length;

            int position = postModel.getPosition();

            if (isRight) {
                --slidePos;
                if (!isMainSwipe && slidePos < 0) slidePos = 0;
                if (slides > 0 && slidePos >= 0) {
                    if (basePostModels[slidePos] instanceof ViewerPostModel) {
                        viewerPostModel = (ViewerPostModel) basePostModels[slidePos];
                    }
                    refreshPost();
                    return;
                }
                if (isMainSwipe && --position < 0) position = itemGetterItems.size() - 1;
            } else {
                ++slidePos;
                if (!isMainSwipe && slidePos >= slides) slidePos = slides - 1;
                if (slides > 0 && slidePos < slides) {
                    if (basePostModels[slidePos] instanceof ViewerPostModel) {
                        viewerPostModel = (ViewerPostModel) basePostModels[slidePos];
                    }
                    refreshPost();
                    return;
                }
                if (isMainSwipe && ++position >= itemGetterItems.size()) position = 0;
            }

            if (isMainSwipe) {
                slidePos = 0;
                Log.d("AWAISKING_APP", "swipe left <<< post[" + position + "]: " + postModel + " -- " + slides);
                postModel = itemGetterItems.get(position);
                postModel.setPosition(position);
                viewPost();
            }
        };
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener(swipeEvent));

        viewPost();
    }

    private void viewPost() {
        lastSlidePos = 0;
        mediaAdapter.setData(null);
        viewsContainer.setVisibility(View.GONE);
        viewerCaptionParent.setVisibility(View.GONE);
        viewerBinding.mediaList.setVisibility(View.GONE);
        viewerBinding.btnDownload.setVisibility(View.GONE);
        viewerBinding.bottomPanel.btnMute.setVisibility(View.GONE);
        viewerBinding.bottomPanel.tvPostDate.setVisibility(View.GONE);
        viewerBinding.bottomPanel.btnComments.setVisibility(View.GONE);
        viewerBinding.bottomPanel.btnDownload.setVisibility(View.INVISIBLE);
        viewerBinding.bottomPanel.viewerCaption.setText(null);
        viewerBinding.bottomPanel.viewerCaption.setMentionClickListener(null);

        viewerBinding.playerView.setVisibility(View.GONE);
        viewerBinding.playerView.setPlayer(null);
        viewerBinding.imageViewer.setImageResource(0);
        viewerBinding.imageViewer.setImageDrawable(null);

        if (postModel.getShortCode() != null)
            new PostFetcher(postModel.getShortCode(), pfl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else if (postModel.getPostId() != null)
            new iPostFetcher(postModel.getPostId(), pfl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void searchUsername(final String text) {
        if (Main.scanHack != null) {
            Main.scanHack.onResult(text);
            setResult(6969);
            Intent intent = new Intent(getApplicationContext(), Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void setupVideo() {
        viewerBinding.playerView.setVisibility(View.VISIBLE);
        viewerBinding.bottomPanel.btnDownload.setVisibility(View.VISIBLE);
        viewerBinding.bottomPanel.btnMute.setVisibility(View.VISIBLE);
        viewsContainer.setVisibility(View.VISIBLE);
        viewerBinding.progressView.setVisibility(View.GONE);
        viewerBinding.imageViewer.setVisibility(View.GONE);
        viewerBinding.imageViewer.setImageDrawable(null);

        viewerBinding.bottomPanel.tvVideoViews.setText(String.valueOf(viewerPostModel.getVideoViews()));

        player = new SimpleExoPlayer.Builder(this).build();
        viewerBinding.playerView.setPlayer(player);
        float vol = Utils.settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;

        player.setVolume(vol);
        player.setPlayWhenReady(Utils.settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));
        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(this, "instagram"))
                .createMediaSource(Uri.parse(url));
        mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            @Override
            public void onLoadCompleted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                viewerBinding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadStarted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                viewerBinding.progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadCanceled(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                viewerBinding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
                viewerBinding.progressView.setVisibility(View.GONE);
            }
        });
        player.prepare(mediaSource);

        player.setVolume(vol);
        viewerBinding.bottomPanel.btnMute.setImageResource(vol == 0f ? R.drawable.vol : R.drawable.mute);

        viewerBinding.bottomPanel.btnMute.setOnClickListener(onClickListener);
    }

    private void setupImage() {
        viewsContainer.setVisibility(View.GONE);
        viewerBinding.playerView.setVisibility(View.GONE);
        viewerBinding.progressView.setVisibility(View.VISIBLE);
        viewerBinding.bottomPanel.btnMute.setVisibility(View.GONE);
        viewerBinding.bottomPanel.btnDownload.setVisibility(View.VISIBLE);

        viewerBinding.imageViewer.setImageDrawable(null);
        viewerBinding.imageViewer.setVisibility(View.VISIBLE);
        viewerBinding.imageViewer.setZoomable(true);
        viewerBinding.imageViewer.setZoomTransitionDuration(420);
        viewerBinding.imageViewer.setMaximumScale(7.2f);

        glideRequestManager.load(url).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                viewerBinding.progressView.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                viewerBinding.progressView.setVisibility(View.GONE);
                return false;
            }
        }).into(viewerBinding.imageViewer);
    }

    private void showDownloadDialog() {
        final ArrayList<BasePostModel> postModels = new ArrayList<>();

        if (!session && viewerBinding.mediaList.getVisibility() == View.VISIBLE) {
            final DialogInterface.OnClickListener clickListener = (dialog, which) -> {
                postModels.clear();

                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    final BasePostModel[] adapterPostModels = mediaAdapter.getPostModels();
                    for (int i = 0, size = mediaAdapter.getItemCount(); i < size; ++i) {
                        if (adapterPostModels[i] instanceof ViewerPostModel)
                            postModels.add(adapterPostModels[i]);
                    }
                } else if (which == DialogInterface.BUTTON_POSITIVE) {
                    postModels.add(viewerPostModel);
                } else {
                    session = true;
                    postModels.add(viewerPostModel);
                }

                if (postModels.size() > 0)
                    Utils.batchDownload(this, viewerPostModel.getUsername(), DownloadMethod.DOWNLOAD_POST_VIEWER, postModels);
            };

            new AlertDialog.Builder(this).setTitle(R.string.post_viewer_download_dialog_title)
                    .setMessage(R.string.post_viewer_download_message)
                    .setNeutralButton(R.string.post_viewer_download_session, clickListener).setPositiveButton(R.string.post_viewer_download_current, clickListener)
                    .setNegativeButton(R.string.post_viewer_download_album, clickListener).show();
        } else {
            Utils.batchDownload(this, viewerPostModel.getUsername(), DownloadMethod.DOWNLOAD_POST_VIEWER, Collections.singletonList(viewerPostModel));
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            showDownloadDialog();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 6969) {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < 24) releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= 24) releasePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player == null && viewerPostModel != null && viewerPostModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO)
            setupVideo();
        else if (player != null) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    private void refreshPost() {
        postShortCode = postModel.getShortCode();
        if (containerLayoutParams.weight != 3.3f) {
            containerLayoutParams.weight = (viewerBinding.mediaList.getVisibility() == View.VISIBLE) ? 1.35f : 1.9f;
            viewerBinding.container.setLayoutParams(containerLayoutParams);
        }
        if (viewerBinding.mediaList.getVisibility() == View.VISIBLE) {
            ViewerPostModel item = mediaAdapter.getItemAt(lastSlidePos);
            if (item != null) {
                item.setCurrentSlide(false);
                mediaAdapter.notifyItemChanged(lastSlidePos, item);
            }

            item = mediaAdapter.getItemAt(slidePos);
            if (item != null) {
                item.setCurrentSlide(true);
                mediaAdapter.notifyItemChanged(slidePos, item);
            }
        }
        lastSlidePos = slidePos;

        postCaption = viewerPostModel.getPostCaption();

        if (Utils.hasMentions(postCaption)) {
            viewerBinding.bottomPanel.viewerCaption.setText(Utils.getMentionText(postCaption), TextView.BufferType.SPANNABLE);
            viewerBinding.bottomPanel.viewerCaption.setMentionClickListener((view, text, isHashtag) ->
                    new AlertDialog.Builder(PostViewer.this).setTitle(text)
                            .setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
                            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok,
                            (dialog, which) -> searchUsername(text)).show());
        } else {
            viewerBinding.bottomPanel.viewerCaption.setMentionClickListener(null);
            viewerBinding.bottomPanel.viewerCaption.setText(postCaption);
        }

        setupPostInfoBar("@"+viewerPostModel.getUsername(), viewerPostModel.getItemType(),
                viewerPostModel.getLocation());

        if (postModel instanceof PostModel) {
            final PostModel postModel = (PostModel) this.postModel;
            postModel.setPostId(viewerPostModel.getPostId());
            postModel.setTimestamp(viewerPostModel.getTimestamp());
            postModel.setPostCaption(viewerPostModel.getPostCaption());
            postModel.setLike(viewerPostModel.getLike());
            postModel.setBookmark(viewerPostModel.getBookmark());
            if (viewerPostModel.getLike() == true) {
                viewerBinding.btnLike.setText(resources.getString(R.string.unlike, viewerPostModel.getLikes()));
                viewerBinding.btnLike.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(
                        R.color.btn_pink_background, null)));
            }
            else {
                viewerBinding.btnLike.setText(resources.getString(R.string.like, viewerPostModel.getLikes()));
                viewerBinding.btnLike.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(
                        R.color.btn_lightpink_background, null)));
            }
            if (viewerPostModel.getBookmark() == true) {
                viewerBinding.btnBookmark.setText(R.string.unbookmark);
                viewerBinding.btnBookmark.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(
                        R.color.btn_orange_background, null)));
            }
            else {
                viewerBinding.btnBookmark.setText(R.string.bookmark);
                viewerBinding.btnBookmark.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(
                        R.color.btn_lightorange_background, null)));
            }
        }

        viewerBinding.bottomPanel.tvPostDate.setText(viewerPostModel.getPostDate());
        viewerBinding.bottomPanel.tvPostDate.setVisibility(containerLayoutParams.weight != 3.3f ? View.VISIBLE : View.GONE);
        viewerBinding.bottomPanel.tvPostDate.setSelected(true);

        url = viewerPostModel.getDisplayUrl();
        releasePlayer();

        viewerBinding.btnDownload.setVisibility(containerLayoutParams.weight == 3.3f ? View.VISIBLE : View.GONE);
        if (viewerPostModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) setupVideo();
        else setupImage();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void setupPostInfoBar(final String from, final MediaItemType mediaItemType, final JSONObject location) {
        if (prevUsername == null || !prevUsername.equals(from)) {
            viewerBinding.topPanel.ivProfilePic.setImageBitmap(null);
            viewerBinding.topPanel.ivProfilePic.setImageDrawable(null);
            viewerBinding.topPanel.ivProfilePic.setImageResource(0);

            if (!Utils.isEmpty(from) && from.charAt(0) == '@')
                new ProfileFetcher(from.substring(1), result -> {
                    profileModel = result;

                    if (result != null) {
                        final String hdProfilePic = result.getHdProfilePic();
                        final String sdProfilePic = result.getSdProfilePic();
                        postUserId = result.getId();

                        final boolean hdPicEmpty = Utils.isEmpty(hdProfilePic);
                        glideRequestManager.load(hdPicEmpty ? sdProfilePic : hdProfilePic).listener(new RequestListener<Drawable>() {
                            private boolean loaded = true;

                            @Override
                            public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                                viewerBinding.topPanel.ivProfilePic.setEnabled(false);
                                viewerBinding.topPanel.ivProfilePic.setOnClickListener(null);
                                if (loaded) {
                                    loaded = false;
                                    if (!Utils.isEmpty(sdProfilePic)) glideRequestManager.load(sdProfilePic).listener(this)
                                            .into(viewerBinding.topPanel.ivProfilePic);
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                                viewerBinding.topPanel.ivProfilePic.setEnabled(true);
                                viewerBinding.topPanel.ivProfilePic.setOnClickListener(onClickListener);
                                return false;
                            }
                        }).into(viewerBinding.topPanel.ivProfilePic);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            prevUsername = from;
        }

        final String titlePrefix = resources.getString(mediaItemType == MediaItemType.MEDIA_TYPE_VIDEO ?
                R.string.post_viewer_video_post : R.string.post_viewer_image_post);
        if (Utils.isEmpty(from)) viewerBinding.topPanel.title.setText(titlePrefix);
        else {
            final int titleLen = from.length();
            final SpannableString spannableString = new SpannableString(from);
            spannableString.setSpan(new CommentMentionClickSpan(), 0, titleLen, 0);
            viewerBinding.topPanel.title.setText(spannableString);
        }

        if (location == null) {
            viewerBinding.topPanel.location.setVisibility(View.GONE);
            viewerBinding.topPanel.title.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
            ));
        }
        else {
            viewerBinding.topPanel.location.setVisibility(View.VISIBLE);
            viewerBinding.topPanel.location.setText(location.optString("name"));
            viewerBinding.topPanel.location.setOnClickListener(v ->
                new AlertDialog.Builder(PostViewer.this).setTitle(location.optString("name"))
                        .setMessage(R.string.comment_view_mention_location_search)
                        .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok,
                        (dialog, which) -> searchUsername(location.optString("id")+"/"+location.optString("slug"))).show()
            );
            viewerBinding.topPanel.title.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    private void toggleFullscreen() {
        final View decorView = getWindow().getDecorView();
        int newUiOptions = decorView.getSystemUiVisibility();
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(newUiOptions);
    }

    /*
     Recommended for PERSONAL use only
     Don't ever think about running a like farm with this
     */

    class PostAction extends AsyncTask<String, Void, Void> {
        boolean ok = false;
        String action;

        protected Void doInBackground(String... rawAction) {
            action = rawAction[0];
            final String url = "https://www.instagram.com/web/"+action+"/"+postModel.getPostId()+"/"+ (action == "save" ?
                    (postModel.getBookmark() == true ? "unsave/" : "save/") :
                    (postModel.getLike() == true ? "unlike/" : "like/"));
            try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                urlConnection.setRequestProperty("x-csrftoken",
                        settingsHelper.getString(Constants.COOKIE).split("csrftoken=")[1].split(";")[0]);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = true;
                }
                else Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", action+": " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok == true && action == "likes") {
                postModel.setLike(!viewerPostModel.getLike());
                viewerPostModel.setManualLike(!viewerPostModel.getLike());
                refreshPost();
            }
            else if (ok == true && action == "save") {
                viewerPostModel.setBookmark(!viewerPostModel.getBookmark());
                postModel.setBookmark(!viewerPostModel.getBookmark());
                refreshPost();
            }
        }
    }
}