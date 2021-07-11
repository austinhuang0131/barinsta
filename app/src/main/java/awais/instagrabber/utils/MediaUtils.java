package awais.instagrabber.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

public final class MediaUtils {
    private static final String TAG = MediaUtils.class.getSimpleName();

    public static void getVideoInfo(@NonNull final ContentResolver contentResolver,
                                    @NonNull final Uri uri,
                                    @NonNull final OnInfoLoadListener<VideoInfo> listener) {
        getInfo(contentResolver, uri, listener, true);
    }

    public static void getVoiceInfo(@NonNull final ContentResolver contentResolver,
                                    @NonNull final Uri uri,
                                    @NonNull final OnInfoLoadListener<VideoInfo> listener) {
        getInfo(contentResolver, uri, listener, false);
    }

    private static void getInfo(@NonNull final ContentResolver contentResolver,
                                @NonNull final Uri uri,
                                @NonNull final OnInfoLoadListener<VideoInfo> listener,
                                @NonNull final Boolean isVideo) {
        AppExecutors.INSTANCE.getTasksThread().submit(() -> {
            try (ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")) {
                if (parcelFileDescriptor == null) {
                    listener.onLoad(null);
                    return;
                }
                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(fileDescriptor);
                String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (TextUtils.isEmpty(duration)) duration = "0";
                if (isVideo) {
                    String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    if (TextUtils.isEmpty(width)) width = "1";
                    String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    if (TextUtils.isEmpty(height)) height = "1";
                    final Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.Video.Media.SIZE}, null, null, null);
                    cursor.moveToFirst();
                    final long fileSize = cursor.getLong(0);
                    cursor.close();
                    listener.onLoad(new VideoInfo(
                            Long.parseLong(duration),
                            Integer.valueOf(width),
                            Integer.valueOf(height),
                            fileSize
                    ));
                    return;
                }
                listener.onLoad(new VideoInfo(
                        Long.parseLong(duration),
                        0,
                        0,
                        0
                ));
            } catch (Exception e) {
                Log.e(TAG, "getInfo: ", e);
                listener.onFailure(e);
            }
        });
    }

    public static class VideoInfo {
        public long duration;
        public int width;
        public int height;
        public long size;

        public VideoInfo(final long duration, final int width, final int height, final long size) {
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.size = size;
        }

    }

    public interface OnInfoLoadListener<T> {
        void onLoad(@Nullable T info);

        void onFailure(Throwable t);
    }
}
