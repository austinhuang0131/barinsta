package awais.instagrabber.asyncs;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import awais.instagrabber.models.ImageUploadOptions;
import awais.instagrabber.utils.Utils;

public class ImageUploader extends AsyncTask<ImageUploadOptions, Void, ImageUploader.ImageUploadResponse> {
    private static final String TAG = "ImageUploader";
    private static final long LOWER = 1000000000L;
    private static final long UPPER = 9999999999L;
    private OnImageUploadCompleteListener listener;

    protected ImageUploadResponse doInBackground(final ImageUploadOptions... imageUploadOptions) {
        if (imageUploadOptions == null || imageUploadOptions.length == 0 || imageUploadOptions[0] == null) {
            return null;
        }
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream inputStream = null;
        BufferedReader r = null;
        ByteArrayOutputStream baos = null;
        try {
            final ImageUploadOptions options = imageUploadOptions[0];
            final Bitmap bitmap = options.getBitmap();
            baos = new ByteArrayOutputStream();
            final boolean compressResult = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            if (!compressResult) {
                Log.e(TAG, "Compress result was false!");
                return null;
            }
            final byte[] bytes = baos.toByteArray();
            final String contentLength = String.valueOf(bytes.length);
            final Map<String, String> headers = new HashMap<>();
            final String uploadId = String.valueOf(new Date().getTime());
            final long random = LOWER + new Random().nextLong() * (UPPER - LOWER + 1);
            final String name = String.format("%s_0_%s", uploadId, random);
            final String waterfallId = options.getWaterfallId() != null ? options.getWaterfallId() : UUID.randomUUID().toString();
            headers.put("X-Entity-Type", "image/jpeg");
            headers.put("Offset", "0");
            headers.put("X_FB_PHOTO_WATERFALL_ID", waterfallId);
            headers.put("X-Instagram-Rupload-Params", new JSONObject(createPhotoRuploadParams(options, uploadId)).toString());
            headers.put("X-Entity-Name", name);
            headers.put("X-Entity-Length", contentLength);
            headers.put("Content-Type", "application/octet-stream");
            headers.put("Content-Length", contentLength);
            headers.put("Accept-Encoding", "gzip");
            final String url = "https://www.instagram.com/rupload_igphoto/" + name + "/";
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            Utils.setConnectionHeaders(connection, headers);
            out = new BufferedOutputStream(connection.getOutputStream());
            out.write(bytes);
            out.flush();
            final int responseCode = connection.getResponseCode();
            Log.d(TAG, "response: " + responseCode);
            final String responseCodeString = String.valueOf(responseCode);
            final InputStream responseInputStream = responseCodeString.startsWith("4") || responseCodeString.startsWith("5")
                    ? connection.getErrorStream() : connection.getInputStream();
            r = new BufferedReader(new InputStreamReader(responseInputStream));
            final StringBuilder builder = new StringBuilder();
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                if (builder.length() != 0) {
                    builder.append("\n");
                }
                builder.append(line);
            }
            return new ImageUploadResponse(responseCode, new JSONObject(builder.toString()));
        } catch (Exception ex) {
            Log.e(TAG, "Image upload error:", ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {}
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final ImageUploadResponse response) {
        if (listener != null) {
            listener.onImageUploadComplete(response);
        }
    }

    private Map<String, String> createPhotoRuploadParams(final ImageUploadOptions options, final String uploadId) {
        final Map<String, Integer> retryContext = new HashMap<>();
        retryContext.put("num_step_auto_retry", 0);
        retryContext.put("num_reupload", 0);
        retryContext.put("num_step_manual_retry", 0);
        final String retryContextString = new JSONObject(retryContext).toString();
        final Map<String, String> params = new HashMap<>();
        params.put("retry_context", retryContextString);
        params.put("media_type", "1");
        params.put("upload_id", uploadId);
        params.put("xsharing_user_ids", "[]");
        final Map<String, String> imageCompression = new HashMap<>();
        imageCompression.put("lib_name", "moz");
        imageCompression.put("lib_version", "3.1.m");
        imageCompression.put("quality", "80");
        params.put("image_compression", new JSONObject(imageCompression).toString());
        if (options.isSidecar()) {
            params.put("is_sidecar", "1");
        }
        return params;
    }

    public void setOnTaskCompleteListener(final OnImageUploadCompleteListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    public interface OnImageUploadCompleteListener {
        void onImageUploadComplete(ImageUploadResponse response);
    }

    public static class ImageUploadResponse {
        private int responseCode;
        private JSONObject response;

        public ImageUploadResponse(int responseCode, JSONObject response) {
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
