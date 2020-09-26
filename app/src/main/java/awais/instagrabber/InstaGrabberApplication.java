package awais.instagrabber;

import android.app.Application;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import java.net.CookieHandler;
import java.text.SimpleDateFormat;
import java.util.UUID;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.SettingsHelper;
import awaisomereport.CrashReporter;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.CookieUtils.NET_COOKIE_MANAGER;
import static awais.instagrabber.utils.Utils.clipboardManager;
import static awais.instagrabber.utils.Utils.dataBox;
import static awais.instagrabber.utils.Utils.datetimeParser;
import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class InstaGrabberApplication extends Application {
    private static final String TAG = "InstaGrabberApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        final ImagePipelineConfig imagePipelineConfig = ImagePipelineConfig
                .newBuilder(this)
                // .setMainDiskCacheConfig(diskCacheConfig)
                .setDownsampleEnabled(true)
                .build();
        Fresco.initialize(this, imagePipelineConfig);

        if (BuildConfig.DEBUG) {
            try {
                Class.forName("dalvik.system.CloseGuard")
                     .getMethod("setEnabled", boolean.class)
                     .invoke(null, true);
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        }

        if (!BuildConfig.DEBUG) CrashReporter.get(this).start();
        logCollector = new LogCollector(this);

        CookieHandler.setDefault(NET_COOKIE_MANAGER);

        final Context appContext = getApplicationContext();

        if (dataBox == null)
            dataBox = DataBox.getInstance(appContext);

        if (settingsHelper == null)
            settingsHelper = new SettingsHelper(this);

        LocaleUtils.setLocale(getBaseContext());

        if (clipboardManager == null)
            clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (datetimeParser == null)
            datetimeParser = new SimpleDateFormat(
                    settingsHelper.getBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED) ?
                    settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT) :
                    settingsHelper.getString(Constants.DATE_TIME_FORMAT), LocaleUtils.getCurrentLocale());

        settingsHelper.putString(Constants.DEVICE_UUID, UUID.randomUUID().toString());
    }
}