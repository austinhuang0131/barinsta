package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.DirectMessageInboxItemViewHolder;
import awais.instagrabber.databinding.LayoutIncludeSimpleItemBinding;
import awais.instagrabber.models.direct_messages.InboxThreadModel;

public final class DirectMessageInboxAdapter extends ListAdapter<InboxThreadModel, DirectMessageInboxItemViewHolder> {
    private final OnItemClickListener onClickListener;

    private static final DiffUtil.ItemCallback<InboxThreadModel> diffCallback = new DiffUtil.ItemCallback<InboxThreadModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final InboxThreadModel oldItem, @NonNull final InboxThreadModel newItem) {
            return oldItem.getThreadId().equals(newItem.getThreadId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final InboxThreadModel oldItem, @NonNull final InboxThreadModel newItem) {
            return oldItem.equals(newItem);
        }
    };

    public DirectMessageInboxAdapter(final OnItemClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public DirectMessageInboxItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final LayoutIncludeSimpleItemBinding binding = LayoutIncludeSimpleItemBinding.inflate(layoutInflater, parent, false);
        return new DirectMessageInboxItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final DirectMessageInboxItemViewHolder holder, final int position) {
        final InboxThreadModel threadModel = getItem(position);
        if (onClickListener != null) {
            holder.itemView.setOnClickListener((v) -> onClickListener.onItemClick(threadModel));
        }
        holder.bind(threadModel);
    }

    public interface OnItemClickListener {
        void onItemClick(final InboxThreadModel inboxThreadModel);
    }
}