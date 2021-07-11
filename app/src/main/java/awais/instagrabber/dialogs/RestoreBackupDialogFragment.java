package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import awais.instagrabber.databinding.DialogRestoreBackupBinding;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.ExportImportUtils;
import awais.instagrabber.utils.PasswordUtils.IncorrectPasswordException;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;

public class RestoreBackupDialogFragment extends DialogFragment {
    private static final String TAG = RestoreBackupDialogFragment.class.getSimpleName();
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private static final int OPEN_FILE_REQUEST_CODE = 1;

    private OnResultListener onResultListener;

    private DialogRestoreBackupBinding binding;
    private boolean isEncrypted;
    private Uri uri;

    public RestoreBackupDialogFragment() {}

    public RestoreBackupDialogFragment(final OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = DialogRestoreBackupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        final int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int width = (int) (Utils.displayMetrics.widthPixels * 0.8);
        window.setLayout(width, height);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (data == null || data.getData() == null) return;
        if (resultCode != RESULT_OK || requestCode != OPEN_FILE_REQUEST_CODE) return;
        final Context context = getContext();
        if (context == null) return;
        isEncrypted = ExportImportUtils.isEncrypted(context, data.getData());
        if (isEncrypted) {
            binding.passwordGroup.setVisibility(View.VISIBLE);
            binding.passwordGroup.post(() -> {
                binding.etPassword.requestFocus();
                binding.etPassword.post(() -> {
                    final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm == null) return;
                    imm.showSoftInput(binding.etPassword, InputMethodManager.SHOW_IMPLICIT);
                });
                binding.btnRestore.setEnabled(!TextUtils.isEmpty(binding.etPassword.getText()));
            });
        } else {
            binding.passwordGroup.setVisibility(View.GONE);
            binding.btnRestore.setEnabled(true);
        }
        uri = data.getData();
        AppExecutors.INSTANCE.getMainThread().execute(() -> {
            Cursor c = null;
            try {
                String[] projection = {MediaStore.Files.FileColumns.DISPLAY_NAME};
                final ContentResolver contentResolver = context.getContentResolver();
                c = contentResolver.query(uri, projection, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        final String displayName = c.getString(0);
                        binding.filePath.setText(displayName);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: ", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        });
    }

    private void init() {
        final Context context = getContext();
        if (context == null) return;
        binding.btnRestore.setEnabled(false);
        binding.btnRestore.setOnClickListener(v -> new Handler(Looper.getMainLooper()).post(() -> {
            if (uri == null) return;
            int flags = 0;
            if (binding.cbFavorites.isChecked()) {
                flags |= ExportImportUtils.FLAG_FAVORITES;
            }
            if (binding.cbSettings.isChecked()) {
                flags |= ExportImportUtils.FLAG_SETTINGS;
            }
            if (binding.cbAccounts.isChecked()) {
                flags |= ExportImportUtils.FLAG_COOKIES;
            }
            final Editable text = binding.etPassword.getText();
            if (isEncrypted && text == null) return;
            try {
                ExportImportUtils.importData(
                        context,
                        flags,
                        uri,
                        !isEncrypted ? null : text.toString(),
                        result -> {
                            if (onResultListener != null) {
                                onResultListener.onResult(result);
                            }
                            dismiss();
                        }
                );
            } catch (IncorrectPasswordException e) {
                binding.passwordField.setError("Incorrect password");
            }
        }));
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                binding.btnRestore.setEnabled(!TextUtils.isEmpty(s));
                binding.passwordField.setError(null);
            }

            @Override
            public void afterTextChanged(final Editable s) {}
        });
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);

    }

    public interface OnResultListener {
        void onResult(boolean result);
    }
}
