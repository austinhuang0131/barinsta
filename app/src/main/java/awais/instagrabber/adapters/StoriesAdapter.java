package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.databinding.ItemStoryBinding;
import awais.instagrabber.models.StoryModel;

public final class StoriesAdapter extends ListAdapter<StoryModel, StoriesAdapter.StoryViewHolder> {
    private final OnItemClickListener onItemClickListener;

    private static final DiffUtil.ItemCallback<StoryModel> diffCallback = new DiffUtil.ItemCallback<StoryModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final StoryModel oldItem, @NonNull final StoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final StoryModel oldItem, @NonNull final StoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId());
        }
    };
    private int activeIndex;

    public StoriesAdapter(final OnItemClickListener onItemClickListener) {
        super(diffCallback);
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemStoryBinding binding = ItemStoryBinding.inflate(layoutInflater, parent, false);
        return new StoryViewHolder(binding, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final StoryViewHolder holder, final int position) {
        final StoryModel storyModel = getItem(position);
        holder.bind(storyModel, position, activeIndex == position);
    }

    public void setActiveIndex(final int activeIndex) {
        int prevActiveIndex = this.activeIndex;
        this.activeIndex = activeIndex;
        // notify prev and current
        notifyItemChanged(prevActiveIndex);
        notifyItemChanged(activeIndex);
    }

    public final static class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemStoryBinding binding;
        private final OnItemClickListener clickListener;

        public StoryViewHolder(final ItemStoryBinding binding,
                               final OnItemClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
        }

        public void bind(final StoryModel model,
                         final int position,
                         final boolean isActive) {
            if (model == null) return;
            // model.setPosition(position);

            itemView.setTag(model);
            itemView.setOnClickListener(v -> {
                if (clickListener == null) return;
                clickListener.onItemClick(model, position);
            });

            binding.selectedView.setVisibility(isActive ? View.VISIBLE : View.GONE);
            binding.icon.setImageURI(model.getStoryUrl());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(StoryModel storyModel, int position);
    }
}