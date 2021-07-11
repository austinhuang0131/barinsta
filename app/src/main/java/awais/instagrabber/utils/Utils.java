package awais.instagrabber.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Browser;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.FavoriteType;

public final class Utils {
    private static final String TAG = "Utils";
    private static final int VIDEO_CACHE_MAX_BYTES = 10 * 1024 * 1024;

    // public static LogCollector logCollector;
    public static SettingsHelper settingsHelper;
    public static boolean sessionVolumeFull = false;
    public static final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public static final DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    public static ClipboardManager clipboardManager;
    public static SimpleCache simpleCache;
    private static int statusBarHeight;
    private static int actionBarHeight;
    public static String cacheDir;
    private static int defaultStatusBarColor;
    private static Object[] volumes;

    public static int convertDpToPx(final float dp) {
        return Math.round((dp * displayMetrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void copyText(@NonNull final Context context, final CharSequence string) {
        if (clipboardManager == null) {
            clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        }
        int toastMessage = R.string.clipboard_error;
        if (clipboardManager != null) {
            try {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), string));
                toastMessage = R.string.clipboard_copied;
            } catch (Exception e) {
                Log.e(TAG, "copyText: ", e);
            }
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    public static Map<String, String> sign(final Map<String, Object> form) {
        // final String signed = sign(Constants.SIGNATURE_KEY, new JSONObject(form).toString());
        // if (signed == null) {
        //     return null;
        // }
        final Map<String, String> map = new HashMap<>();
        // map.put("ig_sig_key_version", Constants.SIGNATURE_VERSION);
        // map.put("signed_body", signed);
        map.put("signed_body", "SIGNATURE." + new JSONObject(form).toString());
        return map;
    }

    // public static String sign(final String key, final String message) {
    //     try {
    //         final Mac hasher = Mac.getInstance("HmacSHA256");
    //         hasher.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
    //         byte[] hash = hasher.doFinal(message.getBytes());
    //         final StringBuilder hexString = new StringBuilder();
    //         for (byte b : hash) {
    //             final String hex = Integer.toHexString(0xff & b);
    //             if (hex.length() == 1) hexString.append('0');
    //             hexString.append(hex);
    //         }
    //         return hexString.toString() + "." + message;
    //     } catch (Exception e) {
    //         Log.e(TAG, "Error signing", e);
    //         return null;
    //     }
    // }

    public static String getMimeType(@NonNull final Uri uri, final ContentResolver contentResolver) {
        String mimeType;
        final String scheme = uri.getScheme();
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (TextUtils.isEmpty(scheme)) {
            mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExtension.toLowerCase());
        } else {
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                mimeType = contentResolver.getType(uri);
            } else {
                mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExtension.toLowerCase());
            }
        }
        if (mimeType == null) return null;
        return mimeType.toLowerCase();
    }

    public static SimpleCache getSimpleCacheInstance(final Context context) {
        if (context == null) {
            return null;
        }
        final ExoDatabaseProvider exoDatabaseProvider = new ExoDatabaseProvider(context);
        final File cacheDir = context.getCacheDir();
        if (simpleCache == null && cacheDir != null) {
            simpleCache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_MAX_BYTES), exoDatabaseProvider);
        }
        return simpleCache;
    }

    @Nullable
    public static Pair<FavoriteType, String> migrateOldFavQuery(final String queryText) {
        if (queryText.startsWith("@")) {
            return new Pair<>(FavoriteType.USER, queryText.substring(1));
        } else if (queryText.contains("/")) {
            return new Pair<>(FavoriteType.LOCATION, queryText.substring(0, queryText.indexOf("/")));
        } else if (queryText.startsWith("#")) {
            return new Pair<>(FavoriteType.HASHTAG, queryText.substring(1));
        }
        return null;
    }

    public static int getStatusBarHeight(final Context context) {
        if (statusBarHeight > 0) {
            return statusBarHeight;
        }
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static int getActionBarHeight(@NonNull final Context context) {
        if (actionBarHeight > 0) {
            return actionBarHeight;
        }
        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, displayMetrics);
        }
        return actionBarHeight;
    }

    public static void openURL(final Context context, final String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }
        final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        i.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true);
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "openURL: No activity found to handle URLs", e);
            Toast.makeText(context, context.getString(R.string.no_external_app_url), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "openURL", e);
        }
    }

    public static void openEmailAddress(final Context context, final String emailAddress) {
        if (context == null || TextUtils.isEmpty(emailAddress)) {
            return;
        }
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAddress));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(emailIntent);
    }

    public static void displayToastAboveView(@NonNull final Context context,
                                             @NonNull final View view,
                                             @NonNull final String text) {
        final Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.START,
                         view.getLeft(),
                         view.getTop());
        toast.show();
    }

    public static PostsLayoutPreferences getPostsLayoutPreferences(final String layoutPreferenceKey) {
        PostsLayoutPreferences layoutPreferences = PostsLayoutPreferences.fromJson(settingsHelper.getString(layoutPreferenceKey));
        if (layoutPreferences == null) {
            layoutPreferences = PostsLayoutPreferences.builder().build();
            settingsHelper.putString(layoutPreferenceKey, layoutPreferences.getJson());
        }
        return layoutPreferences;
    }

    private static Field mAttachInfoField;
    private static Field mStableInsetsField;

    public static int getViewInset(View view) {
        if (view == null
                || view.getHeight() == displayMetrics.heightPixels
                || view.getHeight() == displayMetrics.widthPixels - getStatusBarHeight(view.getContext())) {
            return 0;
        }
        try {
            if (mAttachInfoField == null) {
                //noinspection JavaReflectionMemberAccess
                mAttachInfoField = View.class.getDeclaredField("mAttachInfo");
                mAttachInfoField.setAccessible(true);
            }
            Object mAttachInfo = mAttachInfoField.get(view);
            if (mAttachInfo != null) {
                if (mStableInsetsField == null) {
                    mStableInsetsField = mAttachInfo.getClass().getDeclaredField("mStableInsets");
                    mStableInsetsField.setAccessible(true);
                }
                Rect insets = (Rect) mStableInsetsField.get(mAttachInfo);
                if (insets == null) {
                    return 0;
                }
                return insets.bottom;
            }
        } catch (Exception e) {
            Log.e(TAG, "getViewInset", e);
        }
        return 0;
    }

    public static int getThemeAccentColor(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public static int getAttrValue(@NonNull final Context context, final int attr) {
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue.data;
    }

    public static int getAttrResId(@NonNull final Context context, final int attr) {
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue.resourceId;
    }

    public static void transparentStatusBar(final Activity activity,
                                            final boolean enable,
                                            final boolean fullscreen) {
        if (activity == null) return;
        final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        final Window window = activity.getWindow();
        final View decorView = window.getDecorView();
        if (enable) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            if (actionBar != null) {
                actionBar.hide();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                defaultStatusBarColor = window.getStatusBarColor();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                // FOR TRANSPARENT NAVIGATION BAR
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.setStatusBarColor(Color.TRANSPARENT);
                Log.d(TAG, "Setting Color Transparent " + Color.TRANSPARENT + " Default Color " + defaultStatusBarColor);
                return;
            }
            Log.d(TAG, "Setting Color Trans " + Color.TRANSPARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            return;
        }
        if (fullscreen) {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            return;
        }
        if (actionBar != null) {
            actionBar.show();
        }
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setStatusBarColor(defaultStatusBarColor);
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    // public static void mediaScanFile(@NonNull final Context context,
    //                                  @NonNull File file,
    //                                  @NonNull final OnScanCompletedListener callback) {
    //     //noinspection UnstableApiUsage
    //     final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Files.getFileExtension(file.getName()));
    //     MediaScannerConnection.scanFile(
    //             context,
    //             new String[]{file.getAbsolutePath()},
    //             new String[]{mimeType},
    //             callback
    //     );
    // }

    public static void showKeyboard(@NonNull final View view) {
        final Context context = view.getContext();
        if (context == null) return;
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        view.requestFocus();
        final boolean shown = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        if (!shown) {
            Log.e(TAG, "showKeyboard: System did not display the keyboard");
        }
    }

    public static void hideKeyboard(final View view) {
        if (view == null) return;
        final Context context = view.getContext();
        if (context == null) return;
        try {
            final InputMethodManager manager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (manager == null) return;
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            Log.e(TAG, "hideKeyboard: ", e);
        }
    }

    public static Drawable getAnimatableDrawable(@NonNull final Context context,
                                                 @DrawableRes final int drawableResId) {
        final Drawable drawable;
        if (Build.VERSION.SDK_INT >= 24) {
            drawable = ContextCompat.getDrawable(context, drawableResId);
        } else {
            drawable = AnimatedVectorDrawableCompat.create(context, drawableResId);
        }
        return drawable;
    }

    public static void enabledKeepScreenOn(@NonNull final Activity activity) {
        final Window window = activity.getWindow();
        if (window == null) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void disableKeepScreenOn(@NonNull final Activity activity) {
        final Window window = activity.getWindow();
        if (window == null) return;
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static <T> void moveItem(int sourceIndex, int targetIndex, List<T> list) {
        if (sourceIndex <= targetIndex) {
            Collections.rotate(list.subList(sourceIndex, targetIndex + 1), -1);
        } else {
            Collections.rotate(list.subList(targetIndex, sourceIndex + 1), 1);
        }
    }

    // public static void scanDocumentFile(@NonNull final Context context,
    //                                     @NonNull final DocumentFile documentFile,
    //                                     @NonNull final OnScanCompletedListener callback) {
    //     if (!documentFile.isFile() || !documentFile.exists()) {
    //         Log.d(TAG, "scanDocumentFile: " + documentFile);
    //         callback.onScanCompleted(null, null);
    //         return;
    //     }
    //     File file = null;
    //     try {
    //         file = getDocumentFileRealPath(context, documentFile);
    //     } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
    //         Log.e(TAG, "scanDocumentFile: ", e);
    //     }
    //     if (file == null) return;
    //     MediaScannerConnection.scanFile(context,
    //                                     new String[]{file.getAbsolutePath()},
    //                                     new String[]{documentFile.getType()},
    //                                     callback);
    // }

    public static File getDocumentFileRealPath(@NonNull final Context context,
                                               @NonNull final DocumentFile documentFile)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final String docId = DocumentsContract.getDocumentId(documentFile.getUri());
        final String[] split = docId.split(":");
        final String type = split[0];

        if (type.equalsIgnoreCase("primary")) {
            return new File(Environment.getExternalStorageDirectory(), split[1]);
        } else if (type.equalsIgnoreCase("raw")) {
            return new File(split[1]);
        } else {
            if (volumes == null) {
                final StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                if (sm == null) return null;
                final Method getVolumeListMethod = sm.getClass().getMethod("getVolumeList");
                volumes = (Object[]) getVolumeListMethod.invoke(sm);
            }
            if (volumes == null) return null;
            for (Object volume : volumes) {
                final Method getUuidMethod = volume.getClass().getMethod("getUuid");
                final String uuid = (String) getUuidMethod.invoke(volume);

                if (uuid != null && uuid.equalsIgnoreCase(type)) {
                    final Method getPathMethod = volume.getClass().getMethod("getPath");
                    final String path = (String) getPathMethod.invoke(volume);
                    return new File(path, split[1]);
                }
            }
        }

        return null;
    }

    public static void setupSelectedDir(@NonNull final Context context,
                                        @NonNull final Intent intent) throws DownloadUtils.ReselectDocumentTreeException {
        final Uri dirUri = intent.getData();
        Log.d(TAG, "onActivityResult: " + dirUri);
        if (dirUri == null) return;
        final int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(dirUri, takeFlags);
        // re-init DownloadUtils
        DownloadUtils.init(context, dirUri.toString());
    }

    @NonNull
    public static Point getNavigationBarSize(@NonNull Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    @NonNull
    public static Point getAppUsableScreenSize(@NonNull Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    @NonNull
    public static Point getRealScreenSize(@NonNull Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    public static <F, S> LiveData<Pair<F, S>> zipLiveData(@NonNull final LiveData<F> firstLiveData,
                                                          @NonNull final LiveData<S> secondLiveData) {
        final ZippedLiveData<F, S> zippedLiveData = new ZippedLiveData<>();
        zippedLiveData.addFirstSource(firstLiveData);
        zippedLiveData.addSecondSource(secondLiveData);
        return zippedLiveData;
    }

    public static class ZippedLiveData<F, S> extends MediatorLiveData<Pair<F, S>> {
        private F lastF;
        private S lastS;

        private void update() {
            F localLastF = lastF;
            S localLastS = lastS;
            if (localLastF != null && localLastS != null) {
                setValue(new Pair<>(localLastF, localLastS));
            }
        }

        public void addFirstSource(@NonNull final LiveData<F> firstLiveData) {
            addSource(firstLiveData, f -> {
                lastF = f;
                update();
            });
        }

        public void addSecondSource(@NonNull final LiveData<S> secondLiveData) {
            addSource(secondLiveData, s -> {
                lastS = s;
                update();
            });
        }
    }
}
