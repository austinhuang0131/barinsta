package awais.instagrabber.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.activities.Login;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DirectoryChooser;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.CrashReporter;

import static awais.instagrabber.utils.Constants.APP_LANGUAGE;
import static awais.instagrabber.utils.Constants.APP_THEME;
import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
import static awais.instagrabber.utils.Constants.AUTOPLAY_VIDEOS;
import static awais.instagrabber.utils.Constants.BOTTOM_TOOLBAR;
import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Constants.MARK_AS_SEEN;
import static awais.instagrabber.utils.Constants.MUTED_VIDEOS;
import static awais.instagrabber.utils.Constants.SHOW_FEED;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class SettingsDialog extends BottomSheetDialogFragment implements View.OnClickListener, AdapterView.OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {
    private Activity activity;
    private FragmentManager fragmentManager;
    private View btnSaveTo, btnImportExport, btnLogin, btnLogout, btnTimeSettings, btnReport;
    private AppCompatTextView settingTitle;
    private Spinner spAppTheme, spLanguage;
    private boolean somethingChanged = false;
    private int currentTheme, currentLanguage, selectedLanguage;

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (requestCode != 6200) return;
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) showDirectoryChooser();
        else Toast.makeText(activity, R.string.direct_download_perms_ask, Toast.LENGTH_SHORT).show();
    }

    private void showDirectoryChooser() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) fragmentManager = getChildFragmentManager();

        new DirectoryChooser().setInitialDirectory(settingsHelper.getString(FOLDER_PATH))
                .setInteractionListener(path -> {
                    settingsHelper.putString(FOLDER_PATH, path);
                    somethingChanged = true;
                }).show(fragmentManager, null);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);

        final Context context = getContext();
        activity = context instanceof Activity ? (Activity) context : getActivity();

        fragmentManager = getFragmentManager();
        if (fragmentManager == null) fragmentManager = getChildFragmentManager();

        final View contentView = View.inflate(activity, R.layout.dialog_main_settings, null);

        settingTitle = contentView.findViewById(R.id.settingTitle);
        settingTitle.setText(getString(R.string.action_setting, BuildConfig.VERSION_NAME));
        btnLogin = contentView.findViewById(R.id.btnLogin);
        btnLogout = contentView.findViewById(R.id.btnLogout);
        btnSaveTo = contentView.findViewById(R.id.btnSaveTo);
        btnImportExport = contentView.findViewById(R.id.importExport);
        btnTimeSettings = contentView.findViewById(R.id.btnTimeSettings);
        btnReport = contentView.findViewById(R.id.btnReport);

        Utils.setTooltipText(btnImportExport, R.string.import_export);

        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnReport.setOnClickListener(this);
        btnSaveTo.setOnClickListener(this);
        btnImportExport.setOnClickListener(this);
        btnTimeSettings.setOnClickListener(this);

        if (Utils.isEmpty(settingsHelper.getString(Constants.COOKIE))) btnLogout.setEnabled(false);

        spAppTheme = contentView.findViewById(R.id.spAppTheme);
        currentTheme = settingsHelper.getInteger(APP_THEME);
        spAppTheme.setSelection(currentTheme);
        spAppTheme.setOnItemSelectedListener(this);

        spLanguage = contentView.findViewById(R.id.spLanguage);
        currentLanguage = settingsHelper.getInteger(APP_LANGUAGE);
        spLanguage.setSelection(currentLanguage);
        spLanguage.setOnItemSelectedListener(this);

        final AppCompatCheckBox cbSaveTo = contentView.findViewById(R.id.cbSaveTo);
        final AppCompatCheckBox cbMuteVideos = contentView.findViewById(R.id.cbMuteVideos);
        final AppCompatCheckBox cbBottomToolbar = contentView.findViewById(R.id.cbBottomToolbar);
        final AppCompatCheckBox cbAutoloadPosts = contentView.findViewById(R.id.cbAutoloadPosts);
        final AppCompatCheckBox cbAutoplayVideos = contentView.findViewById(R.id.cbAutoplayVideos);
        final AppCompatCheckBox cbDownloadUsername = contentView.findViewById(R.id.cbDownloadUsername);
        final AppCompatCheckBox cbMarkAsSeen = contentView.findViewById(R.id.cbMarkAsSeen);

        cbSaveTo.setChecked(settingsHelper.getBoolean(FOLDER_SAVE_TO));
        cbMuteVideos.setChecked(settingsHelper.getBoolean(MUTED_VIDEOS));
        cbBottomToolbar.setChecked(settingsHelper.getBoolean(BOTTOM_TOOLBAR));
        cbAutoplayVideos.setChecked(settingsHelper.getBoolean(AUTOPLAY_VIDEOS));
        cbMarkAsSeen.setChecked(settingsHelper.getBoolean(MARK_AS_SEEN));

        cbAutoloadPosts.setChecked(settingsHelper.getBoolean(AUTOLOAD_POSTS));
        cbDownloadUsername.setChecked(settingsHelper.getBoolean(DOWNLOAD_USER_FOLDER));

        setupListener(cbSaveTo);
        setupListener(cbMuteVideos);
        setupListener(cbBottomToolbar);
        setupListener(cbAutoloadPosts);
        setupListener(cbAutoplayVideos);
        setupListener(cbDownloadUsername);
        setupListener(cbMarkAsSeen);

        btnSaveTo.setEnabled(cbSaveTo.isChecked());

        dialog.setContentView(contentView);

        return dialog;
    }

    private void setupListener(@NonNull final AppCompatCheckBox checkBox) {
        checkBox.setOnCheckedChangeListener(this);
        ((View) checkBox.getParent()).setOnClickListener(this);
    }

    @Override
    public void onItemSelected(final AdapterView<?> spinner, final View view, final int position, final long id) {
        if (spinner == spAppTheme) {
            if (position != currentTheme) {
                settingsHelper.putInteger(APP_THEME, position);
                somethingChanged = true;
            }
        } else if (spinner == spLanguage) {
            selectedLanguage = position;
            if (position != currentLanguage) {
                settingsHelper.putInteger(APP_LANGUAGE, position);
                somethingChanged = true;
            }
        }
    }

    @Override
    public void onClick(final View v) {
        if (v == btnLogin) {
            startActivity(new Intent(v.getContext(), Login.class));
            somethingChanged = true;
        } else if (v == btnLogout) {
            Utils.setupCookies("LOGOUT");
            settingsHelper.putString(Constants.COOKIE, null);
            this.dismiss();
            activity.recreate();
        } else if (v == btnImportExport) {
            if (ContextCompat.checkSelfPermission(activity, Utils.PERMS[0]) == PackageManager.PERMISSION_DENIED)
                requestPermissions(Utils.PERMS, 6007);
            else Utils.showImportExportDialog(activity);

        } else if (v == btnTimeSettings) {
            new TimeSettingsDialog().show(fragmentManager, null);

        } else if (v == btnReport) {
            CrashReporter.get(activity.getApplication()).zipLogs().startCrashEmailIntent(activity, true);

        } else if (v == btnSaveTo) {
            if (ContextCompat.checkSelfPermission(activity, Utils.PERMS[0]) == PackageManager.PERMISSION_DENIED)
                requestPermissions(Utils.PERMS, 6200);
            else showDirectoryChooser();

        } else if (v instanceof ViewGroup)
            ((ViewGroup) v).getChildAt(0).performClick();
    }

    @Override
    public void onCheckedChanged(@NonNull final CompoundButton checkBox, final boolean checked) {
        final int id = checkBox.getId();
        if (id == R.id.cbDownloadUsername) settingsHelper.putBoolean(DOWNLOAD_USER_FOLDER, checked);
        else if (id == R.id.cbBottomToolbar) settingsHelper.putBoolean(BOTTOM_TOOLBAR, checked);
        else if (id == R.id.cbAutoplayVideos) settingsHelper.putBoolean(AUTOPLAY_VIDEOS, checked);
        else if (id == R.id.cbMuteVideos) settingsHelper.putBoolean(MUTED_VIDEOS, checked);
        else if (id == R.id.cbAutoloadPosts) settingsHelper.putBoolean(AUTOLOAD_POSTS, checked);
        else if (id == R.id.cbMarkAsSeen) settingsHelper.putBoolean(MARK_AS_SEEN, checked);
        else if (id == R.id.cbSaveTo) {
            settingsHelper.putBoolean(FOLDER_SAVE_TO, checked);
            btnSaveTo.setEnabled(checked);
        }
        somethingChanged = true;
    }

    @Override
    public void onDismiss(@NonNull final DialogInterface dialog) {
        if (selectedLanguage != currentLanguage)
            LocaleUtils.setLocale(activity != null ? activity.getBaseContext() : getLayoutInflater().getContext().getApplicationContext());
        super.onDismiss(dialog);
        if (somethingChanged && activity != null) activity.recreate();
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) { }
}