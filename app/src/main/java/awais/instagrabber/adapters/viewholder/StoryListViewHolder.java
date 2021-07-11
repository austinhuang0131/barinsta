package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedStoriesListAdapter.OnFeedStoryClickListener;
import awais.instagrabber.adapters.HighlightStoriesListAdapter.OnHighlightStoryClickListener;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.ResponseBodyUtils;

public final class StoryListViewHolder extends RecyclerView.ViewHolder {
    private final ItemNotificationBinding binding;

    public StoryListViewHolder(final ItemNotificationBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final Story model,
                     final OnFeedStoryClickListener notificationClickListener) {
        if (model == null) return;

        final int storiesCount = model.getMediaCount();
        binding.tvComment.setVisibility(View.VISIBLE);
        binding.tvComment.setText(itemView.getResources().getQuantityString(R.plurals.stories_count, storiesCount, storiesCount));

        binding.tvSubComment.setVisibility(View.GONE);

        binding.tvDate.setText(model.getDateTime());

        binding.tvUsername.setText(model.getUser().getUsername());
        binding.ivProfilePic.setImageURI(model.getUser().getProfilePicUrl());
        binding.ivProfilePic.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onProfileClick(model.getUser().getUsername());
        });

        if (model.getItems() != null && model.getItems().size() > 0) {
            binding.ivPreviewPic.setVisibility(View.VISIBLE);
            binding.ivPreviewPic.setImageURI(ResponseBodyUtils.getThumbUrl(model.getItems().get(0)));
        } else binding.ivPreviewPic.setVisibility(View.INVISIBLE);

        float alpha = model.getSeen() != null && model.getSeen().equals(model.getLatestReelMedia())
                ? 0.5F : 1.0F;
        binding.ivProfilePic.setAlpha(alpha);
        binding.ivPreviewPic.setAlpha(alpha);
        binding.tvUsername.setAlpha(alpha);
        binding.tvComment.setAlpha(alpha);
        binding.tvDate.setAlpha(alpha);

        itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onFeedStoryClick(model);
        });
    }

    public void bind(final Story model,
                     final int position,
                     final OnHighlightStoryClickListener notificationClickListener) {
        if (model == null) return;

        final int storiesCount = model.getMediaCount();
        binding.tvComment.setVisibility(View.VISIBLE);
        binding.tvComment.setText(itemView.getResources().getQuantityString(R.plurals.stories_count, storiesCount, storiesCount));

        binding.tvSubComment.setVisibility(View.GONE);

        binding.tvUsername.setText(model.getDateTime());

        binding.ivProfilePic.setVisibility(View.GONE);

        binding.ivPreviewPic.setVisibility(View.VISIBLE);
        binding.ivPreviewPic.setImageURI(model.getCoverImageVersion().getUrl());

        itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onHighlightClick(model, position);
        });
    }
}