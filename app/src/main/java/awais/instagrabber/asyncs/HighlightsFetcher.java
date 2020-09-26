package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NetworkUtils;

public final class HighlightsFetcher extends AsyncTask<Void, Void, List<HighlightModel>> {
    private final String id;
    private final FetchListener<List<HighlightModel>> fetchListener;

    public HighlightsFetcher(final String id, final FetchListener<List<HighlightModel>> fetchListener) {
        this.id = id;
        this.fetchListener = fetchListener;
    }

    @Override
    protected List<HighlightModel> doInBackground(final Void... voids) {
        List<HighlightModel> result = null;
        String url = "https://i.instagram.com/api/v1/highlights/" + id + "/highlights_tray/";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONArray highlightsReel = new JSONObject(NetworkUtils.readFromConnection(conn)).getJSONArray("tray");

                final int length = highlightsReel.length();
                final List<HighlightModel> highlightModels = new ArrayList<>();
                // final String[] highlightIds = new String[length];
                for (int i = 0; i < length; ++i) {
                    final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                    highlightModels.add(new HighlightModel(
                            highlightNode.getString("title"),
                            highlightNode.getString(Constants.EXTRAS_ID),
                            highlightNode.getJSONObject("cover_media")
                                         .getJSONObject("cropped_image_version")
                                         .getString("url")
                    ));
                }
                conn.disconnect();
                result = highlightModels;
            }

            conn.disconnect();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final List<HighlightModel> result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}