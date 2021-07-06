package awais.instagrabber.adapters;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.databinding.ItemStoryBinding;
import awais.instagrabber.repositories.responses.stories.StoryMedia;
import awais.instagrabber.utils.ResponseBodyUtils;

public final class StoriesAdapter extends ListAdapter<StoryMedia, StoriesAdapter.StoryViewHolder> {
    private final OnItemClickListener onItemClickListener;

    private static final DiffUtil.ItemCallback<StoryMedia> diffCallback = new DiffUtil.ItemCallback<StoryMedia>() {
        @Override
        public boolean areItemsTheSame(@NonNull final StoryMedia oldItem, @NonNull final StoryMedia newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final StoryMedia oldItem, @NonNull final StoryMedia newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public StoriesAdapter(final OnItemClickListener onItemClickListener) {
        super(diffCallback);
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemStoryBinding binding = ItemStoryBinding.inflate(layoutInflater, parent, false);
        return new StoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final StoryViewHolder holder, final int position) {
        final StoryMedia storyMedia = getItem(position);
        holder.bind(storyMedia, position, onItemClickListener);
    }

    public final static class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemStoryBinding binding;

        public StoryViewHolder(final ItemStoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final StoryMedia model,
                         final int position,
                         final OnItemClickListener clickListener) {
            if (model == null) return;
            model.setPosition(position);

            itemView.setTag(model);
            itemView.setOnClickListener(v -> {
                if (clickListener == null) return;
                clickListener.onItemClick(model, position);
            });

            binding.selectedView.setVisibility(model.isCurrentSlide() ? View.VISIBLE : View.GONE);
            binding.icon.setImageURI(ResponseBodyUtils.getThumbUrl(model));
        }
    }

    public void paginate(final int newIndex) {
        final List<StoryMedia> list = getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            final StoryMedia item = list.get(i);
            if (!item.isCurrentSlide() && i != newIndex) continue;
            item.setCurrentSlide(i == newIndex);
            notifyItemChanged(i, item);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(StoryMedia storyModel, int position);
    }
}