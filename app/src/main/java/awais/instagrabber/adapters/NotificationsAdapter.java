package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.adapters.viewholder.NotificationViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.models.enums.NotificationType;

public final class NotificationsAdapter extends ListAdapter<NotificationModel, NotificationViewHolder> {
    private final OnNotificationClickListener notificationClickListener;
    private final MentionClickListener mentionClickListener;

    private static final DiffUtil.ItemCallback<NotificationModel> DIFF_CALLBACK = new DiffUtil.ItemCallback<NotificationModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final NotificationModel oldItem, @NonNull final NotificationModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final NotificationModel oldItem, @NonNull final NotificationModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public NotificationsAdapter(final OnNotificationClickListener notificationClickListener,
                                final MentionClickListener mentionClickListener) {
        super(DIFF_CALLBACK);
        this.notificationClickListener = notificationClickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemNotificationBinding binding = ItemNotificationBinding.inflate(layoutInflater, parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotificationViewHolder holder, final int position) {
        final NotificationModel notificationModel = getItem(position);
        holder.bind(notificationModel, notificationClickListener);
    }

    @Override
    public void submitList(@Nullable final List<NotificationModel> list, @Nullable final Runnable commitCallback) {
        if (list == null) {
            super.submitList(null, commitCallback);
            return;
        }
        super.submitList(sort(list), commitCallback);
    }

    @Override
    public void submitList(@Nullable final List<NotificationModel> list) {
        if (list == null) {
            super.submitList(null);
            return;
        }
        super.submitList(sort(list));
    }

    private List<NotificationModel> sort(final List<NotificationModel> list) {
        final List<NotificationModel> listCopy = new ArrayList<>(list);
        Collections.sort(listCopy, (o1, o2) -> {
            if (o1.getType() == o2.getType()) return 0;
            // keep requests at top
            if (o1.getType() == NotificationType.REQUEST) return -1;
            if (o2.getType() == NotificationType.REQUEST) return 1;
            return 0;
        });
        return listCopy;
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(final NotificationModel model);
    }
}