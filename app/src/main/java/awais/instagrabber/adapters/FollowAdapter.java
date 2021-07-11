package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.FollowsViewHolder;
import awais.instagrabber.databinding.ItemFollowBinding;
import awais.instagrabber.interfaces.OnGroupClickListener;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import thoughtbot.expandableadapter.ExpandableGroup;
import thoughtbot.expandableadapter.ExpandableList;
import thoughtbot.expandableadapter.ExpandableListPosition;
import thoughtbot.expandableadapter.GroupViewHolder;

// thanks to ThoughtBot's ExpandableRecyclerViewAdapter
//   https://github.com/thoughtbot/expandable-recycler-view
public final class FollowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnGroupClickListener, Filterable {
    private final View.OnClickListener onClickListener;
    private final ExpandableList expandableListOriginal;
    private final boolean hasManyGroups;
    private ExpandableList expandableList;

    private final Filter filter = new Filter() {
        @Nullable
        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            final List<User> filteredItems = new ArrayList<User>();
            if (expandableListOriginal.groups == null || TextUtils.isEmpty(filter)) return null;
            final String query = filter.toString().toLowerCase();
            final ArrayList<ExpandableGroup> groups = new ArrayList<ExpandableGroup>();
            for (int x = 0; x < expandableListOriginal.groups.size(); ++x) {
                final ExpandableGroup expandableGroup = expandableListOriginal.groups.get(x);
                final String title = expandableGroup.getTitle();
                final List<User> items = expandableGroup.getItems();
                if (items != null) {
                    final List<User> toReturn = items.stream()
                            .filter(u -> hasKey(query, u.getUsername(), u.getFullName()))
                            .collect(Collectors.toList());
                    groups.add(new ExpandableGroup(title, toReturn));
                }
            }
            final FilterResults filterResults = new FilterResults();
            filterResults.values = new ExpandableList(groups, expandableList.expandedGroupIndexes);
            return filterResults;
        }

        private boolean hasKey(final String key, final String username, final String name) {
            if (TextUtils.isEmpty(key)) return true;
            final boolean hasUserName = username != null && username.toLowerCase().contains(key);
            if (!hasUserName && name != null) return name.toLowerCase().contains(key);
            return true;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            if (results == null) {
                expandableList = expandableListOriginal;
            }
            else {
                final ExpandableList filteredList = (ExpandableList) results.values;
                expandableList = filteredList;
            }
            notifyDataSetChanged();
        }
    };

    public FollowAdapter(final View.OnClickListener onClickListener, @NonNull final ArrayList<ExpandableGroup> groups) {
        this.expandableListOriginal = new ExpandableList(groups);
        expandableList = this.expandableListOriginal;
        this.onClickListener = onClickListener;
        this.hasManyGroups = groups.size() > 1;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final boolean isGroup = hasManyGroups && viewType == ExpandableListPosition.GROUP;
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final View view;
        if (isGroup) {
            view = layoutInflater.inflate(R.layout.header_follow, parent, false);
            return new GroupViewHolder(view, this);
        } else {
            final ItemFollowBinding binding = ItemFollowBinding.inflate(layoutInflater, parent, false);
            return new FollowsViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final ExpandableListPosition listPos = expandableList.getUnflattenedPosition(position);
        final ExpandableGroup group = expandableList.getExpandableGroup(listPos);

        if (hasManyGroups && listPos.type == ExpandableListPosition.GROUP) {
            final GroupViewHolder gvh = (GroupViewHolder) holder;
            gvh.setTitle(group.getTitle());
            gvh.toggle(isGroupExpanded(group));
            return;
        }
        final User model = group.getItems().get(hasManyGroups ? listPos.childPos : position);
        ((FollowsViewHolder) holder).bind(model, onClickListener);
    }

    @Override
    public int getItemCount() {
        return expandableList.getVisibleItemCount() - (hasManyGroups ? 0 : 1);
    }

    @Override
    public int getItemViewType(final int position) {
        return !hasManyGroups ? 0 : expandableList.getUnflattenedPosition(position).type;
    }

    @Override
    public void toggleGroup(final int flatPos) {
        final ExpandableListPosition listPosition = expandableList.getUnflattenedPosition(flatPos);

        final int groupPos = listPosition.groupPos;
        final int positionStart = expandableList.getFlattenedGroupIndex(listPosition) + 1;
        final int positionEnd = expandableList.groups.get(groupPos).getItemCount();

        final boolean isExpanded = expandableList.expandedGroupIndexes[groupPos];
        expandableList.expandedGroupIndexes[groupPos] = !isExpanded;
        notifyItemChanged(positionStart - 1);
        if (positionEnd > 0) {
            if (isExpanded) notifyItemRangeRemoved(positionStart, positionEnd);
            else notifyItemRangeInserted(positionStart, positionEnd);
        }
    }

    public boolean isGroupExpanded(final ExpandableGroup group) {
        return expandableList.expandedGroupIndexes[expandableList.groups.indexOf(group)];
    }
}