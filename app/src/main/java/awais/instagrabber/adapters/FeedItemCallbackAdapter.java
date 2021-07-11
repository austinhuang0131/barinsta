package awais.instagrabber.adapters;

import android.view.View;

import awais.instagrabber.repositories.responses.Media;


public class FeedItemCallbackAdapter implements FeedAdapterV2.FeedItemCallback {
    @Override
    public void onPostClick(final Media media) {}

    @Override
    public void onProfilePicClick(final Media media) {}

    @Override
    public void onNameClick(final Media media) {}

    @Override
    public void onLocationClick(final Media media) {}

    @Override
    public void onMentionClick(final String mention) {}

    @Override
    public void onHashtagClick(final String hashtag) {}

    @Override
    public void onCommentsClick(final Media media) {}

    @Override
    public void onDownloadClick(final Media media, final int childPosition, final View popupLocation) {}

    @Override
    public void onEmailClick(final String emailId) {}

    @Override
    public void onURLClick(final String url) {}

    @Override
    public void onSliderClick(final Media media, final int position) {}
}
