package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.adapters.viewholder.StoryListViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.TextUtils;

public final class FeedStoriesListAdapter extends ListAdapter<Story, StoryListViewHolder> implements Filterable {
    private final OnFeedStoryClickListener listener;
    private List<Story> list;

    private final Filter filter = new Filter() {
        @NonNull
        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            final String query = TextUtils.isEmpty(filter) ? null : filter.toString().toLowerCase();
            List<Story> filteredList = list;
            if (list != null && query != null) {
                filteredList = list.stream()
                                   .filter(feedStoryModel -> feedStoryModel.getUser()
                                                                           .getUsername()
                                                                           .toLowerCase()
                                                                           .contains(query))
                                   .collect(Collectors.toList());
            }
            final FilterResults filterResults = new FilterResults();
            filterResults.count = filteredList != null ? filteredList.size() : 0;
            filterResults.values = filteredList;
            return filterResults;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            //noinspection unchecked
            submitList((List<Story>) results.values, true);
        }
    };

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

    public FeedStoriesListAdapter(final OnFeedStoryClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private void submitList(@Nullable final List<Story> list, final boolean isFiltered) {
        if (!isFiltered) {
            this.list = list;
        }
        super.submitList(list);
    }

    @Override
    public void submitList(final List<Story> list) {
        submitList(list, false);
    }

    @NonNull
    @Override
    public StoryListViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemNotificationBinding binding = ItemNotificationBinding.inflate(layoutInflater, parent, false);
        return new StoryListViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final StoryListViewHolder holder, final int position) {
        final Story model = getItem(position);
        holder.bind(model, listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(final Story model);

        void onProfileClick(final String username);
    }
}
