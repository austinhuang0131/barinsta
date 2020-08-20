package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ItemMessageItemBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.RavenExpiringMediaType;
import awais.instagrabber.models.enums.RavenMediaViewType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public final class DirectMessageViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "DirectMessageViewHolder";
    private static final int MESSAGE_INCOMING = 69;
    private static final int MESSAGE_OUTGOING = 420;

    private final ProfileModel myProfileHolder = ProfileModel.getDefaultProfileModel(Utils.getUserIdFromCookie(Utils.settingsHelper.getString(Constants.COOKIE)));
    private final ItemMessageItemBinding binding;
    private final List<ProfileModel> users;
    private final List<ProfileModel> leftUsers;
    private final int itemMargin;
    private final String strDmYou;
    private DirectItemModel.DirectItemVoiceMediaModel prevVoiceModel;
    private ImageView prevPlayIcon;
    private View.OnClickListener onClickListener;
    private MentionClickListener mentionClickListener;

    private final View.OnClickListener voicePlayClickListener = v -> {
        final Object tag = v.getTag();
        if (v instanceof ViewGroup && tag instanceof DirectItemModel.DirectItemVoiceMediaModel) {
            final ImageView playIcon = (ImageView) ((ViewGroup) v).getChildAt(0);
            final DirectItemModel.DirectItemVoiceMediaModel voiceMediaModel = (DirectItemModel.DirectItemVoiceMediaModel) tag;
            final boolean voicePlaying = voiceMediaModel.isPlaying();
            voiceMediaModel.setPlaying(!voicePlaying);

            if (voiceMediaModel == prevVoiceModel) {
                // todo pause / resume
            } else {
                // todo release prev audio, start new voice
                if (prevVoiceModel != null) prevVoiceModel.setPlaying(false);
                if (prevPlayIcon != null)
                    prevPlayIcon.setImageResource(android.R.drawable.ic_media_play);
            }

            if (voicePlaying) {
                playIcon.setImageResource(android.R.drawable.ic_media_play);
            } else {
                playIcon.setImageResource(android.R.drawable.ic_media_pause);
            }

            prevVoiceModel = voiceMediaModel;
            prevPlayIcon = playIcon;
        }
    };

    public DirectMessageViewHolder(final ItemMessageItemBinding binding,
                                   final List<ProfileModel> users,
                                   final List<ProfileModel> leftUsers,
                                   final View.OnClickListener onClickListener,
                                   final MentionClickListener mentionClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.users = users;
        this.leftUsers = leftUsers;
        this.itemMargin = Utils.displayMetrics.widthPixels / 5;
        this.onClickListener = onClickListener;
        this.mentionClickListener = mentionClickListener;
        strDmYou = binding.getRoot().getContext().getString(R.string.direct_messages_you);
    }

    public void bind(final DirectItemModel directItemModel) {
        if (directItemModel == null) {
            return;
        }
        final Context context = itemView.getContext();
        //itemView.setTag(directItemModel);
        final DirectItemType itemType = directItemModel.getItemType();
        final ProfileModel user = getUser(directItemModel.getUserId());
        final int type = user == myProfileHolder ? MESSAGE_OUTGOING : MESSAGE_INCOMING;

        final RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        layoutParams.setMargins(type == MESSAGE_OUTGOING ? itemMargin : 0, 0,
                type == MESSAGE_INCOMING ? itemMargin : 0, 0);

        binding.tvMessage.setVisibility(View.GONE);
        final View voiceMessageContainer = (View) binding.waveformSeekBar.getParent();
        final View linkMessageContainer = (View) binding.ivLinkPreview.getParent();
        final View mediaMessageContainer = (View) binding.ivMediaPreview.getParent();
        final View mediaTypeIcon = binding.typeIcon;
        final View profileMessageContainer = (View) binding.profileInfo.getParent();

        voiceMessageContainer.setVisibility(View.GONE);
        binding.ivAnimatedMessage.setVisibility(View.GONE);
        linkMessageContainer.setVisibility(View.GONE);
        mediaMessageContainer.setVisibility(View.GONE);
        mediaTypeIcon.setVisibility(View.GONE);
        binding.mediaExpiredIcon.setVisibility(View.GONE);
        profileMessageContainer.setVisibility(View.GONE);
        binding.isVerified.setVisibility(View.GONE);

        final FrameLayout btnOpenProfile = binding.btnInfo;
        btnOpenProfile.setVisibility(View.GONE);
        btnOpenProfile.setOnClickListener(null);
        btnOpenProfile.setTag(null);

        CharSequence text = "?";
        if (user != null && user != myProfileHolder) text = user.getUsername();
        else if (user == myProfileHolder) text = strDmYou;
        text = text + " - " + directItemModel.getDateTime();

        binding.tvUsername.setText(text);

        binding.ivProfilePic.setVisibility(type == MESSAGE_INCOMING ? View.VISIBLE : View.GONE);
        binding.ivProfilePic.setTag(user);
        binding.ivProfilePic.setOnClickListener(onClickListener);

        binding.tvMessage.setMentionClickListener(mentionClickListener);
        binding.messageCard.setTag(directItemModel);
        LinearLayout parent = (LinearLayout) binding.messageCard.getParent();
        parent.setTag(directItemModel);
        binding.messageCard.setOnClickListener(onClickListener);
        binding.liked.setVisibility(directItemModel.isLiked() ? View.VISIBLE : View.GONE);

        final RequestManager glideRequestManager = Glide.with(itemView);

        if (type == MESSAGE_INCOMING && user != null)
            glideRequestManager.load(user.getSdProfilePic()).into(binding.ivProfilePic);

        DirectItemModel.DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
        switch (itemType) {
            case PLACEHOLDER:
                binding.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), FROM_HTML_MODE_COMPACT));
                binding.tvMessage.setVisibility(View.VISIBLE);
                break;
            case TEXT:
            case LIKE:
                text = directItemModel.getText();
                text = Utils.getSpannableUrl(text.toString()); // for urls
                if (Utils.hasMentions(text)) text = Utils.getMentionText(text); // for mentions

                if (text instanceof Spanned)
                    binding.tvMessage.setText(text, TextView.BufferType.SPANNABLE);
                else if (text == "") {
                    binding.tvMessage.setText(context.getText(R.string.dms_inbox_raven_message_unknown));
                } else binding.tvMessage.setText(text);

                binding.tvMessage.setVisibility(View.VISIBLE);
                break;

            case LINK: {
                final DirectItemModel.DirectItemLinkModel link = directItemModel.getLinkModel();
                final DirectItemModel.DirectItemLinkContext linkContext = link.getLinkContext();

                final String linkImageUrl = linkContext.getLinkImageUrl();
                if (!Utils.isEmpty(linkImageUrl)) {
                    glideRequestManager.load(linkImageUrl).into(binding.ivLinkPreview);
                    binding.tvLinkTitle.setText(linkContext.getLinkTitle());
                    binding.tvLinkSummary.setText(linkContext.getLinkSummary());
                    binding.ivLinkPreview.setVisibility(View.VISIBLE);
                    linkMessageContainer.setVisibility(View.VISIBLE);
                }

                binding.tvMessage.setText(Utils.getSpannableUrl(link.getText()));
                binding.tvMessage.setVisibility(View.VISIBLE);
            }
            break;

            case MEDIA_SHARE: {
                final ProfileModel modelUser = mediaModel.getUser();
                if (modelUser != null) {
                    binding.tvMessage.setText(HtmlCompat.fromHtml("<small>" + context.getString(R.string.dms_inbox_media_shared_from, modelUser.getUsername()) + "</small>", FROM_HTML_MODE_COMPACT));
                    binding.tvMessage.setVisibility(View.VISIBLE);
                }
            }
            case MEDIA: {
                glideRequestManager.load(mediaModel.getThumbUrl()).into(binding.ivMediaPreview);

                final MediaItemType modelMediaType = mediaModel.getMediaType();
                mediaTypeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                        modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
                mediaMessageContainer.setVisibility(View.VISIBLE);
            }
            break;

            case RAVEN_MEDIA: {
                final DirectItemModel.DirectItemRavenMediaModel ravenMediaModel = directItemModel.getRavenMediaModel();

                final boolean isExpired = ravenMediaModel == null || (mediaModel = ravenMediaModel.getMedia()) == null ||
                        Utils.isEmpty(mediaModel.getThumbUrl()) && mediaModel.getPk() < 1;

                DirectItemModel.RavenExpiringMediaActionSummaryModel mediaActionSummary = null;
                if (ravenMediaModel != null) {
                    mediaActionSummary = ravenMediaModel.getExpiringMediaActionSummary();
                }
                binding.mediaExpiredIcon.setVisibility(isExpired ? View.VISIBLE : View.GONE);

                int textRes = R.string.dms_inbox_raven_media_unknown;
                if (isExpired) textRes = R.string.dms_inbox_raven_media_expired;

                if (!isExpired) {
                    if (mediaActionSummary != null) {
                        final RavenExpiringMediaType expiringMediaType = mediaActionSummary.getType();

                        if (expiringMediaType == RavenExpiringMediaType.RAVEN_DELIVERED)
                            textRes = R.string.dms_inbox_raven_media_delivered;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SENT)
                            textRes = R.string.dms_inbox_raven_media_sent;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_OPENED)
                            textRes = R.string.dms_inbox_raven_media_opened;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_REPLAYED)
                            textRes = R.string.dms_inbox_raven_media_replayed;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SENDING)
                            textRes = R.string.dms_inbox_raven_media_sending;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_BLOCKED)
                            textRes = R.string.dms_inbox_raven_media_blocked;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SUGGESTED)
                            textRes = R.string.dms_inbox_raven_media_suggested;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SCREENSHOT)
                            textRes = R.string.dms_inbox_raven_media_screenshot;
                        else if (expiringMediaType == RavenExpiringMediaType.RAVEN_CANNOT_DELIVER)
                            textRes = R.string.dms_inbox_raven_media_cant_deliver;
                    }

                    final RavenMediaViewType ravenMediaViewType = ravenMediaModel.getViewType();
                    if (ravenMediaViewType == RavenMediaViewType.PERMANENT || ravenMediaViewType == RavenMediaViewType.REPLAYABLE) {
                        final MediaItemType mediaType = mediaModel.getMediaType();
                        textRes = -1;
                        mediaTypeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                                mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                        glideRequestManager.load(mediaModel.getThumbUrl()).into(binding.ivMediaPreview);
                        mediaMessageContainer.setVisibility(View.VISIBLE);
                    }
                }
                if (textRes != -1) {
                    binding.tvMessage.setText(context.getText(textRes));
                    binding.tvMessage.setVisibility(View.VISIBLE);
                }
            }
            break;

            case REEL_SHARE: {
                final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
                if (!Utils.isEmpty(text = reelShare.getText())) {
                    binding.tvMessage.setText(text);
                    binding.tvMessage.setVisibility(View.VISIBLE);
                }

                final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
                final MediaItemType mediaType = reelShareMedia.getMediaType();

                if (mediaType == null)
                    binding.mediaExpiredIcon.setVisibility(View.VISIBLE);
                else {
                    mediaTypeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                            mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                    glideRequestManager.load(reelShareMedia.getThumbUrl()).into(binding.ivMediaPreview);
                    mediaMessageContainer.setVisibility(View.VISIBLE);
                }
            }
            break;

            case STORY_SHARE: {
                final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
                if (reelShare == null) {
                    binding.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), FROM_HTML_MODE_COMPACT));
                    binding.tvMessage.setVisibility(View.VISIBLE);
                } else {
                    if (!Utils.isEmpty(text = reelShare.getText())) {
                        binding.tvMessage.setText(text);
                        binding.tvMessage.setVisibility(View.VISIBLE);
                    }

                    final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
                    final MediaItemType mediaType = reelShareMedia.getMediaType();

                    mediaTypeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                            mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                    glideRequestManager.load(reelShareMedia.getThumbUrl()).into(binding.ivMediaPreview);
                    mediaMessageContainer.setVisibility(View.VISIBLE);
                }
            }
            break;

            case VOICE_MEDIA: {
                final DirectItemModel.DirectItemVoiceMediaModel voiceMediaModel = directItemModel.getVoiceMediaModel();

                if (voiceMediaModel != null) {
                    final int[] waveformData = voiceMediaModel.getWaveformData();
                    if (waveformData != null) binding.waveformSeekBar.setSample(waveformData);

                    final long durationMs = voiceMediaModel.getDurationMs();
                    binding.tvVoiceDuration.setText(Utils.millisToString(durationMs));
                    binding.waveformSeekBar.setProgress(voiceMediaModel.getProgress());
                    binding.waveformSeekBar.setProgressChangeListener((waveformSeekBar, progress, fromUser) -> {
                        // todo progress audio player
                        voiceMediaModel.setProgress(progress);
                        if (fromUser)
                            binding.tvVoiceDuration.setText(Utils.millisToString(durationMs * progress / 100));
                    });
                    binding.btnPlayVoice.setTag(voiceMediaModel);
                    binding.btnPlayVoice.setOnClickListener(voicePlayClickListener);
                } else {
                    binding.waveformSeekBar.setProgress(0);
                }
                voiceMessageContainer.setVisibility(View.VISIBLE);
            }
            break;

            case ANIMATED_MEDIA: {
                glideRequestManager.asGif().load(directItemModel.getAnimatedMediaModel().getGifUrl())
                        .into(binding.ivAnimatedMessage);
                binding.ivAnimatedMessage.setVisibility(View.VISIBLE);
            }
            break;

            case PROFILE: {
                final ProfileModel profileModel = directItemModel.getProfileModel();
                Glide.with(binding.profileInfo).load(profileModel.getSdProfilePic())
                        .into(binding.profileInfo);
                btnOpenProfile.setTag(profileModel);
                btnOpenProfile.setOnClickListener(onClickListener);

                binding.tvFullName.setText(profileModel.getName());
                binding.profileInfoText.setText(profileModel.getUsername());
                binding.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);

                btnOpenProfile.setVisibility(View.VISIBLE);
                profileMessageContainer.setVisibility(View.VISIBLE);
            }
            break;

            case VIDEO_CALL_EVENT: {
                // todo add call event info
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.profileInfoText.setBackgroundColor(0xFF_1F90E6);
            }
            break;

            case ACTION_LOG: {
                text = directItemModel.getActionLogModel().getDescription();
                binding.tvMessage.setText(HtmlCompat.fromHtml("<small>" + text + "</small>", FROM_HTML_MODE_COMPACT));
                binding.tvMessage.setVisibility(View.VISIBLE);
            }
            break;
        }
    }

    @Nullable
    private ProfileModel getUser(final long userId) {
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
}