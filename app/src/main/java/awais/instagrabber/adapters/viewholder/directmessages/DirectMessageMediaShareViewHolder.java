package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemMediaModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public class DirectMessageMediaShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmMediaShareBinding binding;
    private final int maxHeight;
    private final int maxWidth;

    public DirectMessageMediaShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                             @NonNull final LayoutDmMediaShareBinding binding,
                                             final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        maxWidth = (int) (Utils.displayMetrics.widthPixels * 0.8);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final Context context = itemView.getContext();
        final DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
        final ProfileModel modelUser = mediaModel.getUser();
        if (modelUser != null) {
            binding.tvMessage.setText(HtmlCompat.fromHtml(
                    "<small>" + context.getString(R.string.dms_inbox_media_shared_from, modelUser.getUsername()) + "</small>",
                    FROM_HTML_MODE_COMPACT));
        }
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                mediaModel.getHeight(),
                mediaModel.getWidth(),
                maxHeight,
                maxWidth
        );
        final ViewGroup.LayoutParams layoutParams = binding.ivMediaPreview.getLayoutParams();
        layoutParams.width = widthHeight.first != null ? widthHeight.first : 0;
        layoutParams.height = widthHeight.second != null ? widthHeight.second : 0;
        binding.ivMediaPreview.requestLayout();
        binding.ivMediaPreview.setImageURI(mediaModel.getThumbUrl());
        final MediaItemType modelMediaType = mediaModel.getMediaType();
        binding.typeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO
                                               || modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
    }

    private class WidthHeight {
        private final DirectItemMediaModel mediaModel;
        private int height;
        private int width;

        public WidthHeight(final DirectItemMediaModel mediaModel) {this.mediaModel = mediaModel;}

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public WidthHeight invoke() {
            height = mediaModel.getHeight();
            width = mediaModel.getWidth();
            // make height 500dp regardless
            width = NumberUtils.getResultingWidth(maxHeight, height, width);
            height = maxHeight;
            if (width > maxWidth) {
                height = NumberUtils.getResultingHeight(maxWidth, height, width);
                width = maxWidth;
            }
            return this;
        }
    }
}
