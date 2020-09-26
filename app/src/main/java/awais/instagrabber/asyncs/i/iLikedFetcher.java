package awais.instagrabber.asyncs.i;

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
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.logCollector;

public final class iLikedFetcher extends AsyncTask<Void, Void, List<PostModel>> {
    private static final String TAG = "iLikedFetcher";

    private final String endCursor;
    private final FetchListener<List<PostModel>> fetchListener;

    public iLikedFetcher(final FetchListener<List<PostModel>> fetchListener) {
        this.endCursor = "";
        this.fetchListener = fetchListener;
    }

    public iLikedFetcher(final String endCursor, final FetchListener<List<PostModel>> fetchListener) {
        this.endCursor = endCursor == null ? "" : endCursor;
        this.fetchListener = fetchListener;
    }

    @Override
    protected List<PostModel> doInBackground(final Void... voids) {
        final String url = "https://i.instagram.com/api/v1/feed/liked/?max_id=" + endCursor;

        List<PostModel> result = new ArrayList<>();
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject body = new JSONObject(NetworkUtils.readFromConnection(conn));

                final String endCursor;
                final boolean hasNextPage;

                if (body.has("more_available")) {
                    hasNextPage = body.optBoolean("more_available");
                    endCursor = hasNextPage ? body.optString("next_max_id") : null;
                } else {
                    hasNextPage = false;
                    endCursor = null;
                }

                final JSONArray edges = body.getJSONArray("items");
                for (int i = 0; i < edges.length(); ++i) {
                    final JSONObject mediaNode = edges.getJSONObject(i);

                    final boolean isSlider = mediaNode.has("carousel_media_count");
                    final boolean isVideo = mediaNode.has("video_duration");

                    final MediaItemType itemType;
                    if (isSlider) itemType = MediaItemType.MEDIA_TYPE_SLIDER;
                    else if (isVideo) itemType = MediaItemType.MEDIA_TYPE_VIDEO;
                    else itemType = MediaItemType.MEDIA_TYPE_IMAGE;

                    final PostModel model = new PostModel(
                            itemType,
                            mediaNode.getString(Constants.EXTRAS_ID),
                            isSlider ? ResponseBodyUtils.getHighQualityImage(mediaNode.getJSONArray("carousel_media")
                                                                                      .getJSONObject(0))
                                     : ResponseBodyUtils.getHighQualityImage(mediaNode),
                            isSlider ? ResponseBodyUtils.getLowQualityImage(mediaNode.getJSONArray("carousel_media")
                                                                                     .getJSONObject(0))
                                     : ResponseBodyUtils.getLowQualityImage(mediaNode),
                            mediaNode.getString("code"),
                            mediaNode.isNull("caption") ? null : mediaNode.getJSONObject("caption").optString("text"),
                            mediaNode.getLong("taken_at"),
                            true,
                            mediaNode.optBoolean("has_viewer_saved"),
                            mediaNode.getLong("like_count"));
                    result.add(model);
                    String username = mediaNode.getJSONObject("user").getString("username");
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
                    DownloadUtils.checkExistence(downloadDir, customDir, isSlider, model);
                }

                final int length = result.size();
                if (length >= 1 && result.get(length - 1) != null) {
                    result.get(length - 1).setPageCursor(hasNextPage, endCursor);
                }
            }

            conn.disconnect();
        } catch (Exception e) {
            if (logCollector != null) {
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_MAIN_POSTS_FETCHER, "doInBackground");
            }
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "", e);
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(final List<PostModel> postModels) {
        if (fetchListener != null) fetchListener.onResult(postModels);
    }
}
