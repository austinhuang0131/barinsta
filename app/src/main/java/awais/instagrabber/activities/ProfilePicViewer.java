package awais.instagrabber.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import awais.instagrabber.R;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.asyncs.ProfilePictureFetcher;
import awais.instagrabber.databinding.ActivityProfilepicBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.HashtagModel;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class ProfilePicViewer extends BaseLanguageActivity {
    private ActivityProfilepicBinding profileBinding;
    private ProfileModel profileModel;
    private HashtagModel hashtagModel;
    private LocationModel locationModel;
    private MenuItem menuItemDownload;
    private String profilePicUrl;
    private FragmentManager fragmentManager;
    private FetchListener<String> fetchListener;
    private boolean errorHandled = false;
    private boolean fallbackToProfile = false;
    private boolean destroyed = false;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileBinding = ActivityProfilepicBinding.inflate(getLayoutInflater());
        setContentView(profileBinding.getRoot());

        setSupportActionBar(profileBinding.toolbar.toolbar);

        final Intent intent = getIntent();
        if (intent == null || (!intent.hasExtra(Constants.EXTRAS_PROFILE) && !intent.hasExtra(Constants.EXTRAS_HASHTAG) && !intent.hasExtra(Constants.EXTRAS_LOCATION))
                || ((profileModel = (ProfileModel) intent.getSerializableExtra(Constants.EXTRAS_PROFILE)) == null
                && (hashtagModel = (HashtagModel) intent.getSerializableExtra(Constants.EXTRAS_HASHTAG)) == null
                && (locationModel = (LocationModel) intent.getSerializableExtra(Constants.EXTRAS_LOCATION)) == null)) {
            Utils.errorFinish(this);
            return;
        }

        fragmentManager = getSupportFragmentManager();

        final String id = hashtagModel != null ? hashtagModel.getId() : (locationModel != null ? locationModel.getId() : profileModel.getId());
        final String username = hashtagModel != null ? hashtagModel.getName() : (locationModel != null ? locationModel.getName() : profileModel.getUsername());

        profileBinding.toolbar.toolbar.setTitle(username);

        profileBinding.progressView.setVisibility(View.VISIBLE);
        profileBinding.imageViewer.setVisibility(View.VISIBLE);

        profileBinding.imageViewer.setZoomable(true);
        profileBinding.imageViewer.setZoomTransitionDuration(420);
        profileBinding.imageViewer.setMaximumScale(7.2f);

        fetchListener = profileUrl -> {
            profilePicUrl = profileUrl;

            if (!fallbackToProfile && Utils.isEmpty(profilePicUrl)) {
                fallbackToProfile = true;
                new ProfilePictureFetcher(username, id, fetchListener, profilePicUrl, (hashtagModel != null || locationModel != null)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return;
            }

            if (errorHandled && fallbackToProfile || Utils.isEmpty(profilePicUrl))
                profilePicUrl = hashtagModel != null ? hashtagModel.getSdProfilePic() : (locationModel != null ? locationModel.getSdProfilePic() : profileModel.getHdProfilePic());

            if (destroyed == true) return;

            final RequestManager glideRequestManager = Glide.with(this);

            glideRequestManager.load(profilePicUrl).addListener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                    fallbackToProfile = true;
                    if (!errorHandled) {
                        errorHandled = true;
                        new ProfilePictureFetcher(username, id, fetchListener, profilePicUrl, (hashtagModel != null || locationModel != null))
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        glideRequestManager.load(profilePicUrl).into(profileBinding.imageViewer);
                        showImageInfo();
                    }
                    profileBinding.progressView.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                    if (menuItemDownload != null) menuItemDownload.setEnabled(true);
                    showImageInfo();
                    profileBinding.progressView.setVisibility(View.GONE);
                    return false;
                }

                private void showImageInfo() {
                    final Drawable drawable = profileBinding.imageViewer.getDrawable();
                    if (drawable != null) {
                        final StringBuilder info = new StringBuilder(getString(R.string.profile_viewer_imageinfo, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()));
                        if (drawable instanceof BitmapDrawable) {
                            final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                            if (bitmap != null) {
                                final String colorDepthPrefix = getString(R.string.profile_viewer_colordepth_prefix);
                                switch (bitmap.getConfig()) {
                                    case ALPHA_8:
                                        info.append(colorDepthPrefix).append(" 8-bits(A)");
                                        break;
                                    case RGB_565:
                                        info.append(colorDepthPrefix).append(" 16-bits-A");
                                        break;
                                    case ARGB_4444:
                                        info.append(colorDepthPrefix).append(" 16-bits+A");
                                        break;
                                    case ARGB_8888:
                                        info.append(colorDepthPrefix).append(" 32-bits+A");
                                        break;
                                    case RGBA_F16:
                                        info.append(colorDepthPrefix).append(" 64-bits+A");
                                        break;
                                    case HARDWARE:
                                        info.append(colorDepthPrefix).append(" auto");
                                        break;
                                }
                            }
                        }
                        profileBinding.imageInfo.setText(info);
                        profileBinding.imageInfo.setVisibility(View.VISIBLE);
                    }
                }
            }).into(profileBinding.imageViewer);
        };

        new ProfilePictureFetcher(username, id, fetchListener, profilePicUrl, (hashtagModel != null || locationModel != null))
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void downloadProfilePicture() {
        int error = 0;

        if (profileModel != null) {
            final File dir = new File(Environment.getExternalStorageDirectory(), "Download");
            if (dir.exists() || dir.mkdirs()) {

                final File saveFile = new File(dir, profileModel.getUsername() + '_' + System.currentTimeMillis()
                        + Utils.getExtensionFromModel(profilePicUrl, profileModel));

                new DownloadAsync(this,
                        profilePicUrl,
                        saveFile,
                        result -> {
                            final int toastRes = result != null && result.exists() ?
                                    R.string.downloader_downloaded_in_folder : R.string.downloader_error_download_file;
                            Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show();
                        }).setItems(null, profileModel.getUsername()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else error = 1;
        } else error = 2;

        if (error == 1) Toast.makeText(this, R.string.downloader_error_creating_folder, Toast.LENGTH_SHORT).show();
        else if (error == 2) Toast.makeText(this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
        destroyed = true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        final MenuItem.OnMenuItemClickListener menuItemClickListener = item -> {
            if (item == menuItemDownload) {
                downloadProfilePicture();
            }
            return true;
        };

        menu.findItem(R.id.action_search).setVisible(false);
        menuItemDownload = menu.findItem(R.id.action_download);
        menuItemDownload.setVisible(true);
        menuItemDownload.setEnabled(false);
        menuItemDownload.setOnMenuItemClickListener(menuItemClickListener);

        return true;
    }
}