package awais.instagrabber.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import awais.instagrabber.R;
import awais.instagrabber.models.enums.FavoriteType;
import awaisomereport.LogCollector;

public final class Utils {
    private static final String TAG = "Utils";
    private static final int VIDEO_CACHE_MAX_BYTES = 10 * 1024 * 1024;

    public static LogCollector logCollector;
    public static SettingsHelper settingsHelper;
    public static DataBox dataBox;
    public static boolean sessionVolumeFull = false;
    public static final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public static ClipboardManager clipboardManager;
    public static DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    public static SimpleDateFormat datetimeParser;
    public static SimpleCache simpleCache;

    public static int convertDpToPx(final float dp) {
        if (displayMetrics == null)
            displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round((dp * displayMetrics.densityDpi) / 160.0f);
    }

    public static void copyText(@NonNull final Context context, final CharSequence string) {
        if (clipboardManager == null)
            clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        int toastMessage = R.string.clipboard_error;
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), string));
            toastMessage = R.string.clipboard_copied;
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    public static Map<String, String> sign(final Map<String, Object> form) {
        final String signed = sign(new JSONObject(form).toString());
        if (signed == null) {
            return null;
        }
        final String[] parts = signed.split("&");
        final Map<String, String> map = new HashMap<>();
        for (final String part : parts) {
            final String[] partSplit = part.split("=");
            map.put(partSplit[0], partSplit[1]);
        }
        return map;
    }

    public static String sign(final String message) {
        return sign(Constants.SIGNATURE_KEY, message);
    }

    public static String sign(final String key, final String message) {
        try {
            final Mac hasher = Mac.getInstance("HmacSHA256");
            hasher.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
            byte[] hash = hasher.doFinal(message.getBytes());
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "ig_sig_key_version=" + Constants.SIGNATURE_VERSION + "&signed_body=" + hexString.toString() + "." + message;
        } catch (Exception e) {
            Log.e(TAG, "Error signing", e);
            return null;
        }
    }

    public static boolean isImage(final Uri itemUri, final ContentResolver contentResolver) {
        String mimeType;
        if (itemUri == null) return false;
        final String scheme = itemUri.getScheme();
        if (TextUtils.isEmpty(scheme))
            mimeType = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(itemUri.toString()).toLowerCase());
        else
            mimeType = scheme.equals(ContentResolver.SCHEME_CONTENT) ? contentResolver.getType(itemUri)
                                                                     : mimeTypeMap.getMimeTypeFromExtension
                                                                             (MimeTypeMap.getFileExtensionFromUrl(itemUri.toString()).toLowerCase());

        if (TextUtils.isEmpty(mimeType)) return true;
        mimeType = mimeType.toLowerCase();
        return mimeType.startsWith("image");
    }

    public static void errorFinish(@NonNull final Activity activity) {
        Toast.makeText(activity, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    public static SimpleCache getSimpleCacheInstance(final Context context) {
        if (context == null) {
            return null;
        }
        final ExoDatabaseProvider exoDatabaseProvider = new ExoDatabaseProvider(context);
        final File cacheDir = context.getCacheDir();
        if (simpleCache == null && cacheDir != null) {
            simpleCache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_MAX_BYTES), exoDatabaseProvider);
        }
        return simpleCache;
    }

    @Nullable
    public static Pair<FavoriteType, String> migrateOldFavQuery(final String queryText) {
        if (queryText.startsWith("@")) {
            return new Pair<>(FavoriteType.USER, queryText.substring(1));
        } else if (queryText.contains("/")) {
            return new Pair<>(FavoriteType.LOCATION, queryText.substring(0, queryText.indexOf("/")));
        } else if (queryText.startsWith("#")) {
            return new Pair<>(FavoriteType.HASHTAG, queryText.substring(1));
        }
        return null;
    }
}
