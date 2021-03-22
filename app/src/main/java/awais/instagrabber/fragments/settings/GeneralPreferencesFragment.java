package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class GeneralPreferencesFragment extends BasePreferencesFragment {

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (isLoggedIn) {
            screen.addPreference(getDefaultTabPreference(context));
        }
        screen.addPreference(getUpdateCheckPreference(context));
        screen.addPreference(getFlagSecurePreference(context));
    }

    private Preference getDefaultTabPreference(@NonNull final Context context) {
        final ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final TypedArray mainNavIds = getResources().obtainTypedArray(R.array.main_nav_ids);
        final int length = mainNavIds.length();
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            final int resourceId = mainNavIds.getResourceId(i, -1);
            if (resourceId < 0) continue;
            values[i] = getResources().getResourceEntryName(resourceId);
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

    private Preference getUpdateCheckPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.CHECK_UPDATES);
        preference.setTitle(R.string.update_check);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getFlagSecurePreference(@NonNull final Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                Constants.FLAG_SECURE,
                R.string.flag_secure,
                -1,
                false,
                (preference, newValue) -> {
                    shouldRecreate();
                    return true;
                });
    }
}
