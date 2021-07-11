package awais.instagrabber.db.dao

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import awais.instagrabber.db.AppDatabase
import awais.instagrabber.db.entities.RecentSearch
import awais.instagrabber.models.enums.FavoriteType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class RecentSearchDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: RecentSearchDao

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.recentSearchDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun writeQueryDelete() = runBlockingTest {
        val recentSearch = insertRecentSearch(1, "1", "test1", FavoriteType.HASHTAG)
        val byIgIdAndType = dao.getRecentSearchByIgIdAndType("1", FavoriteType.HASHTAG)
        Assertions.assertNotNull(byIgIdAndType)
        Assertions.assertEquals(recentSearch, byIgIdAndType)
        dao.deleteRecentSearch(byIgIdAndType ?: throw NullPointerException())
        val deleted = dao.getRecentSearchByIgIdAndType("1", FavoriteType.HASHTAG)
        Assertions.assertNull(deleted)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun queryAllOrdered() = runBlockingTest {
        val insertListReversed: List<RecentSearch> = listOf(
            insertRecentSearch(1, "1", "test1", FavoriteType.HASHTAG),
            insertRecentSearch(2, "2", "test2", FavoriteType.LOCATION),
            insertRecentSearch(3, "3", "test3", FavoriteType.USER),
            insertRecentSearch(4, "4", "test4", FavoriteType.USER),
            insertRecentSearch(5, "5", "test5", FavoriteType.USER)
        ).asReversed() // important
        val fromDb: List<RecentSearch?> = dao.getAllRecentSearches()
        Assertions.assertIterableEquals(insertListReversed, fromDb)
    }

    private fun insertRecentSearch(id: Int, igId: String, name: String, type: FavoriteType): RecentSearch {
        val recentSearch = RecentSearch(
            id,
            igId,
            name,
            null,
            null,
            type,
            LocalDateTime.now()
        )
        runBlocking { dao.insertRecentSearch(recentSearch) }
        return recentSearch
    }
}