package awais.instagrabber.db.datasources

import android.content.Context
import awais.instagrabber.db.AppDatabase
import awais.instagrabber.db.dao.AccountDao
import awais.instagrabber.db.entities.Account

class AccountDataSource(private val accountDao: AccountDao) {
    suspend fun getAccount(uid: String): Account? = accountDao.findAccountByUid(uid)

    suspend fun getAllAccounts(): List<Account> = accountDao.getAllAccounts()

    suspend fun insertOrUpdateAccount(
        uid: String?,
        username: String?,
        cookie: String?,
        fullName: String?,
        profilePicUrl: String?,
    ) {
        val account = uid?.let { getAccount(it) }
        val toUpdate = Account(account?.id ?: 0, uid, username, cookie, fullName, profilePicUrl)
        if (account != null) {
            accountDao.updateAccounts(toUpdate)
            return
        }
        accountDao.insertAccounts(toUpdate)
    }

    suspend fun deleteAccount(account: Account) = accountDao.deleteAccounts(account)

    suspend fun deleteAllAccounts() = accountDao.deleteAllAccounts()

    companion object {
        @Volatile
        private var INSTANCE: AccountDataSource? = null

        fun getInstance(context: Context): AccountDataSource {
            return INSTANCE ?: synchronized(this) {
                val dao: AccountDao = AppDatabase.getDatabase(context).accountDao()
                AccountDataSource(dao).also { INSTANCE = it }
            }
        }
    }
}