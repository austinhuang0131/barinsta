package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class NotificationsFetcher extends AsyncTask<Void, Void, NotificationModel[]> {
    private final FetchListener<NotificationModel[]> fetchListener;

    public NotificationsFetcher(final FetchListener<NotificationModel[]> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected NotificationModel[] doInBackground(final Void... voids) {
        NotificationModel[] result = null;
        final String url = "https://www.instagram.com/accounts/activity/?__a=1";

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("Accept-Language", LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject data = new JSONObject(Utils.readFromConnection(conn))
                        .getJSONObject("graphql").getJSONObject("user").getJSONObject("activity_feed").getJSONObject("edge_web_activity_feed");

                JSONArray media;
                if ((media = data.optJSONArray("edges")) != null && media.length() > 0 &&
                        (data = media.optJSONObject(0).optJSONObject("node")) != null) {

                    final int mediaLen = media.length();

                    final NotificationModel[] models = new NotificationModel[mediaLen];
                    for (int i = 0; i < mediaLen; ++i) {
                        data = media.optJSONObject(i).optJSONObject("node");
                        if (Utils.getNotifType(data.getString("__typename")) == null) continue;
                        models[i] = new NotificationModel(data.getString(Constants.EXTRAS_ID),
                                data.optString("text"), // comments or mentions
                                data.getLong("timestamp"),
                                data.getJSONObject("user").getString("username"),
                                data.getJSONObject("user").getString("profile_pic_url"),
                                !data.isNull("media") ? data.getJSONObject("media").getString("shortcode") : null,
                                !data.isNull("media") ? data.getJSONObject("media").getString("thumbnail_src") : null,
                                Utils.getNotifType(data.getString("__typename")));
                    }
                    result = models;
                }
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_NOTIFICATION_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final NotificationModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}