package awais.instagrabber.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ActivityDirectorySelectBinding;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.viewmodels.DirectorySelectActivityViewModel;

public class DirectorySelectActivity extends BaseLanguageActivity {
    private static final String TAG = DirectorySelectActivity.class.getSimpleName();
    public static final int SELECT_DIR_REQUEST_CODE = 0x01;
    private static final int ERROR_REQUEST_CODE = 0x02;

    private Uri initialUri;
    private ActivityDirectorySelectBinding binding;
    private DirectorySelectActivityViewModel viewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDirectorySelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(DirectorySelectActivityViewModel.class);
        setupObservers();
        binding.selectDir.setOnClickListener(v -> openDirectoryChooser());
        AppExecutors.INSTANCE.getMainThread().execute(() -> viewModel.setInitialUri(getIntent()));
    }

    private void setupObservers() {
        viewModel.getMessage().observe(this, message -> binding.message.setText(message));
        viewModel.getPrevUri().observe(this, prevUri -> {
            if (prevUri == null) {
                binding.prevUri.setVisibility(View.GONE);
                binding.message2.setVisibility(View.GONE);
                return;
            }
            binding.prevUri.setText(prevUri);
            binding.prevUri.setVisibility(View.VISIBLE);
            binding.message2.setVisibility(View.VISIBLE);
        });
        viewModel.getDirSuccess().observe(this, success -> binding.selectDir.setVisibility(success ? View.GONE : View.VISIBLE));
        viewModel.isLoading().observe(this, loading -> {
            binding.message.setVisibility(loading ? View.GONE : View.VISIBLE);
            binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    private void openDirectoryChooser() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SELECT_DIR_REQUEST_CODE) return;
        if (resultCode != RESULT_OK) {
            showErrorDialog(getString(R.string.select_a_folder));
            return;
        }
        if (data == null || data.getData() == null) {
            showErrorDialog(getString(R.string.select_a_folder));
            return;
        }
        if (!"com.android.externalstorage.documents".equals(data.getData().getAuthority())) {
            showErrorDialog(getString(R.string.dir_select_no_download_folder, data.getData().getAuthority()));
            return;
        }
        AppExecutors.INSTANCE.getMainThread().execute(() -> {
            try {
                viewModel.setupSelectedDir(data);
                final Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                // Should not come to this point.
                // If it does, we have to show this error to the user so that they can report it.
                try (final StringWriter sw = new StringWriter();
                     final PrintWriter pw = new PrintWriter(sw)) {
                    e.printStackTrace(pw);
                    showErrorDialog("Please report this error to the developers:\n\n" + sw.toString());
                } catch (IOException ioException) {
                    Log.e(TAG, "onActivityResult: ", ioException);
                }
            }
        }, 500);
    }

    private void showErrorDialog(@NonNull final String message) {
        final ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                ERROR_REQUEST_CODE,
                R.string.error,
                message,
                R.string.ok,
                0,
                0
        );
        dialogFragment.show(getSupportFragmentManager(), ConfirmDialogFragment.class.getSimpleName());
    }
}
