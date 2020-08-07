package awais.instagrabber.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.activities.Main;
import awais.instagrabber.adapters.viewholder.directmessages.TextMessageViewHolder;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemMediaModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemRavenMediaModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.RavenExpiringMediaType;
import awais.instagrabber.models.enums.RavenMediaViewType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemLinkContext;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemLinkModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemReelShareModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemVoiceMediaModel;
import static awais.instagrabber.models.direct_messages.DirectItemModel.RavenExpiringMediaActionSummaryModel;

public final class MessageItemsAdapter extends RecyclerView.Adapter<TextMessageViewHolder> {
    private static final int MESSAGE_INCOMING = 69, MESSAGE_OUTGOING = 420;
    private final ProfileModel myProfileHolder =
            new ProfileModel(false, false, false,
                    Utils.getUserIdFromCookie(Utils.settingsHelper.getString(Constants.COOKIE)),
                    null, null, null, null, null, null, 0, 0, 0, false, false, false, false);
    private final ArrayList<DirectItemModel> directItemModels;
    private final ArrayList<ProfileModel> users, leftusers;
    private final View.OnClickListener onClickListener;
    private final MentionClickListener mentionClickListener;
    private final View.OnClickListener openProfileClickListener = v -> {
        final Object tag = v.getTag();
        if (tag instanceof ProfileModel) {
            // todo do profile stuff
            final ProfileModel profileModel = (ProfileModel) tag;
            Log.d("AWAISKING_APP", "--> " + profileModel);
        }
    };
    private final int itemMargin;
    private DirectItemVoiceMediaModel prevVoiceModel;
    private ImageView prevPlayIcon;
    private final View.OnClickListener voicePlayClickListener = v -> {
        final Object tag = v.getTag();
        if (v instanceof ViewGroup && tag instanceof DirectItemVoiceMediaModel) {
            final ImageView playIcon = (ImageView) ((ViewGroup) v).getChildAt(0);
            final DirectItemVoiceMediaModel voiceMediaModel = (DirectItemVoiceMediaModel) tag;
            final boolean voicePlaying = voiceMediaModel.isPlaying();
            voiceMediaModel.setPlaying(!voicePlaying);

            if (voiceMediaModel == prevVoiceModel) {
                // todo pause / resume
            } else {
                // todo release prev audio, start new voice
                if (prevVoiceModel != null) prevVoiceModel.setPlaying(false);
                if (prevPlayIcon != null) prevPlayIcon.setImageResource(android.R.drawable.ic_media_play);
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
    private Context context;
    private LayoutInflater layoutInflater;
    private String strDmYou;

    public MessageItemsAdapter(final ArrayList<DirectItemModel> directItemModels, final ArrayList<ProfileModel> users,
                               final ArrayList<ProfileModel> leftusers, final View.OnClickListener onClickListener,
                               final MentionClickListener mentionClickListener) {
        this.users = users;
        this.leftusers = leftusers;
        this.directItemModels = directItemModels;
        this.onClickListener = onClickListener;
        this.mentionClickListener = mentionClickListener;
        this.itemMargin = Utils.displayMetrics.widthPixels / 5;
    }

    @NonNull
    @Override
    public TextMessageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        if (context == null) context = parent.getContext();
        if (strDmYou == null) strDmYou = context.getString(R.string.direct_messages_you);
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(context);
        return new TextMessageViewHolder(layoutInflater.inflate(R.layout.item_message_item, parent, false),
                onClickListener, mentionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final TextMessageViewHolder holder, final int position) {
        final DirectItemModel directItemModel = directItemModels.get(position);
        holder.itemView.setTag(directItemModel);

        if (directItemModel != null) {
            final DirectItemType itemType = directItemModel.getItemType();

            final ProfileModel user = getUser(directItemModel.getUserId());
            final int type = user == myProfileHolder ? MESSAGE_OUTGOING : MESSAGE_INCOMING;

            final RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setMargins(type == MESSAGE_OUTGOING ? itemMargin : 0, 0,
                    type == MESSAGE_INCOMING ? itemMargin : 0, 0);

            holder.tvMessage.setVisibility(View.GONE);
            holder.voiceMessageContainer.setVisibility(View.GONE);
            holder.ivAnimatedMessage.setVisibility(View.GONE);
            holder.linkMessageContainer.setVisibility(View.GONE);

            holder.mediaMessageContainer.setVisibility(View.GONE);
            holder.mediaTypeIcon.setVisibility(View.GONE);
            holder.mediaExpiredIcon.setVisibility(View.GONE);

            holder.profileMessageContainer.setVisibility(View.GONE);
            holder.isVerified.setVisibility(View.GONE);

            holder.btnOpenProfile.setVisibility(View.GONE);
            holder.btnOpenProfile.setOnClickListener(null);
            holder.btnOpenProfile.setTag(null);

            CharSequence text = "?";
            if (user != null && user != myProfileHolder) text = user.getUsername();
            else if (user == myProfileHolder) text = strDmYou;
            text = text + " - " + directItemModel.getDateTime();

            holder.tvUsername.setText(text);

            holder.ivProfilePic.setVisibility(type == MESSAGE_INCOMING ? View.VISIBLE : View.GONE);

            final RequestManager glideRequestManager = Glide.with(holder.itemView);

            if (type == MESSAGE_INCOMING && user != null)
                glideRequestManager.load(user.getSdProfilePic()).into(holder.ivProfilePic);

            DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
            switch (itemType) {
                case PLACEHOLDER:
                    holder.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), 63));
                    holder.tvMessage.setVisibility(View.VISIBLE);
                    break;
                case TEXT:
                    text = directItemModel.getText();
                    text = Utils.getSpannableUrl(text.toString()); // for urls
                    if (Utils.hasMentions(text)) text = Utils.getMentionText(text); // for mentions

                    if (text instanceof Spanned) holder.tvMessage.setText(text, TextView.BufferType.SPANNABLE);
                    else if (text == "") holder.tvMessage.setText(context.getText(R.string.dms_inbox_raven_message_unknown));
                    else holder.tvMessage.setText(text);

                    holder.tvMessage.setVisibility(View.VISIBLE);
                    break;

                case LINK: {
                    final DirectItemLinkModel link = directItemModel.getLinkModel();
                    final DirectItemLinkContext linkContext = link.getLinkContext();

                    final String linkImageUrl = linkContext.getLinkImageUrl();
                    if (!Utils.isEmpty(linkImageUrl)) {
                        glideRequestManager.load(linkImageUrl).into(holder.ivLinkPreview);
                        holder.tvLinkTitle.setText(linkContext.getLinkTitle());
                        holder.tvLinkSummary.setText(linkContext.getLinkSummary());
                        holder.ivLinkPreview.setVisibility(View.VISIBLE);
                        holder.linkMessageContainer.setVisibility(View.VISIBLE);
                    }

                    holder.tvMessage.setText(Utils.getSpannableUrl(link.getText()));
                    holder.tvMessage.setVisibility(View.VISIBLE);
                }
                break;

                case MEDIA_SHARE:
                {
                    final ProfileModel modelUser = mediaModel.getUser();
                    if (modelUser != null) {
                        holder.tvMessage.setText(HtmlCompat.fromHtml("<small>"+context.getString(R.string.dms_inbox_media_shared_from, modelUser.getUsername())+"</small>", 63));
                        holder.tvMessage.setVisibility(View.VISIBLE);
                    }
                }
                case MEDIA: {
                    glideRequestManager.load(mediaModel.getThumbUrl()).into(holder.ivMediaPreview);

                    final MediaItemType modelMediaType = mediaModel.getMediaType();
                    holder.mediaTypeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                            modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                    holder.mediaMessageContainer.setVisibility(View.VISIBLE);
                }
                break;

                case RAVEN_MEDIA: {
                    final DirectItemRavenMediaModel ravenMediaModel = directItemModel.getRavenMediaModel();
                    final RavenExpiringMediaActionSummaryModel mediaActionSummary = ravenMediaModel.getExpiringMediaActionSummary();

                    mediaModel = ravenMediaModel.getMedia();

                    final boolean isExpired = mediaModel == null ||
                            Utils.isEmpty(mediaModel.getThumbUrl()) && mediaModel.getPk() < 1;

                    holder.mediaExpiredIcon.setVisibility(isExpired ? View.VISIBLE : View.GONE);

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
                            holder.mediaTypeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                                    mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                            glideRequestManager.load(mediaModel.getThumbUrl()).into(holder.ivMediaPreview);
                            holder.mediaMessageContainer.setVisibility(View.VISIBLE);
                        }
                    }
                    if (textRes != -1) {
                        holder.tvMessage.setText(context.getText(textRes));
                        holder.tvMessage.setVisibility(View.VISIBLE);
                    }
                }
                break;

                case REEL_SHARE: {
                    final DirectItemReelShareModel reelShare = directItemModel.getReelShare();
                    if (!Utils.isEmpty(text = reelShare.getText())) {
                        holder.tvMessage.setText(text);
                        holder.tvMessage.setVisibility(View.VISIBLE);
                    }

                    final DirectItemMediaModel reelShareMedia = reelShare.getMedia();
                    final MediaItemType mediaType = reelShareMedia.getMediaType();

                    if (mediaType == null)
                        holder.mediaExpiredIcon.setVisibility(View.VISIBLE);
                    else {
                        holder.mediaTypeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                                mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                        glideRequestManager.load(reelShareMedia.getThumbUrl()).into(holder.ivMediaPreview);
                        holder.mediaMessageContainer.setVisibility(View.VISIBLE);
                    }
                }
                break;

                case STORY_SHARE: {
                    final DirectItemReelShareModel reelShare = directItemModel.getReelShare();
                    if (reelShare == null) {
                        holder.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), 63));
                        holder.tvMessage.setVisibility(View.VISIBLE);
                    }
                    else {
                        if (!Utils.isEmpty(text = reelShare.getText())) {
                            holder.tvMessage.setText(text);
                            holder.tvMessage.setVisibility(View.VISIBLE);
                        }

                        final DirectItemMediaModel reelShareMedia = reelShare.getMedia();
                        final MediaItemType mediaType = reelShareMedia.getMediaType();

                        holder.mediaTypeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                                mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

                        glideRequestManager.load(reelShareMedia.getThumbUrl()).into(holder.ivMediaPreview);
                        holder.mediaMessageContainer.setVisibility(View.VISIBLE);
                    }
                }
                break;

                case VOICE_MEDIA: {
                    final DirectItemVoiceMediaModel voiceMediaModel = directItemModel.getVoiceMediaModel();

                    if (voiceMediaModel != null) {
                        final int[] waveformData = voiceMediaModel.getWaveformData();
                        if (waveformData != null) holder.waveformSeekBar.setSample(waveformData);

                        final long durationMs = voiceMediaModel.getDurationMs();
                        holder.tvVoiceDuration.setText(Utils.millisToString(durationMs));
                        holder.waveformSeekBar.setProgress(voiceMediaModel.getProgress());
                        holder.waveformSeekBar.setProgressChangeListener((waveformSeekBar, progress, fromUser) -> {
                            // todo progress audio player
                            voiceMediaModel.setProgress(progress);
                            if (fromUser)
                                holder.tvVoiceDuration.setText(Utils.millisToString(durationMs * progress / 100));
                        });
                        holder.btnPlayVoice.setTag(voiceMediaModel);
                        holder.btnPlayVoice.setOnClickListener(voicePlayClickListener);
                    } else {
                        holder.waveformSeekBar.setProgress(0);
                    }

                    holder.voiceMessageContainer.setVisibility(View.VISIBLE);
                }
                break;

                case ANIMATED_MEDIA: {
                    glideRequestManager.asGif().load(directItemModel.getAnimatedMediaModel().getGifUrl())
                            .into(holder.ivAnimatedMessage);
                    holder.ivAnimatedMessage.setVisibility(View.VISIBLE);
                }
                break;

                case PROFILE: {
                    final ProfileModel profileModel = directItemModel.getProfileModel();
                    Glide.with(holder.ivMessageProfilePic).load(profileModel.getSdProfilePic())
                            .into(holder.ivMessageProfilePic);
                    holder.btnOpenProfile.setTag(profileModel);
                    holder.btnOpenProfile.setOnClickListener(openProfileClickListener);

                    holder.tvProfileName.setText(profileModel.getName());
                    holder.tvProfileUsername.setText(profileModel.getUsername());
                    holder.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);

                    holder.btnOpenProfile.setVisibility(View.VISIBLE);
                    holder.profileMessageContainer.setVisibility(View.VISIBLE);
                }
                break;

                case VIDEO_CALL_EVENT: {
                    // todo add call event info
                    holder.tvMessage.setVisibility(View.VISIBLE);
                    holder.itemView.setBackgroundColor(0xFF_1F90E6);
                }
                break;

                case ACTION_LOG: {
                    text = directItemModel.getActionLogModel().getDescription();
                    holder.tvMessage.setText(HtmlCompat.fromHtml("<small>"+text+"</small>", 63));
                    holder.tvMessage.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    }

    @Override
    public int getItemViewType(final int position) {
        return directItemModels.get(position).getItemType().ordinal();
    }

    @Override
    public int getItemCount() {
        return directItemModels == null ? 0 : directItemModels.size();
    }

    @Nullable
    private ProfileModel getUser(final long userId) {
        if (users != null) {
            ProfileModel result = myProfileHolder;
            for (final ProfileModel user : users) {
                if (Long.toString(userId).equals(user.getId())) result = user;
            }
            if (leftusers != null)
                for (final ProfileModel leftuser : leftusers) {
                    if (Long.toString(userId).equals(leftuser.getId())) result = leftuser;
                }
            return result;
        }
        return null;
    }
}