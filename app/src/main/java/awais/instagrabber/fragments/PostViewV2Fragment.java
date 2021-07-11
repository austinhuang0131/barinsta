package awais.instagrabber.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowPositionRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonHighlightAnimation;
import com.skydoves.balloon.BalloonSizeSpec;
import com.skydoves.balloon.overlay.BalloonOverlayAnimation;
import com.skydoves.balloon.overlay.BalloonOverlayCircle;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.SliderCallbackAdapter;
import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.adapters.viewholder.SliderVideoViewHolder;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.customviews.drawee.DoubleTapGestureListener;
import awais.instagrabber.customviews.drawee.ZoomableController;
import awais.instagrabber.customviews.drawee.ZoomableDraweeView;
import awais.instagrabber.databinding.DialogPostViewBinding;
import awais.instagrabber.databinding.LayoutPostViewBottomBinding;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.dialogs.EditTextDialogFragment;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.MediaCandidate;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.PostViewV2ViewModel;

import static awais.instagrabber.fragments.settings.PreferenceKeys.PREF_SHOWN_COUNT_TOOLTIP;

public class PostViewV2Fragment extends Fragment implements EditTextDialogFragment.EditTextDialogFragmentCallback {
    private static final String TAG = "PostViewV2Fragment";
    // private static final int DETAILS_HIDE_DELAY_MILLIS = 2000;
    public static final String ARG_MEDIA = "media";
    public static final String ARG_SLIDER_POSITION = "position";

    private DialogPostViewBinding binding;
    private Context context;
    private boolean detailsVisible = true;
//    private boolean video;
    private VideoPlayerViewHelper videoPlayerViewHelper;
    private SliderItemsAdapter sliderItemsAdapter;
    private int sliderPosition = -1;
    private PostViewV2ViewModel viewModel;
    private PopupMenu optionsPopup;
    private EditTextDialogFragment editTextDialogFragment;
    private boolean wasDeleted;
    private MutableLiveData<Object> backStackSavedStateCollectionLiveData;
    private MutableLiveData<Object> backStackSavedStateResultLiveData;
    private OnDeleteListener onDeleteListener;
    @Nullable
    private ViewPager2 sliderParent;
    private LayoutPostViewBottomBinding bottom;
    private View postView;
    private int originalHeight;
    private boolean isInFullScreenMode;
    private StyledPlayerView playerView;
    private int playerViewOriginalHeight;
    private Drawable originalRootBackground;
    private ColorStateList originalLikeColorStateList;
    private ColorStateList originalSaveColorStateList;
    private WindowInsetsControllerCompat controller;

