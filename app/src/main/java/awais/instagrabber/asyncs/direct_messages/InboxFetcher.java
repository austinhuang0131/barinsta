package awais.instagrabber.asyncs.direct_messages;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.direct_messages.InboxModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.logCollector;
import static awaisomereport.LogCollector.LogFile;

public final class InboxFetcher extends AsyncTask<Void, Void, InboxModel> {
    private final String endCursor;
    private final FetchListener<InboxModel> fetchListener;

    public InboxFetcher(final String endCursor, final FetchListener<InboxModel> fetchListener) {
        this.endCursor = Utils.isEmpty(endCursor) ? "" : "?cursor=" + endCursor;
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected InboxModel doInBackground(final Void... voids) {
        InboxModel result = null;

        final String url = "https://i.instagram.com/api/v1/direct_v2/inbox/" + endCursor;
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            conn.setRequestProperty("Accept-Language", LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
            conn.setUseCaches(false);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject data = new JSONObject(Utils.readFromConnection(conn));
                // try (FileWriter fileWriter = new FileWriter(new File("/sdcard/test.json"))) {
                //     fileWriter.write(data.toString(2));
                // }

                final long seqId = data.optLong("seq_id");
                final int pendingRequestsCount = data.optInt("pending_requests_total");
                final boolean hasPendingTopRequests = data.optBoolean("has_pending_top_requests");

                data = data.getJSONObject("inbox");

                final boolean blendedInboxEnabled = data.optBoolean("blended_inbox_enabled");
                final boolean hasOlder = data.optBoolean("has_older");
                final int unseenCount = data.optInt("unseen_count");
                final long unseenCountTimestamp = data.optLong("unseen_count_ts");
                final String oldestCursor = data.optString("oldest_cursor");

                InboxThreadModel[] inboxThreadModels = null;

                final JSONArray threadsArray = data.optJSONArray("threads");
                if (threadsArray != null) {
                    final int threadsLen = threadsArray.length();
                    inboxThreadModels = new InboxThreadModel[threadsLen];

                    for (int i = 0; i < threadsLen; ++i)
                        inboxThreadModels[i] = Utils.createInboxThreadModel(threadsArray.getJSONObject(i), false);
                }

                result = new InboxModel(hasOlder, hasPendingTopRequests,
                        blendedInboxEnabled, unseenCount, pendingRequestsCount,
                        seqId, unseenCountTimestamp, oldestCursor, inboxThreadModels);
            }

            conn.disconnect();
        } catch (final Exception e) {
            result = null;
            if (logCollector != null)
                logCollector.appendException(e, LogFile.ASYNC_DMS, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final InboxModel inboxModel) {
        if (fetchListener != null) fetchListener.onResult(inboxModel);
    }
}