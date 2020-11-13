package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public abstract class DirectMessageItemViewHolder extends RecyclerView.ViewHolder {
    private static final int MESSAGE_INCOMING = 69;
    private static final int MESSAGE_OUTGOING = 420;

    private final ProfileModel myProfileHolder = ProfileModel.getDefaultProfileModel(
            CookieUtils.getUserIdFromCookie(Utils.settingsHelper.getString(Constants.COOKIE)));
    private final LayoutDmBaseBinding binding;
    private final int itemMargin;

    public DirectMessageItemViewHolder(@NonNull final LayoutDmBaseBinding binding, @NonNull final View.OnClickListener onClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        binding.ivProfilePic.setOnClickListener(onClickListener);
        binding.messageCard.setOnClickListener(onClickListener);
        // final String strDmYou = binding.getRoot().getContext().getString(R.string.direct_messages_you);
        itemMargin = Utils.displayMetrics.widthPixels / 5;
    }

    public void bind(final DirectItemModel directItemModel, final List<ProfileModel> users, final List<ProfileModel> leftUsers) {
        final ProfileModel user = getUser(directItemModel.getUserId(), users, leftUsers);
        final int type = user == myProfileHolder ? MESSAGE_OUTGOING : MESSAGE_INCOMING;

        final RecyclerView.LayoutParams itemViewLayoutParams = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        itemViewLayoutParams.setMargins(type == MESSAGE_OUTGOING ? itemMargin : 0, 0,
                                        type == MESSAGE_INCOMING ? itemMargin : 0, 0);

        final ViewGroup messageCardParent = (ViewGroup) binding.messageCard.getParent();
        binding.contentContainer.setGravity(type == MESSAGE_INCOMING ? Gravity.START : Gravity.END);

        CharSequence text = "?";
        if (user != null && user != myProfileHolder) {
            text = user.getUsername();
        } else if (user == myProfileHolder) text = "";
        text = (TextUtils.isEmpty(text) ? "" : text + " - ") + directItemModel.getDateTime();
        binding.tvUsername.setText(text);
        binding.tvUsername.setGravity(type == MESSAGE_INCOMING ? Gravity.START : Gravity.END);
        binding.ivProfilePic.setVisibility(type == MESSAGE_INCOMING ? View.VISIBLE : View.GONE);
        binding.ivProfilePic.setTag(user);
        binding.likedContainer.setVisibility(directItemModel.isLiked() ? View.VISIBLE : View.GONE);
        messageCardParent.setTag(directItemModel);
        binding.messageCard.setTag(directItemModel);

        if (type == MESSAGE_INCOMING && user != null) {
            binding.ivProfilePic.setImageURI(user.getSdProfilePic());
        }

        bindItem(directItemModel);
    }

    public void setItemView(final View view) {
        this.binding.messageCard.addView(view);
    }

    public int getItemMargin() {
        return itemMargin;
    }

    public abstract void bindItem(final DirectItemModel directItemModel);

    @Nullable
    private ProfileModel getUser(final long userId, final List<ProfileModel> users, final List<ProfileModel> leftUsers) {
        if (users != null) {
            ProfileModel result = myProfileHolder;
            for (final ProfileModel user : users) {
                if (Long.toString(userId).equals(user.getId())) result = user;
            }
            if (leftUsers != null)
                for (final ProfileModel leftUser : leftUsers) {
                    if (Long.toString(userId).equals(leftUser.getId())) result = leftUser;
                }
            return result;
        }
        return null;
    }

    protected void setupForAnimatedMedia() {
        binding.messageCard.setCardElevation(0);
        binding.messageCard.setCardBackgroundColor(itemView.getResources().getColor(android.R.color.transparent));
    }
}
