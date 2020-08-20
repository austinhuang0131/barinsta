package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;

import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageViewHolder;
import awais.instagrabber.databinding.ItemMessageItemBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;

public final class MessageItemsAdapter extends ListAdapter<DirectItemModel, DirectMessageViewHolder> {
    private final List<ProfileModel> users;
    private final List<ProfileModel> leftUsers;
    private final View.OnClickListener onClickListener;
    private final MentionClickListener mentionClickListener;

    private static final DiffUtil.ItemCallback<DirectItemModel> diffCallback = new DiffUtil.ItemCallback<DirectItemModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final DirectItemModel oldItem, @NonNull final DirectItemModel newItem) {
            return oldItem.getItemId().equals(newItem.getItemId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final DirectItemModel oldItem, @NonNull final DirectItemModel newItem) {
            return oldItem.getItemId().equals(newItem.getItemId());
        }
    };

    public MessageItemsAdapter(final List<ProfileModel> users,
                               final List<ProfileModel> leftUsers,
                               final View.OnClickListener onClickListener,
                               final MentionClickListener mentionClickListener) {
        super(diffCallback);
        this.users = users;
        this.leftUsers = leftUsers;
        this.onClickListener = onClickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public DirectMessageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemMessageItemBinding binding = ItemMessageItemBinding.inflate(layoutInflater, parent, false);
        return new DirectMessageViewHolder(binding, users, leftUsers, onClickListener, mentionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final DirectMessageViewHolder holder, final int position) {
        final DirectItemModel directItemModel = getItem(position);
        holder.bind(directItemModel);
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position).getItemType().ordinal();
    }
}