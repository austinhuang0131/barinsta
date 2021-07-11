package awais.instagrabber.adapters.viewholder.feed;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.databinding.ItemFeedTopBinding;
import awais.instagrabber.databinding.LayoutPostViewBottomBinding;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.text.TextUtils.TruncateAt.END;

public abstract class FeedItemViewHolder extends RecyclerView.ViewHolder {
    public static final int MAX_LINES_COLLAPSED = 5;
    private final ItemFeedTopBinding topBinding;
    private final LayoutPostViewBottomBinding bottomBinding;
    private final ViewGroup bottomFrame;
    private final FeedAdapterV2.FeedItemCallback feedItemCallback;

    public FeedItemViewHolder(@NonNull final ViewGroup root,
                              final FeedAdapterV2.FeedItemCallback feedItemCallback) {
        super(root);
        this.bottomFrame = root;
        this.topBinding = ItemFeedTopBinding.bind(root);
        this.bottomBinding = LayoutPostViewBottomBinding.bind(root);
        this.feedItemCallback = feedItemCallback;
    }

    public void bind(final Media media) {
        if (media == null) {
            return;
        }
        setupProfilePic(media);
        bottomBinding.date.setText(media.getDate());
        setupComments(media);
        setupCaption(media);
        setupActions(media);
        if (media.getType() != MediaItemType.MEDIA_TYPE_SLIDER) {
            bottomBinding.download.setOnClickListener(v ->
                    feedItemCallback.onDownloadClick(media, -1, null)
            );
        }
        bindItem(media);
        bottomFrame.post(() -> setupLocation(media));
    }

    private void setupComments(@NonNull final Media feedModel) {
        final long commentsCount = feedModel.getCommentCount();
        bottomBinding.commentsCount.setText(String.valueOf(commentsCount));
        bottomBinding.comment.setOnClickListener(v -> feedItemCallback.onCommentsClick(feedModel));
    }

    private void setupProfilePic(@NonNull final Media media) {
        final User user = media.getUser();
        if (user == null) {
            topBinding.profilePic.setVisibility(View.GONE);
            topBinding.title.setVisibility(View.GONE);
            topBinding.subtitle.setVisibility(View.GONE);
            return;
        }
        topBinding.profilePic.setOnClickListener(v -> feedItemCallback.onProfilePicClick(media));
        topBinding.profilePic.setImageURI(user.getProfilePicUrl());
        setupTitle(media);
    }

    private void setupTitle(@NonNull final Media media) {
        // final int titleLen = profileModel.getUsername().length() + 1;
        // final SpannableString spannableString = new SpannableString();
        // spannableString.setSpan(new CommentMentionClickSpan(), 0, titleLen, 0);
        final User user = media.getUser();
        if (user == null) return;
        setUsername(user);
        topBinding.title.setOnClickListener(v -> feedItemCallback.onNameClick(media));
        final String fullName = user.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            topBinding.subtitle.setVisibility(View.GONE);
        } else {
            topBinding.subtitle.setVisibility(View.VISIBLE);
            topBinding.subtitle.setText(fullName);
        }
        topBinding.subtitle.setOnClickListener(v -> feedItemCallback.onNameClick(media));
    }

    private void setupCaption(final Media media) {
        bottomBinding.caption.clearOnMentionClickListeners();
        bottomBinding.caption.clearOnHashtagClickListeners();
        bottomBinding.caption.clearOnURLClickListeners();
        bottomBinding.caption.clearOnEmailClickListeners();
        final Caption caption = media.getCaption();
        if (caption == null) {
            bottomBinding.caption.setVisibility(View.GONE);
            return;
        }
        final CharSequence postCaption = caption.getText();
        final boolean captionEmpty = TextUtils.isEmpty(postCaption);
        bottomBinding.caption.setVisibility(captionEmpty ? View.GONE : View.VISIBLE);
        if (captionEmpty) return;
        bottomBinding.caption.setText(postCaption);
        bottomBinding.caption.setMaxLines(MAX_LINES_COLLAPSED);
        bottomBinding.caption.setEllipsize(END);
        bottomBinding.caption.setOnClickListener(v -> bottomFrame.post(() -> {
            TransitionManager.beginDelayedTransition(bottomFrame);
            if (bottomBinding.caption.getMaxLines() == MAX_LINES_COLLAPSED) {
                bottomBinding.caption.setMaxLines(Integer.MAX_VALUE);
                bottomBinding.caption.setEllipsize(null);
                return;
            }
            bottomBinding.caption.setMaxLines(MAX_LINES_COLLAPSED);
            bottomBinding.caption.setEllipsize(END);
        }));
        bottomBinding.caption.addOnMentionClickListener(autoLinkItem -> feedItemCallback.onMentionClick(autoLinkItem.getOriginalText()));
        bottomBinding.caption.addOnHashtagListener(autoLinkItem -> feedItemCallback.onHashtagClick(autoLinkItem.getOriginalText()));
        bottomBinding.caption.addOnEmailClickListener(autoLinkItem -> feedItemCallback.onEmailClick(autoLinkItem.getOriginalText()));
        bottomBinding.caption.addOnURLClickListener(autoLinkItem -> feedItemCallback.onURLClick(autoLinkItem.getOriginalText()));
    }

    private void setupLocation(@NonNull final Media media) {
        final Location location = media.getLocation();
        if (location == null) {
            topBinding.location.setVisibility(View.GONE);
        } else {
            final String locationName = location.getName();
            if (TextUtils.isEmpty(locationName)) {
                topBinding.location.setVisibility(View.GONE);
            } else {
                topBinding.location.setVisibility(View.VISIBLE);
                topBinding.location.setText(locationName);
                topBinding.location.setOnClickListener(v -> feedItemCallback.onLocationClick(media));
            }
        }
    }

    private void setupActions(@NonNull final Media media) {
        // temporary - to be set up later
        bottomBinding.like.setVisibility(View.GONE);
        bottomBinding.save.setVisibility(View.GONE);
        bottomBinding.translate.setVisibility(View.GONE);
        bottomBinding.share.setVisibility(View.GONE);
    }

    private void setUsername(final User user) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
        final int drawableSize = Utils.convertDpToPx(24);
        if (user.isVerified()) {
            final Drawable verifiedDrawable = itemView.getResources().getDrawable(R.drawable.verified);
            VerticalImageSpan verifiedSpan = null;
            if (verifiedDrawable != null) {
                final Drawable drawable = verifiedDrawable.mutate();
                drawable.setBounds(0, 0, drawableSize, drawableSize);
                verifiedSpan = new VerticalImageSpan(drawable);
            }
            try {
                if (verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (Exception e) {
                Log.e("FeedItemViewHolder", "setUsername: ", e);
            }
        }
        topBinding.title.setText(sb);
    }

    public abstract void bindItem(final Media media);
}