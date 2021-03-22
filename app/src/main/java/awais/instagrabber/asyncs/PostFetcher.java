package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
//import awaisomereport.LogCollector;

//import static awais.instagrabber.utils.Utils.logCollector;

public final class PostFetcher extends AsyncTask<Void, Void, Media> {
    private static final String TAG = "PostFetcher";

    private final String shortCode;
    private final FetchListener<Media> fetchListener;

    public PostFetcher(final String shortCode, final FetchListener<Media> fetchListener) {
        this.shortCode = shortCode;
        this.fetchListener = fetchListener;
    }

    @Override
    protected Media doInBackground(final Void... voids) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://www.instagram.com/p/" + shortCode + "/?__a=1").openConnection();
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                final JSONObject media = new JSONObject(NetworkUtils.readFromConnection(conn)).getJSONObject("graphql")
                                                                                              .getJSONObject("shortcode_media");
                // ProfileModel profileModel = null;
                // if (media.has("owner")) {
                //     final JSONObject owner = media.getJSONObject("owner");
                //     profileModel = new ProfileModel(
                //             owner.optBoolean("is_private"),
                //             owner.optBoolean("is_private"),
                //             owner.optBoolean("is_verified"),
                //             owner.optString("id"),
                //             owner.optString("username"),
                //             owner.optString("full_name"),
                //             null,
                //             null,
                //             owner.optString("profile_pic_url"),
                //             owner.optString("profile_pic_url"),
                //             owner.optInt("edge_owner_to_timeline_media"),
                //             owner.optInt("edge_followed_by"),
                //             -1,
                //             owner.optBoolean("followed_by_viewer"),
                //             false,
                //             owner.optBoolean("restricted_by_viewer"),
                //             owner.optBoolean("blocked_by_viewer"),
                //             owner.optBoolean("requested_by_viewer")
                //     );
                // }
                // final long timestamp = media.getLong("taken_at_timestamp");
                //
                // final boolean isVideo = media.has("is_video") && media.optBoolean("is_video");
                // final boolean isSlider = media.has("edge_sidecar_to_children");
                //
                // final MediaItemType mediaItemType;
                // if (isSlider) mediaItemType = MediaItemType.MEDIA_TYPE_SLIDER;
                // else if (isVideo) mediaItemType = MediaItemType.MEDIA_TYPE_VIDEO;
                // else mediaItemType = MediaItemType.MEDIA_TYPE_IMAGE;
                //
                // final String postCaption;
                // final JSONObject mediaToCaption = media.optJSONObject("edge_media_to_caption");
                // if (mediaToCaption == null) postCaption = null;
                // else {
                //     final JSONArray captions = mediaToCaption.optJSONArray("edges");
                //     postCaption = captions != null && captions.length() > 0 ?
                //                   captions.getJSONObject(0).getJSONObject("node").optString("text") : null;
                // }
                //
                // JSONObject commentObject = media.optJSONObject("edge_media_to_parent_comment");
                // final long commentsCount = commentObject != null ? commentObject.optLong("count") : 0;
                // final FeedModel.Builder feedModelBuilder = new FeedModel.Builder()
                //         .setItemType(mediaItemType)
                //         .setPostId(media.getString(Constants.EXTRAS_ID))
                //         .setDisplayUrl(isVideo ? media.getString("video_url")
                //                                : ResponseBodyUtils.getHighQualityImage(media))
                //         .setThumbnailUrl(media.getString("display_url"))
                //         .setImageHeight(media.getJSONObject("dimensions").getInt("height"))
                //         .setImageWidth(media.getJSONObject("dimensions").getInt("width"))
                //         .setShortCode(shortCode)
                //         .setPostCaption(TextUtils.isEmpty(postCaption) ? null : postCaption)
                //         .setProfileModel(profileModel)
                //         .setViewCount(isVideo && media.has("video_view_count")
                //                       ? media.getLong("video_view_count")
                //                       : -1)
                //         .setTimestamp(timestamp)
                //         .setLiked(media.getBoolean("viewer_has_liked"))
                //         .setBookmarked(media.getBoolean("viewer_has_saved"))
                //         .setLikesCount(media.getJSONObject("edge_media_preview_like")
                //                             .getLong("count"))
                //         .setLocationName(media.isNull("location")
                //                          ? null
                //                          : media.getJSONObject("location").optString("name"))
                //         .setLocationId(media.isNull("location")
                //                        ? null
                //                        : media.getJSONObject("location").optString("id"))
                //         .setCommentsCount(commentsCount);
                // if (isSlider) {
                //     final JSONArray children = media.getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
                //     final List<PostChild> postModels = new ArrayList<>();
                //     for (int i = 0; i < children.length(); ++i) {
                //         final JSONObject childNode = children.getJSONObject(i).getJSONObject("node");
                //         final boolean isChildVideo = childNode.getBoolean("is_video");
                //         postModels.add(new PostChild.Builder()
                //                                .setItemType(isChildVideo ? MediaItemType.MEDIA_TYPE_VIDEO
                //                                                          : MediaItemType.MEDIA_TYPE_IMAGE)
                //                                .setDisplayUrl(isChildVideo ? childNode.getString("video_url")
                //                                                            : childNode.getString("display_url"))
                //                                .setShortCode(media.getString(Constants.EXTRAS_SHORTCODE))
                //                                .setVideoViews(isChildVideo && childNode.has("video_view_count")
                //                                               ? childNode.getLong("video_view_count")
                //                                               : -1)
                //                                .setThumbnailUrl(childNode.getString("display_url"))
                //                                .setHeight(childNode.getJSONObject("dimensions").getInt("height"))
                //                                .setWidth(childNode.getJSONObject("dimensions").getInt("width"))
                //                                .build());
                //     }
                //     feedModelBuilder.setSliderItems(postModels);
                // }
                // return feedModelBuilder.build();
                return ResponseBodyUtils.parseGraphQLItem(media, null);
            }
        } catch (Exception e) {
//            if (logCollector != null) {
//                logCollector.appendException(e, LogCollector.LogFile.ASYNC_POST_FETCHER, "doInBackground");
//            }
            Log.e(TAG, "Error fetching post", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final Media feedModel) {
        if (fetchListener != null) fetchListener.onResult(feedModel);
    }
}
