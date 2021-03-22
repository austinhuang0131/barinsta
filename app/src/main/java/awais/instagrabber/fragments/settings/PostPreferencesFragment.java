package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.KeywordsFilterDialog;
import awais.instagrabber.utils.Constants;

public class PostPreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        // generalCategory.addPreference(getAutoPlayVideosPreference(context));
        screen.addPreference(getAlwaysMuteVideosPreference(context));
        screen.addPreference(getShowCaptionPreference(context));
        screen.addPreference(getToggleKeywordFilterPreference(context));
        screen.addPreference(getEditKeywordFilterPreference(context));
    }

    private Preference getAutoPlayVideosPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.AUTOPLAY_VIDEOS);
        preference.setTitle(R.string.post_viewer_autoplay_video);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getAlwaysMuteVideosPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.MUTED_VIDEOS);
        preference.setTitle(R.string.post_viewer_muted_autoplay);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getShowCaptionPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.SHOW_CAPTIONS);
        preference.setDefaultValue(true);
        preference.setTitle(R.string.post_viewer_show_captions);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getToggleKeywordFilterPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.TOGGLE_KEYWORD_FILTER);
        preference.setDefaultValue(false);
        preference.setTitle(R.string.toggle_keyword_filter);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getEditKeywordFilterPreference(@NonNull final Context context){
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.edit_keyword_filter);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(view ->{
            new KeywordsFilterDialog().show(getParentFragmentManager(), null);
            return true;
        });
        return preference;
    }
}
