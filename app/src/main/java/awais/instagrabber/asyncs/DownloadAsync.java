package awais.instagrabber.asyncs;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.activities.ProfilePicViewer;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.CHANNEL_ID;
import static awais.instagrabber.utils.Utils.CHANNEL_NAME;
import static awais.instagrabber.utils.Utils.NOTIF_GROUP_NAME;
import static awais.instagrabber.utils.Utils.isChannelCreated;
import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.notificationManager;
import static awaisomereport.LogCollector.LogFile;

public final class DownloadAsync extends AsyncTask<Void, Float, File> {
    private static int lastNotifId = 1;
    private final int currentNotifId;
    private final AtomicReference<Context> context;
    private final File outFile;
    private final String url;
    private final FetchListener<File> fetchListener;
    private final Resources resources;
    private final NotificationCompat.Builder downloadNotif;
    private String shortCode, username;

    public DownloadAsync(final Context context, final String url, final File outFile, final FetchListener<File> fetchListener) {
        this.context = new AtomicReference<>(context);
        this.resources = context.getResources();
        this.url = url;
        this.outFile = outFile;
        this.fetchListener = fetchListener;
        this.shortCode = this.username = resources.getString(R.string.downloader_started);
        this.currentNotifId = ++lastNotifId;
        if (++lastNotifId + 1 == Integer.MAX_VALUE) lastNotifId = 1;

        if (notificationManager == null)
            notificationManager = NotificationManagerCompat.from(context.getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isChannelCreated) {
            notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH));
            isChannelCreated = true;
        }

        @StringRes final int titleRes = context instanceof ProfilePicViewer ? R.string.downloader_downloading_pfp : R.string.downloader_downloading_post;

        downloadNotif = new NotificationCompat.Builder(context, CHANNEL_ID).setCategory(NotificationCompat.CATEGORY_STATUS)
                .setSmallIcon(R.mipmap.ic_launcher).setContentText(shortCode == null ? username : shortCode).setOngoing(true)
                .setProgress(100, 0, false).setAutoCancel(false).setOnlyAlertOnce(true)
                .setContentTitle(resources.getString(titleRes));

        notificationManager.notify(currentNotifId, downloadNotif.build());
    }

    public DownloadAsync setItems(final String shortCode, final String username) {
        this.shortCode = shortCode;
        this.username = username;
        if (downloadNotif != null) downloadNotif.setContentText(this.shortCode == null ? this.username : this.shortCode);
        return this;
    }

    @Nullable
    @Override
    protected File doInBackground(final Void... voids) {
        try {
            final URLConnection urlConnection = new URL(url).openConnection();
            final long fileSize = Build.VERSION.SDK_INT >= 24 ? urlConnection.getContentLengthLong() :
                    urlConnection.getContentLength();
            float totalRead = 0;

            try (final BufferedInputStream bis = new BufferedInputStream(urlConnection.getInputStream());
                 final FileOutputStream fos = new FileOutputStream(outFile)) {
                final byte[] buffer = new byte[0x2000];

                int count;
                boolean deletedIPTC = false;
                while ((count = bis.read(buffer, 0, 0x2000)) != -1) {
                    totalRead = totalRead + count;

                    if (!deletedIPTC) {
                        int iptcStart = -1;
                        int fbmdStart = -1;
                        int fbmdBytesLen = -1;

                        for (int i = 0; i < buffer.length; ++i) {
                            if (buffer[i] == (byte) 0xFF && buffer[i + 1] == (byte) 0xED)
                                iptcStart = i;
                            else if (buffer[i] == (byte) 'F' && buffer[i + 1] == (byte) 'B'
                                    && buffer[i + 2] == (byte) 'M' && buffer[i + 3] == (byte) 'D') {
                                fbmdStart = i;
                                fbmdBytesLen = buffer[i - 10] << 24 | (buffer[i - 9] & 0xFF) << 16 |
                                        (buffer[i - 8] & 0xFF) << 8 | (buffer[i - 7] & 0xFF) |
                                        (buffer[i - 6] & 0xFF);
                                break;
                            }
                        }

                        if (iptcStart != -1 && fbmdStart != -1 && fbmdBytesLen != -1) {
                            final int fbmdDataLen = (iptcStart + (fbmdStart - iptcStart) + (fbmdBytesLen - iptcStart)) - 4;

                            fos.write(buffer, 0, iptcStart);
                            fos.write(buffer, fbmdDataLen + iptcStart, count - fbmdDataLen - iptcStart);

                            publishProgress(totalRead * 100f / fileSize);

                            deletedIPTC = true;
                            continue;
                        }
                    }

                    fos.write(buffer, 0, count);
                    publishProgress(totalRead * 100f / fileSize);
                }
                fos.flush();
            }

            return outFile;
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogFile.ASYNC_DOWNLOADER, "doInBackground",
                        new Pair<>("context", context.get()),
                        new Pair<>("resources", resources),
                        new Pair<>("lastNotifId", lastNotifId),
                        new Pair<>("downloadNotif", downloadNotif),
                        new Pair<>("currentNotifId", currentNotifId),
                        new Pair<>("notificationManager", notificationManager));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onProgressUpdate(@NonNull final Float... values) {
        if (downloadNotif != null) {
            downloadNotif.setProgress(100, values[0].intValue(), false);
            notificationManager.notify(currentNotifId, downloadNotif.build());
        }
    }

    @Override
    protected void onPostExecute(final File result) {
        if (result != null) {
            final Context context = this.context.get();

            context.sendBroadcast(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ?
                    new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(Environment.getExternalStorageDirectory())) :
                    new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(result.getAbsoluteFile()))
            );
            MediaScannerConnection.scanFile(context, new String[]{result.getAbsolutePath()}, null, null);

            if (notificationManager != null) {
                final Uri uri = FileProvider.getUriForFile(context, "me.austinhuang.instagrabber.provider", result);

                final ContentResolver contentResolver = context.getContentResolver();
                Bitmap bitmap = null;
                if (Utils.isImage(uri, contentResolver)) {
                    try {
                        bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
                    } catch (final Exception e) {
                        if (logCollector != null)
                            logCollector.appendException(e, LogFile.ASYNC_DOWNLOADER, "onPostExecute::bitmap_1");
                        if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                    }
                }

                if (bitmap == null) {
                    final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        try {
                            retriever.setDataSource(context, uri);
                        } catch (final Exception e) {
                            retriever.setDataSource(result.getAbsolutePath());
                        }
                        bitmap = retriever.getFrameAtTime();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            try {
                                retriever.close();
                            } catch (final Exception e) {
                                if (logCollector != null)
                                    logCollector.appendException(e, LogFile.ASYNC_DOWNLOADER, "onPostExecute::bitmap_2");
                            }
                    } catch (final Exception e) {
                        if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                        if (logCollector != null)
                            logCollector.appendException(e, LogFile.ASYNC_DOWNLOADER, "onPostExecute::bitmap_3");
                    }
                }

                final String downloadComplete = resources.getString(R.string.downloader_complete);

                downloadNotif.setContentText(null).setContentTitle(downloadComplete).setProgress(0, 0, false)
                        .setWhen(System.currentTimeMillis()).setOngoing(false).setOnlyAlertOnce(false).setAutoCancel(true)
                        .setGroup(NOTIF_GROUP_NAME).setGroupSummary(true).setContentIntent(
                        PendingIntent.getActivity(context, 2020, new Intent(Intent.ACTION_VIEW, uri)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                .putExtra(Intent.EXTRA_STREAM, uri), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT));

                if (bitmap != null)
                    downloadNotif.setStyle(new NotificationCompat.BigPictureStyle().setBigContentTitle(downloadComplete).bigPicture(bitmap))
                            .setLargeIcon(bitmap).setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);

                notificationManager.cancel(currentNotifId);
                notificationManager.notify(currentNotifId + 1, downloadNotif.build());
            }
        }

        if (fetchListener != null) fetchListener.onResult(result);
    }
}