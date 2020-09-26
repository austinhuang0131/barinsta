package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.logCollector;

public final class PostsFetcher extends AsyncTask<Void, Void, List<PostModel>> {
    private static final String TAG = "PostsFetcher";
    private final PostItemType type;
    private final String endCursor;
    private final String id;
    private final FetchListener<List<PostModel>> fetchListener;
    private String username = null;

    public PostsFetcher(final String id,
                        final PostItemType type,
                        final String endCursor,
                        final FetchListener<List<PostModel>> fetchListener) {
        this.id = id;
        this.type = type;
        this.endCursor = endCursor == null ? "" : endCursor;
        this.fetchListener = fetchListener;
    }

    public PostsFetcher setUsername(final String username) {
        this.username = username;
        return this;
    }

    @Override
    protected List<PostModel> doInBackground(final Void... voids) {
        // final boolean isHashTag = id.charAt(0) == '#';
        // final boolean isSaved = id.charAt(0) == '$';
        // final boolean isTagged = id.charAt(0) == '%';
        // final boolean isLocation = id.contains("/");

        final String url;
        switch (type) {
            case HASHTAG:
                url = "https://www.instagram.com/graphql/query/?query_hash=9b498c08113f1e09617a1703c22b2f32&variables=" +
                        "{\"tag_name\":\"" + id.toLowerCase() + "\",\"first\":150,\"after\":\"" + endCursor + "\"}";
                break;
            case LOCATION:
                url = "https://www.instagram.com/graphql/query/?query_hash=36bd0f2bf5911908de389b8ceaa3be6d&variables=" +
                        "{\"id\":\"" + id + "\",\"first\":150,\"after\":\"" + endCursor + "\"}";
                break;
            case SAVED:
                url = "https://www.instagram.com/graphql/query/?query_hash=8c86fed24fa03a8a2eea2a70a80c7b6b&variables=" +
                        "{\"id\":\"" + id + "\",\"first\":150,\"after\":\"" + endCursor + "\"}";
                break;
            case TAGGED:
                url = "https://www.instagram.com/graphql/query/?query_hash=31fe64d9463cbbe58319dced405c6206&variables=" +
                        "{\"id\":\"" + id + "\",\"first\":150,\"after\":\"" + endCursor + "\"}";
                break;
            default:
                url = "https://www.instagram.com/graphql/query/?query_hash=18a7b935ab438c4514b1f742d8fa07a7&variables=" +
                        "{\"id\":\"" + id + "\",\"first\":150,\"after\":\"" + endCursor + "\"}";
        }
        List<PostModel> result = new ArrayList<>();
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // to check if file exists
                final File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download" +
                        (Utils.settingsHelper.getBoolean(DOWNLOAD_USER_FOLDER) ? ("/" + username) : ""));
                File customDir = null;
                if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
                    final String customPath = Utils.settingsHelper.getString(FOLDER_PATH +
                                                                                     (Utils.settingsHelper.getBoolean(DOWNLOAD_USER_FOLDER)
                                                                                      ? ("/" + username)
                                                                                      : ""));
                    if (!TextUtils.isEmpty(customPath)) customDir = new File(customPath);
                }

                final boolean isHashtag = type == PostItemType.HASHTAG;
                final boolean isLocation = type == PostItemType.LOCATION;
                final boolean isSaved = type == PostItemType.SAVED;
                final boolean isTagged = type == PostItemType.TAGGED;
                final JSONObject mediaPosts = new JSONObject(NetworkUtils.readFromConnection(conn))
                        .getJSONObject("data")
                        .getJSONObject(isHashtag
                                       ? Constants.EXTRAS_HASHTAG
                                       : (isLocation ? Constants.EXTRAS_LOCATION
                                                     : Constants.EXTRAS_USER))
                        .getJSONObject(isHashtag ? "edge_hashtag_to_media" :
                                       isLocation ? "edge_location_to_media" : isSaved ? "edge_saved_media"
                                                                                       : isTagged ? "edge_user_to_photos_of_you"
                                                                                                  : "edge_owner_to_timeline_media");

                final String endCursor;
                final boolean hasNextPage;

                final JSONObject pageInfo = mediaPosts.getJSONObject("page_info");
                if (pageInfo.has("has_next_page")) {
                    hasNextPage = pageInfo.getBoolean("has_next_page");
                    endCursor = hasNextPage ? pageInfo.getString("end_cursor") : null;
                } else {
                    hasNextPage = false;
                    endCursor = null;
                }

                final JSONArray edges = mediaPosts.getJSONArray("edges");
                for (int i = 0; i < edges.length(); ++i) {
                    final JSONObject mediaNode = edges.getJSONObject(i).getJSONObject("node");
                    final JSONArray captions = mediaNode.getJSONObject("edge_media_to_caption").getJSONArray("edges");

                    final boolean isSlider = mediaNode.has("__typename") && mediaNode.getString("__typename").equals("GraphSidecar");
                    final boolean isVideo = mediaNode.getBoolean("is_video");

                    final MediaItemType itemType;
                    if (isSlider) itemType = MediaItemType.MEDIA_TYPE_SLIDER;
                    else if (isVideo) itemType = MediaItemType.MEDIA_TYPE_VIDEO;
                    else itemType = MediaItemType.MEDIA_TYPE_IMAGE;

                    final PostModel model = new PostModel(
                            itemType,
                            mediaNode.getString(Constants.EXTRAS_ID),
                            mediaNode.getString("display_url"),
                            mediaNode.getString("thumbnail_src"),
                            mediaNode.getString(Constants.EXTRAS_SHORTCODE),
                            captions.length() > 0 ? captions.getJSONObject(0)
                                                            .getJSONObject("node")
                                                            .getString("text")
                                                  : null,
                            mediaNode.getLong("taken_at_timestamp"),
                            mediaNode.optBoolean("viewer_has_liked"),
                            mediaNode.optBoolean("viewer_has_saved"),
                            mediaNode.isNull("edge_liked_by") ? 0 : mediaNode.getJSONObject("edge_liked_by").getLong("count")
                    );
                    result.add(model);
                    DownloadUtils.checkExistence(downloadDir, customDir, isSlider, model);
                }

                if (!result.isEmpty() && result.get(result.size() - 1) != null)
                    result.get(result.size() - 1).setPageCursor(hasNextPage, endCursor);
            }
            conn.disconnect();
        } catch (Exception e) {
            if (logCollector != null) {
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_MAIN_POSTS_FETCHER, "doInBackground");
            }
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error fetching posts", e);
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(final List<PostModel> postModels) {
        if (fetchListener != null) fetchListener.onResult(postModels);
    }
}
