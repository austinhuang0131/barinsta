package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class FeedFetcher extends AsyncTask<Void, Void, FeedModel[]> {
    private static final int maxItemsToLoad = 25; // max is 50, but that's too many posts, setting more than 30 is gay
    private final String endCursor;
    private final FetchListener<FeedModel[]> fetchListener;

    public FeedFetcher(final FetchListener<FeedModel[]> fetchListener) {
        this.endCursor = "";
        this.fetchListener = fetchListener;
    }

    public FeedFetcher(final String endCursor, final FetchListener<FeedModel[]> fetchListener) {
        this.endCursor = endCursor == null ? "" : endCursor;
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected final FeedModel[] doInBackground(final Void... voids) {
        FeedModel[] result = null;
        try {
            //
            //  stories: 04334405dbdef91f2c4e207b84c204d7 && https://i.instagram.com/api/v1/feed/reels_tray/
            //  https://www.instagram.com/graphql/query/?query_hash=04334405dbdef91f2c4e207b84c204d7&variables={"only_stories":true,"stories_prefetch":false,"stories_video_dash_manifest":false}
            //  ///////////////////////////////////////////////
            //  feed:
            //  https://www.instagram.com/graphql/query/?query_hash=6b838488258d7a4820e48d209ef79eb1&variables=
            //    {"cached_feed_item_ids":[],"fetch_media_item_count":12,"fetch_media_item_cursor":"<end_cursor>","fetch_comment_count":4,"fetch_like":3,"has_stories":false,"has_threaded_comments":true}
            //  only used: fetch_media_item_cursor, fetch_media_item_count: 100 (max 50), has_threaded_comments = true
            //  //////////////////////////////////////////////
            //  more unknowns: https://github.com/qsniyg/rssit/blob/master/rssit/generators/instagram.py
            //

            final String url = "https://www.instagram.com/graphql/query/?query_hash=6b838488258d7a4820e48d209ef79eb1&variables=" +
                    "{\"fetch_media_item_count\":" + maxItemsToLoad + ",\"has_threaded_comments\":true,\"fetch_media_item_cursor\":\"" + endCursor + "\"}";
            final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject timelineFeed = new JSONObject(Utils.readFromConnection(urlConnection)).getJSONObject("data")
                        .getJSONObject(Constants.EXTRAS_USER).getJSONObject("edge_web_feed_timeline");

                final String endCursor;
                final boolean hasNextPage;

                final JSONObject pageInfo = timelineFeed.getJSONObject("page_info");
                if (pageInfo.has("has_next_page")) {
                    hasNextPage = pageInfo.getBoolean("has_next_page");
                    endCursor = hasNextPage ? pageInfo.getString("end_cursor") : null;
                } else {
                    hasNextPage = false;
                    endCursor = null;
                }

                final JSONArray feedItems = timelineFeed.getJSONArray("edges");

                final int feedLen = feedItems.length();
                final ArrayList<FeedModel> feedModelsList = new ArrayList<>(feedLen);
                for (int i = 0; i < feedLen; ++i) {
                    final JSONObject feedItem = feedItems.getJSONObject(i).getJSONObject("node");
                    final String mediaType = feedItem.optString("__typename");
                    if (mediaType.isEmpty() || "GraphSuggestedUserFeedUnit".equals(mediaType)) continue;

                    final boolean isVideo = feedItem.optBoolean("is_video");
                    final long videoViews = feedItem.optLong("video_view_count", 0);

                    final String displayUrl = feedItem.getString("display_url");
                    final String resourceUrl;

                    if (isVideo) resourceUrl = feedItem.getString("video_url");
                    else resourceUrl = feedItem.has("display_resources") ? Utils.getHighQualityImage(feedItem) : displayUrl;

                    ProfileModel profileModel = null;
                    if (feedItem.has("owner")) {
                        final JSONObject owner = feedItem.getJSONObject("owner");
                        profileModel = new ProfileModel(owner.optBoolean("is_private"),
                                false, // if you can see it then you def follow
                                owner.optBoolean("is_verified"),
                                owner.getString(Constants.EXTRAS_ID),
                                owner.getString(Constants.EXTRAS_USERNAME),
                                owner.optString("full_name"),
                                null, null,
                                owner.getString("profile_pic_url"),
                                null, 0, 0, 0, false, false, false, false);
                    }

                    JSONObject tempJsonObject = feedItem.optJSONObject("edge_media_preview_comment");
                    final long commentsCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;

                    tempJsonObject = feedItem.optJSONObject("edge_media_to_caption");
                    final JSONArray captions = tempJsonObject != null ? tempJsonObject.getJSONArray("edges") : null;

                    String captionText = null;
                    if (captions != null && captions.length() > 0) {
                        if ((tempJsonObject = captions.optJSONObject(0)) != null &&
                                (tempJsonObject = tempJsonObject.optJSONObject("node")) != null)
                            captionText = tempJsonObject.getString("text");
                    }

                    final FeedModel feedModel = new FeedModel(profileModel,
                            isVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                            videoViews,
                            feedItem.getString(Constants.EXTRAS_ID),
                            resourceUrl,
                            displayUrl,
                            feedItem.getString(Constants.EXTRAS_SHORTCODE),
                            captionText,
                            commentsCount,
                            feedItem.optLong("taken_at_timestamp", -1),
                            feedItem.getBoolean("viewer_has_liked"),
                            feedItem.getBoolean("viewer_has_saved"),
                            feedItem.getJSONObject("edge_media_preview_like").getLong("count"));

                    final boolean isSlider = "GraphSidecar".equals(mediaType) && feedItem.has("edge_sidecar_to_children");

                    if (isSlider) {
                        final JSONObject sidecar = feedItem.optJSONObject("edge_sidecar_to_children");
                        if (sidecar != null) {
                            final JSONArray children = sidecar.optJSONArray("edges");

                            if (children != null) {
                                final ViewerPostModel[] sliderItems = new ViewerPostModel[children.length()];

                                for (int j = 0; j < sliderItems.length; ++j) {
                                    final JSONObject node = children.optJSONObject(j).getJSONObject("node");
                                    final boolean isChildVideo = node.optBoolean("is_video");

                                    sliderItems[j] = new ViewerPostModel(
                                            isChildVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                            node.getString(Constants.EXTRAS_ID),
                                            isChildVideo ? node.getString("video_url") : Utils.getHighQualityImage(node),
                                            null, null, null,
                                            node.optLong("video_view_count", -1), -1, false, false,
                                            feedItem.getJSONObject("edge_media_preview_like").getLong("count"));

                                    sliderItems[j].setSliderDisplayUrl(node.getString("display_url"));
                                }

                                feedModel.setSliderItems(sliderItems);
                            }
                        }
                    }

                    feedModelsList.add(feedModel);
                }

                feedModelsList.trimToSize();

                final FeedModel[] feedModels = feedModelsList.toArray(new FeedModel[0]);
                if (feedModels[feedModels.length - 1] != null)
                    feedModels[feedModels.length - 1].setPageCursor(hasNextPage, endCursor);

                result = feedModels;
            }

            urlConnection.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_FEED_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final FeedModel[] postModels) {
        if (fetchListener != null) fetchListener.onResult(postModels);
    }
}