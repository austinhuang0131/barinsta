package awais.instagrabber.adapters.viewholder;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmInboxItemBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.DirectItemType;

public final class DirectMessageInboxItemViewHolder extends RecyclerView.ViewHolder {
    private final LinearLayout multipleProfilePicsContainer;
    private final SimpleDraweeView[] multipleProfilePics;
    private final LayoutDmInboxItemBinding binding;

    public DirectMessageInboxItemViewHolder(@NonNull final LayoutDmInboxItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        multipleProfilePicsContainer = binding.multiPicContainer;
        final LinearLayout containerChild = (LinearLayout) multipleProfilePicsContainer.getChildAt(1);
        multipleProfilePics = new SimpleDraweeView[]{
                (SimpleDraweeView) multipleProfilePicsContainer.getChildAt(0),
                (SimpleDraweeView) containerChild.getChildAt(0),
                (SimpleDraweeView) containerChild.getChildAt(1)
        };
        binding.tvDate.setSelected(true);
        binding.tvUsername.setSelected(true);
    }

    public void bind(final InboxThreadModel model) {
        final DirectItemModel[] itemModels;
        if (model == null || (itemModels = model.getItems()) == null) {
            return;
        }
        itemView.setTag(model);
        final ProfileModel[] users = model.getUsers();
        if (users.length > 1) {
            binding.ivProfilePic.setVisibility(View.GONE);
            multipleProfilePicsContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < Math.min(3, users.length); ++i) {
                multipleProfilePics[i].setImageURI(users[i].getSdProfilePic());
            }
        } else {
            final String uriString = users.length == 1 ? users[0].getSdProfilePic() : null;
            if (uriString == null) {
                binding.ivProfilePic.setVisibility(View.GONE);
            } else {
                binding.ivProfilePic.setVisibility(View.VISIBLE);
                multipleProfilePicsContainer.setVisibility(View.GONE);
                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uriString))
                                                                .setResizeOptions(new ResizeOptions(50, 50))
                                                                .build();
                binding.ivProfilePic.setController(
                        Fresco.newDraweeControllerBuilder()
                              .setOldController(binding.ivProfilePic.getController())
                              .setImageRequest(request)
                              .build()
                );
            }
        }
        binding.tvUsername.setText(model.getThreadTitle());
        final int length = itemModels.length;
        DirectItemModel lastItemModel = null;
        if (length != 0) {
            lastItemModel = itemModels[length - 1];
        }
        if (lastItemModel == null) {
            return;
        }
        final DirectItemType itemType = lastItemModel.getItemType();
        // binding.notTextType.setVisibility(itemType != DirectItemType.TEXT ? View.VISIBLE : View.GONE);
        final Context context = itemView.getContext();
        final CharSequence messageText;
        switch (itemType) {
            case TEXT:
            case LIKE:
                messageText = lastItemModel.getText();
                break;
            case LINK:
                messageText = context.getString(R.string.direct_messages_sent_link);
                break;
            case MEDIA:
            case MEDIA_SHARE:
            case RAVEN_MEDIA:
            case CLIP:
                messageText = context.getString(R.string.direct_messages_sent_media);
                break;
            case ACTION_LOG:
                final DirectItemModel.DirectItemActionLogModel logModel = lastItemModel.getActionLogModel();
                messageText = logModel != null ? logModel.getDescription() : "...";
                break;
            case REEL_SHARE:
                final DirectItemModel.DirectItemReelShareModel reelShare = lastItemModel.getReelShare();
                if (reelShare == null)
                    messageText = context.getString(R.string.direct_messages_sent_media);
                else {
                    final String reelType = reelShare.getType();
                    final int textRes;
                    if ("reply".equals(reelType))
                        textRes = R.string.direct_messages_replied_story;
                    else if ("mention".equals(reelType))
                        textRes = R.string.direct_messages_mention_story;
                    else if ("reaction".equals(reelType))
                        textRes = R.string.direct_messages_reacted_story;
                    else textRes = R.string.direct_messages_sent_media;

                    messageText = context.getString(textRes) + " : " + reelShare.getText();
                }
                break;
            default:
                messageText = "<i>Unsupported message</i>";
        }
        binding.tvComment.setText(HtmlCompat.fromHtml(messageText.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT));
        binding.tvDate.setText(lastItemModel.getDateTime());
        binding.unread.setVisibility(model.getUnreadCount() > 0L ? View.VISIBLE : View.INVISIBLE);
    }
}