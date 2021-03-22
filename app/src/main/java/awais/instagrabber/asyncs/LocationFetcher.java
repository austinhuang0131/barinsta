package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NetworkUtils;
//import awaisomereport.LogCollector;

//import static awais.instagrabber.utils.Utils.logCollector;

public final class LocationFetcher extends AsyncTask<Void, Void, LocationModel> {
    private static final String TAG = "LocationFetcher";

    private final FetchListener<LocationModel> fetchListener;
    private final long id;

    public LocationFetcher(final long id, final FetchListener<LocationModel> fetchListener) {
        // idSlug = id + "/" + slug UPDATE: slug can be ignored tbh
        this.id = id;
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected LocationModel doInBackground(final Void... voids) {
        LocationModel result = null;

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://www.instagram.com/explore/locations/" + id + "/?__a=1")
                    .openConnection();
            conn.setUseCaches(true);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject location = new JSONObject(NetworkUtils.readFromConnection(conn)).getJSONObject("graphql")
                                                                                                 .getJSONObject(Constants.EXTRAS_LOCATION);

                final JSONObject timelineMedia = location.getJSONObject("edge_location_to_media");
                // if (timelineMedia.has("edges")) {
                //     final JSONArray edges = timelineMedia.getJSONArray("edges");
                // }
                result = new LocationModel(
                        location.getLong(Constants.EXTRAS_ID),
                        location.getString("name"),
                        location.getString("blurb"),
                        location.getString("website"),
                        location.getString("profile_pic_url"),
                        timelineMedia.getLong("count"),
                        BigDecimal.valueOf(location.optDouble("lat", 0d)).toString(),
                        BigDecimal.valueOf(location.optDouble("lng", 0d)).toString()
                );
            }

            conn.disconnect();
        } catch (final Exception e) {
//            if (logCollector != null)
//                logCollector.appendException(e, LogCollector.LogFile.ASYNC_LOCATION_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "", e);
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(final LocationModel result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}
