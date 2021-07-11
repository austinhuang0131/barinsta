package awais.instagrabber.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import awais.instagrabber.db.dao.AccountDao
import awais.instagrabber.db.dao.DMLastNotifiedDao
import awais.instagrabber.db.dao.FavoriteDao
import awais.instagrabber.db.dao.RecentSearchDao
import awais.instagrabber.db.entities.Account
import awais.instagrabber.db.entities.DMLastNotified
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.entities.RecentSearch
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Database(entities = [Account::class, Favorite::class, DMLastNotified::class, RecentSearch::class], version = 6)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun dmLastNotifiedDao(): DMLastNotifiedDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        private lateinit var INSTANCE: AppDatabase

        fun getDatabase(context: Context): AppDatabase {
            if (!this::INSTANCE.isInitialized) {
                synchronized(AppDatabase::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cookiebox.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .build()
                    }
                }
            }
            return INSTANCE
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cookies ADD " + Account.COL_FULL_NAME + " TEXT")
                db.execSQL("ALTER TABLE cookies ADD " + Account.COL_PROFILE_PIC + " TEXT")
            }
        }
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val oldFavorites = backupOldFavorites(db)
                // recreate with new columns (as there will be no doubt about the `query_display` column being present or not in the future versions)
                db.execSQL("DROP TABLE " + Favorite.TABLE_NAME)
                db.execSQL("CREATE TABLE " + Favorite.TABLE_NAME + " ("
                           + Favorite.COL_ID + " INTEGER PRIMARY KEY,"
                           + Favorite.COL_QUERY + " TEXT,"
                           + Favorite.COL_TYPE + " TEXT,"
                           + Favorite.COL_DISPLAY_NAME + " TEXT,"
                           + Favorite.COL_PIC_URL + " TEXT,"
                           + Favorite.COL_DATE_ADDED + " INTEGER)")
                // add the old favorites back
                for (oldFavorite in oldFavorites) {
                    insertOrUpdateFavorite(db, oldFavorite)
                }
            }
        }
        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Required when migrating to Room.
                // The original table primary keys were not 'NOT NULL', so the migration to Room were failing without the below migration.
                // Taking this opportunity to rename cookies table to accounts

                // Create new table with name 'accounts'
                db.execSQL("CREATE TABLE " + Account.TABLE_NAME + " ("
                           + Account.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                           + Account.COL_UID + " TEXT,"
                           + Account.COL_USERNAME + " TEXT,"
                           + Account.COL_COOKIE + " TEXT,"
                           + Account.COL_FULL_NAME + " TEXT,"
                           + Account.COL_PROFILE_PIC + " TEXT)")
                // Insert all data from table 'cookies' to 'accounts'
                db.execSQL("INSERT INTO " + Account.TABLE_NAME + " ("
                           + Account.COL_UID + ","
                           + Account.COL_USERNAME + ","
                           + Account.COL_COOKIE + ","
                           + Account.COL_FULL_NAME + ","
                           + Account.COL_PROFILE_PIC + ") "
                           + "SELECT "
                           + Account.COL_UID + ","
                           + Account.COL_USERNAME + ","
                           + Account.COL_COOKIE + ","
                           + Account.COL_FULL_NAME + ","
                           + Account.COL_PROFILE_PIC
                           + " FROM cookies")
                // Drop old cookies table
                db.execSQL("DROP TABLE cookies")

                // Create favorite backup table
                db.execSQL("CREATE TABLE " + Favorite.TABLE_NAME + "_backup ("
                           + Favorite.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                           + Favorite.COL_QUERY + " TEXT,"
                           + Favorite.COL_TYPE + " TEXT,"
                           + Favorite.COL_DISPLAY_NAME + " TEXT,"
                           + Favorite.COL_PIC_URL + " TEXT,"
                           + Favorite.COL_DATE_ADDED + " INTEGER)")
                // Insert all data from table 'favorite' to 'favorite_backup'
                db.execSQL("INSERT INTO " + Favorite.TABLE_NAME + "_backup ("
                           + Favorite.COL_QUERY + ","
                           + Favorite.COL_TYPE + ","
                           + Favorite.COL_DISPLAY_NAME + ","
                           + Favorite.COL_PIC_URL + ","
                           + Favorite.COL_DATE_ADDED + ") "
                           + "SELECT "
                           + Favorite.COL_QUERY + ","
                           + Favorite.COL_TYPE + ","
                           + Favorite.COL_DISPLAY_NAME + ","
                           + Favorite.COL_PIC_URL + ","
                           + Favorite.COL_DATE_ADDED
                           + " FROM " + Favorite.TABLE_NAME)
                // Drop favorites
                db.execSQL("DROP TABLE " + Favorite.TABLE_NAME)
                // Rename favorite_backup to favorites
                db.execSQL("ALTER TABLE " + Favorite.TABLE_NAME + "_backup RENAME TO " + Favorite.TABLE_NAME)
            }
        }

        @JvmField
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `dm_last_notified` (" +
                                 "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                 "`thread_id` TEXT, " +
                                 "`last_notified_msg_ts` INTEGER, " +
                                 "`last_notified_at` INTEGER)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dm_last_notified_thread_id` ON `dm_last_notified` (`thread_id`)")
            }
        }

        @JvmField
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `recent_searches` (" +
                                 "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                 "`ig_id` TEXT NOT NULL, " +
                                 "`name` TEXT NOT NULL, " +
                                 "`username` TEXT, " +
                                 "`pic_url` TEXT, " +
                                 "`type` TEXT NOT NULL, " +
                                 "`last_searched_on` INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recent_searches_ig_id_type` ON `recent_searches` (`ig_id`, `type`)")
            }
        }

        private fun backupOldFavorites(db: SupportSQLiteDatabase): List<Favorite> {
            // check if old favorites table had the column query_display
            val queryDisplayExists = checkColumnExists(db, Favorite.TABLE_NAME, "query_display")
            Log.d(TAG, "backupOldFavorites: queryDisplayExists: $queryDisplayExists")
            val oldModels: MutableList<Favorite> = ArrayList()
            val sql = ("SELECT "
                       + "query_text,"
                       + "date_added"
                       + (if (queryDisplayExists) ",query_display" else "")
                       + " FROM " + Favorite.TABLE_NAME)
            try {
                db.query(sql).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            try {
                                val queryText = cursor.getString(cursor.getColumnIndex("query_text"))
                                val favoriteTypeQueryPair = Utils.migrateOldFavQuery(queryText) ?: continue
                                val type = favoriteTypeQueryPair.first
                                val query = favoriteTypeQueryPair.second
                                val epochMillis = cursor.getLong(cursor.getColumnIndex("date_added"))
                                val localDateTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(epochMillis),
                                    ZoneId.systemDefault()
                                )
                                oldModels.add(Favorite(
                                    0,
                                    query,
                                    type,
                                    if (queryDisplayExists) cursor.getString(cursor.getColumnIndex("query_display")) else null,
                                    null,
                                    localDateTime
                                ))
                            } catch (e: Exception) {
                                Log.e(TAG, "onUpgrade", e)
                            }
                        } while (cursor.moveToNext())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "onUpgrade", e)
            }
            Log.d(TAG, "backupOldFavorites: oldModels:$oldModels")
            return oldModels
        }

        @Synchronized
        private fun insertOrUpdateFavorite(db: SupportSQLiteDatabase, model: Favorite) {
            val values = ContentValues()
            values.put(Favorite.COL_QUERY, model.query)
            values.put(Favorite.COL_TYPE, model.type.toString())
            values.put(Favorite.COL_DISPLAY_NAME, model.displayName)
            values.put(Favorite.COL_PIC_URL, model.picUrl)
            values.put(Favorite.COL_DATE_ADDED, model.dateAdded!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            val rows: Int = if (model.id >= 1) {
                db.update(Favorite.TABLE_NAME,
                    SQLiteDatabase.CONFLICT_IGNORE,
                    values,
                    Favorite.COL_ID + "=?", arrayOf(model.id.toString()))
            } else {
                db.update(Favorite.TABLE_NAME,
                    SQLiteDatabase.CONFLICT_IGNORE,
                    values,
                    Favorite.COL_QUERY + "=?" + " AND " + Favorite.COL_TYPE + "=?", arrayOf(model.query, model.type.toString()))
            }
            if (rows != 1) {
                db.insert(Favorite.TABLE_NAME, SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }

        @Suppress("SameParameterValue")
        private fun checkColumnExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
        ): Boolean {
            var exists = false
            try {
                db.query("PRAGMA table_info($tableName)").use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val currentColumn = cursor.getString(cursor.getColumnIndex("name"))
                            if (currentColumn == columnName) {
                                exists = true
                            }
                        } while (cursor.moveToNext())
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "checkColumnExists", ex)
            }
            return exists
        }
    }
}