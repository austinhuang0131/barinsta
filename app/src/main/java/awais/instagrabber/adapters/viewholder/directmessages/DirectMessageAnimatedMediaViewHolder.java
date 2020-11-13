package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.facebook.drawee.backends.pipeline.Fresco;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

public class DirectMessageAnimatedMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmAnimatedMediaBinding binding;
    private final int maxHeight;
    private final int maxWidth;

    public DirectMessageAnimatedMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                                @NonNull final LayoutDmAnimatedMediaBinding binding,
                                                final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        maxWidth = Utils.displayMetrics.widthPixels - Utils.convertDpToPx(64) - getItemMargin();
        setItemView(binding.getRoot());
        setupForAnimatedMedia();
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemAnimatedMediaModel animatedMediaModel = directItemModel.getAnimatedMediaModel();
        final String url = animatedMediaModel.getWebpUrl();
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                animatedMediaModel.getHeight(),
                animatedMediaModel.getWidth(),
                maxHeight,
                maxWidth
        );
        binding.ivAnimatedMessage.setVisibility(View.VISIBLE);
        final ViewGroup.LayoutParams layoutParams = binding.ivAnimatedMessage.getLayoutParams();
        final int width = widthHeight.first != null ? widthHeight.first : 0;
        final int height = widthHeight.second != null ? widthHeight.second : 0;
        layoutParams.width = width;
        layoutParams.height = height;
        binding.ivAnimatedMessage.requestLayout();
        binding.ivAnimatedMessage.setController(Fresco.newDraweeControllerBuilder()
                                                      .setUri(url)
                                                      .setAutoPlayAnimations(true)
                                                      .build());
    }
}
