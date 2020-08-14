package awais.instagrabber.asyncs.i;

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
import awais.instagrabber.models.DiscoverTopicModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class iTopicFetcher extends AsyncTask<Void, Void, DiscoverTopicModel> {
    private final FetchListener<DiscoverTopicModel> fetchListener;

    public iTopicFetcher(final FetchListener<DiscoverTopicModel> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected DiscoverTopicModel doInBackground(final Void... voids) {
        final String url = "https://i.instagram.com/api/v1/discover/topical_explore/";

        DiscoverTopicModel result = null;
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            conn.setRequestProperty("Accept-Language", LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject body = new JSONObject(Utils.readFromConnection(conn));

                final JSONArray edges = body.getJSONArray("clusters");
                String[] names = new String[edges.length()], ids = new String[edges.length()];
                for (int i = 0; i < names.length; ++i) {
                    final JSONObject mediaNode = edges.getJSONObject(i);
                    ids[i] = mediaNode.getString("id");
                    names[i] = mediaNode.getString("title");
                }

                result = new DiscoverTopicModel(ids, names, body.getString("rank_token"));
            }

            conn.disconnect();
        } catch (Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_DISCOVER_TOPICS_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final DiscoverTopicModel discoverTopicModel) {
        if (fetchListener != null) fetchListener.onResult(discoverTopicModel);
    }
}
