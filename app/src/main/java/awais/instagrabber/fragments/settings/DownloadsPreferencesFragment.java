package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;
import static awais.instagrabber.activities.DirectorySelectActivity.SELECT_DIR_REQUEST_CODE;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class DownloadsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = DownloadsPreferencesFragment.class.getSimpleName();
    private Preference dirPreference;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        screen.addPreference(getDownloadUserFolderPreference(context));
        screen.addPreference(getSaveToCustomFolderPreference(context));
        screen.addPreference(getPrependUsernameToFilenamePreference(context));
    }

    private Preference getDownloadUserFolderPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.DOWNLOAD_USER_FOLDER);
        preference.setTitle(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getSaveToCustomFolderPreference(@NonNull final Context context) {
        dirPreference = new Preference(context);
        dirPreference.setIconSpaceReserved(false);
        dirPreference.setTitle(R.string.barinsta_folder);
        final String currentValue = settingsHelper.getString(PreferenceKeys.PREF_BARINSTA_DIR_URI);
        if (TextUtils.isEmpty(currentValue)) dirPreference.setSummary("");
        else {
            String path;
            try {
                path = URLDecoder.decode(currentValue, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                path = currentValue;
            }
            dirPreference.setSummary(path);
        }
        dirPreference.setOnPreferenceClickListener(p -> {
            openDirectoryChooser(DownloadUtils.getRootDirUri());
            return true;
        });
        return dirPreference;
    }

    private void openDirectoryChooser(final Uri initialUri) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (requestCode != SELECT_DIR_REQUEST_CODE) return;
        if (resultCode != RESULT_OK) return;
        if (data == null || data.getData() == null) return;
        final Context context = getContext();
        if (context == null) return;
        AppExecutors.INSTANCE.getMainThread().execute(() -> {
            try {
                Utils.setupSelectedDir(context, data);
                String path;
                try {
                    path = URLDecoder.decode(data.getData().toString(), StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    path = data.getData().toString();
                }
                dirPreference.setSummary(path);
            } catch (Exception e) {
                // Should not come to this point.
                // If it does, we have to show this error to the user so that they can report it.
                try (final StringWriter sw = new StringWriter();
                     final PrintWriter pw = new PrintWriter(sw)) {
                    e.printStackTrace(pw);
                    final ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                            123,
                            R.string.error,
                            "com.android.externalstorage.documents".equals(data.getData().getAuthority())
                                ? "Please report this error to the developers:\n\n" + sw.toString()
                                : getString(R.string.dir_select_no_download_folder, data.getData().getAuthority()),
                            R.string.ok,
                            0,
                            0
                    );
                    dialogFragment.show(getChildFragmentManager(), ConfirmDialogFragment.class.getSimpleName());
                } catch (IOException ioException) {
                    Log.e(TAG, "onActivityResult: ", ioException);
                }
            }
        }, 500);
    }

    private Preference getPrependUsernameToFilenamePreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME);
        preference.setTitle(R.string.download_prepend_username);
        preference.setIconSpaceReserved(false);
        return preference;
    }
}
