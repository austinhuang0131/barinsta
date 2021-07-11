package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.CreateBackupDialogFragment;
import awais.instagrabber.dialogs.RestoreBackupDialogFragment;

public class BackupPreferencesFragment extends BasePreferencesFragment {

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            final PreferenceCategory autoCategory = new PreferenceCategory(context);
            screen.addPreference(autoCategory);
            autoCategory.setTitle(R.string.auto_backup);
            autoCategory.addPreference(getAboutPreference(context, true));
            autoCategory.addPreference(getWarningPreference(context, true));
            autoCategory.addPreference(getAutoBackupPreference(context));
        }
        final PreferenceCategory manualCategory = new PreferenceCategory(context);
        screen.addPreference(manualCategory);
        manualCategory.setTitle(R.string.manual_backup);
        manualCategory.addPreference(getAboutPreference(context, false));
        manualCategory.addPreference(getWarningPreference(context, false));
        manualCategory.addPreference(getCreatePreference(context));
        manualCategory.addPreference(getRestorePreference(context));
    }

    private Preference getAboutPreference(@NonNull final Context context,
                                          @NonNull final boolean auto) {
        final Preference preference = new Preference(context);
        preference.setSummary(auto ? R.string.auto_backup_summary : R.string.backup_summary);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_outline_info_24);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getWarningPreference(@NonNull final Context context,
                                            @NonNull final boolean auto) {
        final Preference preference = new Preference(context);
        preference.setSummary(auto ? R.string.auto_backup_warning : R.string.backup_warning);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_warning);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getAutoBackupPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.PREF_AUTO_BACKUP_ENABLED);
        preference.setTitle(R.string.auto_backup_setting);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getCreatePreference(@NonNull final Context context) {
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.create_backup);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            final FragmentManager fragmentManager = getParentFragmentManager();
            final CreateBackupDialogFragment fragment = new CreateBackupDialogFragment(result -> {
                final View view = getView();
                if (view != null) {
                    Snackbar.make(view,
                                  result ? R.string.dialog_export_success
                                         : R.string.dialog_export_failed,
                                  BaseTransientBottomBar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setAction(R.string.ok, v -> {})
                            .show();
                    return;
                }
                Toast.makeText(context,
                               result ? R.string.dialog_export_success
                                      : R.string.dialog_export_failed,
                               Toast.LENGTH_LONG)
                     .show();
            });
            final FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
              .add(fragment, "createBackup")
              .commit();
            return true;
        });
        return preference;
    }

    private Preference getRestorePreference(@NonNull final Context context) {
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.restore_backup);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            final FragmentManager fragmentManager = getParentFragmentManager();
            final RestoreBackupDialogFragment fragment = new RestoreBackupDialogFragment(result -> {
                final View view = getView();
                if (view != null) {
                    Snackbar.make(view,
                                  result ? R.string.dialog_import_success
                                         : R.string.dialog_import_failed,
                                  BaseTransientBottomBar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setAction(R.string.ok, v -> {})
                            .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                @Override
                                public void onDismissed(final Snackbar transientBottomBar, final int event) {
                                    recreateActivity(result);
                                }
                            })
                            .show();
                    return;
                }
                recreateActivity(result);
            });
            final FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
              .add(fragment, "restoreBackup")
              .commit();
            return true;
        });
        return preference;
    }

    private void recreateActivity(final boolean result) {
        if (!result) return;
        final FragmentActivity activity = getActivity();
        if (activity == null) return;
        activity.recreate();
    }
}
