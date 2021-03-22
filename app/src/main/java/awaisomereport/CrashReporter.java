package awaisomereport;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipOutputStream;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.utils.Utils;

public final class CrashReporter implements Thread.UncaughtExceptionHandler {
    private static CrashReporter reporterInstance;
    private final Application application;
    private final String email;
//    private final File crashLogsZip;
    private boolean startAttempted = false;

    public static CrashReporter get(final Application application) {
        if (reporterInstance == null) reporterInstance = new CrashReporter(application);
        return reporterInstance;
    }

    private CrashReporter(@NonNull final Application application) {
        this.application = application;
        this.email = "barinsta@austinhuang.me";
//        this.crashLogsZip = new File(application.getExternalCacheDir(), "crash_logs.zip");
    }

    public void start() {
        if (!startAttempted) {
            Thread.setDefaultUncaughtExceptionHandler(this);
            startAttempted = true;
        }
    }

    @Override
    public void uncaughtException(@NonNull final Thread t, @NonNull final Throwable exception) {
        final StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("IMPORTANT: If sending by email, your email address and the entire content will be made public on GitHub issues.");
        reportBuilder.append("\r\nIMPORTANT: When possible, please describe the steps leading to this crash. Thank you for your cooperation.");
        reportBuilder.append("\r\n\r\nError report collected on: ").append(new Date().toString());

        reportBuilder
                .append("\r\n\r\nInformation:\r\n==============")
                .append("\r\nVERSION		: ").append(BuildConfig.VERSION_NAME)
                .append("\r\nVERSION_CODE	: ").append(BuildConfig.VERSION_CODE)
                .append("\r\nPHONE-MODEL	: ").append(Build.MODEL)
                .append("\r\nANDROID_VERS	: ").append(Build.VERSION.RELEASE)
                .append("\r\nANDROID_REL	: ").append(Build.VERSION.SDK_INT)
                .append("\r\nBRAND			: ").append(Build.BRAND)
                .append("\r\nMANUFACTURER	: ").append(Build.MANUFACTURER)
                .append("\r\nBOARD			: ").append(Build.BOARD)
                .append("\r\nDEVICE			: ").append(Build.DEVICE)
                .append("\r\nPRODUCT		: ").append(Build.PRODUCT)
                .append("\r\nHOST			: ").append(Build.HOST)
                .append("\r\nTAGS			: ").append(Build.TAGS);

        reportBuilder.append("\r\n\r\nStack:\r\n==============\r\n");
        final Writer result = new StringWriter();
        try (final PrintWriter printWriter = new PrintWriter(result)) {
            exception.printStackTrace(printWriter);
            reportBuilder.append(result.toString());

            reportBuilder.append("\r\nCause:\r\n==============");

            // for AsyncTask crashes
            Throwable cause = exception.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                reportBuilder.append(result.toString());
                cause = cause.getCause();
            }
        }
        reportBuilder.append("\r\n\r\n**** End of current Report ***");

        final String errorContent = reportBuilder.toString();
        try (final FileOutputStream trace = application.openFileOutput("stack-" + System.currentTimeMillis() + ".stacktrace", Context.MODE_PRIVATE)) {
            trace.write(errorContent.getBytes());
        } catch (final Exception ex) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", ex);
        }

        application.startActivity(new Intent(application, ErrorReporterActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

//        zipLogs();

        Process.killProcess(Process.myPid());
        System.exit(10);
    }

//    public synchronized CrashReporter zipLogs() {
//        final File logDir = Utils.logCollector != null ? Utils.logCollector.getLogDir() :
//                new File(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? application.getDataDir() : application.getFilesDir(), "crashlogs");
//
//        try (final FileOutputStream fos = new FileOutputStream(crashLogsZip);
//             final ZipOutputStream zos = new ZipOutputStream(fos)) {
//
//            final File[] files = logDir.listFiles();
//
//            if (files != null) {
//                zos.setLevel(5);
//                byte[] buffer;
//                for (final File file : files) {
//                    if (file != null && file.length() > 0) {
//                        buffer = new byte[1024];
//                        try (final FileInputStream fis = new FileInputStream(file)) {
//                            zos.putNextEntry(new ZipEntry(file.getName()));
//                            int length;
//                            while ((length = fis.read(buffer)) > 0) zos.write(buffer, 0, length);
//                            zos.closeEntry();
//                        }
//                    }
//                }
//            }
//
//        } catch (final Exception e) {
//            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
//        }
//
//        return this;
//    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void startCrashEmailIntent(final Context context) {
        try {
            final String filePath = context.getFilesDir().getAbsolutePath();

            String[] errorFileList;

            try {
                final File dir = new File(filePath);
                if (dir.exists() && !dir.isDirectory()) dir.delete();
                dir.mkdir();
                errorFileList = dir.list((d, name) -> name.endsWith(".stacktrace"));
            } catch (final Exception e) {
                errorFileList = null;
            }

            if (errorFileList != null && errorFileList.length > 0) {
                final StringBuilder errorStringBuilder;

                errorStringBuilder = new StringBuilder("\r\n\r\n");
                final int maxSendMail = 5;

                int curIndex = 0;
                for (final String curString : errorFileList) {
                    final File file = new File(filePath + '/' + curString);

                    if (curIndex++ <= maxSendMail) {
                        errorStringBuilder.append("New Trace collected:\r\n=====================\r\n");
                        try (final BufferedReader input = new BufferedReader(new FileReader(file))) {
                            String line;
                            while ((line = input.readLine()) != null)
                                errorStringBuilder.append(line).append("\r\n");
                        }
                    }

                    file.delete();
                }

                errorStringBuilder.append("\r\n\r\n");

                context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("message/rfc822")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{email})
//                        .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(application, BuildConfig.APPLICATION_ID + ".provider", crashLogsZip))
                        .putExtra(Intent.EXTRA_SUBJECT, "Barinsta Crash Report")
                        .putExtra(Intent.EXTRA_TEXT, errorStringBuilder.toString()), "Select an email app to send crash logs"));
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
    }
}
