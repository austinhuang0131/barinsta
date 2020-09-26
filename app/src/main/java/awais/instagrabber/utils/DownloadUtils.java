package awais.instagrabber.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.regex.Pattern;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.MediaItemType;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;

public final class DownloadUtils {
    public static final String[] PERMS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static void batchDownload(@NonNull final Context context,
                                     @Nullable String username,
                                     final DownloadMethod method,
                                     final List<? extends BasePostModel> itemsToDownload) {
        if (Utils.settingsHelper == null) Utils.settingsHelper = new SettingsHelper(context);

        if (itemsToDownload == null || itemsToDownload.size() < 1) return;

        if (username != null && username.charAt(0) == '@') username = username.substring(1);

        if (ContextCompat.checkSelfPermission(context, PERMS[0]) == PackageManager.PERMISSION_GRANTED)
            batchDownloadImpl(context, username, method, itemsToDownload);
        else if (context instanceof Activity)
            ActivityCompat.requestPermissions((Activity) context, PERMS, 8020);
    }

    private static void batchDownloadImpl(@NonNull final Context context,
                                          @Nullable final String username,
                                          final DownloadMethod method,
                                          final List<? extends BasePostModel> itemsToDownload) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");

        if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
            final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
            if (!TextUtils.isEmpty(customPath)) dir = new File(customPath);
        }

        if (Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !TextUtils.isEmpty(username))
            dir = new File(dir, username);

        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean checkEachPost = false;
        switch (method) {
            case DOWNLOAD_SAVED:
            case DOWNLOAD_MAIN:
                checkEachPost = true;
                break;
            case DOWNLOAD_FEED:
                checkEachPost = false;
                break;
        }
        final int itemsToDownloadSize = itemsToDownload.size();
        for (int i = 0; i < itemsToDownloadSize; i++) {
            final BasePostModel selectedItem = itemsToDownload.get(i);
            if (!checkEachPost) {
                final boolean isSlider = itemsToDownloadSize > 1;
                final File saveFile = getDownloadSaveFile(dir, selectedItem, isSlider ? "_slide_" + (i + 1) : "");
                new DownloadAsync(context,
                                  selectedItem.getDisplayUrl(),
                                  saveFile,
                                  file -> selectedItem.setDownloaded(true))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                final File finalDir = dir;
                new PostFetcher(selectedItem.getShortCode(), result -> {
                    if (result != null) {
                        final int resultsSize = result.length;
                        final boolean multiResult = resultsSize > 1;
                        for (int j = 0; j < resultsSize; j++) {
                            final BasePostModel model = result[j];
                            final File saveFile = getDownloadSaveFile(finalDir, model, multiResult ? "_slide_" + (j + 1) : "");
                            new DownloadAsync(context,
                                              model.getDisplayUrl(),
                                              saveFile,
                                              file -> model.setDownloaded(true))
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    public static void dmDownload(@NonNull final Context context, @Nullable final String username, final DownloadMethod method,
                                  final DirectItemModel.DirectItemMediaModel itemsToDownload) {
        if (Utils.settingsHelper == null) Utils.settingsHelper = new SettingsHelper(context);

        if (itemsToDownload == null) return;

        if (ContextCompat.checkSelfPermission(context, PERMS[0]) == PackageManager.PERMISSION_GRANTED)
            dmDownloadImpl(context, username, method, itemsToDownload);
        else if (context instanceof Activity)
            ActivityCompat.requestPermissions((Activity) context, PERMS, 8020);
    }

    private static void dmDownloadImpl(@NonNull final Context context, @Nullable final String username,
                                       final DownloadMethod method, final DirectItemModel.DirectItemMediaModel selectedItem) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");

        if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
            final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
            if (!TextUtils.isEmpty(customPath)) dir = new File(customPath);
        }

        if (Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !TextUtils.isEmpty(username))
            dir = new File(dir, username);

        if (dir.exists() || dir.mkdirs()) {
            new DownloadAsync(context,
                              selectedItem.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO ? selectedItem.getVideoUrl() : selectedItem.getThumbUrl(),
                              getDownloadSaveFileDm(dir, selectedItem, ""),
                              null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private static File getDownloadSaveFile(final File finalDir, @NonNull final BasePostModel model, final String sliderPrefix) {
        final String displayUrl = model.getDisplayUrl();
        return new File(finalDir, model.getPostId() + '_' + model.getPosition() + sliderPrefix +
                getExtensionFromModel(displayUrl, model));
    }

    @NonNull
    private static File getDownloadSaveFileDm(final File finalDir,
                                              @NonNull final DirectItemModel.DirectItemMediaModel model,
                                              final String sliderPrefix) {
        final String displayUrl = model.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO ? model.getVideoUrl() : model.getThumbUrl();
        return new File(finalDir, model.getId() + sliderPrefix +
                getExtensionFromModel(displayUrl, model));
    }

    @NonNull
    public static String getExtensionFromModel(@NonNull final String url, final Object model) {
        final String extension;
        final int index = url.indexOf('?');

        if (index != -1) extension = url.substring(index - 4, index);
        else {
            final boolean isVideo;
            if (model instanceof StoryModel)
                isVideo = ((StoryModel) model).getItemType() == MediaItemType.MEDIA_TYPE_VIDEO;
            else if (model instanceof BasePostModel)
                isVideo = ((BasePostModel) model).getItemType() == MediaItemType.MEDIA_TYPE_VIDEO;
            else
                isVideo = false;
            extension = isVideo || url.contains(".mp4") ? ".mp4" : ".jpg";
        }

        return extension;
    }

    public static void checkExistence(final File downloadDir, final File customDir, final boolean isSlider,
                                      @NonNull final BasePostModel model) {
        boolean exists = false;

        try {
            final String displayUrl = model.getDisplayUrl();
            int index = displayUrl.indexOf('?');
            if (index < 0) {
                return;
            }
            final String fileName = model.getPostId() + '_';
            final String extension = displayUrl.substring(index - 4, index);

            final String fileWithoutPrefix = fileName + '0' + extension;
            exists = new File(downloadDir, fileWithoutPrefix).exists();
            if (!exists) {
                final String fileWithPrefix = fileName + "[\\d]+(|_slide_[\\d]+)(\\.mp4|\\" + extension + ")";
                final FilenameFilter filenameFilter = (dir, name) -> Pattern.matches(fileWithPrefix, name);

                File[] files = downloadDir.listFiles(filenameFilter);
                if ((files == null || files.length < 1) && customDir != null)
                    files = customDir.listFiles(filenameFilter);

                if (files != null && files.length >= 1) exists = true;
            }
        } catch (final Exception e) {
            if (Utils.logCollector != null)
                Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "checkExistence",
                                                   new Pair<>("isSlider", isSlider),
                                                   new Pair<>("model", model));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        model.setDownloaded(exists);
    }
}
