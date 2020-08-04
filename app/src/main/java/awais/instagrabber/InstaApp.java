package awais.instagrabber;

import android.content.ClipboardManager;
import android.content.Context;

import androidx.core.app.NotificationManagerCompat;
import androidx.multidex.MultiDexApplication;

import java.net.CookieHandler;
import java.text.SimpleDateFormat;
import java.util.UUID;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.SettingsHelper;
import awaisomereport.CrashReporter;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.NET_COOKIE_MANAGER;
import static awais.instagrabber.utils.Utils.changeTheme;
import static awais.instagrabber.utils.Utils.clipboardManager;
import static awais.instagrabber.utils.Utils.dataBox;
import static awais.instagrabber.utils.Utils.datetimeParser;
import static awais.instagrabber.utils.Utils.getInstalledTelegramPackage;
import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.notificationManager;
import static awais.instagrabber.utils.Utils.settingsHelper;
import static awais.instagrabber.utils.Utils.telegramPackage;

public final class InstaApp extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) CrashReporter.get(this).start();
        logCollector = new LogCollector(this);

        CookieHandler.setDefault(NET_COOKIE_MANAGER);

        final Context appContext = getApplicationContext();

        telegramPackage = getInstalledTelegramPackage(appContext);

        if (dataBox == null)
            dataBox = DataBox.getInstance(appContext);

        if (settingsHelper == null)
            settingsHelper = new SettingsHelper(this);

        LocaleUtils.setLocale(getBaseContext());

        if (notificationManager == null)
            notificationManager = NotificationManagerCompat.from(appContext);

        if (clipboardManager == null)
            clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (datetimeParser == null)
            datetimeParser = new SimpleDateFormat(
                    settingsHelper.getBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED) ?
                            settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT) :
                            settingsHelper.getString(Constants.DATE_TIME_FORMAT), LocaleUtils.getCurrentLocale());

        settingsHelper.putString(Constants.DEVICE_UUID, UUID.randomUUID().toString());

        changeTheme();
    }
}