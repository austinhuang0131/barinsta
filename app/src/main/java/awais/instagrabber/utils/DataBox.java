package awais.instagrabber.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.models.enums.FavoriteType;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class DataBox extends SQLiteOpenHelper {
    private static final String TAG = "DataBox";

    private static DataBox sInstance;

    private final static int VERSION = 3;
    private final static String TABLE_COOKIES = "cookies";
    private final static String TABLE_FAVORITES = "favorites";

    private final static String KEY_ID = "id";
    private final static String KEY_USERNAME = Constants.EXTRAS_USERNAME;
    private final static String KEY_COOKIE = "cookie";
    private final static String KEY_UID = "uid";
    private final static String KEY_FULL_NAME = "full_name";
    private final static String KEY_PROFILE_PIC = "profile_pic";

    private final static String FAV_COL_ID = "id";
    private final static String FAV_COL_QUERY = "query_text";
    private final static String FAV_COL_TYPE = "type";
    private final static String FAV_COL_DISPLAY_NAME = "display_name";
    private final static String FAV_COL_PIC_URL = "pic_url";
    private final static String FAV_COL_DATE_ADDED = "date_added";

    public static synchronized DataBox getInstance(final Context context) {
        if (sInstance == null) sInstance = new DataBox(context.getApplicationContext());
        return sInstance;
    }

    private DataBox(@Nullable final Context context) {
        super(context, "cookiebox.db", null, VERSION);
    }

    @Override
    public void onCreate(@NonNull final SQLiteDatabase db) {
        Log.i(TAG, "Creating tables...");
        db.execSQL("CREATE TABLE " + TABLE_COOKIES + " ("
                           + KEY_ID + " INTEGER PRIMARY KEY,"
                           + KEY_UID + " TEXT,"
                           + KEY_USERNAME + " TEXT,"
                           + KEY_COOKIE + " TEXT,"
                           + KEY_FULL_NAME + " TEXT,"
                           + KEY_PROFILE_PIC + " TEXT)");
        // db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY, query_text TEXT, date_added INTEGER, query_display TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " ("
                           + FAV_COL_ID + " INTEGER PRIMARY KEY,"
                           + FAV_COL_QUERY + " TEXT,"
                           + FAV_COL_TYPE + " TEXT,"
                           + FAV_COL_DISPLAY_NAME + " TEXT,"
                           + FAV_COL_PIC_URL + " TEXT,"
                           + FAV_COL_DATE_ADDED + " INTEGER)");
        Log.i(TAG, "Tables created!");
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Log.i(TAG, String.format("Updating DB from v%d to v%d", oldVersion, newVersion));
        // switch without break, so that all migrations from a previous version to new are run
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE " + TABLE_COOKIES + " ADD " + KEY_FULL_NAME + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_COOKIES + " ADD " + KEY_PROFILE_PIC + " TEXT");
            case 2:
                final List<FavoriteModel> oldFavorites = backupOldFavorites(db);
                // recreate with new columns (as there will be no doubt about the `query_display` column being present or not in the future versions)
                db.execSQL("DROP TABLE " + TABLE_FAVORITES);
                db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " ("
                                   + FAV_COL_ID + " INTEGER PRIMARY KEY,"
                                   + FAV_COL_QUERY + " TEXT,"
                                   + FAV_COL_TYPE + " TEXT,"
                                   + FAV_COL_DISPLAY_NAME + " TEXT,"
                                   + FAV_COL_PIC_URL + " TEXT,"
                                   + FAV_COL_DATE_ADDED + " INTEGER)");
                // add the old favorites back
                for (final FavoriteModel oldFavorite : oldFavorites) {
                    addOrUpdateFavorite(db, oldFavorite);
                }
        }
        Log.i(TAG, String.format("DB update from v%d to v%d completed!", oldVersion, newVersion));
    }

    @NonNull
    private List<FavoriteModel> backupOldFavorites(@NonNull final SQLiteDatabase db) {
        // check if old favorites table had the column query_display
        final boolean queryDisplayExists = checkColumnExists(db, TABLE_FAVORITES, "query_display");
        Log.d(TAG, "backupOldFavorites: queryDisplayExists: " + queryDisplayExists);
        final List<FavoriteModel> oldModels = new ArrayList<>();
        final String sql = "SELECT "
                + "query_text,"
                + "date_added"
                + (queryDisplayExists ? ",query_display" : "")
                + " FROM " + TABLE_FAVORITES;
        try (final Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        final String queryText = cursor.getString(cursor.getColumnIndex("query_text"));
                        final Pair<FavoriteType, String> favoriteTypeQueryPair = Utils.migrateOldFavQuery(queryText);
                        if (favoriteTypeQueryPair == null) continue;
                        final FavoriteType type = favoriteTypeQueryPair.first;
                        final String query = favoriteTypeQueryPair.second;
                        oldModels.add(new FavoriteModel(
                                -1,
                                query,
                                type,
                                queryDisplayExists ? cursor.getString(cursor.getColumnIndex("query_display"))
                                                   : null,
                                null,
                                new Date(cursor.getLong(cursor.getColumnIndex("date_added")))
                        ));
                    } catch (Exception e) {
                        Log.e(TAG, "onUpgrade", e);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "onUpgrade", e);
        }
        Log.d(TAG, "backupOldFavorites: oldModels:" + oldModels);
        return oldModels;
    }

    public boolean checkColumnExists(@NonNull final SQLiteDatabase db,
                                     @NonNull final String tableName,
                                     @NonNull final String columnName) {
        boolean exists = false;
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            if (cursor.moveToFirst()) {
                do {
                    final String currentColumn = cursor.getString(cursor.getColumnIndex("name"));
                    if (currentColumn.equals(columnName)) {
                        exists = true;
                    }
                } while (cursor.moveToNext());

            }
        } catch (Exception ex) {
            Log.e(TAG, "checkColumnExists", ex);
        }
        return exists;
    }

    public final void addOrUpdateFavorite(@NonNull final FavoriteModel model) {
        final String query = model.getQuery();
        if (!TextUtils.isEmpty(query)) {
            try (final SQLiteDatabase db = getWritableDatabase()) {
                db.beginTransaction();
                try {
                    addOrUpdateFavorite(db, model);
                    db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (logCollector != null) {
                        logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "addOrUpdateFavorite");
                    }
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error adding/updating favorite", e);
                    }
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    private void addOrUpdateFavorite(@NonNull final SQLiteDatabase db, @NonNull final FavoriteModel model) {
        final ContentValues values = new ContentValues();
        values.put(FAV_COL_QUERY, model.getQuery());
        values.put(FAV_COL_TYPE, model.getType().toString());
        values.put(FAV_COL_DISPLAY_NAME, model.getDisplayName());
        values.put(FAV_COL_PIC_URL, model.getPicUrl());
        values.put(FAV_COL_DATE_ADDED, model.getDateAdded().getTime());
        int rows;
        if (model.getId() >= 1) {
            rows = db.update(TABLE_FAVORITES, values, FAV_COL_ID + "=?", new String[]{String.valueOf(model.getId())});
        } else {
            rows = db.update(TABLE_FAVORITES,
                             values,
                             FAV_COL_QUERY + "=?" +
                                     " AND " + FAV_COL_TYPE + "=?",
                             new String[]{model.getQuery(), model.getType().toString()});
        }
        if (rows != 1) {
            db.insertOrThrow(TABLE_FAVORITES, null, values);
        }
    }

    public final synchronized void deleteFavorite(@NonNull final String query, @NonNull final FavoriteType type) {
        if (!TextUtils.isEmpty(query)) {
            try (final SQLiteDatabase db = getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final int rowsDeleted = db.delete(TABLE_FAVORITES,
                                                      FAV_COL_QUERY + "=?" +
                                                              " AND " + FAV_COL_TYPE + "=?",
                                                      new String[]{query, type.toString()});

                    if (rowsDeleted > 0) db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (logCollector != null) {
                        logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "deleteFavorite");
                    }
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error", e);
                    }
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    @NonNull
    public final List<FavoriteModel> getAllFavorites() {
        final List<FavoriteModel> favorites = new ArrayList<>();
        final SQLiteDatabase db = getWritableDatabase();
        try (final Cursor cursor = db.rawQuery("SELECT "
                                                       + FAV_COL_ID + ","
                                                       + FAV_COL_QUERY + ","
                                                       + FAV_COL_TYPE + ","
                                                       + FAV_COL_DISPLAY_NAME + ","
                                                       + FAV_COL_PIC_URL + ","
                                                       + FAV_COL_DATE_ADDED
                                                       + " FROM " + TABLE_FAVORITES,
                                               null)) {
            if (cursor != null && cursor.moveToFirst()) {
                db.beginTransaction();
                FavoriteModel tempFav;
                do {
                    FavoriteType type = null;
                    try {
                        type = FavoriteType.valueOf(cursor.getString(cursor.getColumnIndex(FAV_COL_TYPE)));
                    } catch (IllegalArgumentException ignored) {}
                    tempFav = new FavoriteModel(
                            cursor.getInt(cursor.getColumnIndex(FAV_COL_ID)),
                            cursor.getString(cursor.getColumnIndex(FAV_COL_QUERY)),
                            type,
                            cursor.getString(cursor.getColumnIndex(FAV_COL_DISPLAY_NAME)),
                            cursor.getString(cursor.getColumnIndex(FAV_COL_PIC_URL)),
                            new Date(cursor.getLong(cursor.getColumnIndex(FAV_COL_DATE_ADDED)))
                    );
                    favorites.add(tempFav);
                } while (cursor.moveToNext());
                db.endTransaction();
            }
        } catch (final Exception e) {
            Log.e(TAG, "", e);
        }
        return favorites;
    }

    @Nullable
    public final FavoriteModel getFavorite(@NonNull final String query, @NonNull final FavoriteType type) {
        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery("SELECT "
                                                       + FAV_COL_ID + ","
                                                       + FAV_COL_QUERY + ","
                                                       + FAV_COL_TYPE + ","
                                                       + FAV_COL_DISPLAY_NAME + ","
                                                       + FAV_COL_PIC_URL + ","
                                                       + FAV_COL_DATE_ADDED
                                                       + " FROM " + TABLE_FAVORITES
                                                       + " WHERE " + FAV_COL_QUERY + "='" + query + "'"
                                                       + " AND " + FAV_COL_TYPE + "='" + type.toString() + "'",
                                               null)) {
            if (cursor != null && cursor.moveToFirst()) {
                FavoriteType favoriteType = null;
                try {
                    favoriteType = FavoriteType.valueOf(cursor.getString(cursor.getColumnIndex(FAV_COL_TYPE)));
                } catch (IllegalArgumentException ignored) {}
                return new FavoriteModel(
                        cursor.getInt(cursor.getColumnIndex(FAV_COL_ID)),
                        cursor.getString(cursor.getColumnIndex(FAV_COL_QUERY)),
                        favoriteType,
                        cursor.getString(cursor.getColumnIndex(FAV_COL_DISPLAY_NAME)),
                        cursor.getString(cursor.getColumnIndex(FAV_COL_PIC_URL)),
                        new Date(cursor.getLong(cursor.getColumnIndex(FAV_COL_DATE_ADDED)))
                );
            }
        }
        return null;
    }

    public final void addOrUpdateUser(@NonNull final DataBox.CookieModel cookieModel) {
        addOrUpdateUser(
                cookieModel.getUid(),
                cookieModel.getUsername(),
                cookieModel.getCookie(),
                cookieModel.getFullName(),
                cookieModel.getProfilePic()
        );
    }

    public final void addOrUpdateUser(final String uid,
                                      final String username,
                                      final String cookie,
                                      final String fullName,
                                      final String profilePicUrl) {
        if (TextUtils.isEmpty(uid)) return;
        try (final SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                final ContentValues values = new ContentValues();
                values.put(KEY_USERNAME, username);
                values.put(KEY_COOKIE, cookie);
                values.put(KEY_UID, uid);
                values.put(KEY_FULL_NAME, fullName);
                values.put(KEY_PROFILE_PIC, profilePicUrl);

                final int rows = db.update(TABLE_COOKIES, values, KEY_UID + "=?", new String[]{uid});

                if (rows != 1)
                    db.insertOrThrow(TABLE_COOKIES, null, values);

                db.setTransactionSuccessful();
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    public final synchronized void delUserCookie(@NonNull final CookieModel cookieModel) {
        final String cookieModelUid = cookieModel.getUid();
        if (!TextUtils.isEmpty(cookieModelUid)) {
            try (final SQLiteDatabase db = getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final int rowsDeleted = db.delete(TABLE_COOKIES, KEY_UID + "=? AND " + KEY_USERNAME + "=? AND " + KEY_COOKIE + "=?",
                                                      new String[]{cookieModelUid, cookieModel.getUsername(), cookieModel.getCookie()});

                    if (rowsDeleted > 0) db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    public final synchronized void deleteAllUserCookies() {
        try (final SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                final int rowsDeleted = db.delete(TABLE_COOKIES, null, null);

                if (rowsDeleted > 0) db.setTransactionSuccessful();
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    @Nullable
    public final CookieModel getCookie(final String uid) {
        CookieModel cookie = null;
        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery(
                     "SELECT "
                             + KEY_UID + ","
                             + KEY_USERNAME + ","
                             + KEY_COOKIE + ","
                             + KEY_FULL_NAME + ","
                             + KEY_PROFILE_PIC
                             + " FROM " + TABLE_COOKIES
                             + " WHERE " + KEY_UID + " = ?",
                     new String[]{uid})
        ) {
            if (cursor != null && cursor.moveToFirst())
                cookie = new CookieModel(
                        cursor.getString(cursor.getColumnIndex(KEY_UID)),
                        cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),
                        cursor.getString(cursor.getColumnIndex(KEY_COOKIE)),
                        cursor.getString(cursor.getColumnIndex(KEY_FULL_NAME)),
                        cursor.getString(cursor.getColumnIndex(KEY_PROFILE_PIC))
                );
        }
        return cookie;
    }

    @NonNull
    public final List<CookieModel> getAllCookies() {
        final List<CookieModel> cookies = new ArrayList<>();
        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery(
                     "SELECT "
                             + KEY_UID + ","
                             + KEY_USERNAME + ","
                             + KEY_COOKIE + ","
                             + KEY_FULL_NAME + ","
                             + KEY_PROFILE_PIC
                             + " FROM " + TABLE_COOKIES, null)
        ) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    cookies.add(new CookieModel(
                            cursor.getString(cursor.getColumnIndex(KEY_UID)),
                            cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),
                            cursor.getString(cursor.getColumnIndex(KEY_COOKIE)),
                            cursor.getString(cursor.getColumnIndex(KEY_FULL_NAME)),
                            cursor.getString(cursor.getColumnIndex(KEY_PROFILE_PIC))
                    ));
                } while (cursor.moveToNext());
            }
        }
        return cookies;
    }

    public static class CookieModel {
        private final String uid;
        private final String username;
        private final String cookie;
        private final String fullName;
        private final String profilePic;
        private boolean selected;

        public CookieModel(final String uid,
                           final String username,
                           final String cookie,
                           final String fullName,
                           final String profilePic) {
            this.uid = uid;
            this.username = username;
            this.cookie = cookie;
            this.fullName = fullName;
            this.profilePic = profilePic;
        }

        public String getUid() {
            return uid;
        }

        public String getUsername() {
            return username;
        }

        public String getCookie() {
            return cookie;
        }

        public String getFullName() {
            return fullName;
        }

        public String getProfilePic() {
            return profilePic;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(final boolean selected) {
            this.selected = selected;
        }

        public boolean isValid() {
            return !TextUtils.isEmpty(uid)
                    && !TextUtils.isEmpty(username)
                    && !TextUtils.isEmpty(cookie);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CookieModel that = (CookieModel) o;
            return ObjectsCompat.equals(uid, that.uid) &&
                    ObjectsCompat.equals(username, that.username) &&
                    ObjectsCompat.equals(cookie, that.cookie);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(uid, username, cookie);
        }

        @NonNull
        @Override
        public String toString() {
            return "CookieModel{" +
                    "uid='" + uid + '\'' +
                    ", username='" + username + '\'' +
                    ", cookie='" + cookie + '\'' +
                    ", fullName='" + fullName + '\'' +
                    ", profilePic='" + profilePic + '\'' +
                    ", selected=" + selected +
                    '}';
        }
    }

    public static class FavoriteModel {
        private final int id;
        private final String query;
        private final FavoriteType type;
        private final String displayName;
        private final String picUrl;
        private final Date dateAdded;

        public FavoriteModel(final int id,
                             final String query,
                             final FavoriteType type,
                             final String displayName,
                             final String picUrl,
                             final Date dateAdded) {
            this.id = id;
            this.query = query;
            this.type = type;
            this.displayName = displayName;
            this.picUrl = picUrl;
            this.dateAdded = dateAdded;
        }

        public int getId() {
            return id;
        }

        public String getQuery() {
            return query;
        }

        public FavoriteType getType() {
            return type;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPicUrl() {
            return picUrl;
        }

        public Date getDateAdded() {
            return dateAdded;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final FavoriteModel that = (FavoriteModel) o;
            return id == that.id &&
                    ObjectsCompat.equals(query, that.query) &&
                    type == that.type &&
                    ObjectsCompat.equals(displayName, that.displayName) &&
                    ObjectsCompat.equals(picUrl, that.picUrl) &&
                    ObjectsCompat.equals(dateAdded, that.dateAdded);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(id, query, type, displayName, picUrl, dateAdded);
        }

        @NonNull
        @Override
        public String toString() {
            return "FavoriteModel{" +
                    "id=" + id +
                    ", query='" + query + '\'' +
                    ", type=" + type +
                    ", displayName='" + displayName + '\'' +
                    ", picUrl='" + picUrl + '\'' +
                    ", dateAdded=" + dateAdded +
                    '}';
        }
    }
}