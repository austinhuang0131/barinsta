package awais.instagrabber.db.dao

import androidx.room.*
import awais.instagrabber.db.entities.Account

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT * FROM accounts WHERE uid = :uid")
    suspend fun findAccountByUid(uid: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(vararg accounts: Account)

    @Update
    suspend fun updateAccounts(vararg accounts: Account)

    @Delete
    suspend fun deleteAccounts(vararg accounts: Account)

    @Query("DELETE from accounts")
    suspend fun deleteAllAccounts()
}