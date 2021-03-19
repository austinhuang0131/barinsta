package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.dialogs.KeywordsFilterDialogViewHolder;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.SettingsHelper;

public class KeywordsFilterAdapter extends RecyclerView.Adapter<KeywordsFilterDialogViewHolder> {

    private final Context context;
    private final ArrayList<String> items;

    public KeywordsFilterAdapter(Context context, ArrayList<String> items){
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public KeywordsFilterDialogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keyword, parent, false);
        return new KeywordsFilterDialogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KeywordsFilterDialogViewHolder holder, int position) {
        holder.getTextView().setText(items.get(position));
        holder.getDeleteButton().setOnClickListener(view -> {
            SettingsHelper settingsHelper = new SettingsHelper(context);
            items.remove(items.get(position));
            settingsHelper.putStringSet(Constants.KEYWORD_FILTERS, new HashSet<>(items));
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return 0;
    }
}