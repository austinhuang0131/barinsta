package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.HighlightViewHolder;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.ProfileModel;

public final class FeedStoriesAdapter extends RecyclerView.Adapter<HighlightViewHolder> {
    private final View.OnClickListener clickListener;
    private LayoutInflater layoutInflater;
    private FeedStoryModel[] feedStoryModels;

    public FeedStoriesAdapter(final FeedStoryModel[] feedStoryModels, final View.OnClickListener clickListener) {
        this.feedStoryModels = feedStoryModels;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public HighlightViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(parent.getContext());
        return new HighlightViewHolder(layoutInflater.inflate(R.layout.item_highlight, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final HighlightViewHolder holder, final int position) {
        final FeedStoryModel feedStoryModel = feedStoryModels[position];
        if (feedStoryModel != null) {
            holder.itemView.setTag(feedStoryModel);
            holder.itemView.setOnClickListener(clickListener);

            final ProfileModel profileModel = feedStoryModel.getProfileModel();

            holder.title.setText(profileModel.getUsername());
            Glide.with(layoutInflater.getContext()).load(profileModel.getSdProfilePic()).into(holder.icon);
            holder.icon.setAlpha(feedStoryModel.getFullyRead() ? 0.5F : 1.0F);
            holder.title.setAlpha(feedStoryModel.getFullyRead() ? 0.5F : 1.0F);
        }
    }

    public void setData(final FeedStoryModel[] feedStoryModels) {
        this.feedStoryModels = feedStoryModels;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return feedStoryModels == null ? 0 : feedStoryModels.length;
    }
}
