package awais.instagrabber.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.PermissionChecker;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.Date;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.SliderCallbackAdapter;
import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.adapters.viewholder.SliderVideoViewHolder;
import awais.instagrabber.customviews.SharedElementTransitionDialogFragment;
import awais.instagrabber.customviews.VerticalDragHelper;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.databinding.DialogPostViewBinding;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.MediaService;
import awais.instagrabber.webservices.ServiceCallback;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.fragments.HashTagFragment.ARG_HASHTAG;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class PostViewV2Fragment extends SharedElementTransitionDialogFragment {
    private static final String TAG = "PostViewV2Fragment";
    private static final String COOKIE = settingsHelper.getString(Constants.COOKIE);
    private static final int DETAILS_HIDE_DELAY_MILLIS = 2000;
    private static final String ARG_FEED_MODEL = "feedModel";
    private static final String ARG_SLIDER_POSITION = "position";
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;

    private FeedModel feedModel;
    private View sharedProfilePicElement;
    private View sharedMainPostElement;
    private MainActivity fragmentActivity;
    private DialogPostViewBinding binding;
    private MediaService mediaService;
    private Context context;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private boolean detailsVisible = true, video;
    private VideoPlayerViewHelper videoPlayerViewHelper;
    private SliderItemsAdapter sliderItemsAdapter;
    private boolean wasControlsVisible;
    private boolean wasPaused;
    private int captionState = BottomSheetBehavior.STATE_HIDDEN;
    private int sliderPosition = -1;
    private DialogInterface.OnShowListener onShowListener;
    private boolean isLoggedIn;

    private final VerticalDragHelper.OnVerticalDragListener onVerticalDragListener = new VerticalDragHelper.OnVerticalDragListener() {

        @Override
        public void onDrag(final float dY) {
            // allow the view to be draggable
            final ConstraintLayout v = binding.getRoot();
            final float finalY = v.getY() + dY;
            animateY(v, finalY, 0, null);
        }

        @Override
        public void onDragEnd() {
            // animate and dismiss if user drags the view more that 30% of the view
            if (Math.abs(binding.getRoot().getY()) > Utils.displayMetrics.heightPixels * 0.35) {
                animateAndDismiss(binding.getRoot().getY() < 0 ? -1 : 1);
                return;
            }
            // animate back the view to proper position
            animateY(binding.getRoot(), 0, 200, null);
        }

        @Override
        public void onFling(final float flingVelocity) {
            // animate and dismiss if user flings up/down
            animateAndDismiss(flingVelocity < 0 ? -1 : 1);
        }

        private void animateAndDismiss(final int direction) {
            final int height = binding.getRoot().getHeight();
            final int finalYDist = height + Utils.getStatusBarHeight(context);
            // less than 0 means up direction, else down
            final int finalY = direction < 0 ? -finalYDist : finalYDist;
            animateY(binding.getRoot(), finalY, 200, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    dismiss();
                }
            });
        }
    };

    public void setOnShowListener(final DialogInterface.OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }

    public static class Builder {
        private final FeedModel feedModel;
        private View profilePicElement;
        private View mainPostElement;
        private int position;

        public Builder setSharedProfilePicElement(final View profilePicElement) {
            this.profilePicElement = profilePicElement;
            return this;
        }

        public Builder setSharedMainPostElement(final View mainPostElement) {
            this.mainPostElement = mainPostElement;
            return this;
        }

        public Builder setPosition(final int position) {
            this.position = position;
            return this;
        }

        public PostViewV2Fragment build() {
            return PostViewV2Fragment.newInstance(feedModel, profilePicElement, mainPostElement, position);
        }

        public Builder(final FeedModel feedModel) {
            this.feedModel = feedModel;
        }
    }

    private static PostViewV2Fragment newInstance(final FeedModel feedModel,
                                                  final View profilePicElement,
                                                  final View mainPostElement,
                                                  final int position) {
        final PostViewV2Fragment f = new PostViewV2Fragment(profilePicElement, mainPostElement);
        final Bundle args = new Bundle();
        args.putSerializable(ARG_FEED_MODEL, feedModel);
        if (position >= 0) {
            args.putInt(ARG_SLIDER_POSITION, position);
        }
        f.setArguments(args);
        return f;
    }

    public static Builder builder(final FeedModel feedModel) {
        return new Builder(feedModel);
    }

    // default constructor for fragment manager
    public PostViewV2Fragment() {}

    private PostViewV2Fragment(final View sharedProfilePicElement,
                               final View sharedMainPostElement) {
        this.sharedProfilePicElement = sharedProfilePicElement;
        this.sharedMainPostElement = sharedMainPostElement;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Translucent);
        fragmentActivity = (MainActivity) getActivity();
        mediaService = MediaService.getInstance();
        final Bundle arguments = getArguments();
        if (arguments == null) return;
        final Serializable feedModelSerializable = arguments.getSerializable(ARG_FEED_MODEL);
        if (feedModelSerializable == null) {
            Log.e(TAG, "onCreate: feedModelSerializable is null");
            return;
        }
        if (!(feedModelSerializable instanceof FeedModel)) {
            return;
        }
        feedModel = (FeedModel) feedModelSerializable;
        if (feedModel.getItemType() == MediaItemType.MEDIA_TYPE_SLIDER) {
            sliderPosition = arguments.getInt(ARG_SLIDER_POSITION, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = DialogPostViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        setupToolbar();
        init();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0);
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setLayout(width, height);
        if (!wasPaused && (sharedProfilePicElement != null || sharedMainPostElement != null)) {
            final ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                    binding.getRoot().getBackground().mutate(),
                    PropertyValuesHolder.ofInt("alpha", 0, 255)
            );
            addAnimator(animator);
        }
        if (onShowListener != null) {
            onShowListener.onShow(dialog);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        wasPaused = true;
        if (bottomSheetBehavior != null) {
            captionState = bottomSheetBehavior.getState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        switch (feedModel.getItemType()) {
            case MEDIA_TYPE_VIDEO:
                if (videoPlayerViewHelper != null) {
                    videoPlayerViewHelper.releasePlayer();
                }
                return;
            case MEDIA_TYPE_SLIDER:
                if (sliderItemsAdapter != null) {
                    releaseAllSliderPlayers();
                }
            default:
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (feedModel.getItemType() == MediaItemType.MEDIA_TYPE_SLIDER) {
            outState.putInt(ARG_SLIDER_POSITION, sliderPosition);
        }
    }

    @Override
    protected void onBeforeSharedElementAnimation(@NonNull final View startView,
                                                  @NonNull final View destView,
                                                  @NonNull final SharedElementTransitionDialogFragment.ViewBounds viewBounds) {
        GenericDraweeHierarchy hierarchy = null;
        if (destView == binding.postImage) {
            hierarchy = binding.postImage.getHierarchy();
        } else if (destView == binding.videoPost.thumbnailParent) {
            hierarchy = binding.videoPost.thumbnail.getHierarchy();
        }
        if (hierarchy != null) {
            final ScalingUtils.ScaleType scaleTypeTo = ScalingUtils.ScaleType.FIT_CENTER;
            final ScalingUtils.InterpolatingScaleType scaleType = new ScalingUtils.InterpolatingScaleType(
                    ScalingUtils.ScaleType.CENTER_CROP,
                    scaleTypeTo,
                    viewBounds.getStartBounds(),
                    viewBounds.getDestBounds()
            );
            hierarchy.setActualImageScaleType(scaleType);
            final ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(getAnimationDuration());
            animator.addUpdateListener(animation -> {
                float fraction = (float) animation.getAnimatedValue();
                scaleType.setValue(fraction);
            });
            final GenericDraweeHierarchy finalHierarchy = hierarchy;
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finalHierarchy.setActualImageScaleType(scaleTypeTo);
                    destView.requestLayout();
                }
            });
            addAnimator(animator);
        }
    }

    @Override
    protected void onEndSharedElementAnimation(@NonNull final View startView,
                                               @NonNull final View destView,
                                               @NonNull final ViewBounds viewBounds) {
        if (destView == binding.postImage) {
            binding.postImage.setTranslationX(0);
            binding.postImage.setTranslationY(0);
            binding.postImage.setX(0);
            binding.postImage.setY(0);
            binding.postImage.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                ViewGroup.LayoutParams.MATCH_PARENT));
            binding.postImage.requestLayout();
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            return;
        }
        if (destView == binding.sliderParent) {
            binding.sliderParent.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                   ViewGroup.LayoutParams.MATCH_PARENT));
            binding.sliderParent.requestLayout();
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            return;
        }
        if (destView == binding.videoPost.thumbnailParent) {
            final FrameLayout.LayoutParams params = new ViewSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                  ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            binding.videoPost.thumbnailParent.setLayoutParams(params);
            binding.videoPost.thumbnailParent.requestLayout();
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERM_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            DownloadUtils.showDownloadDialog(context, feedModel, sliderPosition);
        }
    }

    private void init() {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != null;
        if (!wasPaused && (sharedProfilePicElement != null || sharedMainPostElement != null)) {
            binding.getRoot().getBackground().mutate().setAlpha(0);
        }
        setupProfilePic();
        setupTitles();
        setupCaption();
        setupCounts();
        setupPostTypeLayout();
        setupCommonActions();
    }

    private void setupCommonActions() {
        setupLike();
        setupSave();
        setupDownload();
        setupComment();
    }

    private void setupComment() {
        if (!isLoggedIn) {
            binding.comment.setVisibility(View.GONE);
            return;
        }
        binding.comment.setOnClickListener(v -> {
            final NavController navController = getNavController();
            if (navController == null) return;
            final Bundle bundle = new Bundle();
            bundle.putString("shortCode", feedModel.getShortCode());
            bundle.putString("postId", feedModel.getPostId());
            bundle.putString("postUserId", feedModel.getProfileModel().getId());
            navController.navigate(R.id.action_global_commentsViewerFragment, bundle);
        });
        binding.comment.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.comment));
            return true;
        });
    }

    private void setupDownload() {
        binding.download.setOnClickListener(v -> {
            if (checkSelfPermission(context, WRITE_PERMISSION) == PermissionChecker.PERMISSION_GRANTED) {
                DownloadUtils.showDownloadDialog(context, feedModel, sliderPosition);
                return;
            }
            requestPermissions(DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE);
        });
        binding.download.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.action_download));
            return true;
        });
    }

    private void setupLike() {
        if (!isLoggedIn) {
            binding.like.setVisibility(View.GONE);
            return;
        }
        if (mediaService == null) return;
        setLikedResources(feedModel.getLike());
        final ServiceCallback<Boolean> likeCallback = new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                binding.like.setEnabled(true);
                if (result) {
                    setLikedResources(!feedModel.getLike());
                    final long currentLikesCount = feedModel.getLikesCount();
                    final long updatedCount;
                    if (!feedModel.getLike()) {
                        updatedCount = currentLikesCount + 1;
                        feedModel.setLiked(true);
                    } else {
                        updatedCount = currentLikesCount - 1;
                        feedModel.setLiked(false);
                    }
                    feedModel.setLikesCount(updatedCount);
                    setupCounts();
                    return;
                }
                unsuccessfulLike();
            }

            @Override
            public void onFailure(final Throwable t) {
                binding.like.setEnabled(true);
                Log.e(TAG, "Error during like/unlike", t);
                unsuccessfulLike();
            }

            private void unsuccessfulLike() {
                final int errorTextResId;
                if (!feedModel.getLike()) {
                    Log.e(TAG, "like unsuccessful!");
                    errorTextResId = R.string.like_unsuccessful;
                } else {
                    Log.e(TAG, "unlike unsuccessful!");
                    errorTextResId = R.string.unlike_unsuccessful;
                }
                setLikedResources(feedModel.getLike());
                final Snackbar snackbar = Snackbar.make(binding.getRoot(), errorTextResId, BaseTransientBottomBar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.ok, null);
                snackbar.show();
            }
        };
        binding.like.setOnClickListener(v -> {
            final String userId = CookieUtils.getUserIdFromCookie(COOKIE);
            final String csrfToken = CookieUtils.getCsrfTokenFromCookie(COOKIE);
            v.setEnabled(false);
            final int textRes;
            if (!feedModel.getLike()) {
                textRes = R.string.liking;
            } else {
                textRes = R.string.unliking;
            }
            if (!feedModel.getLike()) {
                mediaService.like(feedModel.getPostId(), userId, csrfToken, likeCallback);
            } else {
                mediaService.unlike(feedModel.getPostId(), userId, csrfToken, likeCallback);
            }
        });
        binding.like.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.like_without_count));
            return true;
        });
    }

    private void setLikedResources(final boolean liked) {
        final int iconResource;
        final int tintResource;
        final int textResId;
        if (liked) {
            iconResource = R.drawable.ic_like;
            tintResource = R.color.red_600;
            textResId = R.string.unlike_without_count;
        } else {
            iconResource = R.drawable.ic_not_liked;
            tintResource = R.color.white;
            textResId = R.string.like_without_count;
        }
        binding.like.setIconResource(iconResource);
        binding.like.setIconTintResource(tintResource);
        // binding.like.setText(textResId);
    }

    private void setupSave() {
        if (!isLoggedIn) {
            binding.save.setVisibility(View.GONE);
            return;
        }
        if (mediaService == null) return;
        setSavedResources(feedModel.isSaved());
        final ServiceCallback<Boolean> saveCallback = new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                binding.save.setEnabled(true);
                if (result) {
                    setSavedResources(!feedModel.isSaved());
                    feedModel.setSaved(!feedModel.isSaved());
                    return;
                }
                unsuccessfulSave();
            }

            private void unsuccessfulSave() {
                final int errorTextResId;
                if (!feedModel.isSaved()) {
                    Log.e(TAG, "save unsuccessful!");
                    errorTextResId = R.string.save_unsuccessful;
                } else {
                    Log.e(TAG, "save remove unsuccessful!");
                    errorTextResId = R.string.save_remove_unsuccessful;
                }
                setSavedResources(feedModel.isSaved());
                final Snackbar snackbar = Snackbar.make(binding.getRoot(), errorTextResId, BaseTransientBottomBar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.ok, null);
                snackbar.show();
            }

            @Override
            public void onFailure(final Throwable t) {
                binding.save.setEnabled(true);
                Log.e(TAG, "Error during save/unsave", t);
                unsuccessfulSave();
            }
        };
        binding.save.setOnClickListener(v -> {
            final String userId = CookieUtils.getUserIdFromCookie(COOKIE);
            final String csrfToken = CookieUtils.getCsrfTokenFromCookie(COOKIE);
            binding.save.setEnabled(false);
            final int textRes;
            if (!feedModel.isSaved()) {
                textRes = R.string.saving;
            } else {
                textRes = R.string.removing;
            }
            if (!feedModel.isSaved()) {
                mediaService.save(feedModel.getPostId(), userId, csrfToken, saveCallback);
            } else {
                mediaService.unsave(feedModel.getPostId(), userId, csrfToken, saveCallback);
            }
        });
        binding.save.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.save));
            return true;
        });
    }

    private void setSavedResources(final boolean saved) {
        final int iconResource;
        final int tintResource;
        final int textResId;
        if (saved) {
            iconResource = R.drawable.ic_class_24;
            tintResource = R.color.blue_700;
            textResId = R.string.saved;
        } else {
            iconResource = R.drawable.ic_outline_class_24;
            tintResource = R.color.white;
            textResId = R.string.save;
        }
        binding.save.setIconResource(iconResource);
        binding.save.setIconTintResource(tintResource);
        // binding.save.setText(textResId);
    }

    private void setupProfilePic() {
        if (!wasPaused && sharedProfilePicElement != null) {
            addSharedElement(sharedProfilePicElement, binding.profilePic);
        }
        final String uri = feedModel.getProfileModel().getSdProfilePic();
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri)).build();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setImageRequest(requestBuilder)
                .setOldController(binding.profilePic.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(final String id, final Throwable throwable) {
                        startPostponedEnterTransition();
                    }

                    @Override
                    public void onFinalImageSet(final String id,
                                                final ImageInfo imageInfo,
                                                final Animatable animatable) {
                        startPostponedEnterTransition();
                    }
                })
                .build();
        binding.profilePic.setController(controller);
        binding.profilePic.setOnClickListener(v -> navigateToProfile("@" + feedModel.getProfileModel().getUsername()));
    }

    private void setupToolbar() {
        // fragmentActivity.fitSystemWindows(true);
        // final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        // if (actionBar == null) return;
        // actionBar.setTitle(null);
        // actionBar.setSubtitle(null);
    }

    private void setupTitles() {
        binding.title.setText(feedModel.getProfileModel().getUsername());
        binding.righttitle.setText(feedModel.getProfileModel().getName());
        binding.isVerified.setVisibility(feedModel.getProfileModel().isVerified() ? View.VISIBLE : View.GONE);
        binding.title.setOnClickListener(v -> navigateToProfile("@" + feedModel.getProfileModel().getUsername()));
        binding.righttitle.setOnClickListener(v -> navigateToProfile("@" + feedModel.getProfileModel().getUsername()));
        final String locationName = feedModel.getLocationName();
        if (!TextUtils.isEmpty(locationName)) {
            binding.subtitle.setText(locationName);
            binding.subtitle.setVisibility(View.VISIBLE);
            binding.subtitle.setOnClickListener(v -> {
                final NavController navController = getNavController();
                if (navController == null) return;
                final Bundle bundle = new Bundle();
                bundle.putString("locationId", feedModel.getLocationId());
                navController.navigate(R.id.action_global_locationFragment, bundle);
            });
            return;
        }
        binding.subtitle.setVisibility(View.GONE);
    }

    private void setupCaption() {
        final CharSequence postCaption = feedModel.getPostCaption();
        binding.date.setText(Utils.datetimeParser.format(new Date(feedModel.getTimestamp() * 1000L)));
        if (TextUtils.isEmpty(postCaption)) {
            binding.caption.setVisibility(View.GONE);
            binding.captionToggle.setVisibility(View.GONE);
            return;
        }
        binding.caption.addOnHashtagListener(autoLinkItem -> {
            final NavController navController = NavHostFragment.findNavController(this);
            final Bundle bundle = new Bundle();
            final String originalText = autoLinkItem.getOriginalText().trim();
            bundle.putString(ARG_HASHTAG, originalText);
            navController.navigate(R.id.action_global_hashTagFragment, bundle);
        });
        binding.caption.addOnMentionClickListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText().trim();
            navigateToProfile(originalText);
        });
        binding.caption.addOnEmailClickListener(autoLinkItem -> Utils.openEmailAddress(getContext(), autoLinkItem.getOriginalText().trim()));
        binding.caption.addOnURLClickListener(autoLinkItem -> Utils.openURL(getContext(), autoLinkItem.getOriginalText().trim()));
        binding.caption.setOnLongClickListener(v -> {
            Utils.copyText(context, postCaption);
            return true;
        });
        binding.caption.setText(postCaption);
        bottomSheetBehavior = BottomSheetBehavior.from(binding.captionParent);
        bottomSheetBehavior.setState(captionState);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull final View bottomSheet, final int newState) {}

            @Override
            public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                binding.captionParent.getBackground().mutate().setAlpha((int) (128 + (128 * (slideOffset < 0 ? 0 : slideOffset))));
            }
        });
        binding.caption.setOnClickListener(v -> {
            if (bottomSheetBehavior == null) return;
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) return;
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        binding.captionToggle.setOnClickListener(v -> {
            if (bottomSheetBehavior == null) return;
            switch (bottomSheetBehavior.getState()) {
                case BottomSheetBehavior.STATE_HIDDEN:
                    binding.captionParent.fullScroll(ScrollView.FOCUS_UP); // reset scroll position
                    if (binding.playerControls.getRoot().getVisibility() == View.VISIBLE) {
                        hidePlayerControls();
                    }
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return;
                case BottomSheetBehavior.STATE_COLLAPSED:
                case BottomSheetBehavior.STATE_EXPANDED:
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    return;
                case BottomSheetBehavior.STATE_DRAGGING:
                case BottomSheetBehavior.STATE_HALF_EXPANDED:
                case BottomSheetBehavior.STATE_SETTLING:
                default:
            }
        });
        binding.captionToggle.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.caption));
            return true;
        });
        if (sharedProfilePicElement == null || sharedMainPostElement == null) {
            binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (bottomSheetBehavior == null) return;
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        }
        binding.share.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.share));
            return true;
        });
        binding.share.setOnClickListener(v -> {
            final boolean isPrivate = feedModel.getProfileModel().isPrivate();
            if (isPrivate)
                Toast.makeText(context, R.string.share_private_post, Toast.LENGTH_LONG).show();
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/" + feedModel.getShortCode());
            startActivity(Intent.createChooser(sharingIntent,
                                               isPrivate ? getString(R.string.share_private_post) : getString(R.string.share_public_post)));
        });
    }

    private void setupCounts() {
        try {
            final int commentsCount = (int) feedModel.getCommentsCount();
            final String commentsString = getResources().getQuantityString(R.plurals.comments_count, commentsCount, commentsCount);
            binding.commentsCount.setText(commentsString);
            final int likesCount = (int) feedModel.getLikesCount();
            final String likesString = getResources().getQuantityString(R.plurals.likes_count, likesCount, likesCount);
            binding.likesCount.setText(likesString);
        } catch (IllegalStateException ignored) {}
    }

    private void setupPostTypeLayout() {
        switch (feedModel.getItemType()) {
            case MEDIA_TYPE_IMAGE:
                setupPostImage();
                break;
            case MEDIA_TYPE_SLIDER:
                setupSlider();
                break;
            case MEDIA_TYPE_VIDEO:
                setupVideo();
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPostImage() {
        binding.videoPost.root.setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.GONE);
        binding.playerControlsToggle.setVisibility(View.GONE);
        binding.playerControls.getRoot().setVisibility(View.GONE);
        binding.mediaCounter.setVisibility(View.GONE);
        binding.postImage.setVisibility(View.VISIBLE);
        if (!wasPaused && sharedMainPostElement != null) {
            binding.postImage.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
            addSharedElement(sharedMainPostElement, binding.postImage);
        }
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(feedModel.getDisplayUrl()))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .build();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setLowResImageRequest(ImageRequest.fromUri(feedModel.getThumbnailUrl()))
                .setImageRequest(requestBuilder)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(final String id, final Throwable throwable) {
                        startPostponedEnterTransition();
                    }

                    @Override
                    public void onFinalImageSet(final String id,
                                                final ImageInfo imageInfo,
                                                final Animatable animatable) {
                        startPostponedEnterTransition();
                    }
                })
                .build();
        binding.postImage.setController(controller);
        binding.postImage.setOnClickListener(v -> toggleDetails());
        final AnimatedZoomableController zoomableController = AnimatedZoomableController.newInstance();
        zoomableController.setMaxScaleFactor(3f);
        binding.postImage.setZoomableController(zoomableController);
        binding.postImage.setAllowTouchInterceptionWhileZoomed(true);
        binding.postImage.setOnVerticalDragListener(onVerticalDragListener);
    }

    private void setupSlider() {
        binding.postImage.setVisibility(View.GONE);
        binding.videoPost.root.setVisibility(View.GONE);
        binding.playerControlsToggle.setVisibility(View.GONE);
        binding.playerControls.getRoot().setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.VISIBLE);
        binding.mediaCounter.setVisibility(View.VISIBLE);
        if (!wasPaused && sharedMainPostElement != null) {
            addSharedElement(sharedMainPostElement, binding.sliderParent);
        }
        sliderItemsAdapter = new SliderItemsAdapter(onVerticalDragListener, binding.playerControls, true, new SliderCallbackAdapter() {
            @Override
            public void onThumbnailLoaded(final int position) {
                if (position != 0) return;
                startPostponedEnterTransition();
            }

            @Override
            public void onItemClicked(final int position) {
                toggleDetails();
            }

            @Override
            public void onPlayerPlay(final int position) {
                if (!detailsVisible) return;
                toggleDetails();
                showPlayerControls();
            }

            @Override
            public void onPlayerPause(final int position) {
                if (detailsVisible) return;
                toggleDetails();
            }
        });
        binding.sliderParent.setAdapter(sliderItemsAdapter);
        if (sliderPosition >= 0 && sliderPosition < feedModel.getSliderItems().size()) {
            binding.sliderParent.setCurrentItem(sliderPosition);
        }
        binding.sliderParent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            int prevPosition = -1;

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                if (prevPosition != -1) {
                    final View view = binding.sliderParent.getChildAt(0);
                    if (view instanceof RecyclerView) {
                        pausePlayerAtPosition(prevPosition, (RecyclerView) view);
                        pausePlayerAtPosition(position, (RecyclerView) view);
                    }
                }
                if (positionOffset == 0) {
                    prevPosition = position;
                }
            }

            @Override
            public void onPageSelected(final int position) {
                final int size = feedModel.getSliderItems().size();
                if (position < 0 || position >= size) return;
                sliderPosition = position;
                final String text = (position + 1) + "/" + size;
                binding.mediaCounter.setText(text);
                final PostChild postChild = feedModel.getSliderItems().get(position);
                if (postChild.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                    enablePlayerControls(true);
                    return;
                }
                enablePlayerControls(false);
            }

            private void pausePlayerAtPosition(final int position, final RecyclerView view) {
                final RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof SliderVideoViewHolder) {
                    ((SliderVideoViewHolder) viewHolder).pause();
                }
            }
        });
        final String text = "1/" + feedModel.getSliderItems().size();
        binding.mediaCounter.setText(text);
        sliderItemsAdapter.submitList(feedModel.getSliderItems());
    }

    private void releaseAllSliderPlayers() {
        if (binding.sliderParent.getVisibility() != View.VISIBLE) return;
        final View view = binding.sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        final int itemCount = sliderItemsAdapter.getItemCount();
        for (int position = itemCount - 1; position >= 0; position--) {
            final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
            if (!(viewHolder instanceof SliderVideoViewHolder)) continue;
            ((SliderVideoViewHolder) viewHolder).releasePlayer();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupVideo() {
        binding.postImage.setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.GONE);
        binding.mediaCounter.setVisibility(View.GONE);
        // binding.playerControls.getRoot().setVisibility(View.VISIBLE);
        if (!wasPaused && sharedMainPostElement != null) {
            final GenericDraweeHierarchy hierarchy = binding.videoPost.thumbnail.getHierarchy();
            hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
            addSharedElement(sharedMainPostElement, binding.videoPost.thumbnailParent);
        }
        binding.videoPost.root.setVisibility(View.VISIBLE);
        final VerticalDragHelper thumbnailVerticalDragHelper = new VerticalDragHelper(binding.videoPost.thumbnailParent);
        final VerticalDragHelper playerVerticalDragHelper = new VerticalDragHelper(binding.videoPost.playerView);
        thumbnailVerticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
        playerVerticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
        enablePlayerControls(true);
        binding.videoPost.thumbnailParent.setOnTouchListener((v, event) -> {
            final boolean onDragTouch = thumbnailVerticalDragHelper.onDragTouch(event);
            if (onDragTouch) {
                return true;
            }
            return thumbnailVerticalDragHelper.onGestureTouchEvent(event);
        });
        binding.videoPost.playerView.setOnTouchListener((v, event) -> {
            final boolean onDragTouch = playerVerticalDragHelper.onDragTouch(event);
            if (onDragTouch) {
                return true;
            }
            return playerVerticalDragHelper.onGestureTouchEvent(event);
        });
        binding.videoPost.playerView.setOnClickListener(v -> toggleDetails());
        final float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {
            @Override
            public void onThumbnailLoaded() {
                startPostponedEnterTransition();
            }

            @Override
            public void onPlayerViewLoaded() {
                binding.playerControls.getRoot().setVisibility(View.VISIBLE);
                final ViewGroup.LayoutParams layoutParams = binding.videoPost.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils.getResultingHeight(requiredWidth, feedModel.getImageHeight(), feedModel.getImageWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                binding.videoPost.playerView.requestLayout();
            }

            @Override
            public void onPlay() {
                if (detailsVisible) {
                    new Handler().postDelayed(() -> toggleDetails(), DETAILS_HIDE_DELAY_MILLIS);
                }
            }
        };
        final float aspectRatio = (float) feedModel.getImageWidth() / feedModel.getImageHeight();
        videoPlayerViewHelper = new VideoPlayerViewHelper(
                binding.getRoot().getContext(),
                binding.videoPost,
                feedModel.getDisplayUrl(),
                vol,
                aspectRatio,
                feedModel.getThumbnailUrl(),
                true,
                binding.playerControls,
                videoPlayerCallback);
    }

    private void enablePlayerControls(final boolean enable) {
        video = enable;
        if (enable) {
            binding.playerControlsToggle.setVisibility(View.VISIBLE);
            binding.playerControlsToggle.setOnClickListener(v -> {
                final int visibility = binding.playerControls.getRoot().getVisibility();
                if (visibility == View.GONE) {
                    showPlayerControls();
                    return;
                }
                hidePlayerControls();
            });
            return;
        }
        binding.playerControlsToggle.setVisibility(View.GONE);
        hidePlayerControls();
    }

    private void hideCaption() {
        if (bottomSheetBehavior == null) return;
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showPlayerControls() {
        hideCaption();

        // previously invisible view
        View view = binding.playerControls.getRoot();
        // get the center for the clipping circle
        int cx = view.getWidth() / 2;
        // int cy = view.getHeight() / 2;
        int cy = view.getHeight();

        // get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        // create the animator for this view (the start radius is zero)
        Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius);

        // make the view visible and start the animation
        view.setVisibility(View.VISIBLE);
        anim.start();

    }

    private void hidePlayerControls() {
        // previously visible view
        final View view = binding.playerControls.getRoot();

        // get the center for the clipping circle
        int cx = view.getWidth() / 2;
        // int cy = view.getHeight() / 2;
        int cy = view.getHeight();

        // get the initial radius for the clipping circle
        float initialRadius = (float) Math.hypot(cx, cy);

        // create the animation (the final radius is zero)
        Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(View.GONE);
            }
        });

        // start the animation
        anim.start();
    }

    private void toggleDetails() {
        binding.getRoot().post(() -> {
            TransitionManager.beginDelayedTransition(binding.getRoot());
            if (detailsVisible) {
                detailsVisible = false;
                binding.profilePic.setVisibility(View.GONE);
                binding.title.setVisibility(View.GONE);
                binding.isVerified.setVisibility(View.GONE);
                binding.righttitle.setVisibility(View.GONE);
                binding.topBg.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(binding.subtitle.getText())) {
                    binding.subtitle.setVisibility(View.GONE);
                }
                binding.captionParent.setVisibility(View.GONE);
                binding.bottomBg.setVisibility(View.GONE);
                binding.likesCount.setVisibility(View.GONE);
                binding.commentsCount.setVisibility(View.GONE);
                binding.date.setVisibility(View.GONE);
                binding.comment.setVisibility(View.GONE);
                binding.captionToggle.setVisibility(View.GONE);
                binding.playerControlsToggle.setVisibility(View.GONE);
                binding.like.setVisibility(View.GONE);
                binding.save.setVisibility(View.GONE);
                binding.share.setVisibility(View.GONE);
                binding.download.setVisibility(View.GONE);
                binding.mediaCounter.setVisibility(View.GONE);
                wasControlsVisible = binding.playerControls.getRoot().getVisibility() == View.VISIBLE;
                if (wasControlsVisible) {
                    hidePlayerControls();
                }
                return;
            }
            binding.profilePic.setVisibility(View.VISIBLE);
            binding.title.setVisibility(View.VISIBLE);
            binding.isVerified.setVisibility(feedModel.getProfileModel().isVerified() ? View.VISIBLE : View.GONE);
            binding.righttitle.setVisibility(View.VISIBLE);
            binding.topBg.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(binding.subtitle.getText())) {
                binding.subtitle.setVisibility(View.VISIBLE);
            }
            binding.captionParent.setVisibility(View.VISIBLE);
            binding.bottomBg.setVisibility(View.VISIBLE);
            binding.likesCount.setVisibility(View.VISIBLE);
            binding.commentsCount.setVisibility(View.VISIBLE);
            binding.date.setVisibility(View.VISIBLE);
            binding.captionToggle.setVisibility(View.VISIBLE);
            binding.download.setVisibility(View.VISIBLE);
            binding.share.setVisibility(View.VISIBLE);
            if (isLoggedIn) {
                binding.comment.setVisibility(View.VISIBLE);
                binding.like.setVisibility(View.VISIBLE);
                binding.save.setVisibility(View.VISIBLE);
            }
            if (video) {
                binding.playerControlsToggle.setVisibility(View.VISIBLE);
            }
            if (wasControlsVisible) {
                showPlayerControls();
            }
            if (feedModel.getItemType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                binding.mediaCounter.setVisibility(View.VISIBLE);
            }
            detailsVisible = true;
        });
    }

    private void animateY(final View v,
                          final float finalY,
                          final int duration,
                          final AnimatorListenerAdapter listener) {
        v.animate()
         .y(finalY)
         .setDuration(duration)
         .setListener(listener).start();
    }

    private void navigateToProfile(final String username) {
        final NavController navController = getNavController();
        if (navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("username", username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    @Nullable
    private NavController getNavController() {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            Log.e(TAG, "navigateToProfile", e);
        }
        return navController;
    }
}