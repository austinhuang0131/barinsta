package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.logCollector;

public final class PostFetcher extends AsyncTask<Void, Void, ViewerPostModel[]> {
    private final String shortCode;
    private final FetchListener<ViewerPostModel[]> fetchListener;

    public PostFetcher(final String shortCode, final FetchListener<ViewerPostModel[]> fetchListener) {
        this.shortCode = shortCode;
        this.fetchListener = fetchListener;
    }

    @Override
    protected ViewerPostModel[] doInBackground(final Void... voids) {
        ViewerPostModel[] result = null;
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://www.instagram.com/p/" + shortCode + "/?__a=1").openConnection();
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // to check if file exists
                final File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download");
                File customDir = null;
                if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
                    final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
                    if (!Utils.isEmpty(customPath)) customDir = new File(customPath);
                }

                final JSONObject media = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("graphql")
                        .getJSONObject("shortcode_media");

                final String username = media.has("owner") ? media.getJSONObject("owner").getString(Constants.EXTRAS_USERNAME) : null;

                final long timestamp = media.getLong("taken_at_timestamp");

                final boolean isVideo = media.has("is_video") && media.optBoolean("is_video");
                final boolean isSlider = media.has("edge_sidecar_to_children");

                final MediaItemType mediaItemType;
                if (isSlider) mediaItemType = MediaItemType.MEDIA_TYPE_SLIDER;
                else if (isVideo) mediaItemType = MediaItemType.MEDIA_TYPE_VIDEO;
                else mediaItemType = MediaItemType.MEDIA_TYPE_IMAGE;

                final String postCaption;
                final JSONObject mediaToCaption = media.optJSONObject("edge_media_to_caption");
                if (mediaToCaption == null) postCaption = null;
                else {
                    final JSONArray captions = mediaToCaption.optJSONArray("edges");
                    postCaption = captions != null && captions.length() > 0 ?
                            captions.getJSONObject(0).getJSONObject("node").optString("text") : null;
                }

                JSONObject commentObject = media.optJSONObject("edge_media_to_parent_comment");
                final long commentsCount = commentObject != null ? commentObject.optLong("count") : 0;

                String endCursor = null;
                if (commentObject != null && (commentObject = commentObject.optJSONObject("page_info")) != null)
                    endCursor = commentObject.optString("end_cursor");

                if (mediaItemType != MediaItemType.MEDIA_TYPE_SLIDER) {
                    final ViewerPostModel postModel = new ViewerPostModel(mediaItemType,
                            media.getString(Constants.EXTRAS_ID),
                            isVideo ? media.getString("video_url") : Utils.getHighQualityImage(media),
                            shortCode,
                            Utils.isEmpty(postCaption) ? null : postCaption,
                            username,
                            isVideo && media.has("video_view_count") ? media.getLong("video_view_count") : -1,
                            timestamp, media.getBoolean("viewer_has_liked"), media.getBoolean("viewer_has_saved"),
                            media.getJSONObject("edge_media_preview_like").getLong("count"));

                    postModel.setCommentsCount(commentsCount);
                    postModel.setCommentsEndCursor(endCursor);

                    Utils.checkExistence(downloadDir, customDir, username, false, -1, postModel);

                    result = new ViewerPostModel[]{postModel};

                } else {
                    final JSONArray children = media.getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
                    final ViewerPostModel[] postModels = new ViewerPostModel[children.length()];

                    for (int i = 0; i < postModels.length; ++i) {
                        final JSONObject node = children.getJSONObject(i).getJSONObject("node");
                        final boolean isChildVideo = node.getBoolean("is_video");

                        postModels[i] = new ViewerPostModel(isChildVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                media.getString(Constants.EXTRAS_ID),
                                isChildVideo ? node.getString("video_url") : Utils.getHighQualityImage(node),
                                node.getString(Constants.EXTRAS_SHORTCODE),
                                postCaption,
                                username,
                                isChildVideo && node.has("video_view_count") ? node.getLong("video_view_count") : -1,
                                timestamp, media.getBoolean("viewer_has_liked"), media.getBoolean("viewer_has_saved"),
                                media.getJSONObject("edge_media_preview_like").getLong("count"));
                        postModels[i].setSliderDisplayUrl(node.getString("display_url"));

                        Utils.checkExistence(downloadDir, customDir, username, true, i, postModels[i]);
                    }

                    postModels[0].setCommentsCount(commentsCount);
                    postModels[0].setCommentsEndCursor(endCursor);

                    result = postModels;
                }
            }

            conn.disconnect();
        } catch (Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_POST_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final ViewerPostModel[] postModels) {
        if (fetchListener != null) fetchListener.onResult(postModels);
    }
}
