package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.TextUtils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class ProfileFetcher extends AsyncTask<Void, Void, ProfileModel> {
    private final FetchListener<ProfileModel> fetchListener;
    private final String userName;

    public ProfileFetcher(String userName, FetchListener<ProfileModel> fetchListener) {
        this.userName = userName;
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected ProfileModel doInBackground(final Void... voids) {
        ProfileModel result = null;

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://www.instagram.com/" + userName + "/?__a=1").openConnection();
            conn.setUseCaches(true);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject user = new JSONObject(NetworkUtils.readFromConnection(conn)).getJSONObject("graphql").getJSONObject(Constants.EXTRAS_USER);

                boolean isPrivate = user.getBoolean("is_private");
                boolean reallyPrivate = isPrivate;
                final JSONObject timelineMedia = user.getJSONObject("edge_owner_to_timeline_media");
                if (timelineMedia.has("edges")) {
                    final JSONArray edges = timelineMedia.getJSONArray("edges");
                    if (edges.length() > 0 && timelineMedia.getLong("count") > 0L) reallyPrivate = false;
                }

                String url = user.optString("external_url");
                if (TextUtils.isEmpty(url)) url = null;

                result = new ProfileModel(isPrivate,
                        reallyPrivate,
                        user.getBoolean("is_verified"),
                        user.getString(Constants.EXTRAS_ID),
                        userName,
                        user.getString("full_name"),
                        user.getString("biography"),
                        url,
                        user.getString("profile_pic_url"),
                        user.getString("profile_pic_url_hd"),
                        timelineMedia.getLong("count"),
                        user.getJSONObject("edge_followed_by").getLong("count"),
                        user.getJSONObject("edge_follow").getLong("count"),
                        user.optBoolean("followed_by_viewer"),
                        user.optBoolean("restricted_by_viewer"),
                        user.optBoolean("blocked_by_viewer"),
                        user.optBoolean("requested_by_viewer"));
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_PROFILE_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final ProfileModel result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}
