package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.DirectMessageViewHolder;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemActionLogModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemReelShareModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.DirectItemType;

public final class DirectMessagesAdapter extends RecyclerView.Adapter<DirectMessageViewHolder> {
    private final ArrayList<InboxThreadModel> inboxThreadModels;
    private final View.OnClickListener onClickListener;
    private LayoutInflater layoutInflater;

    public DirectMessagesAdapter(final ArrayList<InboxThreadModel> inboxThreadModels, final View.OnClickListener onClickListener) {
        this.inboxThreadModels = inboxThreadModels;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public DirectMessageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(parent.getContext());
        return new DirectMessageViewHolder(layoutInflater.inflate(R.layout.layout_include_simple_item, parent, false),
                onClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final DirectMessageViewHolder holder, final int position) {
        final InboxThreadModel threadModel = inboxThreadModels.get(position);
        final DirectItemModel[] itemModels;

        holder.itemView.setTag(threadModel);

        final RequestManager glideRequestManager = Glide.with(holder.itemView);

        if (threadModel != null && (itemModels = threadModel.getItems()) != null) {
            final ProfileModel[] users = threadModel.getUsers();

            if (users.length > 1) {
                holder.ivProfilePic.setVisibility(View.GONE);
                holder.multipleProfilePicsContainer.setVisibility(View.VISIBLE);

                for (int i = 0; i < Math.min(3, users.length); ++i)
                    glideRequestManager.load(users[i].getSdProfilePic()).into(holder.multipleProfilePics[i]);

            } else if (users.length == 1) {
                holder.ivProfilePic.setVisibility(View.VISIBLE);
                holder.multipleProfilePicsContainer.setVisibility(View.GONE);

                glideRequestManager.load(users[0].getSdProfilePic()).into(holder.ivProfilePic);
            }

            holder.tvUsername.setText(threadModel.getThreadTitle());

            final DirectItemModel lastItemModel = itemModels[itemModels.length - 1];
            final DirectItemType itemType = lastItemModel.getItemType();

            holder.notTextType.setVisibility(itemType != DirectItemType.TEXT ? View.VISIBLE : View.GONE);

            final Context context = layoutInflater.getContext();

            final CharSequence messageText;
            switch (itemType) {
                case TEXT:
                    messageText = lastItemModel.getText();
                    break;
                case LINK:
                    messageText = context.getString(R.string.direct_messages_sent_link);
                    break;
                case MEDIA:
                case MEDIA_SHARE:
                    messageText = context.getString(R.string.direct_messages_sent_media);
                    break;
                case ACTION_LOG:
                    final DirectItemActionLogModel logModel = lastItemModel.getActionLogModel();
                    messageText = logModel != null ? logModel.getDescription() : "...";
                    break;
                case REEL_SHARE:
                    final DirectItemReelShareModel reelShare = lastItemModel.getReelShare();
                    if (reelShare == null)
                        messageText = context.getString(R.string.direct_messages_sent_media);
                    else {
                        final String reelType = reelShare.getType();
                        final int textRes;
                        if ("reply".equals(reelType))
                            textRes = R.string.direct_messages_replied_story;
                        else if ("mention".equals(reelType))
                            textRes = R.string.direct_messages_mention_story;
                        else if ("reaction".equals(reelType))
                            textRes = R.string.direct_messages_reacted_story;
                        else textRes = R.string.direct_messages_sent_media;

                        messageText = context.getString(textRes) + " : " + reelShare.getText();
                    }
                    break;
                case RAVEN_MEDIA:
                    messageText = context.getString(R.string.direct_messages_sent_media);
                    break;
                default:
                    messageText = "<i>Unsupported message</i>";
            }

            holder.tvMessage.setText(HtmlCompat.fromHtml(messageText.toString(), 63));

            holder.tvDate.setText(lastItemModel.getDateTime());
        }
    }

    @Override
    public int getItemCount() {
        return inboxThreadModels == null ? 0 : inboxThreadModels.size();
    }
}