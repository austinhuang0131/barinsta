package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public class DirectMessageStoryShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmStoryShareBinding binding;
    private final int maxHeight;
    private final int maxWidth;

    public DirectMessageStoryShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                             @NonNull final LayoutDmStoryShareBinding binding,
                                             final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        maxWidth = (int) (Utils.displayMetrics.widthPixels * 0.8);
        binding.tvMessage.setVisibility(View.GONE);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
        if (reelShare == null) {
            binding.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), FROM_HTML_MODE_COMPACT));
            binding.tvMessage.setVisibility(View.VISIBLE);
        } else {
            final String text = reelShare.getText();
            if (!TextUtils.isEmpty(text)) {
                binding.tvMessage.setText(text);
                binding.tvMessage.setVisibility(View.VISIBLE);
            } else {
                final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
                final MediaItemType mediaType = reelShareMedia.getMediaType();
                binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ? View.VISIBLE : View.GONE);
                final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                        reelShareMedia.getHeight(),
                        reelShareMedia.getWidth(),
                        maxHeight,
                        maxWidth
                );
                final ViewGroup.LayoutParams layoutParams = binding.ivMediaPreview.getLayoutParams();
                layoutParams.width = widthHeight.first != null ? widthHeight.first : 0;
                layoutParams.height = widthHeight.second != null ? widthHeight.second : 0;
                binding.ivMediaPreview.requestLayout();
                binding.ivMediaPreview.setImageURI(reelShareMedia.getThumbUrl());
            }
        }
    }
}
