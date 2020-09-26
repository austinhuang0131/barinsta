package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.adapters.FavoritesAdapter;
import awais.instagrabber.databinding.ItemSuggestionBinding;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;

public class FavoriteViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "FavoriteViewHolder";

    private final ItemSuggestionBinding binding;

    public FavoriteViewHolder(@NonNull final ItemSuggestionBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        binding.isVerified.setVisibility(View.GONE);
    }

    public void bind(final DataBox.FavoriteModel model,
                     final FavoritesAdapter.OnFavoriteClickListener clickListener,
                     final FavoritesAdapter.OnFavoriteLongClickListener longClickListener) {
        // Log.d(TAG, "bind: " + model);
        if (model == null) return;
        itemView.setOnClickListener(v -> {
            if (clickListener == null) return;
            clickListener.onClick(model);
        });
        itemView.setOnLongClickListener(v -> {
            if (clickListener == null) return false;
            return longClickListener.onLongClick(model);
        });
        if (model.getType() == FavoriteType.HASHTAG) {
            binding.ivProfilePic.setImageURI(Constants.DEFAULT_HASH_TAG_PIC);
        } else {
            binding.ivProfilePic.setImageURI(model.getPicUrl());
        }
        binding.tvFullName.setText(model.getDisplayName());
        binding.tvUsername.setVisibility(View.VISIBLE);
        String query = model.getQuery();
        switch (model.getType()) {
            case HASHTAG:
                query = "#" + query;
                break;
            case USER:
                query = "@" + query;
                break;
            case LOCATION:
                binding.tvUsername.setVisibility(View.GONE);
                break;
            default:
                // do nothing
        }
        binding.tvUsername.setText(query);
    }
}
