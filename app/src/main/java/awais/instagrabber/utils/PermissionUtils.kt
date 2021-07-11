package awais.instagrabber.utils

import android.Manifest.permission
import android.content.Context
import androidx.core.content.PermissionChecker
import awais.instagrabber.utils.PermissionUtils
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

object PermissionUtils {
    val AUDIO_RECORD_PERMS = arrayOf(permission.RECORD_AUDIO)
    val ATTACH_MEDIA_PERMS = arrayOf(permission.READ_EXTERNAL_STORAGE)
    val CAMERA_PERMS = arrayOf(permission.CAMERA)
    @JvmStatic
    fun hasAudioRecordPerms(context: Context): Boolean {
        return PermissionChecker.checkSelfPermission(
            context,
            permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    @JvmStatic
    fun requestAudioRecordPerms(fragment: Fragment, requestCode: Int) {
        fragment.requestPermissions(AUDIO_RECORD_PERMS, requestCode)
    }

    @JvmStatic
    fun hasAttachMediaPerms(context: Context): Boolean {
        return PermissionChecker.checkSelfPermission(
            context,
            permission.READ_EXTERNAL_STORAGE
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    @JvmStatic
    fun requestAttachMediaPerms(fragment: Fragment, requestCode: Int) {
        fragment.requestPermissions(ATTACH_MEDIA_PERMS, requestCode)
    }

    fun hasCameraPerms(context: Context?): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPerms(activity: AppCompatActivity?, requestCode: Int) {
        ActivityCompat.requestPermissions(activity!!, CAMERA_PERMS, requestCode)
    }
}