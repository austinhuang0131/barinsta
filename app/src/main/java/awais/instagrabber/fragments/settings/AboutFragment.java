package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;

public class AboutFragment extends BasePreferencesFragment {
    private static AppCompatTextView customPathTextView;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        final PreferenceCategory generalCategory = new PreferenceCategory(context);
        screen.addPreference(generalCategory);
        generalCategory.setTitle(R.string.pref_category_general);
        generalCategory.setIconSpaceReserved(false);
        generalCategory.addPreference(getDocsPreference());
        generalCategory.addPreference(getRepoPreference());
        generalCategory.addPreference(getFeedbackPreference());

        final PreferenceCategory thirdPartyCategory = new PreferenceCategory(context);
        screen.addPreference(thirdPartyCategory);
        thirdPartyCategory.setTitle(R.string.about_category_3pt);
        //thirdPartyCategory.setSummary(R.string.about_category_3pt_summary);
        thirdPartyCategory.setIconSpaceReserved(false);
        // alphabetical order!!!
        thirdPartyCategory.addPreference(getExoPlayerPreference());
        thirdPartyCategory.addPreference(getFrescoPreference());
        thirdPartyCategory.addPreference(getJsoupPreference());
        thirdPartyCategory.addPreference(getMDIPreference());
        thirdPartyCategory.addPreference(getRetrofitPreference());

        final PreferenceCategory licenseCategory = new PreferenceCategory(context);
        screen.addPreference(licenseCategory);
        licenseCategory.setTitle(R.string.about_category_license);
        licenseCategory.setIconSpaceReserved(false);
        licenseCategory.addPreference(getLicensePreference());
        licenseCategory.addPreference(getLiabilityPreference());
    }

    private Preference getDocsPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.about_documentation);
        preference.setSummary(R.string.about_documentation_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://instagrabber.austinhuang.me"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getRepoPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.about_repository);
        preference.setSummary(R.string.about_repository_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/austinhuang0131/instagrabber"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getFeedbackPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.about_feedback);
        preference.setSummary(R.string.about_feedback_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse(getString(R.string.about_feedback_summary)));
            if (intent.resolveActivity(context.getPackageManager()) != null) startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getRetrofitPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle("Retrofit");
        preference.setSummary("Copyright 2013 Square, Inc. Apache Version 2.0.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://square.github.io/retrofit/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getJsoupPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle("jsoup");
        preference.setSummary("Copyright (c) 2009-2020 Jonathan Hedley. MIT License.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://jsoup.org/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getFrescoPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle("Fresco");
        preference.setSummary("Copyright (c) Facebook, Inc. and its affiliates. MIT License.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://frescolib.org/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getExoPlayerPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle("ExoPlayer");
        preference.setSummary("Copyright (C) 2016 The Android Open Source Project. Apache Version 2.0.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://exoplayer.dev/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getMDIPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setTitle("Material Design Icons");
        preference.setSummary("Copyright (C) 2014 Austin Andrews & Google LLC. Apache Version 2.0.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://materialdesignicons.com/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getLicensePreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setSummary(R.string.license);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_outline_info_24);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getLiabilityPreference() {
        final Context context = getContext();
        if (context == null) return null;
        final Preference preference = new Preference(context);
        preference.setSummary(R.string.liability);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_warning);
        preference.setIconSpaceReserved(true);
        return preference;
    }
}
