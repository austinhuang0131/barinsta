package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class DirectMessageReelShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmRavenMediaBinding binding;
    private final int maxHeight;
    private final int maxWidth;

    public DirectMessageReelShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                            @NonNull final LayoutDmRavenMediaBinding binding,
                                            final View.OnClickListener onClickListener,
                                            final MentionClickListener mentionClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        maxWidth = (int) (Utils.displayMetrics.widthPixels * 0.8);
        binding.tvMessage.setMentionClickListener(mentionClickListener);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
        CharSequence text = reelShare.getText();
        if (TextUtils.isEmpty(text)) {
            binding.tvMessage.setVisibility(View.GONE);
        } else {
            if (TextUtils.hasMentions(text)) text = TextUtils.getMentionText(text); // for mentions
            binding.tvMessage.setText(text);
        }
        final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
        final MediaItemType mediaType = reelShareMedia.getMediaType();
        if (mediaType == null) {
            binding.mediaExpiredIcon.setVisibility(View.VISIBLE);
        } else {
            binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                                                   mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
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
