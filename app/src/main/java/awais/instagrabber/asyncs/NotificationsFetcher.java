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
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class NotificationsFetcher extends AsyncTask<Void, Void, NotificationModel[]> {
    private final FetchListener<NotificationModel[]> fetchListener;

    public NotificationsFetcher(final FetchListener<NotificationModel[]> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected NotificationModel[] doInBackground(final Void... voids) {
        NotificationModel[] result = null;
        final String url = "https://www.instagram.com/accounts/activity/?__a=1";
        Utils.setupCookies(settingsHelper.getString(Constants.COOKIE));

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("Accept-Language", LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject page = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("graphql").getJSONObject("user"),
                        ewaf = page.getJSONObject("activity_feed").optJSONObject("edge_web_activity_feed"),
                        efr = page.optJSONObject("edge_follow_requests"),
                        data;
                JSONArray media;
                int totalLength = 0, mediaLen = 0, reqLen = 0;
                NotificationModel[] models = null, req = null;

                if ((media = ewaf.optJSONArray("edges")) != null && media.length() > 0 &&
                        (data = media.optJSONObject(0).optJSONObject("node")) != null) {
                    mediaLen = media.length();
                    models = new NotificationModel[mediaLen];
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
                }

                if (efr != null && (media = efr.optJSONArray("edges")) != null && media.length() > 0 &&
                        (data = media.optJSONObject(0).optJSONObject("node")) != null) {
                    reqLen = media.length();
                    req = new NotificationModel[reqLen];
                    for (int i = 0; i < reqLen; ++i) {
                        data = media.optJSONObject(i).optJSONObject("node");
                        req[i] = new NotificationModel(data.getString(Constants.EXTRAS_ID),
                                data.optString("full_name"), 0L, data.getString("username"),
                                data.getString("profile_pic_url"), null, null, NotificationType.REQUEST);
                    }
                }

                result = new NotificationModel[mediaLen + reqLen];
                if (req != null) System.arraycopy(req, 0, result, 0, reqLen);
                if (models != null) System.arraycopy(models, 0, result, reqLen, mediaLen);
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