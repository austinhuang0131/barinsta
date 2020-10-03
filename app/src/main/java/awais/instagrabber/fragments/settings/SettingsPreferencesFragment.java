package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.TimeSettingsDialog;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DirectoryChooser;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class SettingsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = "SettingsPrefsFrag";
    private static AppCompatTextView customPathTextView;
    private boolean isLoggedIn;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != null;
        final Context context = getContext();
        if (context == null) return;
        final PreferenceCategory generalCategory = new PreferenceCategory(context);
        screen.addPreference(generalCategory);
        generalCategory.setTitle(R.string.pref_category_general);
        generalCategory.setIconSpaceReserved(false);
        generalCategory.addPreference(getThemePreference(context));
        generalCategory.addPreference(getDefaultTabPreference());
        generalCategory.addPreference(getUpdateCheckPreference());
        generalCategory.addPreference(getAutoPlayVideosPreference());
        generalCategory.addPreference(getAlwaysMuteVideosPreference());

        // screen.addPreference(getDivider(context));
        // final PreferenceCategory themeCategory = new PreferenceCategory(context);
        // screen.addPreference(themeCategory);
        // themeCategory.setTitle(R.string.pref_category_theme);
        // themeCategory.setIconSpaceReserved(false);
        // themeCategory.addPreference(getAmoledThemePreference());

        final PreferenceCategory downloadsCategory = new PreferenceCategory(context);
        screen.addPreference(downloadsCategory);
        downloadsCategory.setTitle(R.string.pref_category_downloads);
        downloadsCategory.setIconSpaceReserved(false);
        downloadsCategory.addPreference(getDownloadUserFolderPreference());
        downloadsCategory.addPreference(getSaveToCustomFolderPreference());

        final PreferenceCategory localeCategory = new PreferenceCategory(context);
        screen.addPreference(localeCategory);
        localeCategory.setTitle(R.string.pref_category_locale);
        localeCategory.setIconSpaceReserved(false);
        localeCategory.addPreference(getLanguagePreference());
        localeCategory.addPreference(getPostTimePreference());

        if (isLoggedIn) {
            final PreferenceCategory loggedInUsersPreferenceCategory = new PreferenceCategory(context);
            screen.addPreference(loggedInUsersPreferenceCategory);
            loggedInUsersPreferenceCategory.setIconSpaceReserved(false);
            loggedInUsersPreferenceCategory.setTitle(R.string.login_settings);
            loggedInUsersPreferenceCategory.addPreference(getMarkStoriesSeenPreference());
            loggedInUsersPreferenceCategory.addPreference(getMarkDMSeenPreference());
            loggedInUsersPreferenceCategory.addPreference(getEnableActivityNotificationsPreference());
        } else {
            final PreferenceCategory anonUsersPreferenceCategory = new PreferenceCategory(context);
            screen.addPreference(anonUsersPreferenceCategory);
            anonUsersPreferenceCategory.setIconSpaceReserved(false);
            anonUsersPreferenceCategory.setTitle(R.string.anonymous_settings);
            anonUsersPreferenceCategory.addPreference(getUseInstaDpPreference());
        }

    }

    private Preference getLanguagePreference() {
        final Context context = getContext();
        if (context == null) return null;
        final ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final int length = getResources().getStringArray(R.array.languages).length;
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(Constants.APP_LANGUAGE);
        preference.setTitle(R.string.select_language);
        preference.setDialogTitle(R.string.select_language);
        preference.setEntries(R.array.languages);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            return true;
        });
        return preference;
    }

    private Preference getDefaultTabPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final ListPreference preference = new ListPreference(context);
        preference.setEnabled(isLoggedIn);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final TypedArray mainNavIds = getResources().obtainTypedArray(R.array.main_nav_ids);
        final int length = mainNavIds.length();
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            final int resourceId = mainNavIds.getResourceId(i, -1);
            if (resourceId < 0) continue;
            values[i] = String.valueOf(resourceId);
        }
        mainNavIds.recycle();
        preference.setKey(Constants.DEFAULT_TAB);
        preference.setTitle(R.string.pref_start_screen);
        preference.setDialogTitle(R.string.pref_start_screen);
        preference.setEntries(R.array.main_nav_ids_values);
        preference.setEntryValues(values);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getUpdateCheckPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.CHECK_UPDATES);
        preference.setTitle(R.string.update_check);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getThemePreference(@NonNull final Context context) {
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.pref_category_theme);
        // preference.setIcon(R.drawable.ic_format_paint_24);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            final NavDirections navDirections = SettingsPreferencesFragmentDirections.actionSettingsPreferencesFragmentToThemePreferencesFragment();
            NavHostFragment.findNavController(this).navigate(navDirections);
            return true;
        });
        return preference;
    }

    private Preference getDownloadUserFolderPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DOWNLOAD_USER_FOLDER);
        preference.setTitle(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getSaveToCustomFolderPreference() {
        final Context context = getContext();
        if (context == null) return null;
        return new SaveToCustomFolderPreference(context, (resultCallback) -> new DirectoryChooser()
                .setInitialDirectory(settingsHelper.getString(FOLDER_PATH))
                .setInteractionListener(file -> {
                    settingsHelper.putString(FOLDER_PATH, file.getAbsolutePath());
                    resultCallback.onResult(file.getAbsolutePath());
                })
                .show(getParentFragmentManager(), null));
    }

    private Preference getAutoPlayVideosPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.AUTOPLAY_VIDEOS);
        preference.setTitle(R.string.post_viewer_autoplay_video);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getAlwaysMuteVideosPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.MUTED_VIDEOS);
        preference.setTitle(R.string.post_viewer_muted_autoplay);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getMarkStoriesSeenPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.MARK_AS_SEEN);
        preference.setTitle(R.string.mark_as_seen_setting);
        preference.setSummary(R.string.mark_as_seen_setting_summary);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getMarkDMSeenPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DM_MARK_AS_SEEN);
        preference.setTitle(R.string.dm_mark_as_seen_setting);
        preference.setSummary(R.string.dm_mark_as_seen_setting_summary);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getEnableActivityNotificationsPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.CHECK_ACTIVITY);
        preference.setTitle(R.string.activity_setting);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            return true;
        });
        return preference;
    }

    private Preference getUseInstaDpPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.INSTADP);
        preference.setTitle(R.string.instadp_settings);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getPostTimePreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.time_settings);
        preference.setSummary(Utils.datetimeParser.format(new Date()));
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            new TimeSettingsDialog(
                    settingsHelper.getBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED),
                    settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT),
                    settingsHelper.getString(Constants.DATE_TIME_SELECTION),
                    settingsHelper.getBoolean(Constants.SWAP_DATE_TIME_FORMAT_ENABLED),
                    (isCustomFormat,
                     formatSelection,
                     spTimeFormatSelectedItemPosition,
                     spSeparatorSelectedItemPosition,
                     spDateFormatSelectedItemPosition,
                     selectedFormat,
                     currentFormat,
                     swapDateTime) -> {
                        if (isCustomFormat) {
                            settingsHelper.putString(Constants.CUSTOM_DATE_TIME_FORMAT, formatSelection);
                        } else {
                            final String formatSelectionUpdated = spTimeFormatSelectedItemPosition + ";"
                                    + spSeparatorSelectedItemPosition + ';'
                                    + spDateFormatSelectedItemPosition; // time;separator;date
                            settingsHelper.putString(Constants.DATE_TIME_FORMAT, selectedFormat);
                            settingsHelper.putString(Constants.DATE_TIME_SELECTION, formatSelectionUpdated);
                        }
                        settingsHelper.putBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED, isCustomFormat);
                        settingsHelper.putBoolean(Constants.SWAP_DATE_TIME_FORMAT_ENABLED, swapDateTime);
                        Utils.datetimeParser = (SimpleDateFormat) currentFormat.clone();
                        preference.setSummary(Utils.datetimeParser.format(new Date()));
                    }
            ).show(getParentFragmentManager(), null);
            return true;
        });
        return preference;
    }

    public static class SaveToCustomFolderPreference extends Preference {

        private final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener;
        private String key;

        public SaveToCustomFolderPreference(final Context context, final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener) {
            super(context);
            this.onSelectFolderButtonClickListener = onSelectFolderButtonClickListener;
            key = Constants.FOLDER_SAVE_TO;
            setLayoutResource(R.layout.pref_custom_folder);
            setKey(key);
            setTitle(R.string.save_to_folder);
            setIconSpaceReserved(false);
        }

        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            final SwitchMaterial cbSaveTo = (SwitchMaterial) holder.findViewById(R.id.cbSaveTo);
            final View buttonContainer = holder.findViewById(R.id.button_container);
            customPathTextView = (AppCompatTextView) holder.findViewById(R.id.custom_path);
            cbSaveTo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsHelper.putBoolean(FOLDER_SAVE_TO, isChecked);
                buttonContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                final String customPath = settingsHelper.getString(FOLDER_PATH);
                customPathTextView.setText(customPath);
            });
            final boolean savedToEnabled = settingsHelper.getBoolean(key);
            holder.itemView.setOnClickListener(v -> cbSaveTo.toggle());
            cbSaveTo.setChecked(savedToEnabled);
            buttonContainer.setVisibility(savedToEnabled ? View.VISIBLE : View.GONE);
            final AppCompatButton btnSaveTo = (AppCompatButton) holder.findViewById(R.id.btnSaveTo);
            btnSaveTo.setOnClickListener(v -> {
                if (onSelectFolderButtonClickListener == null) return;
                onSelectFolderButtonClickListener.onClick(result -> {
                    if (TextUtils.isEmpty(result)) return;
                    customPathTextView.setText(result);
                });
            });
        }

        public interface ResultCallback {
            void onResult(String result);
        }

        public interface OnSelectFolderButtonClickListener {
            void onClick(ResultCallback resultCallback);
        }
    }
}