    private final Observer<Object> backStackSavedStateObserver = result -> {
        if (result == null) return;
        if (result instanceof String) {
            final String collection = (String) result;
            handleSaveUnsaveResourceLiveData(viewModel.toggleSave(collection, viewModel.getMedia().getHasViewerSaved()));
        } else if ((result instanceof RankedRecipient)) {
            // Log.d(TAG, "result: " + result);
            final Context context = getContext();
            if (context != null) {
                Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show();
            }
            viewModel.shareDm((RankedRecipient) result, sliderPosition);
        } else if ((result instanceof Set)) {
            try {
                // Log.d(TAG, "result: " + result);
                final Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show();
                }
                //noinspection unchecked
                viewModel.shareDm((Set<RankedRecipient>) result, sliderPosition);
            } catch (Exception e) {
                Log.e(TAG, "share: ", e);
            }
        }
        // clear result
        backStackSavedStateCollectionLiveData.postValue(null);
        backStackSavedStateResultLiveData.postValue(null);
    };

    public void setOnDeleteListener(final OnDeleteListener onDeleteListener) {
        if (onDeleteListener == null) return;
        this.onDeleteListener = onDeleteListener;
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    // default constructor for fragment manager
    public PostViewV2Fragment() {}

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PostViewV2ViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = DialogPostViewBinding.inflate(inflater, container, false);
        bottom = LayoutPostViewBottomBinding.bind(binding.getRoot());
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return null;
        controller = new WindowInsetsControllerCompat(activity.getWindow(), activity.getRootView());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        // postponeEnterTransition();
        init();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onPause() {
        super.onPause();
        // wasPaused = true;
        if (Utils.settingsHelper.getBoolean(PreferenceKeys.PLAY_IN_BACKGROUND)) return;
        final Media media = viewModel.getMedia();
        if (media.getType() == null) return;
        switch (media.getType()) {
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
            backStackSavedStateCollectionLiveData = backStackEntry.getSavedStateHandle().getLiveData("collection");
            backStackSavedStateCollectionLiveData.observe(getViewLifecycleOwner(), backStackSavedStateObserver);
            backStackSavedStateResultLiveData = backStackEntry.getSavedStateHandle().getLiveData("result");
            backStackSavedStateResultLiveData.observe(getViewLifecycleOwner(), backStackSavedStateObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showSystemUI();
        final Media media = viewModel.getMedia();
        if (media.getType() == null) return;
        switch (media.getType()) {
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
        if (media.getType() == MediaItemType.MEDIA_TYPE_SLIDER) {
            outState.putInt(ARG_SLIDER_POSITION, sliderPosition);
        }
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            // dismiss();
            return;
        }
        final Serializable feedModelSerializable = arguments.getSerializable(ARG_MEDIA);
        if (feedModelSerializable == null) {
            Log.e(TAG, "onCreate: feedModelSerializable is null");
            // dismiss();
            return;
        }
        if (!(feedModelSerializable instanceof Media)) {
            // dismiss();
            return;
        }
        final Media media = (Media) feedModelSerializable;
        if (media.getType() == MediaItemType.MEDIA_TYPE_SLIDER && sliderPosition == -1) {
            sliderPosition = arguments.getInt(ARG_SLIDER_POSITION, 0);
        }
        viewModel.setMedia(media);
        // if (!wasPaused && (sharedProfilePicElement != null || sharedMainPostElement != null)) {
        //     binding.getRoot().getBackground().mutate().setAlpha(0);
        // }
        // setProfilePicSharedElement();
        // setupCaptionBottomSheet();
        setupCommonActions();
        setObservers();
    }

    private void setObservers() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                binding.profilePic.setVisibility(View.GONE);
                binding.title.setVisibility(View.GONE);
                binding.subtitle.setVisibility(View.GONE);
                return;
            }
            binding.profilePic.setVisibility(View.VISIBLE);
            binding.title.setVisibility(View.VISIBLE);
            binding.subtitle.setVisibility(View.VISIBLE);
            binding.getRoot().post(() -> setupProfilePic(user));
            binding.getRoot().post(() -> setupTitles(user));
        });
        viewModel.getCaption().observe(getViewLifecycleOwner(), caption -> binding.getRoot().post(() -> setupCaption(caption)));
        viewModel.getLocation().observe(getViewLifecycleOwner(), location -> binding.getRoot().post(() -> setupLocation(location)));
        viewModel.getDate().observe(getViewLifecycleOwner(), date -> binding.getRoot().post(() -> {
            if (date == null) {
                bottom.date.setVisibility(View.GONE);
                return;
            }
            bottom.date.setVisibility(View.VISIBLE);
            bottom.date.setText(date);
        }));
        viewModel.getLikeCount().observe(getViewLifecycleOwner(), count -> {
            bottom.likesCount.setNumber(getSafeCount(count));
            binding.getRoot().postDelayed(() -> bottom.likesCount.setAnimateChanges(true), 1000);
            if (count > 1000 && !Utils.settingsHelper.getBoolean(PREF_SHOWN_COUNT_TOOLTIP)) {
                binding.getRoot().postDelayed(this::showCountTooltip, 1000);
            }
        });
        if (!viewModel.getMedia().getCommentsDisabled()) {
            viewModel.getCommentCount().observe(getViewLifecycleOwner(), count -> {
                bottom.commentsCount.setNumber(getSafeCount(count));
                binding.getRoot().postDelayed(() -> bottom.commentsCount.setAnimateChanges(true), 1000);
            });
        }
        viewModel.getViewCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) {
                bottom.viewsCount.setVisibility(View.GONE);
                return;
            }
            bottom.viewsCount.setVisibility(View.VISIBLE);
            final long safeCount = getSafeCount(count);
            final String viewString = getResources().getQuantityString(R.plurals.views_count, (int) safeCount, safeCount);
            bottom.viewsCount.setText(viewString);
        });
        viewModel.getType().observe(getViewLifecycleOwner(), this::setupPostTypeLayout);
        viewModel.getLiked().observe(getViewLifecycleOwner(), this::setLikedResources);
        viewModel.getSaved().observe(getViewLifecycleOwner(), this::setSavedResources);
        viewModel.getOptions().observe(getViewLifecycleOwner(), options -> binding.getRoot().post(() -> {
            setupOptions(options != null && !options.isEmpty());
            createOptionsPopupMenu();
        }));
    }

    private void showCountTooltip() {
        final Context context = getContext();
        if (context == null) return;
        final Rect rect = new Rect();
        bottom.likesCount.getGlobalVisibleRect(rect);
        final Balloon balloon = new Balloon.Builder(context)
                .setArrowSize(8)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setPadding(4)
                .setTextSize(16)
                .setAlpha(0.9f)
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setBalloonHighlightAnimation(BalloonHighlightAnimation.HEARTBEAT, 0)
                .setIsVisibleOverlay(true)
                .setOverlayColorResource(R.color.black_a50)
                .setOverlayShape(new BalloonOverlayCircle((float) Math.max(
                        bottom.likesCount.getMeasuredWidth(),
                        bottom.likesCount.getMeasuredHeight()
                ) / 2f))
                .setBalloonOverlayAnimation(BalloonOverlayAnimation.FADE)
                .setLifecycleOwner(getViewLifecycleOwner())
                .setTextResource(R.string.click_to_show_full)
                .setDismissWhenTouchOutside(false)
                .setDismissWhenOverlayClicked(false)
                .build();
        balloon.showAlignBottom(bottom.likesCount);
        Utils.settingsHelper.putBoolean(PREF_SHOWN_COUNT_TOOLTIP, true);
        balloon.setOnBalloonOutsideTouchListener((view, motionEvent) -> {
            if (rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                bottom.likesCount.setShowAbbreviation(false);
            }
            balloon.dismiss();
        });
    }

    @NonNull
    private Long getSafeCount(final Long count) {
        Long safeCount = count;
        if (count == null) {
            safeCount = 0L;
        }
        return safeCount;
    }

    private void setupCommonActions() {
        setupLike();
        setupSave();
        setupDownload();
        setupComment();
        setupShare();
    }

    private void setupComment() {
        if (!viewModel.hasPk() || viewModel.getMedia().getCommentsDisabled()) {
            bottom.comment.setVisibility(View.GONE);
            // bottom.commentsCount.setVisibility(View.GONE);
            return;
        }
        bottom.comment.setVisibility(View.VISIBLE);
        bottom.comment.setOnClickListener(v -> {
            final Media media = viewModel.getMedia();
            final User user = media.getUser();
            if (user == null) return;
            final NavController navController = getNavController();
            if (navController == null) return;
            try {
                final NavDirections action = PostViewV2FragmentDirections.actionToComments(media.getCode(), media.getPk(), user.getPk());
                navController.navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "setupComment: ", e);
            }
        });
        TooltipCompat.setTooltipText(bottom.comment, getString(R.string.comment));
    }

    private void setupDownload() {
        bottom.download.setOnClickListener(v -> DownloadUtils.showDownloadDialog(context, viewModel.getMedia(), sliderPosition, bottom.download));
        TooltipCompat.setTooltipText(bottom.download, getString(R.string.action_download));
    }

    private void setupLike() {
        originalLikeColorStateList = bottom.like.getIconTint();
        final boolean likableMedia = viewModel.hasPk() /*&& viewModel.getMedia().isCommentLikesEnabled()*/;
        if (!likableMedia) {
            bottom.like.setVisibility(View.GONE);
            // bottom.likesCount.setVisibility(View.GONE);
            return;
        }
        if (!viewModel.isLoggedIn()) {
            bottom.like.setVisibility(View.GONE);
            return;
        }
        bottom.like.setOnClickListener(v -> {
            v.setEnabled(false);
            handleLikeUnlikeResourceLiveData(viewModel.toggleLike());
        });
        bottom.like.setOnLongClickListener(v -> {
            final NavController navController = getNavController();
            if (navController != null && viewModel.isLoggedIn()) {
                try {
                    final NavDirections action = PostViewV2FragmentDirections.actionToLikes(viewModel.getMedia().getPk(), false);
                    navController.navigate(action);
                } catch (Exception e) {
                    Log.e(TAG, "setupLike: ", e);
                }
                return true;
            }
            return true;
        });
    }

    private void handleLikeUnlikeResourceLiveData(@NonNull final LiveData<Resource<Object>> resource) {
        resource.observe(getViewLifecycleOwner(), value -> {
            switch (value.status) {
                case SUCCESS:
                    bottom.like.setEnabled(true);
                    break;
                case ERROR:
                    bottom.like.setEnabled(true);
                    unsuccessfulLike();
                    break;
                case LOADING:
                    bottom.like.setEnabled(false);
                    break;
            }
        });

    }

    private void unsuccessfulLike() {
        final int errorTextResId;
        final Media media = viewModel.getMedia();
        if (!media.getHasLiked()) {
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
        final ColorStateList tintColorStateList;
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        if (resources == null) return;
        if (liked) {
            iconResource = R.drawable.ic_like;
            tintColorStateList = ColorStateList.valueOf(resources.getColor(R.color.red_600));
        } else {
            iconResource = R.drawable.ic_not_liked;
            tintColorStateList = originalLikeColorStateList != null ? originalLikeColorStateList
                                                                    : ColorStateList.valueOf(resources.getColor(R.color.white));
        }
        bottom.like.setIconResource(iconResource);
        bottom.like.setIconTint(tintColorStateList);
    }

    private void setupSave() {
        originalSaveColorStateList = bottom.save.getIconTint();
        if (!viewModel.isLoggedIn() || !viewModel.hasPk() || !viewModel.getMedia().getCanViewerSave()) {
            bottom.save.setVisibility(View.GONE);
            return;
        }
        bottom.save.setOnClickListener(v -> {
            bottom.save.setEnabled(false);
            handleSaveUnsaveResourceLiveData(viewModel.toggleSave());
        });
        bottom.save.setOnLongClickListener(v -> {
            try {
                final NavDirections action = PostViewV2FragmentDirections.actionToSavedCollections().setIsSaving(true);
                NavHostFragment.findNavController(this).navigate(action);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "setupSave: ", e);
            }
            return false;
        });
    }

    private void handleSaveUnsaveResourceLiveData(@NonNull final LiveData<Resource<Object>> resource) {
        resource.observe(getViewLifecycleOwner(), value -> {
            if (value == null) return;
            switch (value.status) {
                case SUCCESS:
                    bottom.save.setEnabled(true);
                    break;
                case ERROR:
                    bottom.save.setEnabled(true);
                    unsuccessfulSave();
                    break;
                case LOADING:
                    bottom.save.setEnabled(false);
                    break;
            }
        });
    }

    private void unsuccessfulSave() {
        final int errorTextResId;
        final Media media = viewModel.getMedia();
        if (!media.getHasViewerSaved()) {
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
        final ColorStateList tintColorStateList;
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        if (resources == null) return;
        if (saved) {
            iconResource = R.drawable.ic_bookmark;
            tintColorStateList = ColorStateList.valueOf(resources.getColor(R.color.blue_700));
        } else {
            iconResource = R.drawable.ic_round_bookmark_border_24;
            tintColorStateList = originalSaveColorStateList != null ? originalSaveColorStateList
                                                                    : ColorStateList.valueOf(resources.getColor(R.color.white));
        }
        bottom.save.setIconResource(iconResource);
        bottom.save.setIconTint(tintColorStateList);
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
        final String fullName = user.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            binding.subtitle.setVisibility(View.GONE);
        } else {
            binding.subtitle.setVisibility(View.VISIBLE);
            binding.subtitle.setText(fullName);
        }
        setUsername(user);
        binding.title.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
        binding.subtitle.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
    }

    private void setUsername(final User user) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
        final int drawableSize = Utils.convertDpToPx(24);
        if (user.isVerified()) {
            final Context context = getContext();
            if (context == null) return;
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
            bottom.caption.setVisibility(View.GONE);
            bottom.translate.setVisibility(View.GONE);
            return;
        }
        final String postCaption = caption.getText();
        bottom.caption.addOnHashtagListener(autoLinkItem -> {
            try {
                final String originalText = autoLinkItem.getOriginalText().trim();
                final NavDirections action = PostViewV2FragmentDirections.actionToHashtag(originalText);
                NavHostFragment.findNavController(this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "setupCaption: ", e);
            }
        });
        bottom.caption.addOnMentionClickListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText().trim();
            navigateToProfile(originalText);
        });
        bottom.caption.addOnEmailClickListener(autoLinkItem -> Utils.openEmailAddress(getContext(), autoLinkItem.getOriginalText().trim()));
        bottom.caption.addOnURLClickListener(autoLinkItem -> Utils.openURL(getContext(), autoLinkItem.getOriginalText().trim()));
        bottom.caption.setOnLongClickListener(v -> {
            final Context context = getContext();
            if (context == null) return false;
            Utils.copyText(context, postCaption);
            return true;
        });
        bottom.caption.setText(postCaption);
        bottom.translate.setOnClickListener(v -> handleTranslateCaptionResource(viewModel.translateCaption()));
    }

    private void handleTranslateCaptionResource(@NonNull final LiveData<Resource<String>> data) {
        data.observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case SUCCESS:
                    bottom.translate.setVisibility(View.GONE);
                    bottom.caption.setText(resource.data);
                    break;
                case ERROR:
                    bottom.translate.setEnabled(true);
                    String message = resource.message;
                    if (TextUtils.isEmpty(message)) {
                        message = getString(R.string.downloader_unknown_error);
                    }
                    final Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    bottom.translate.setEnabled(false);
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
            try {
                final NavController navController = getNavController();
                if (navController == null) return;
                final NavDirections action = PostViewV2FragmentDirections.actionToLocation(location.getPk());
                navController.navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "setupLocation: ", e);
            }
        });
    }

    private void setupShare() {
        if (!viewModel.hasPk()) {
            bottom.share.setVisibility(View.GONE);
            return;
        }
        bottom.share.setVisibility(View.VISIBLE);
        TooltipCompat.setTooltipText(bottom.share, getString(R.string.share));
        bottom.share.setOnClickListener(v -> {
            final Media media = viewModel.getMedia();
            final User profileModel = media.getUser();
            if (profileModel == null) return;
            if (viewModel.isLoggedIn()) {
                final Context context = getContext();
                if (context == null) return;
                final ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
                final PopupMenu popupMenu = new PopupMenu(themeWrapper, bottom.share);
                final Menu menu = popupMenu.getMenu();
                menu.add(0, R.id.share_dm, 0, R.string.share_via_dm);
                menu.add(0, R.id.share, 1, R.string.share_link);
                popupMenu.setOnMenuItemClickListener(item -> {
                    final int itemId = item.getItemId();
                    if (itemId == R.id.share_dm) {
                        if (profileModel.isPrivate()) Toast.makeText(context, R.string.share_private_post, Toast.LENGTH_SHORT).show();
                        final PostViewV2FragmentDirections.ActionToUserSearch actionGlobalUserSearch = PostViewV2FragmentDirections
                                .actionToUserSearch()
                                .setTitle(getString(R.string.share))
                                .setActionLabel(getString(R.string.send))
                                .setShowGroups(true)
                                .setMultiple(true)
                                .setSearchMode(UserSearchMode.RAVEN);
                        final NavController navController = NavHostFragment.findNavController(PostViewV2Fragment.this);
                        try {
                            navController.navigate(actionGlobalUserSearch);
                        } catch (Exception e) {
                            Log.e(TAG, "setupShare: ", e);
                        }
                        return true;
                    } else if (itemId == R.id.share) {
                        shareLink(media, profileModel.isPrivate());
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
                return;
            }
            shareLink(media, false);
        });
    }

    private void shareLink(@NonNull final Media media, final boolean isPrivate) {
        final Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TITLE,
                               getString(isPrivate ? R.string.share_private_post : R.string.share_public_post));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/" + media.getCode());
        startActivity(Intent.createChooser(
                sharingIntent,
                isPrivate ? getString(R.string.share_private_post)
                          : getString(R.string.share_public_post)
        ));
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

    private void setupPostImage() {
        // binding.mediaCounter.setVisibility(View.GONE);
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        if (resources == null) return;
        final Media media = viewModel.getMedia();
        final String imageUrl = ResponseBodyUtils.getImageUrl(media);
        if (TextUtils.isEmpty(imageUrl)) return;
        final ZoomableDraweeView postImage = new ZoomableDraweeView(context);
        postView = postImage;
        final NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                                                                                            media.getOriginalWidth(),
                                                                                            (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                            Utils.displayMetrics.widthPixels);
        originalHeight = widthHeight.second;
        final ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                                                                                             originalHeight);
        postImage.setLayoutParams(layoutParams);
        postImage.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                                       .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                                       .build());

        postImage.setController(Fresco.newDraweeControllerBuilder()
                                      .setLowResImageRequest(ImageRequest.fromUri(ResponseBodyUtils.getThumbUrl(media)))
                                      .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl))
                                                                          .setLocalThumbnailPreviewsEnabled(true)
                                                                          .build())
                                      .build());
        final AnimatedZoomableController zoomableController = (AnimatedZoomableController) postImage.getZoomableController();
        zoomableController.setMaxScaleFactor(3f);
        zoomableController.setGestureZoomEnabled(true);
        zoomableController.setEnabled(true);
        postImage.setZoomingEnabled(true);
        final DoubleTapGestureListener tapListener = new DoubleTapGestureListener(postImage) {
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                if (!isInFullScreenMode) {
                    zoomableController.reset();
                    hideSystemUI();
                } else {
                    showSystemUI();
                    binding.getRoot().postDelayed(zoomableController::reset, 500);
                }
                return super.onSingleTapConfirmed(e);
            }
        };
        postImage.setTapListener(tapListener);
        binding.postContainer.addView(postView);
    }

    private void setupSlider() {
        final Media media = viewModel.getMedia();
        binding.mediaCounter.setVisibility(View.VISIBLE);
        final Context context = getContext();
        if (context == null) return;
        sliderParent = new ViewPager2(context);
        final List<Media> carouselMedia = media.getCarouselMedia();
        if (carouselMedia == null) return;
        final NullSafePair<Integer, Integer> maxHW = carouselMedia
                .stream()
                .reduce(new NullSafePair<>(0, 0),
                        (prev, m) -> {
                            final int height = m.getOriginalHeight() > prev.first ? m.getOriginalHeight() : prev.first;
                            final int width = m.getOriginalWidth() > prev.second ? m.getOriginalWidth() : prev.second;
                            return new NullSafePair<>(height, width);
                        },
                        (p1, p2) -> {
                            final int height = p1.first > p2.first ? p1.first : p2.first;
                            final int width = p1.second > p2.second ? p1.second : p2.second;
                            return new NullSafePair<>(height, width);
                        });
        final NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(maxHW.first,
                                                                                            maxHW.second,
                                                                                            (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                            Utils.displayMetrics.widthPixels);
        originalHeight = widthHeight.second;
        final ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                                                                                             originalHeight);
        sliderParent.setLayoutParams(layoutParams);
        postView = sliderParent;
        // binding.contentRoot.addView(sliderParent, 0);
        binding.postContainer.addView(postView);

        final boolean hasVideo = media.getCarouselMedia()
                                      .stream()
                                      .anyMatch(postChild -> postChild.getType() == MediaItemType.MEDIA_TYPE_VIDEO);
        if (hasVideo) {
            final View child = sliderParent.getChildAt(0);
            if (child instanceof RecyclerView) {
                ((RecyclerView) child).setItemViewCacheSize(media.getCarouselMedia().size());
                ((RecyclerView) child).addRecyclerListener(holder -> {
                    if (holder instanceof SliderVideoViewHolder) {
                        ((SliderVideoViewHolder) holder).releasePlayer();
                    }
                });
            }
        }
        sliderItemsAdapter = new SliderItemsAdapter(true, new SliderCallbackAdapter() {
            @Override
            public void onItemClicked(final int position, final Media media, final View view) {
                if (media == null
                        || media.getType() != MediaItemType.MEDIA_TYPE_IMAGE
                        || !(view instanceof ZoomableDraweeView)) {
                    return;
                }
                final ZoomableController zoomableController = ((ZoomableDraweeView) view).getZoomableController();
                if (!(zoomableController instanceof AnimatedZoomableController)) return;
                if (!isInFullScreenMode) {
                    ((AnimatedZoomableController) zoomableController).reset();
                    hideSystemUI();
                    return;
                }
                showSystemUI();
                binding.getRoot().postDelayed(((AnimatedZoomableController) zoomableController)::reset, 500);
            }

            @Override
            public void onPlayerPlay(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                // if (!detailsVisible || hasBeenToggled) return;
                // showPlayerControls();
            }

            @Override
            public void onPlayerPause(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
                // if (detailsVisible || hasBeenToggled) return;
                // toggleDetails();
            }

            @Override
            public void onPlayerRelease(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }

            @Override
            public void onFullScreenModeChanged(final boolean isFullScreen, final StyledPlayerView playerView) {
                PostViewV2Fragment.this.playerView = playerView;
                if (isFullScreen) {
                    hideSystemUI();
                    return;
                }
                showSystemUI();
            }

            @Override
            public boolean isInFullScreen() {
                return isInFullScreenMode;
            }
        });
        sliderParent.setAdapter(sliderItemsAdapter);
        if (sliderPosition >= 0 && sliderPosition < media.getCarouselMedia().size()) {
            sliderParent.setCurrentItem(sliderPosition);
        }
        sliderParent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            int prevPosition = -1;

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                if (prevPosition != -1) {
                    final View view = sliderParent.getChildAt(0);
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
//                video = false;
//                if (childMedia.getType() == MediaItemType.MEDIA_TYPE_VIDEO) {
//                    video = true;
//                    viewModel.setViewCount(childMedia.getViewCount());
//                    return;
//                }
//                viewModel.setViewCount(null);
            }

            private void pausePlayerAtPosition(final int position, final RecyclerView view) {
                final RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof SliderVideoViewHolder) {
                    ((SliderVideoViewHolder) viewHolder).pause();
                }
            }
        });
        final String text = "1/" + carouselMedia.size();
        binding.mediaCounter.setText(text);
        sliderItemsAdapter.submitList(media.getCarouselMedia());
        sliderParent.setCurrentItem(sliderPosition);
    }

    private void pauseSliderPlayer() {
        if (sliderParent == null) return;
        final int currentItem = sliderParent.getCurrentItem();
        final View view = sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(currentItem);
        if (!(viewHolder instanceof SliderVideoViewHolder)) return;
        ((SliderVideoViewHolder) viewHolder).pause();
    }

    private void releaseAllSliderPlayers() {
        if (sliderParent == null) return;
        final View view = sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        final int itemCount = sliderItemsAdapter.getItemCount();
        for (int position = itemCount - 1; position >= 0; position--) {
            final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
            if (!(viewHolder instanceof SliderVideoViewHolder)) continue;
            ((SliderVideoViewHolder) viewHolder).releasePlayer();
        }
    }

    private void setupVideo() {
//        video = true;
        final Media media = viewModel.getMedia();
        binding.mediaCounter.setVisibility(View.GONE);
        final Context context = getContext();
        if (context == null) return;
        final LayoutVideoPlayerWithThumbnailBinding videoPost = LayoutVideoPlayerWithThumbnailBinding
                .inflate(LayoutInflater.from(context), binding.contentRoot, false);
        final ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) videoPost.getRoot().getLayoutParams();
        final NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                                                                                            media.getOriginalWidth(),
                                                                                            (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                            Utils.displayMetrics.widthPixels);
        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
        originalHeight = widthHeight.second;
        layoutParams.height = originalHeight;
        postView = videoPost.getRoot();
        binding.postContainer.addView(postView);

        // final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
        //     @Override
        //     public boolean onSingleTapConfirmed(final MotionEvent e) {
        //         videoPost.playerView.performClick();
        //         return true;
        //     }
        // });
        // videoPost.playerView.setOnTouchListener((v, event) -> {
        //     gestureDetector.onTouchEvent(event);
        //     return true;
        // });
        final float vol = Utils.settingsHelper.getBoolean(PreferenceKeys.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {
            @Override
            public void onThumbnailLoaded() {
                startPostponedEnterTransition();
            }

            @Override
            public void onPlayerViewLoaded() {
                // binding.playerControls.getRoot().setVisibility(View.VISIBLE);
                final ViewGroup.LayoutParams layoutParams = videoPost.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils
                        .getResultingHeight(requiredWidth, media.getOriginalHeight(), media.getOriginalWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                videoPost.playerView.requestLayout();
            }

            @Override
            public void onPlay() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                // if (detailsVisible) {
                //     new Handler().postDelayed(() -> toggleDetails(), DETAILS_HIDE_DELAY_MILLIS);
                // }
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

            @Override
            public void onFullScreenModeChanged(final boolean isFullScreen, final StyledPlayerView playerView) {
                PostViewV2Fragment.this.playerView = playerView;
                if (isFullScreen) {
                    hideSystemUI();
                    return;
                }
                showSystemUI();
            }
        };
        final float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
        String videoUrl = null;
        final List<MediaCandidate> videoVersions = media.getVideoVersions();
        if (videoVersions != null && !videoVersions.isEmpty()) {
            final MediaCandidate videoVersion = videoVersions.get(0);
            if (videoVersion != null) {
                videoUrl = videoVersion.getUrl();
            }
        }
        if (videoUrl != null) {
            videoPlayerViewHelper = new VideoPlayerViewHelper(
                    binding.getRoot().getContext(),
                    videoPost,
                    videoUrl,
                    vol,
                    aspectRatio,
                    ResponseBodyUtils.getThumbUrl(media),
                    true,
                    videoPlayerCallback);
        }
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
            final Context context = getContext();
            if (context == null) return;
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
        // final boolean hasBeenToggled = true;
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        final Media media = viewModel.getMedia();
        binding.getRoot().post(() -> {
            TransitionManager.beginDelayedTransition(binding.getRoot());
            if (detailsVisible) {
                final Context context = getContext();
                if (context == null) return;
                originalRootBackground = binding.getRoot().getBackground();
                final Resources resources = context.getResources();
                if (resources == null) return;
                final ColorDrawable colorDrawable = new ColorDrawable(resources.getColor(R.color.black));
                binding.getRoot().setBackground(colorDrawable);
                if (postView != null) {
                    // Make post match parent
                    final int fullHeight = Utils.displayMetrics.heightPixels - Utils.getStatusBarHeight(context);
                    postView.getLayoutParams().height = fullHeight;
                    binding.postContainer.getLayoutParams().height = fullHeight;
                    if (playerView != null) {
                        playerViewOriginalHeight = playerView.getLayoutParams().height;
                        playerView.getLayoutParams().height = fullHeight;
                    }
                }
                final BottomNavigationView bottomNavView = activity.getBottomNavView();
                bottomNavView.setVisibility(View.GONE);
                detailsVisible = false;
                if (media.getUser() != null) {
                    binding.profilePic.setVisibility(View.GONE);
                    binding.title.setVisibility(View.GONE);
                    binding.subtitle.setVisibility(View.GONE);
                }
                if (media.getLocation() != null) {
                    binding.location.setVisibility(View.GONE);
                }
                if (media.getCaption() != null && !TextUtils.isEmpty(media.getCaption().getText())) {
                    bottom.caption.setVisibility(View.GONE);
                    bottom.translate.setVisibility(View.GONE);
                }
                bottom.likesCount.setVisibility(View.GONE);
                bottom.commentsCount.setVisibility(View.GONE);
                bottom.date.setVisibility(View.GONE);
                bottom.comment.setVisibility(View.GONE);
                bottom.like.setVisibility(View.GONE);
                bottom.save.setVisibility(View.GONE);
                bottom.share.setVisibility(View.GONE);
                bottom.download.setVisibility(View.GONE);
                binding.mediaCounter.setVisibility(View.GONE);
                bottom.viewsCount.setVisibility(View.GONE);
                final List<Integer> options = viewModel.getOptions().getValue();
                if (options != null && !options.isEmpty()) {
                    binding.options.setVisibility(View.GONE);
                }
                return;
            }
            if (originalRootBackground != null) {
                binding.getRoot().setBackground(originalRootBackground);
            }
            if (postView != null) {
                // Make post height back to original
                postView.getLayoutParams().height = originalHeight;
                binding.postContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                if (playerView != null) {
                    playerView.getLayoutParams().height = playerViewOriginalHeight;
                    playerView = null;
                }
            }
            final BottomNavigationView bottomNavView = activity.getBottomNavView();
            bottomNavView.setVisibility(View.VISIBLE);
            if (media.getUser() != null) {
                binding.profilePic.setVisibility(View.VISIBLE);
                binding.title.setVisibility(View.VISIBLE);
                binding.subtitle.setVisibility(View.VISIBLE);
                // binding.topBg.setVisibility(View.VISIBLE);
            }
            if (media.getLocation() != null) {
                binding.location.setVisibility(View.VISIBLE);
            }
            if (media.getCaption() != null && !TextUtils.isEmpty(media.getCaption().getText())) {
                bottom.caption.setVisibility(View.VISIBLE);
                bottom.translate.setVisibility(View.VISIBLE);
            }
            if (viewModel.hasPk()) {
                bottom.likesCount.setVisibility(View.VISIBLE);
                bottom.date.setVisibility(View.VISIBLE);
                // binding.captionParent.setVisibility(View.VISIBLE);
                // binding.captionToggle.setVisibility(View.VISIBLE);
                bottom.share.setVisibility(View.VISIBLE);
            }
            if (viewModel.hasPk() && !viewModel.getMedia().getCommentsDisabled()) {
                bottom.comment.setVisibility(View.VISIBLE);
                bottom.commentsCount.setVisibility(View.VISIBLE);
            }
            bottom.download.setVisibility(View.VISIBLE);
            final List<Integer> options = viewModel.getOptions().getValue();
            if (options != null && !options.isEmpty()) {
                binding.options.setVisibility(View.VISIBLE);
            }
            if (viewModel.isLoggedIn() && viewModel.hasPk()) {
                bottom.like.setVisibility(View.VISIBLE);
                bottom.save.setVisibility(View.VISIBLE);
            }
            // if (video) {
            if (media.getType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                // binding.playerControlsToggle.setVisibility(View.VISIBLE);
                bottom.viewsCount.setVisibility(View.VISIBLE);
            }
            // if (wasControlsVisible) {
            //     showPlayerControls();
            // }
            if (media.getType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                binding.mediaCounter.setVisibility(View.VISIBLE);
            }
            detailsVisible = true;
        });
    }

    private void hideSystemUI() {
        if (detailsVisible) {
            toggleDetails();
        }
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        final CollapsingToolbarLayout appbarLayout = activity.getCollapsingToolbarView();
        appbarLayout.setVisibility(View.GONE);
        final Toolbar toolbar = activity.getToolbar();
        toolbar.setVisibility(View.GONE);
        binding.getRoot().setPadding(binding.getRoot().getPaddingLeft(),
                                     binding.getRoot().getPaddingTop(),
                                     binding.getRoot().getPaddingRight(),
                                     0);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE);
        isInFullScreenMode = true;
    }

    private void showSystemUI() {
        if (!detailsVisible) {
            toggleDetails();
        }
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
        final CollapsingToolbarLayout appbarLayout = activity.getCollapsingToolbarView();
        appbarLayout.setVisibility(View.VISIBLE);
        final Toolbar toolbar = activity.getToolbar();
        toolbar.setVisibility(View.VISIBLE);
        final Context context = getContext();
        if (context == null) return;
        binding.getRoot().setPadding(binding.getRoot().getPaddingLeft(),
                                     binding.getRoot().getPaddingTop(),
                                     binding.getRoot().getPaddingRight(),
                                     Utils.getActionBarHeight(context));
        controller.show(WindowInsetsCompat.Type.systemBars());
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        isInFullScreenMode = false;
    }

    private void navigateToProfile(final String username) {
        final NavController navController = getNavController();
        if (navController == null) return;
        final NavDirections actionToProfile = PostViewV2FragmentDirections.actionToProfile().setUsername(username);
        navController.navigate(actionToProfile);
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