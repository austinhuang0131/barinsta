package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Locale;

import awais.instagrabber.databinding.DialogCreateBackupBinding;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.ExportImportUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;

public class CreateBackupDialogFragment extends DialogFragment {
    private static final String TAG = CreateBackupDialogFragment.class.getSimpleName();
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private static final DateTimeFormatter BACKUP_FILE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US);
    private static final int CREATE_FILE_REQUEST_CODE = 1;


    private final OnResultListener onResultListener;
    private DialogCreateBackupBinding binding;

    public CreateBackupDialogFragment(final OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = DialogCreateBackupBinding.inflate(inflater, container, false);
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

    private void init() {
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                binding.btnSaveTo.setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(final Editable s) {}
        });
        final Context context = getContext();
        if (context == null) {
            return;
        }
        binding.cbPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (TextUtils.isEmpty(binding.etPassword.getText())) {
                    binding.btnSaveTo.setEnabled(false);
                }
                binding.passwordField.setVisibility(View.VISIBLE);
                binding.etPassword.requestFocus();
                final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.showSoftInput(binding.etPassword, InputMethodManager.SHOW_IMPLICIT);
                return;
            }
            binding.btnSaveTo.setEnabled(true);
            binding.passwordField.setVisibility(View.GONE);
            final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) return;
            imm.hideSoftInputFromWindow(binding.etPassword.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        });
        binding.btnSaveTo.setOnClickListener(v -> {
            createFile();
        });
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (data == null || data.getData() == null) return;
        if (resultCode != RESULT_OK || requestCode != CREATE_FILE_REQUEST_CODE) return;
        final Context context = getContext();
        if (context == null) return;
        final Editable passwordText = binding.etPassword.getText();
        final String password = binding.cbPassword.isChecked()
                                        && passwordText != null
                                        && !TextUtils.isEmpty(passwordText.toString())
                                ? passwordText.toString().trim()
                                : null;
        int flags = 0;
        if (binding.cbExportFavorites.isChecked()) {
            flags |= ExportImportUtils.FLAG_FAVORITES;
        }
        if (binding.cbExportSettings.isChecked()) {
            flags |= ExportImportUtils.FLAG_SETTINGS;
        }
        if (binding.cbExportLogins.isChecked()) {
            flags |= ExportImportUtils.FLAG_COOKIES;
        }
        ExportImportUtils.exportData(context, flags, data.getData(), password, result -> {
            if (onResultListener != null) {
                onResultListener.onResult(result);
            }
            dismiss();
        });
    }

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        final String fileName = String.format("barinsta_%s.backup", LocalDateTime.now().format(BACKUP_FILE_DATE_TIME_FORMAT));
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DownloadUtils.getBackupsDir().getUri());
        }

        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }


    public interface OnResultListener {
        void onResult(boolean result);
    }
}
