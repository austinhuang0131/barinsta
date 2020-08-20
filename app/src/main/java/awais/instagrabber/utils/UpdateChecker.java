package awais.instagrabber.utils;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;

public final class UpdateChecker extends AsyncTask<Void, Void, Boolean> {
    private final FetchListener<String> fetchListener;
    private String version;

    public UpdateChecker(final FetchListener<String> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @NonNull
    @Override
    protected Boolean doInBackground(final Void... voids) {
        try {
            version = "";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL("https://github.com/austinhuang0131/instagrabber/releases/latest").openConnection();
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", Constants.A_USER_AGENT);
            conn.connect();

            final int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP && !BuildConfig.DEBUG) {
                version = conn.getHeaderField("Location").split("/v")[1];
                return version != BuildConfig.VERSION_NAME;
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if (result != null && result && fetchListener != null)
            fetchListener.onResult("v"+version);
    }
}