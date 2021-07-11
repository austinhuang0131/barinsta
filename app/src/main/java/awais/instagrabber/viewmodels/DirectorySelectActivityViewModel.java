package awais.instagrabber.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.fragments.settings.PreferenceKeys.FOLDER_PATH;

public class DirectorySelectActivityViewModel extends AndroidViewModel {
    private static final String TAG = DirectorySelectActivityViewModel.class.getSimpleName();

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> prevUri = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> dirSuccess = new MutableLiveData<>(false);

    public DirectorySelectActivityViewModel(final Application application) {
        super(application);
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<String> getPrevUri() {
        return prevUri;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<Boolean> getDirSuccess() {
        return dirSuccess;
    }

    public void setInitialUri(final Intent intent) {
        if (intent == null) {
            setMessage(null);
            return;
        }
        final Parcelable initialUriParcelable = intent.getParcelableExtra(Constants.EXTRA_INITIAL_URI);
        if (!(initialUriParcelable instanceof Uri)) {
            setMessage(null);
            return;
        }
        setMessage((Uri) initialUriParcelable);
    }

    private void setMessage(@Nullable final Uri initialUri) {
        if (initialUri == null) {
            final String prevVersionFolderPath = Utils.settingsHelper.getString(FOLDER_PATH);
            if (TextUtils.isEmpty(prevVersionFolderPath)) {
                // default message
                message.postValue(getApplication().getString(R.string.dir_select_default_message));
                prevUri.postValue(null);
                return;
            }
            message.postValue(getApplication().getString(R.string.dir_select_reselect_message));
            prevUri.postValue(prevVersionFolderPath);
            return;
        }
        final List<UriPermission> existingPermissions = getApplication().getContentResolver().getPersistedUriPermissions();
        final boolean anyMatch = existingPermissions.stream().anyMatch(uriPermission -> uriPermission.getUri().equals(initialUri));
        final DocumentFile documentFile = DocumentFile.fromSingleUri(getApplication(), initialUri);
        String path;
        try {
            path = URLDecoder.decode(initialUri.toString(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            path = initialUri.toString();
        }
        if (!anyMatch) {
            message.postValue(getApplication().getString(R.string.dir_select_permission_revoked_message));
            prevUri.postValue(path);
            return;
        }
        if (documentFile == null || !documentFile.exists() || documentFile.lastModified() == 0) {
            message.postValue(getApplication().getString(R.string.dir_select_folder_not_exist));
            prevUri.postValue(path);
        }
    }

    public void setupSelectedDir(@NonNull final Intent data) throws DownloadUtils.ReselectDocumentTreeException {
        loading.postValue(true);
        try {
            Utils.setupSelectedDir(getApplication(), data);
            message.postValue(getApplication().getString(R.string.dir_select_success_message));
            dirSuccess.postValue(true);
        } finally {
            loading.postValue(false);
        }
    }
}
