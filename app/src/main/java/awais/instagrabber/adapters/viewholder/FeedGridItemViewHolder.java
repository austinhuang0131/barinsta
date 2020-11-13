package awais.instagrabber.adapters.viewholder;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.DownloadedCheckerAsyncTask;
import awais.instagrabber.databinding.ItemFeedGridBinding;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.models.PostsLayoutPreferences.PostsLayoutType.STAGGERED_GRID;

public class FeedGridItemViewHolder extends RecyclerView.ViewHolder {
    private final ItemFeedGridBinding binding;

    public FeedGridItemViewHolder(@NonNull final ItemFeedGridBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final int position,
                     @NonNull final FeedModel feedModel,
                     @NonNull final PostsLayoutPreferences layoutPreferences,
                     final FeedAdapterV2.FeedItemCallback feedItemCallback,
                     final FeedAdapterV2.AdapterSelectionCallback adapterSelectionCallback,
                     final boolean selectionModeActive,
                     final boolean selected) {
        itemView.setOnClickListener(v -> {
            if (!selectionModeActive && feedItemCallback != null) {
                feedItemCallback.onPostClick(feedModel, binding.profilePic, binding.postImage);
                return;
            }
            if (selectionModeActive && adapterSelectionCallback != null) {
                adapterSelectionCallback.onPostClick(position, feedModel);
            }
        });
        if (adapterSelectionCallback != null) {
            itemView.setOnLongClickListener(v -> adapterSelectionCallback.onPostLongClick(position, feedModel));
        }
        binding.selectedView.setVisibility(selected ? View.VISIBLE : View.GONE);
        // for rounded borders (clip view to background shape)
        itemView.setClipToOutline(layoutPreferences.getHasRoundedCorners());
        if (layoutPreferences.getType() == STAGGERED_GRID) {
            final float aspectRatio = (float) feedModel.getImageWidth() / feedModel.getImageHeight();
            binding.postImage.setAspectRatio(aspectRatio);
        } else {
            binding.postImage.setAspectRatio(1);
        }
        if (layoutPreferences.isAvatarVisible()) {
            binding.profilePic.setVisibility(TextUtils.isEmpty(feedModel.getProfileModel().getSdProfilePic()) ? View.GONE : View.VISIBLE);
            binding.profilePic.setImageURI(feedModel.getProfileModel().getSdProfilePic());
            final ViewGroup.LayoutParams layoutParams = binding.profilePic.getLayoutParams();
            @DimenRes final int dimenRes;
            switch (layoutPreferences.getProfilePicSize()) {
                case SMALL:
                    dimenRes = R.dimen.profile_pic_size_small;
                    break;
                case TINY:
                    dimenRes = R.dimen.profile_pic_size_tiny;
                    break;
                default:
                case REGULAR:
                    dimenRes = R.dimen.profile_pic_size_regular;
                    break;
            }
            final int dimensionPixelSize = itemView.getResources().getDimensionPixelSize(dimenRes);
            layoutParams.width = dimensionPixelSize;
            layoutParams.height = dimensionPixelSize;
            binding.profilePic.requestLayout();
        } else {
            binding.profilePic.setVisibility(View.GONE);
        }
        if (layoutPreferences.isNameVisible()) {
            binding.name.setVisibility(TextUtils.isEmpty(feedModel.getProfileModel().getUsername()) ? View.GONE : View.VISIBLE);
            binding.name.setText(feedModel.getProfileModel().getUsername());
        } else {
            binding.name.setVisibility(View.GONE);
        }
        String thumbnailUrl = null;
        final int typeIconRes;
        switch (feedModel.getItemType()) {
            case MEDIA_TYPE_IMAGE:
                typeIconRes = -1;
                thumbnailUrl = feedModel.getThumbnailUrl();
                break;
            case MEDIA_TYPE_VIDEO:
                thumbnailUrl = feedModel.getThumbnailUrl();
                typeIconRes = R.drawable.exo_icon_play;
                break;
            case MEDIA_TYPE_SLIDER:
                final List<PostChild> sliderItems = feedModel.getSliderItems();
                if (sliderItems != null) {
                    thumbnailUrl = sliderItems.get(0).getThumbnailUrl();
                }
                typeIconRes = R.drawable.ic_checkbox_multiple_blank_stroke;
                break;
            default:
                typeIconRes = -1;
                thumbnailUrl = null;
        }
        if (TextUtils.isEmpty(thumbnailUrl)) {
            binding.postImage.setController(null);
            return;
        }
        if (typeIconRes <= 0) {
            binding.typeIcon.setVisibility(View.GONE);
        } else {
            binding.typeIcon.setVisibility(View.VISIBLE);
            binding.typeIcon.setImageResource(typeIconRes);
        }
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(thumbnailUrl))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .setProgressiveRenderingEnabled(true)
                                                               .build();
        final PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
                                                              .setImageRequest(requestBuilder)
                                                              .setOldController(binding.postImage.getController());
        binding.postImage.setController(builder.build());
        final DownloadedCheckerAsyncTask task = new DownloadedCheckerAsyncTask(result -> {
            final List<Boolean> checkList = result.get(feedModel.getPostId());
            if (checkList == null || checkList.isEmpty()) {
                return;
            }
            switch (feedModel.getItemType()) {
                case MEDIA_TYPE_IMAGE:
                case MEDIA_TYPE_VIDEO:
                    binding.downloaded.setVisibility(checkList.get(0) ? View.VISIBLE : View.GONE);
                    binding.downloaded.setImageTintList(ColorStateList.valueOf(itemView.getResources().getColor(R.color.green_A400)));
                    break;
                case MEDIA_TYPE_SLIDER:
                    binding.downloaded.setVisibility(checkList.get(0) ? View.VISIBLE : View.GONE);
                    boolean allDownloaded = checkList.size() == feedModel.getSliderItems().size();
                    if (allDownloaded) {
                        allDownloaded = checkList.stream().allMatch(downloaded -> downloaded);
                    }
                    binding.downloaded.setImageTintList(ColorStateList.valueOf(itemView.getResources().getColor(
                            allDownloaded ? R.color.green_A400 : R.color.yellow_400)));
                    break;
                default:
            }
        });
        task.execute(feedModel);
    }
}
