@file:JvmName("UpdateCheckCommon")

package awais.instagrabber.utils

import android.content.Context
import android.content.DialogInterface
import awais.instagrabber.BuildConfig
import awais.instagrabber.R
import awais.instagrabber.utils.AppExecutors.mainThread
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun shouldShowUpdateDialog(
    force: Boolean,
    version: String
): Boolean {
    val skippedVersion = Utils.settingsHelper.getString(Constants.SKIPPED_VERSION)
    return force || !BuildConfig.DEBUG && skippedVersion != version
}

fun showUpdateDialog(
    context: Context,
    version: String,
    onDownloadClickListener: DialogInterface.OnClickListener
) {
    mainThread.execute {
        MaterialAlertDialogBuilder(context).apply {
            setTitle(context.getString(R.string.update_available, version))
            setNeutralButton(R.string.skip_update) { dialog: DialogInterface, which: Int ->
                Utils.settingsHelper.putString(Constants.SKIPPED_VERSION, version)
                dialog.dismiss()
            }
            setPositiveButton(R.string.action_download, onDownloadClickListener)
            setNegativeButton(R.string.cancel, null)
        }.show()
    }
}