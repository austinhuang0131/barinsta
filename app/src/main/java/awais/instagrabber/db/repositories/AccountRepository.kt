package awais.instagrabber.db.repositories

import android.content.Context
import awais.instagrabber.db.datasources.AccountDataSource
import awais.instagrabber.db.entities.Account

class AccountRepository(private val accountDataSource: AccountDataSource) {
    suspend fun getAccount(uid: Long): Account? = accountDataSource.getAccount(uid.toString())

    suspend fun getAllAccounts(): List<Account> = accountDataSource.getAllAccounts()

    suspend fun insertOrUpdateAccounts(accounts: List<Account>) {
        for (account in accounts) {
            accountDataSource.insertOrUpdateAccount(
                account.uid,
                account.username,
                account.cookie,
                account.fullName,
                account.profilePic
            )
        }
    }

    suspend fun insertOrUpdateAccount(
        uid: Long,
        username: String,
        cookie: String,
        fullName: String,
        profilePicUrl: String?,
    ): Account? {
        accountDataSource.insertOrUpdateAccount(uid.toString(), username, cookie, fullName, profilePicUrl)
        return accountDataSource.getAccount(uid.toString())
    }

    suspend fun deleteAccount(account: Account) = accountDataSource.deleteAccount(account)

    suspend fun deleteAllAccounts() = accountDataSource.deleteAllAccounts()

    companion object {
        @Volatile
        private var INSTANCE: AccountRepository? = null

        fun getInstance(context: Context): AccountRepository {
            return INSTANCE ?: synchronized(this) {
                val dataSource: AccountDataSource = AccountDataSource.getInstance(context)
                AccountRepository(dataSource).also { INSTANCE = it }
            }
        }
    }
}