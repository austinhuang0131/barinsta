package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import awais.instagrabber.adapters.DirectItemsAdapter.DirectItemCallback;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemPlaceholderViewHolder extends DirectItemViewHolder {

    private final LayoutDmStoryShareBinding binding;

    public DirectItemPlaceholderViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                           final LayoutDmStoryShareBinding binding,
                                           final User currentUser,
                                           final DirectThread thread,
                                           final DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        binding.shareInfo.setText(directItemModel.getPlaceholder().getTitle());
        binding.text.setVisibility(View.VISIBLE);
        binding.text.setText(directItemModel.getPlaceholder().getMessage());
        binding.ivMediaPreview.setVisibility(View.GONE);
        binding.typeIcon.setVisibility(View.GONE);
    }

    @Override
    protected boolean allowLongClick() {
        return false;
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }
}
