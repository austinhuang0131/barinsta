package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;

public class MainNavigationBarPreferenceFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;

        // The original idea was to be able to see them using images.
        // Maybe in a future version ?

        screen.addPreference(getMainNavBar(context));
    }


    private Preference getMainNavBar(@NonNull final Context context) {
        final ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final int length = getResources().getStringArray(R.array.main_navbar_presets).length;
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(Constants.APP_MAIN_NAVBAR);
        preference.setTitle(R.string.main_navbar_settings);
        preference.setDialogTitle(R.string.main_navbar_settings);
        preference.setEntries(R.array.main_navbar_presets);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setDefaultValue(values[0]);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            return true;
        });
        return preference;
    }
}
