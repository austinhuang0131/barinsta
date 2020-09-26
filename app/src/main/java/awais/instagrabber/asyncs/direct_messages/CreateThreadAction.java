package awais.instagrabber.asyncs.direct_messages;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class CreateThreadAction extends AsyncTask<Void, Void, String> {
    private static final String TAG = "CommentAction";

    private final String cookie;
    private final String userId;
    private final OnTaskCompleteListener onTaskCompleteListener;

    public CreateThreadAction(final String cookie, final String userId, final OnTaskCompleteListener onTaskCompleteListener) {
        this.cookie = cookie;
        this.userId = userId;
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    protected String doInBackground(Void... lmao) {
        final String url = "https://i.instagram.com/api/v1/direct_v2/create_group_thread/";
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            urlConnection.setUseCaches(false);
            final String urlParameters = Utils.sign("{\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                    + "\",\"_uid\":\"" + CookieUtils.getUserIdFromCookie(cookie)
                    + "\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                    + "\",\"recipient_users\":\"[" + userId // <- string of array of number (not joking)
                    + "]\"}");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (urlParameters != null) {
                urlConnection.setRequestProperty("Content-Length", "" + urlParameters.getBytes().length);
            }
            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return new JSONObject(NetworkUtils.readFromConnection(urlConnection)).getString("thread_id");
            }
        } catch (Throwable ex) {
            Log.e(TAG, "reply (CT): " + ex);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final String threadId) {
        if (threadId == null || onTaskCompleteListener == null) {
            return;
        }
        onTaskCompleteListener.onTaskComplete(threadId);
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final String threadId);
    }
}
