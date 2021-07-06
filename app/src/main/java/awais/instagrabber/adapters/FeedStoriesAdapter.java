package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.FeedStoryViewHolder;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.repositories.responses.stories.Story;

public final class FeedStoriesAdapter extends ListAdapter<Story, FeedStoryViewHolder> {
    private final OnFeedStoryClickListener listener;

    private static final DiffUtil.ItemCallback<Story> diffCallback = new DiffUtil.ItemCallback<Story>() {
        @Override
        public boolean areItemsTheSame(@NonNull final Story oldItem, @NonNull final Story newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final Story oldItem, @NonNull final Story newItem) {
            return oldItem.getId().equals(newItem.getId()) && oldItem.getSeen() == newItem.getSeen();
        }
    };

    public FeedStoriesAdapter(final OnFeedStoryClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedStoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemHighlightBinding binding = ItemHighlightBinding.inflate(layoutInflater, parent, false);
        return new FeedStoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final FeedStoryViewHolder holder, final int position) {
        final Story model = getItem(position);
        holder.bind(model, position, listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(Story model, int position);

        void onFeedStoryLongClick(Story model, int position);
    }
}
