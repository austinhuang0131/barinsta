package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector.LogFile;

import static awais.instagrabber.utils.Utils.logCollector;

public final class FeedStoriesFetcher extends AsyncTask<Void, Void, FeedStoryModel[]> {
    private final FetchListener<FeedStoryModel[]> fetchListener;

    public FeedStoriesFetcher(final FetchListener<FeedStoryModel[]> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected FeedStoryModel[] doInBackground(final Void... voids) {
        FeedStoryModel[] result = null;
        String url = "https://www.instagram.com/graphql/query/?query_hash=b7b84d884400bc5aa7cfe12ae843a091&variables=" +
                "{\"only_stories\":true,\"stories_prefetch\":false,\"stories_video_dash_manifest\":false}";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONArray feedStoriesReel = new JSONObject(Utils.readFromConnection(conn))
                        .getJSONObject("data")
                        .getJSONObject(Constants.EXTRAS_USER)
                        .getJSONObject("feed_reels_tray")
                        .getJSONObject("edge_reels_tray_to_reel")
                        .getJSONArray("edges");

                conn.disconnect();

                final int storiesLen = feedStoriesReel.length();
                final FeedStoryModel[] feedStoryModels = new FeedStoryModel[storiesLen];
                final String[] feedStoryIDs = new String[storiesLen];

                for (int i = 0; i < storiesLen; ++i) {
                    final JSONObject node = feedStoriesReel.getJSONObject(i).getJSONObject("node");

                    final JSONObject user = node.getJSONObject(node.has("user") ? "user" : "owner");
                    final ProfileModel profileModel = new ProfileModel(false, false, false,
                            user.getString("id"),
                            user.getString("username"),
                            null, null, null,
                            user.getString("profile_pic_url"),
                            null, 0, 0, 0, false, false, false, false);

                    final String id = node.getString("id");
                    feedStoryIDs[i] = id;
                    feedStoryModels[i] = new FeedStoryModel(id, profileModel);
                }
                result = feedStoryModels;
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogFile.ASYNC_FEED_STORY_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final FeedStoryModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}