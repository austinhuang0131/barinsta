package awais.instagrabber.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.utils.PasswordUtils.IncorrectPasswordException;
import awaisomereport.LogCollector.LogFile;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class ExportImportUtils {
    private static final String TAG = "ExportImportUtils";

    public static final int FLAG_COOKIES = 1;
    public static final int FLAG_FAVORITES = 1 << 1;
    public static final int FLAG_SETTINGS = 1 << 2;

    @IntDef(value = {FLAG_COOKIES, FLAG_FAVORITES, FLAG_SETTINGS}, flag = true)
    @interface ExportImportFlags {}

    public static void exportData(@Nullable final String password,
                                  @ExportImportFlags final int flags,
                                  @NonNull final File filePath,
                                  final FetchListener<Boolean> fetchListener,
                                  @NonNull final Context context) {
        final String exportString = getExportString(flags, context);
        if (TextUtils.isEmpty(exportString)) return;
        final boolean isPass = !TextUtils.isEmpty(password);
        byte[] exportBytes = null;
        if (isPass) {
            final byte[] passwordBytes = password.getBytes();
            final byte[] bytes = new byte[32];
            System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
            try {
                exportBytes = PasswordUtils.enc(exportString, bytes);
            } catch (final Exception e) {
                if (fetchListener != null) fetchListener.onResult(false);
                if (logCollector != null)
                    logCollector.appendException(e, LogFile.UTILS_EXPORT, "Export::isPass");
                if (BuildConfig.DEBUG) Log.e(TAG, "", e);
            }
        } else {
            exportBytes = Base64.encode(exportString.getBytes(), Base64.DEFAULT | Base64.NO_WRAP | Base64.NO_PADDING);
        }
        if (exportBytes != null && exportBytes.length > 1) {
            try (final FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(isPass ? 'A' : 'Z');
                fos.write(exportBytes);
                if (fetchListener != null) fetchListener.onResult(true);
            } catch (final Exception e) {
                if (fetchListener != null) fetchListener.onResult(false);
                if (logCollector != null)
                    logCollector.appendException(e, LogFile.UTILS_EXPORT, "Export::notPass");
                if (BuildConfig.DEBUG) Log.e(TAG, "", e);
            }
        } else if (fetchListener != null) fetchListener.onResult(false);
    }

    public static void importData(@NonNull final Context context,
                                  @ExportImportFlags final int flags,
                                  @NonNull final File file,
                                  final String password,
                                  final FetchListener<Boolean> fetchListener) throws IncorrectPasswordException {
        try (final FileInputStream fis = new FileInputStream(file)) {
            final int configType = fis.read();
            final StringBuilder builder = new StringBuilder();
            int c;
            while ((c = fis.read()) != -1) {
                builder.append((char) c);
            }
            if (configType == 'A') {
                // password
                if (TextUtils.isEmpty(password)) return;
                try {
                    final byte[] passwordBytes = password.getBytes();
                    final byte[] bytes = new byte[32];
                    System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
                    importJson(new String(PasswordUtils.dec(builder.toString(), bytes)),
                               flags,
                               fetchListener);
                } catch (final IncorrectPasswordException e) {
                    throw e;
                } catch (final Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (logCollector != null)
                        logCollector.appendException(e, LogFile.UTILS_IMPORT, "Import::pass");
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error importing backup", e);
                }
            } else if (configType == 'Z') {
                importJson(new String(Base64.decode(builder.toString(), Base64.DEFAULT | Base64.NO_PADDING | Base64.NO_WRAP)),
                           flags,
                           fetchListener);

            } else {
                Toast.makeText(context, "File is corrupted!", Toast.LENGTH_LONG).show();
                if (fetchListener != null) fetchListener.onResult(false);
            }
        } catch (IncorrectPasswordException e) {
            // separately handle incorrect password
            throw e;
        } catch (final Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_IMPORT, "Import");
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        }
    }

    private static void importJson(@NonNull final String json,
                                   @ExportImportFlags final int flags,
                                   final FetchListener<Boolean> fetchListener) {
        try {
            final JSONObject jsonObject = new JSONObject(json);
            if ((flags & FLAG_SETTINGS) == FLAG_SETTINGS && jsonObject.has("settings")) {
                importSettings(jsonObject);
            }
            if ((flags & FLAG_COOKIES) == FLAG_COOKIES && jsonObject.has("cookies")) {
                importAccounts(jsonObject);
            }
            if ((flags & FLAG_FAVORITES) == FLAG_FAVORITES && jsonObject.has("favs")) {
                importFavorites(jsonObject);
            }
            if (fetchListener != null) fetchListener.onResult(true);
        } catch (final Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_IMPORT, "importJson");
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        }
    }

    private static void importFavorites(final JSONObject jsonObject) throws JSONException {
        final JSONArray favs = jsonObject.getJSONArray("favs");
        for (int i = 0; i < favs.length(); i++) {
            final JSONObject favsObject = favs.getJSONObject(i);
            final String queryText = favsObject.optString("q");
            if (TextUtils.isEmpty(queryText)) continue;
            final Pair<FavoriteType, String> favoriteTypeQueryPair;
            String query = null;
            FavoriteType favoriteType = null;
            if (queryText.contains("@")
                    || queryText.contains("#")
                    || queryText.contains("/")) {
                favoriteTypeQueryPair = Utils.migrateOldFavQuery(queryText);
                if (favoriteTypeQueryPair != null) {
                    query = favoriteTypeQueryPair.second;
                    favoriteType = favoriteTypeQueryPair.first;
                }
            } else {
                query = queryText;
                favoriteType = FavoriteType.valueOf(favsObject.optString("type"));
            }
            if (query == null || favoriteType == null) {
                continue;
            }
            final DataBox.FavoriteModel favoriteModel = new DataBox.FavoriteModel(
                    -1,
                    query,
                    favoriteType,
                    favsObject.optString("s"),
                    favoriteType == FavoriteType.HASHTAG ? null
                                                         : favsObject.optString("pic_url"),
                    new Date(favsObject.getLong("d")));
            // Log.d(TAG, "importJson: favoriteModel: " + favoriteModel);
            Utils.dataBox.addOrUpdateFavorite(favoriteModel);
        }
    }

    private static void importAccounts(final JSONObject jsonObject) throws JSONException {
        final JSONArray cookies = jsonObject.getJSONArray("cookies");
        for (int i = 0; i < cookies.length(); i++) {
            final JSONObject cookieObject = cookies.getJSONObject(i);
            final DataBox.CookieModel cookieModel = new DataBox.CookieModel(
                    cookieObject.optString("i"),
                    cookieObject.optString("u"),
                    cookieObject.optString("c"),
                    cookieObject.optString("full_name"),
                    cookieObject.optString("profile_pic")
            );
            if (!cookieModel.isValid()) continue;
            // Log.d(TAG, "importJson: cookieModel: " + cookieModel);
            Utils.dataBox.addOrUpdateUser(cookieModel);
        }
    }

    private static void importSettings(final JSONObject jsonObject) throws JSONException {
        final JSONObject objSettings = jsonObject.getJSONObject("settings");
        final Iterator<String> keys = objSettings.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            final Object val = objSettings.opt(key);
            // Log.d(TAG, "importJson: key: " + key + ", val: " + val);
            if (val instanceof String) {
                settingsHelper.putString(key, (String) val);
            } else if (val instanceof Integer) {
                settingsHelper.putInteger(key, (int) val);
            } else if (val instanceof Boolean) {
                settingsHelper.putBoolean(key, (boolean) val);
            }
        }
    }

    public static boolean isEncrypted(final File file) {
        try (final FileInputStream fis = new FileInputStream(file)) {
            final int configType = fis.read();
            if (configType == 'A') {
                return true;
            }
        } catch (final Exception e) {
            Log.e(TAG, "isEncrypted", e);
        }
        return false;
    }

    @Nullable
    private static String getExportString(@ExportImportFlags final int flags,
                                          @NonNull final Context context) {
        String result = null;
        try {
            final JSONObject jsonObject = new JSONObject();
            if ((flags & FLAG_SETTINGS) == FLAG_SETTINGS) {
                jsonObject.put("settings", getSettings(context));
            }
            if ((flags & FLAG_COOKIES) == FLAG_COOKIES) {
                jsonObject.put("cookies", getCookies());
            }
            if ((flags & FLAG_FAVORITES) == FLAG_FAVORITES) {
                jsonObject.put("favs", getFavorites());
            }

            result = jsonObject.toString();
        } catch (final Exception e) {
            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_EXPORT, "getExportString");
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        }
        return result;
    }

    @NonNull
    private static JSONObject getSettings(@NonNull final Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final Map<String, ?> allPrefs = sharedPreferences.getAll();
        if (allPrefs == null) {
            return new JSONObject();
        }
        try {
            final JSONObject jsonObject = new JSONObject(allPrefs);
            jsonObject.remove(Constants.COOKIE);
            jsonObject.remove(Constants.DEVICE_UUID);
            jsonObject.remove(Constants.PREV_INSTALL_VERSION);
            return jsonObject;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting settings", e);
        }
        return new JSONObject();
    }

    @NonNull
    private static JSONArray getFavorites() {
        if (Utils.dataBox == null) return new JSONArray();
        try {
            final List<DataBox.FavoriteModel> allFavorites = Utils.dataBox.getAllFavorites();
            final JSONArray jsonArray = new JSONArray();
            for (final DataBox.FavoriteModel favorite : allFavorites) {
                final JSONObject jsonObject = new JSONObject();
                jsonObject.put("q", favorite.getQuery());
                jsonObject.put("type", favorite.getType().toString());
                jsonObject.put("s", favorite.getDisplayName());
                jsonObject.put("pic_url", favorite.getPicUrl());
                jsonObject.put("d", favorite.getDateAdded().getTime());
                jsonArray.put(jsonObject);
            }
            return jsonArray;
        } catch (final Exception e) {
            if (logCollector != null) {
                logCollector.appendException(e, LogFile.UTILS_EXPORT, "getFavorites");
            }
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error exporting favorites", e);
            }
        }
        return new JSONArray();
    }

    @NonNull
    private static JSONArray getCookies() {
        if (Utils.dataBox == null) return new JSONArray();
        try {
            final List<DataBox.CookieModel> allCookies = Utils.dataBox.getAllCookies();
            final JSONArray jsonArray = new JSONArray();
            for (final DataBox.CookieModel cookie : allCookies) {
                final JSONObject jsonObject = new JSONObject();
                jsonObject.put("i", cookie.getUid());
                jsonObject.put("u", cookie.getUsername());
                jsonObject.put("c", cookie.getCookie());
                jsonObject.put("full_name", cookie.getFullName());
                jsonObject.put("profile_pic", cookie.getProfilePic());
                jsonArray.put(jsonObject);
            }
            return jsonArray;
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error exporting accounts", e);
            }
        }
        return new JSONArray();
    }
}