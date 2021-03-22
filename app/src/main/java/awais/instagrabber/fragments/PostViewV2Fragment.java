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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.PermissionChecker;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
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
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.SliderCallbackAdapter;
import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.adapters.viewholder.SliderVideoViewHolder;
import awais.instagrabber.customviews.SharedElementTransitionDialogFragment;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.databinding.DialogPostViewBinding;
import awais.instagrabber.dialogs.EditTextDialogFragment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.VideoVersion;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.PostViewV2ViewModel;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.fragments.HashTagFragment.ARG_HASHTAG;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class PostViewV2Fragment extends SharedElementTransitionDialogFragment implements EditTextDialogFragment.EditTextDialogFragmentCallback {
    private static final String TAG = "PostViewV2Fragment";
    private static final int DETAILS_HIDE_DELAY_MILLIS = 2000;
    private static final String ARG_MEDIA = "media";
    private static final String ARG_SLIDER_POSITION = "position";
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;

    // private Media media;
    private View sharedProfilePicElement;
    private View sharedMainPostElement;
    private DialogPostViewBinding binding;
    // private MediaService mediaService;
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
    private boolean hasBeenToggled = false;
    private PostViewV2ViewModel viewModel;
    private PopupMenu optionsPopup;
    private EditTextDialogFragment editTextDialogFragment;
    private boolean wasDeleted;
    private MutableLiveData<Object> backStackSavedStateResultLiveData;
    private OnDeleteListener onDeleteListener;

    private final Observer<Object> backStackSavedStateObserver = result -> {
        if (result == null) return;
        if (result instanceof String) {
            final String collection = (String) result;
            handleSaveUnsaveResourceLiveData(viewModel.toggleSave(collection, viewModel.getMedia().hasViewerSaved()));
        }
        // clear result
        backStackSavedStateResultLiveData.postValue(null);
    };
    private final GestureDetector.OnGestureListener videoPlayerViewGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            binding.videoPost.playerView.performClick();
            return true;
        }
    };

    // private final VerticalDragHelper.OnVerticalDragListener onVerticalDragListener = new VerticalDragHelper.OnVerticalDragListener() {
    //
    //     @Override
    //     public void onDrag(final float dY) {
    //         // allow the view to be draggable
    //         final ConstraintLayout v = binding.getRoot();
    //         final float finalY = v.getY() + dY;
    //         animateY(v, finalY, 0, null);
    //     }
    //
    //     @Override
    //     public void onDragEnd() {
    //         // animate and dismiss if user drags the view more that 30% of the view
    //         if (Math.abs(binding.getRoot().getY()) > Utils.displayMetrics.heightPixels * 0.25) {
    //             animateAndDismiss(binding.getRoot().getY() < 0 ? 1 : -1);
    //             return;
    //         }
    //         // animate back the view to proper position
    //         animateY(binding.getRoot(), 0, 200, null);
    //     }
    //
    //     @Override
    //     public void onFling(final double flingVelocity) {
    //         // animate and dismiss if user flings up/down
    //         animateAndDismiss(flingVelocity > 0 ? 1 : -1);
    //     }
    //
    //     private void animateAndDismiss(final int direction) {
    //         final int height = binding.getRoot().getHeight();
    //         final int finalYDist = height + Utils.getStatusBarHeight(context);
    //         // less than 0 means up direction, else down
    //         final int finalY = direction > 0 ? -finalYDist : finalYDist;
    //         animateY(binding.getRoot(), finalY, 200, new AnimatorListenerAdapter() {
    //             @Override
    //             public void onAnimationEnd(final Animator animation) {
    //                 dismiss();
    //             }
    //         });
    //     }
    // };

    public void setOnShowListener(final DialogInterface.OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }

    public void setOnDeleteListener(final OnDeleteListener onDeleteListener) {
        if (onDeleteListener == null) return;
        this.onDeleteListener = onDeleteListener;
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    public static class Builder {
        private final Media feedModel;
        private View profilePicElement;
        private View mainPostElement;
        private int position;

        public Builder setSharedProfilePicElement(final View profilePicElement) {
            this.profilePicElement = profilePicElement;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
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

        public Builder(final Media feedModel) {
            this.feedModel = feedModel;
        }
    }

    private static PostViewV2Fragment newInstance(final Media feedModel,
                                                  final View profilePicElement,
                                                  final View mainPostElement,
                                                  final int position) {
        final PostViewV2Fragment f = new PostViewV2Fragment(profilePicElement, mainPostElement);
        final Bundle args = new Bundle();
        args.putSerializable(ARG_MEDIA, feedModel);
        if (position >= 0) {
            args.putInt(ARG_SLIDER_POSITION, position);
        }
        f.setArguments(args);
        return f;
    }

    public static Builder builder(final Media feedModel) {
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
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.PostViewV2Style);
        viewModel = new ViewModelProvider(this).get(PostViewV2ViewModel.class);
        captionState = settingsHelper.getBoolean(Constants.SHOW_CAPTIONS) ?
                       BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_HIDDEN;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = DialogPostViewBinding.inflate(inflater, container, false);
        final ConstraintLayout root = binding.getRoot();
        final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        };
        root.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
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
        final Media media = viewModel.getMedia();
        if (media == null) return;
        switch (media.getMediaType()) {
            case MEDIA_TYPE_VIDEO:
                if (videoPlayerViewHelper != null) {
                    videoPlayerViewHelper.pause();
                }
                return;
            case MEDIA_TYPE_SLIDER:
                if (sliderItemsAdapter != null) {
                    pauseSliderPlayer();
                }
            default:
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final NavController navController = NavHostFragment.findNavController(this);
        final NavBackStackEntry backStackEntry = navController.getCurrentBackStackEntry();
        if (backStackEntry != null) {
            backStackSavedStateResultLiveData = backStackEntry.getSavedStateHandle().getLiveData("collection");
            backStackSavedStateResultLiveData.observe(getViewLifecycleOwner(), backStackSavedStateObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final Media media = viewModel.getMedia();
        if (media == null) return;
        switch (media.getMediaType()) {
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
        final Media media = viewModel.getMedia();
        if (media == null) return;
        if (media.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
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
                bottomSheetBehavior.setState(captionState);
            }
            return;
        }
        if (destView == binding.sliderParent) {
            binding.sliderParent.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                   ViewGroup.LayoutParams.MATCH_PARENT));
            binding.sliderParent.requestLayout();
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(captionState);
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
                bottomSheetBehavior.setState(captionState);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERM_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            DownloadUtils.showDownloadDialog(context, viewModel.getMedia(), sliderPosition);
        }
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            dismiss();
            return;
        }
        final Serializable feedModelSerializable = arguments.getSerializable(ARG_MEDIA);
        if (feedModelSerializable == null) {
            Log.e(TAG, "onCreate: feedModelSerializable is null");
            dismiss();
            return;
        }
        if (!(feedModelSerializable instanceof Media)) {
            dismiss();
            return;
        }
        final Media media = (Media) feedModelSerializable;
        if (media.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
            sliderPosition = arguments.getInt(ARG_SLIDER_POSITION, 0);
        }
        viewModel.setMedia(media);
        if (!wasPaused && (sharedProfilePicElement != null || sharedMainPostElement != null)) {
            binding.getRoot().getBackground().mutate().setAlpha(0);
        }
        setProfilePicSharedElement();
        setupCaptionBottomSheet();
        setupCommonActions();
        setObservers();
    }

    private void setObservers() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                binding.userDetailsGroup.setVisibility(View.GONE);
                return;
            }
            binding.userDetailsGroup.setVisibility(View.VISIBLE);
            binding.getRoot().post(() -> setupProfilePic(user));
            binding.getRoot().post(() -> setupTitles(user));
        });
        viewModel.getCaption().observe(getViewLifecycleOwner(), caption -> binding.getRoot().post(() -> setupCaption(caption)));
        viewModel.getLocation().observe(getViewLifecycleOwner(), location -> binding.getRoot().post(() -> setupLocation(location)));
        viewModel.getDate().observe(getViewLifecycleOwner(), date -> binding.getRoot().post(() -> {
            if (date == null) {
                binding.date.setVisibility(View.GONE);
                return;
            }
            binding.date.setVisibility(View.VISIBLE);
            binding.date.setText(date);
        }));
        viewModel.getLikeCount().observe(getViewLifecycleOwner(), count -> {
            final long safeCount = getSafeCount(count);
            final String likesString = getResources().getQuantityString(R.plurals.likes_count, (int) safeCount, safeCount);
            binding.likesCount.setText(likesString);
        });
        if (!viewModel.getMedia().isCommentsDisabled()) {
            viewModel.getCommentCount().observe(getViewLifecycleOwner(), count -> {
                final long safeCount = getSafeCount(count);
                final String likesString = getResources().getQuantityString(R.plurals.comments_count, (int) safeCount, safeCount);
                binding.commentsCount.setText(likesString);
            });
        }
        viewModel.getViewCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) {
                binding.viewsCount.setVisibility(View.GONE);
                return;
            }
            binding.viewsCount.setVisibility(View.VISIBLE);
            final long safeCount = getSafeCount(count);
            final String viewString = getResources().getQuantityString(R.plurals.views_count, (int) safeCount, safeCount);
            binding.viewsCount.setText(viewString);
        });
        viewModel.getType().observe(getViewLifecycleOwner(), this::setupPostTypeLayout);
        viewModel.getLiked().observe(getViewLifecycleOwner(), this::setLikedResources);
        viewModel.getSaved().observe(getViewLifecycleOwner(), this::setSavedResources);
        viewModel.getOptions().observe(getViewLifecycleOwner(), options -> binding.getRoot().post(() -> {
            setupOptions(options != null && !options.isEmpty());
            createOptionsPopupMenu();
        }));
    }

    @NonNull
    private Long getSafeCount(final Long count) {
        Long safeCount = count;
        if (count == null) {
            safeCount = 0L;
        }
        return safeCount;
    }

    private void setupCaptionBottomSheet() {
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
    }

    private void setupCommonActions() {
        setupLike();
        setupSave();
        setupDownload();
        setupComment();
        setupShare();
    }

    private void setupComment() {
        if (!viewModel.hasPk() || viewModel.getMedia().isCommentsDisabled()) {
            binding.comment.setVisibility(View.GONE);
            binding.commentsCount.setVisibility(View.GONE);
            return;
        }
        binding.comment.setVisibility(View.VISIBLE);
        binding.comment.setOnClickListener(v -> {
            final Media media = viewModel.getMedia();
            final User user = media.getUser();
            if (user == null) return;
            final NavController navController = getNavController();
            if (navController == null) return;
            final Bundle bundle = new Bundle();
            bundle.putString("shortCode", media.getCode());
            bundle.putString("postId", media.getPk());
            bundle.putLong("postUserId", user.getPk());
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
                DownloadUtils.showDownloadDialog(context, viewModel.getMedia(), sliderPosition);
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
        final boolean likableMedia = viewModel.hasPk() /*&& viewModel.getMedia().isCommentLikesEnabled()*/;
        if (!likableMedia) {
            binding.like.setVisibility(View.GONE);
            binding.likesCount.setVisibility(View.GONE);
            return;
        }
        if (!viewModel.isLoggedIn()) {
            binding.like.setVisibility(View.GONE);
            return;
        }
        binding.like.setOnClickListener(v -> {
            v.setEnabled(false);
            handleLikeUnlikeResourceLiveData(viewModel.toggleLike());
        });
        binding.like.setOnLongClickListener(v -> {
            final NavController navController = getNavController();
            if (navController != null && viewModel.isLoggedIn()) {
                final Bundle bundle = new Bundle();
                bundle.putString("postId", viewModel.getMedia().getPk());
                bundle.putBoolean("isComment", false);
                navController.navigate(R.id.action_global_likesViewerFragment, bundle);
                return true;
            }
            return true;
        });
    }

    private void handleLikeUnlikeResourceLiveData(@NonNull final LiveData<Resource<Object>> resource) {
        resource.observe(getViewLifecycleOwner(), value -> {
            switch (value.status) {
                case SUCCESS:
                    binding.like.setEnabled(true);
                    break;
                case ERROR:
                    binding.like.setEnabled(true);
                    unsuccessfulLike();
                    break;
                case LOADING:
                    binding.like.setEnabled(false);
                    break;
            }
        });

    }

    private void unsuccessfulLike() {
        final int errorTextResId;
        final Media media = viewModel.getMedia();
        if (!media.hasLiked()) {
            Log.e(TAG, "like unsuccessful!");
            errorTextResId = R.string.like_unsuccessful;
        } else {
            Log.e(TAG, "unlike unsuccessful!");
            errorTextResId = R.string.unlike_unsuccessful;
        }
        final Snackbar snackbar = Snackbar.make(binding.getRoot(), errorTextResId, BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, null);
        snackbar.show();
    }

    private void setLikedResources(final boolean liked) {
        final int iconResource;
        final int tintResource;
        if (liked) {
            iconResource = R.drawable.ic_like;
            tintResource = R.color.red_600;
            // textResId = R.string.unlike_without_count;
        } else {
            iconResource = R.drawable.ic_not_liked;
            tintResource = R.color.white;
            // textResId = R.string.like_without_count;
        }
        binding.like.setIconResource(iconResource);
        binding.like.setIconTintResource(tintResource);
    }

    private void setupSave() {
        if (!viewModel.isLoggedIn() || !viewModel.hasPk() || !viewModel.getMedia().canViewerSave()) {
            binding.save.setVisibility(View.GONE);
            return;
        }
        binding.save.setOnClickListener(v -> {
            binding.save.setEnabled(false);
            handleSaveUnsaveResourceLiveData(viewModel.toggleSave());
        });
        binding.save.setOnLongClickListener(v -> {
            final NavController navController = NavHostFragment.findNavController(this);
            final Bundle bundle = new Bundle();
            bundle.putBoolean("isSaving", true);
            navController.navigate(R.id.action_global_savedCollectionsFragment, bundle);
            return true;
        });
    }

    private void handleSaveUnsaveResourceLiveData(@NonNull final LiveData<Resource<Object>> resource) {
        resource.observe(getViewLifecycleOwner(), value -> {
            if (value == null) return;
            switch (value.status) {
                case SUCCESS:
                    binding.save.setEnabled(true);
                    break;
                case ERROR:
                    binding.save.setEnabled(true);
                    unsuccessfulSave();
                    break;
                case LOADING:
                    binding.save.setEnabled(false);
                    break;
            }
        });
    }

    private void unsuccessfulSave() {
        final int errorTextResId;
        final Media media = viewModel.getMedia();
        if (!media.hasViewerSaved()) {
            Log.e(TAG, "save unsuccessful!");
            errorTextResId = R.string.save_unsuccessful;
        } else {
            Log.e(TAG, "save remove unsuccessful!");
            errorTextResId = R.string.save_remove_unsuccessful;
        }
        final Snackbar snackbar = Snackbar.make(binding.getRoot(), errorTextResId, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, null);
        snackbar.show();
    }

    private void setSavedResources(final boolean saved) {
        final int iconResource;
        final int tintResource;
        if (saved) {
            iconResource = R.drawable.ic_class_24;
            tintResource = R.color.blue_700;
            // textResId = R.string.saved;
        } else {
            iconResource = R.drawable.ic_outline_class_24;
            tintResource = R.color.white;
            // textResId = R.string.save;
        }
        binding.save.setIconResource(iconResource);
        binding.save.setIconTintResource(tintResource);
    }

    private void setProfilePicSharedElement() {
        if (!wasPaused && sharedProfilePicElement != null) {
            addSharedElement(sharedProfilePicElement, binding.profilePic);
        }
    }

    private void setupProfilePic(final User user) {
        if (user == null) {
            binding.profilePic.setImageURI((String) null);
            return;
        }
        final String uri = user.getProfilePicUrl();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setUri(uri)
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
        binding.profilePic.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
    }

    private void setupTitles(final User user) {
        if (user == null) {
            binding.title.setVisibility(View.GONE);
            binding.subtitle.setVisibility(View.GONE);
            return;
        }
        binding.subtitle.setText(user.getFullName());
        setUsername(user);
        binding.title.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
        binding.subtitle.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
    }

    private void setUsername(final User user) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
        final int drawableSize = Utils.convertDpToPx(24);
        if (user.isVerified()) {
            final Drawable verifiedDrawable = AppCompatResources.getDrawable(context, R.drawable.verified);
            VerticalImageSpan verifiedSpan = null;
            if (verifiedDrawable != null) {
                final Drawable drawable = verifiedDrawable.mutate();
                drawable.setBounds(0, 0, drawableSize, drawableSize);
                verifiedSpan = new VerticalImageSpan(drawable);
            }
            try {
                if (verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (Exception e) {
                Log.e(TAG, "setUsername: ", e);
            }
        }
        binding.title.setText(sb);
    }

    private void setupCaption(final Caption caption) {
        if (caption == null || TextUtils.isEmpty(caption.getText())) {
            binding.caption.setVisibility(View.GONE);
            binding.translate.setVisibility(View.GONE);
            binding.captionToggle.setVisibility(View.GONE);
            return;
        }
        final String postCaption = caption.getText();
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
        binding.translate.setOnClickListener(v -> handleTranslateCaptionResource(viewModel.translateCaption()));
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
    }

    private void handleTranslateCaptionResource(@NonNull final LiveData<Resource<String>> data) {
        data.observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case SUCCESS:
                    binding.translate.setVisibility(View.GONE);
                    binding.caption.setText(resource.data);
                    break;
                case ERROR:
                    binding.translate.setEnabled(true);
                    String message = resource.message;
                    if (TextUtils.isEmpty(resource.message)) {
                        message = getString(R.string.downloader_unknown_error);
                    }
                    final Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    binding.translate.setEnabled(false);
                    break;
            }
        });
    }

    private void setupLocation(final Location location) {
        if (location == null || !detailsVisible) {
            binding.location.setVisibility(View.GONE);
            return;
        }
        final String locationName = location.getName();
        if (TextUtils.isEmpty(locationName)) return;
        binding.location.setText(locationName);
        binding.location.setVisibility(View.VISIBLE);
        binding.location.setOnClickListener(v -> {
            final NavController navController = getNavController();
            if (navController == null) return;
            final Bundle bundle = new Bundle();
            bundle.putLong("locationId", location.getPk());
            navController.navigate(R.id.action_global_locationFragment, bundle);
        });
    }

    private void setupShare() {
        if (!viewModel.hasPk()) {
            binding.share.setVisibility(View.GONE);
            return;
        }
        binding.share.setVisibility(View.VISIBLE);
        binding.share.setOnLongClickListener(v -> {
            Utils.displayToastAboveView(context, v, getString(R.string.share));
            return true;
        });
        binding.share.setOnClickListener(v -> {
            final Media media = viewModel.getMedia();
            final User profileModel = media.getUser();
            if (profileModel == null) return;
            final boolean isPrivate = profileModel.isPrivate();
            if (isPrivate) {
                // is this necessary?
                Toast.makeText(context, R.string.share_private_post, Toast.LENGTH_LONG).show();
            }
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/" + media.getCode());
            startActivity(Intent.createChooser(sharingIntent,
                                               isPrivate ? getString(R.string.share_private_post) : getString(R.string.share_public_post)));
        });
    }

    private void setupPostTypeLayout(final MediaItemType type) {
        if (type == null) return;
        switch (type) {
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
        final Media media = viewModel.getMedia();
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(ResponseBodyUtils.getImageUrl(media)))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .build();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setLowResImageRequest(ImageRequest.fromUri(ResponseBodyUtils.getThumbUrl(media)))
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
        // binding.postImage.setOnClickListener(v -> toggleDetails());
        final AnimatedZoomableController zoomableController = AnimatedZoomableController.newInstance();
        zoomableController.setMaxScaleFactor(3f);
        binding.postImage.setZoomableController(zoomableController);
        binding.postImage.setTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(final MotionEvent e) {
                toggleDetails();
                return true;
            }
        });
        binding.postImage.setAllowTouchInterceptionWhileZoomed(true);
        // binding.postImage.setOnVerticalDragListener(onVerticalDragListener);
    }

    private void setupSlider() {
        final Media media = viewModel.getMedia();
        binding.postImage.setVisibility(View.GONE);
        binding.videoPost.root.setVisibility(View.GONE);
        binding.playerControlsToggle.setVisibility(View.GONE);
        binding.playerControls.getRoot().setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.VISIBLE);
        binding.mediaCounter.setVisibility(View.VISIBLE);
        if (!wasPaused && sharedMainPostElement != null) {
            addSharedElement(sharedMainPostElement, binding.sliderParent);
        }
        final boolean hasVideo = media.getCarouselMedia()
                                      .stream()
                                      .anyMatch(postChild -> postChild.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO);
        if (hasVideo) {
            final View child = binding.sliderParent.getChildAt(0);
            if (child instanceof RecyclerView) {
                ((RecyclerView) child).setItemViewCacheSize(media.getCarouselMedia().size());
                ((RecyclerView) child).addRecyclerListener(holder -> {
                    if (holder instanceof SliderVideoViewHolder) {
                        ((SliderVideoViewHolder) holder).releasePlayer();
                    }
                });
            }
        }
        sliderItemsAdapter = new SliderItemsAdapter(null, binding.playerControls, true, new SliderCallbackAdapter() {
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
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                if (!detailsVisible || hasBeenToggled) return;
                showPlayerControls();
            }

            @Override
            public void onPlayerPause(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
                if (detailsVisible || hasBeenToggled) return;
                toggleDetails();
            }

            @Override
            public void onPlayerRelease(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }
        });
        binding.sliderParent.setAdapter(sliderItemsAdapter);
        if (sliderPosition >= 0 && sliderPosition < media.getCarouselMedia().size()) {
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
                final int size = media.getCarouselMedia().size();
                if (position < 0 || position >= size) return;
                sliderPosition = position;
                final String text = (position + 1) + "/" + size;
                binding.mediaCounter.setText(text);
                final Media childMedia = media.getCarouselMedia().get(position);
                final View view = binding.sliderParent.getChildAt(0);
                if (prevPosition != -1) {
                    if (view instanceof RecyclerView) {
                        final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(prevPosition);
                        if (viewHolder instanceof SliderVideoViewHolder) {
                            ((SliderVideoViewHolder) viewHolder).removeCallbacks();
                        }
                    }
                }
                if (childMedia.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                    if (view instanceof RecyclerView) {
                        final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
                        if (viewHolder instanceof SliderVideoViewHolder) {
                            ((SliderVideoViewHolder) viewHolder).resetPlayerTimeline();
                        }
                    }
                    enablePlayerControls(true);
                    viewModel.setViewCount(childMedia.getViewCount());
                    return;
                }
                viewModel.setViewCount(null);
                enablePlayerControls(false);
            }

            private void pausePlayerAtPosition(final int position, final RecyclerView view) {
                final RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof SliderVideoViewHolder) {
                    ((SliderVideoViewHolder) viewHolder).pause();
                }
            }
        });
        final String text = "1/" + media.getCarouselMedia().size();
        binding.mediaCounter.setText(text);
        sliderItemsAdapter.submitList(media.getCarouselMedia());
    }

    private void pauseSliderPlayer() {
        if (binding.sliderParent.getVisibility() != View.VISIBLE) return;
        final int currentItem = binding.sliderParent.getCurrentItem();
        final View view = binding.sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(currentItem);
        if (!(viewHolder instanceof SliderVideoViewHolder)) return;
        ((SliderVideoViewHolder) viewHolder).pause();
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
        final Media media = viewModel.getMedia();
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
        // final VerticalDragHelper thumbnailVerticalDragHelper = new VerticalDragHelper(binding.videoPost.thumbnailParent);
        // final VerticalDragHelper playerVerticalDragHelper = new VerticalDragHelper(binding.videoPost.playerView);
        // thumbnailVerticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
        // playerVerticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
        enablePlayerControls(true);
        // binding.videoPost.thumbnailParent.setOnTouchListener((v, event) -> {
        //     final boolean onDragTouch = thumbnailVerticalDragHelper.onDragTouch(event);
        //     if (onDragTouch) {
        //         return true;
        //     }
        //     return thumbnailVerticalDragHelper.onGestureTouchEvent(event);
        // });
        // binding.videoPost.playerView.setOnTouchListener((v, event) -> {
        //     final boolean onDragTouch = playerVerticalDragHelper.onDragTouch(event);
        //     if (onDragTouch) {
        //         return true;
        //     }
        //     return playerVerticalDragHelper.onGestureTouchEvent(event);
        // });
        binding.videoPost.playerView.setOnClickListener(v -> toggleDetails());
        final GestureDetector gestureDetector = new GestureDetector(context, videoPlayerViewGestureListener);
        binding.videoPost.playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        final float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {
            @Override
            public void onThumbnailLoaded() {
                startPostponedEnterTransition();
            }

            @Override
            public void onPlayerViewLoaded() {
                // binding.playerControls.getRoot().setVisibility(View.VISIBLE);
                final ViewGroup.LayoutParams layoutParams = binding.videoPost.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils
                        .getResultingHeight(requiredWidth, media.getOriginalHeight(), media.getOriginalWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                binding.videoPost.playerView.requestLayout();
            }

            @Override
            public void onPlay() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                if (detailsVisible) {
                    new Handler().postDelayed(() -> toggleDetails(), DETAILS_HIDE_DELAY_MILLIS);
                }
            }

            @Override
            public void onPause() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }

            @Override
            public void onRelease() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }
        };
        final float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
        String videoUrl = null;
        final List<VideoVersion> videoVersions = media.getVideoVersions();
        if (videoVersions != null && !videoVersions.isEmpty()) {
            final VideoVersion videoVersion = videoVersions.get(0);
            if (videoVersion != null) {
                videoUrl = videoVersion.getUrl();
            }
        }
        if (videoUrl != null) {
            videoPlayerViewHelper = new VideoPlayerViewHelper(
                    binding.getRoot().getContext(),
                    binding.videoPost,
                    videoUrl,
                    vol,
                    aspectRatio,
                    ResponseBodyUtils.getThumbUrl(media),
                    true,
                    binding.playerControls,
                    videoPlayerCallback);
        }
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
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }
        if (!ViewCompat.isAttachedToWindow(view)) {
            view.setVisibility(View.VISIBLE);
            return;
        }
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
        if (view.getVisibility() == View.GONE) {
            return;
        }
        if (!ViewCompat.isAttachedToWindow(view)) {
            view.setVisibility(View.GONE);
            return;
        }

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

    private void setupOptions(final Boolean show) {
        if (!show) {
            binding.options.setVisibility(View.GONE);
            return;
        }
        binding.options.setVisibility(View.VISIBLE);
        binding.options.setOnClickListener(v -> {
            if (optionsPopup == null) return;
            optionsPopup.show();
        });
    }

    private void createOptionsPopupMenu() {
        if (optionsPopup == null) {
            final ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
            optionsPopup = new PopupMenu(themeWrapper, binding.options);
        } else {
            optionsPopup.getMenu().clear();
        }
        optionsPopup.getMenuInflater().inflate(R.menu.post_view_menu, optionsPopup.getMenu());
        // final Menu menu = optionsPopup.getMenu();
        // final int size = menu.size();
        // for (int i = 0; i < size; i++) {
        //     final MenuItem item = menu.getItem(i);
        //     if (item == null) continue;
        //     if (options.contains(item.getItemId())) continue;
        //     menu.removeItem(item.getItemId());
        // }
        optionsPopup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.edit_caption) {
                showCaptionEditDialog();
                return true;
            }
            if (itemId == R.id.delete) {
                item.setEnabled(false);
                final LiveData<Resource<Object>> resourceLiveData = viewModel.delete();
                handleDeleteResource(resourceLiveData, item);
            }
            return true;
        });
    }

    private void handleDeleteResource(final LiveData<Resource<Object>> resourceLiveData, final MenuItem item) {
        if (resourceLiveData == null) return;
        resourceLiveData.observe(getViewLifecycleOwner(), new Observer<Resource<Object>>() {
            @Override
            public void onChanged(final Resource<Object> resource) {
                try {
                    switch (resource.status) {
                        case SUCCESS:
                            wasDeleted = true;
                            if (onDeleteListener != null) {
                                onDeleteListener.onDelete();
                            }
                            break;
                        case ERROR:
                            if (item != null) {
                                item.setEnabled(true);
                            }
                            final Snackbar snackbar = Snackbar.make(binding.getRoot(),
                                                                    R.string.delete_unsuccessful,
                                                                    Snackbar.LENGTH_INDEFINITE);
                            snackbar.setAction(R.string.ok, null);
                            snackbar.show();
                            break;
                        case LOADING:
                            if (item != null) {
                                item.setEnabled(false);
                            }
                            break;
                    }
                } finally {
                    resourceLiveData.removeObserver(this);
                }
            }
        });
    }

    private void showCaptionEditDialog() {
        final Caption caption = viewModel.getCaption().getValue();
        final String captionText = caption != null ? caption.getText() : null;
        editTextDialogFragment = EditTextDialogFragment
                .newInstance(R.string.edit_caption, R.string.confirm, R.string.cancel, captionText);
        editTextDialogFragment.show(getChildFragmentManager(), "edit_caption");
    }

    @Override
    public void onPositiveButtonClicked(final String caption) {
        handleEditCaptionResource(viewModel.updateCaption(caption));
        if (editTextDialogFragment == null) return;
        editTextDialogFragment.dismiss();
        editTextDialogFragment = null;
    }

    private void handleEditCaptionResource(final LiveData<Resource<Object>> updateCaption) {
        if (updateCaption == null) return;
        updateCaption.observe(getViewLifecycleOwner(), resource -> {
            final MenuItem item = optionsPopup.getMenu().findItem(R.id.edit_caption);
            switch (resource.status) {
                case SUCCESS:
                    if (item != null) {
                        item.setEnabled(true);
                    }
                    break;
                case ERROR:
                    if (item != null) {
                        item.setEnabled(true);
                    }
                    final Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.edit_unsuccessful, BaseTransientBottomBar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    if (item != null) {
                        item.setEnabled(false);
                    }
                    break;
            }
        });
    }

    @Override
    public void onNegativeButtonClicked() {
        if (editTextDialogFragment == null) return;
        editTextDialogFragment.dismiss();
        editTextDialogFragment = null;
    }

    private void toggleDetails() {
        hasBeenToggled = true;
        final Media media = viewModel.getMedia();
        binding.getRoot().post(() -> {
            TransitionManager.beginDelayedTransition(binding.getRoot());
            if (detailsVisible) {
                detailsVisible = false;
                if (media.getUser() != null) {
                    binding.profilePic.setVisibility(View.GONE);
                    binding.title.setVisibility(View.GONE);
                    binding.subtitle.setVisibility(View.GONE);
                    binding.topBg.setVisibility(View.GONE);
                }
                if (media.getLocation() != null) {
                    binding.location.setVisibility(View.GONE);
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
                binding.viewsCount.setVisibility(View.GONE);
                final List<Integer> options = viewModel.getOptions().getValue();
                if (options != null && !options.isEmpty()) {
                    binding.options.setVisibility(View.GONE);
                }
                wasControlsVisible = binding.playerControls.getRoot().getVisibility() == View.VISIBLE;
                if (wasControlsVisible) {
                    hidePlayerControls();
                }
                return;
            }
            if (media.getUser() != null) {
                binding.profilePic.setVisibility(View.VISIBLE);
                binding.title.setVisibility(View.VISIBLE);
                binding.subtitle.setVisibility(View.VISIBLE);
                binding.topBg.setVisibility(View.VISIBLE);
            }
            if (media.getLocation() != null) {
                binding.location.setVisibility(View.VISIBLE);
            }
            binding.bottomBg.setVisibility(View.VISIBLE);
            if (viewModel.hasPk()) {
                binding.likesCount.setVisibility(View.VISIBLE);
                binding.date.setVisibility(View.VISIBLE);
                binding.captionParent.setVisibility(View.VISIBLE);
                binding.captionToggle.setVisibility(View.VISIBLE);
                binding.share.setVisibility(View.VISIBLE);
            }
            if (viewModel.hasPk() && !viewModel.getMedia().isCommentsDisabled()) {
                binding.comment.setVisibility(View.VISIBLE);
                binding.commentsCount.setVisibility(View.VISIBLE);
            }
            binding.download.setVisibility(View.VISIBLE);
            final List<Integer> options = viewModel.getOptions().getValue();
            if (options != null && !options.isEmpty()) {
                binding.options.setVisibility(View.VISIBLE);
            }
            if (viewModel.isLoggedIn() && viewModel.hasPk()) {
                binding.like.setVisibility(View.VISIBLE);
                binding.save.setVisibility(View.VISIBLE);
            }
            if (video) {
                binding.playerControlsToggle.setVisibility(View.VISIBLE);
                binding.viewsCount.setVisibility(View.VISIBLE);
            }
            if (wasControlsVisible) {
                showPlayerControls();
            }
            if (media.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                binding.mediaCounter.setVisibility(View.VISIBLE);
            }
            detailsVisible = true;
        });
    }

    // private void animateY(final View v,
    //                       final float finalY,
    //                       final int duration,
    //                       final AnimatorListenerAdapter listener) {
    //     v.animate()
    //      .y(finalY)
    //      .setDuration(duration)
    //      .setListener(listener).start();
    // }

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

    public boolean wasDeleted() {
        return wasDeleted;
    }
}