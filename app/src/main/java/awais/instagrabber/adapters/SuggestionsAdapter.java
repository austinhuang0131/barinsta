package awais.instagrabber.adapters;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;

import awais.instagrabber.databinding.ItemSuggestionBinding;
import awais.instagrabber.models.enums.SuggestionType;

public final class SuggestionsAdapter extends CursorAdapter {
    private static final String TAG = "SuggestionsAdapter";

    private final OnSuggestionClickListener onSuggestionClickListener;

    public SuggestionsAdapter(final Context context,
                              final OnSuggestionClickListener onSuggestionClickListener) {
        super(context, null, FLAG_REGISTER_CONTENT_OBSERVER);
        this.onSuggestionClickListener = onSuggestionClickListener;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        final ItemSuggestionBinding binding = ItemSuggestionBinding.inflate(layoutInflater, parent, false);
        return binding.getRoot();
        // return layoutInflater.inflate(R.layout.item_suggestion, parent, false);
    }

    @Override
    public void bindView(@NonNull final View view, final Context context, @NonNull final Cursor cursor) {
        // i, username, fullname, type, query, picUrl, verified
        // 0, 1       , 2       , 3   , 4    , 5     , 6
        final String fullName = cursor.getString(2);
        String username = cursor.getString(1);
        String picUrl = cursor.getString(5);
        final boolean verified = cursor.getString(6).charAt(0) == 't';

        final String type = cursor.getString(3);
        SuggestionType suggestionType = null;
        try {
            suggestionType = SuggestionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown suggestion type: " + type, e);
        }
        if (suggestionType == null) return;
        String query = cursor.getString(4);
        switch (suggestionType) {
            case TYPE_USER:
                username = '@' + username;
                break;
            case TYPE_HASHTAG:
                username = '#' + username;
                break;
        }

        if (onSuggestionClickListener != null) {
            final SuggestionType finalSuggestionType = suggestionType;
            view.setOnClickListener(v -> onSuggestionClickListener.onSuggestionClick(finalSuggestionType, query));
        }
        final ItemSuggestionBinding binding = ItemSuggestionBinding.bind(view);
        binding.isVerified.setVisibility(verified ? View.VISIBLE : View.GONE);
        binding.tvUsername.setText(username);
        binding.tvFullName.setVisibility(View.VISIBLE);
        binding.tvFullName.setText(fullName);
        binding.ivProfilePic.setImageURI(picUrl);
    }

    public interface OnSuggestionClickListener {
        void onSuggestionClick(final SuggestionType type, final String query);
    }
}