package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

public class DirectMessageMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmMediaBinding binding;
    private final int maxHeight;
    private final int maxWidth;

    public DirectMessageMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                        @NonNull final LayoutDmMediaBinding binding,
                                        final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        maxWidth = (int) (Utils.displayMetrics.widthPixels * 0.8);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
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
        binding.ivMediaPreview.requestLayout();
        binding.ivMediaPreview.setImageURI(mediaModel.getThumbUrl());
        final MediaItemType modelMediaType = mediaModel.getMediaType();
        binding.typeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO || modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER
                                       ? View.VISIBLE
                                       : View.GONE);
    }
}
