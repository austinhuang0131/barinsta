package awais.instagrabber.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.utils.Utils.settingsHelper

class BarinstaBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput?) {
        super.onFullBackup(if (settingsHelper.getBoolean(PreferenceKeys.PREF_AUTO_BACKUP_ENABLED)) data else null)
    }

    // no key-value backups
    override fun onBackup(oldState: ParcelFileDescriptor?,
                          data: BackupDataOutput?, newState: ParcelFileDescriptor?) {}

    override fun onRestore(data: BackupDataInput, appVersionCode: Int,
                           newState: ParcelFileDescriptor) {}
}