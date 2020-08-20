package awais.instagrabber.asyncs.direct_messages;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DirectThreadBroadcaster extends AsyncTask<DirectThreadBroadcaster.BroadcastOptions, Void, DirectThreadBroadcaster.DirectThreadBroadcastResponse> {
    private static final String TAG = "DirectThreadBroadcaster";

    private final String threadId;

    private OnBroadcastCompleteListener listener;

    public DirectThreadBroadcaster(String threadId) {
        this.threadId = threadId;
    }

    @Override
    protected DirectThreadBroadcastResponse doInBackground(final BroadcastOptions... broadcastOptionsArray) {
        if (broadcastOptionsArray == null || broadcastOptionsArray.length == 0 || broadcastOptionsArray[0] == null) {
            return null;
        }
        final BroadcastOptions broadcastOptions = broadcastOptionsArray[0];
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String cc = UUID.randomUUID().toString();
        final Map<String, String> form = new HashMap<>();
        form.put("_csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
        form.put("_uid", Utils.getUserIdFromCookie(cookie));
        form.put("__uuid", settingsHelper.getString(Constants.DEVICE_UUID));
        form.put("client_context", cc);
        form.put("mutation_token", cc);
        form.putAll(broadcastOptions.getFormMap());
        form.put("thread_id", threadId);
        form.put("action", "send_item");
        final String message = new JSONObject(form).toString();
        final String content = Utils.sign(message);
        final String url = "https://i.instagram.com/api/v1/direct_v2/threads/broadcast/" + broadcastOptions.getItemType().getValue() + "/";
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        BufferedReader r = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (content != null) {
                connection.setRequestProperty("Content-Length", "" + content.getBytes().length);
            }
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(content);
            outputStream.flush();
            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, responseCode + ": " + content + ": " + cookie);
                return new DirectThreadBroadcastResponse(responseCode, null);
            }
            r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            final StringBuilder builder = new StringBuilder();
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                if (builder.length() != 0) {
                    builder.append("\n");
                }
                builder.append(line);
            }
            return new DirectThreadBroadcastResponse(responseCode, new JSONObject(builder.toString()));
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final DirectThreadBroadcastResponse result) {
        if (listener != null) {
            listener.onTaskComplete(result);
        }
    }

    public void setOnTaskCompleteListener(final OnBroadcastCompleteListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    public interface OnBroadcastCompleteListener {
        void onTaskComplete(DirectThreadBroadcastResponse response);
    }

    public enum ItemType {
        TEXT("text"),
        REACTION("reaction"),
        REELSHARE("reel_share"),
        IMAGE("configure_photo");

        private final String value;

        ItemType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static abstract class BroadcastOptions {
        private final ItemType itemType;

        public BroadcastOptions(final ItemType itemType) {
            this.itemType = itemType;
        }

        public ItemType getItemType() {
            return itemType;
        }

        abstract Map<String, String> getFormMap();
    }

    public static class TextBroadcastOptions extends BroadcastOptions {
        private final String text;

        public TextBroadcastOptions(String text) throws UnsupportedEncodingException {
            super(ItemType.TEXT);
            this.text = URLEncoder.encode(text, "UTF-8")
                    .replaceAll("\\+", "%20").replaceAll("%21", "!").replaceAll("%27", "'")
                    .replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("%7E", "~");
        }

        @Override
        Map<String, String> getFormMap() {
            return Collections.singletonMap("text", text);
        }
    }

    public static class ReactionBroadcastOptions extends BroadcastOptions {
        private final String itemId;
        private final boolean delete;

        public ReactionBroadcastOptions(String itemId, boolean delete) {
            super(ItemType.REACTION);
            this.itemId = itemId;
            this.delete = delete;
        }

        @Override
        Map<String, String> getFormMap() {
            final Map<String, String> form = new HashMap<>();
            form.put("item_id", itemId);
            form.put("reaction_status", delete ? "deleted" : "created");
            form.put("reaction_type", "like");
            return form;
        }
    }

    public static class StoryReplyBroadcastOptions extends BroadcastOptions {
        private final String text, mediaId, reelId;

        public StoryReplyBroadcastOptions(String text, String mediaId, String reelId) throws UnsupportedEncodingException {
            super(ItemType.REELSHARE);
            this.text = URLEncoder.encode(text, "UTF-8")
                    .replaceAll("\\+", "%20").replaceAll("%21", "!").replaceAll("%27", "'")
                    .replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("%7E", "~");
            this.mediaId = mediaId;
            this.reelId = reelId; // or user id, usually same
        }

        @Override
        Map<String, String> getFormMap() {
            final Map<String, String> form = new HashMap<>();
            form.put("text", text);
            form.put("media_id", mediaId);
            form.put("reel_id", reelId);
            form.put("entry", "reel");
            return form;
        }
    }

    public static class ImageBroadcastOptions extends BroadcastOptions {
        final boolean allowFullAspectRatio;
        final String uploadId;

        public ImageBroadcastOptions(final boolean allowFullAspectRatio, final String uploadId) {
            super(ItemType.IMAGE);
            this.allowFullAspectRatio = allowFullAspectRatio;
            this.uploadId = uploadId;
        }

        @Override
        Map<String, String> getFormMap() {
            final Map<String, String> form = new HashMap<>();
            form.put("allow_full_aspect_ratio", String.valueOf(allowFullAspectRatio));
            form.put("upload_id", uploadId);
            return form;
        }
    }

    public static class DirectThreadBroadcastResponse {
        private int responseCode;
        private JSONObject response;

        public DirectThreadBroadcastResponse(int responseCode, JSONObject response) {
            this.responseCode = responseCode;
            this.response = response;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public JSONObject getResponse() {
            return response;
        }
    }
}
