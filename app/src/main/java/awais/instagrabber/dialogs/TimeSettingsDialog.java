package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import awais.instagrabber.R;
import awais.instagrabber.databinding.DialogTimeSettingsBinding;
import awais.instagrabber.utils.DateUtils;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.TextUtils;

public final class TimeSettingsDialog extends DialogFragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, TextWatcher {
    private DialogTimeSettingsBinding binding;
    private final LocalDateTime magicDate;
    private DateTimeFormatter currentFormat;
    private String selectedFormat;
    private final boolean customDateTimeFormatEnabled;
    private final String customDateTimeFormat;
    private final String dateTimeSelection;
    private final boolean swapDateTimeEnabled;
    private final OnConfirmListener onConfirmListener;

    public TimeSettingsDialog(final boolean customDateTimeFormatEnabled,
                              final String customDateTimeFormat,
                              final String dateTimeSelection,
                              final boolean swapDateTimeEnabled,
                              final OnConfirmListener onConfirmListener) {
        this.customDateTimeFormatEnabled = customDateTimeFormatEnabled;
        this.customDateTimeFormat = customDateTimeFormat;
        this.dateTimeSelection = dateTimeSelection;
        this.swapDateTimeEnabled = swapDateTimeEnabled;
        this.onConfirmListener = onConfirmListener;
        magicDate = LocalDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        binding = DialogTimeSettingsBinding.inflate(inflater, container, false);

        binding.cbCustomFormat.setOnCheckedChangeListener(this);
        binding.cbCustomFormat.setChecked(customDateTimeFormatEnabled);
        binding.cbSwapTimeDate.setChecked(swapDateTimeEnabled);
        binding.customFormatEditText.setText(customDateTimeFormat);

        final String[] dateTimeFormat = dateTimeSelection.split(";"); // output = time;separator;date
        binding.spTimeFormat.setSelection(Integer.parseInt(dateTimeFormat[0]));
        binding.spSeparator.setSelection(Integer.parseInt(dateTimeFormat[1]));
        binding.spDateFormat.setSelection(Integer.parseInt(dateTimeFormat[2]));

        binding.cbSwapTimeDate.setOnCheckedChangeListener(this);

        refreshTimeFormat();

        binding.spTimeFormat.setOnItemSelectedListener(this);
        binding.spDateFormat.setOnItemSelectedListener(this);
        binding.spSeparator.setOnItemSelectedListener(this);

        binding.customFormatEditText.addTextChangedListener(this);
        binding.btnConfirm.setOnClickListener(this);
        binding.customFormatField.setEndIconOnClickListener(this);

        return binding.getRoot();
    }

    private void refreshTimeFormat() {
        final boolean isCustom = binding.cbCustomFormat.isChecked();
        if (isCustom) {
            final Editable text = binding.customFormatEditText.getText();
            if (text != null) {
                selectedFormat = text.toString();
            }
        } else {
            final String sepStr = String.valueOf(binding.spSeparator.getSelectedItem());
            final String timeStr = String.valueOf(binding.spTimeFormat.getSelectedItem());
            final String dateStr = String.valueOf(binding.spDateFormat.getSelectedItem());

            final boolean isSwapTime = binding.cbSwapTimeDate.isChecked();
            final boolean isBlankSeparator = binding.spSeparator.getSelectedItemPosition() <= 0;

            selectedFormat = (isSwapTime ? dateStr : timeStr)
                    + (isBlankSeparator ? " " : " '" + sepStr + "' ")
                    + (isSwapTime ? timeStr : dateStr);
        }

        binding.btnConfirm.setEnabled(true);
        try {
            currentFormat = DateTimeFormatter.ofPattern(selectedFormat, LocaleUtils.getCurrentLocale());
            if (isCustom) {
                final boolean valid = !TextUtils.isEmpty(selectedFormat) && DateUtils.checkFormatterValid(currentFormat);
                binding.customFormatField.setError(valid ? null :getString(R.string.invalid_format));
                if (!valid) {
                    binding.btnConfirm.setEnabled(false);
                }
            }
            binding.timePreview.setText(magicDate.format(currentFormat));
        } catch (Exception e) {
            binding.btnConfirm.setEnabled(false);
            binding.timePreview.setText(null);
        }
    }

    @Override
    public void onItemSelected(final AdapterView<?> p, final View v, final int pos, final long id) {
        refreshTimeFormat();
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if (buttonView == binding.cbCustomFormat) {
            binding.customFormatField.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.customFormatField.setEnabled(isChecked);

            binding.spTimeFormat.setEnabled(!isChecked);
            binding.spDateFormat.setEnabled(!isChecked);
            binding.spSeparator.setEnabled(!isChecked);
            binding.cbSwapTimeDate.setEnabled(!isChecked);
        }
        refreshTimeFormat();
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        refreshTimeFormat();
    }

    @Override
    public void onClick(final View v) {
        if (v == binding.btnConfirm) {
            if (onConfirmListener != null) {
                onConfirmListener.onConfirm(
                        binding.cbCustomFormat.isChecked(),
                        binding.spTimeFormat.getSelectedItemPosition(),
                        binding.spSeparator.getSelectedItemPosition(),
                        binding.spDateFormat.getSelectedItemPosition(),
                        selectedFormat,
                        binding.cbSwapTimeDate.isChecked());
            }
            dismiss();
        } else if (v == binding.customFormatField.findViewById(R.id.text_input_end_icon)) {
            binding.customPanel.setVisibility(
                    binding.customPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );

        }
    }

    public interface OnConfirmListener {
        void onConfirm(boolean isCustomFormat,
                       int spTimeFormatSelectedItemPosition,
                       int spSeparatorSelectedItemPosition,
                       int spDateFormatSelectedItemPosition,
                       final String selectedFormat,
                       final boolean swapDateTime);
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) { }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) { }

    @Override
    public void afterTextChanged(final Editable s) { }

    @Override
    public void onResume() {
        super.onResume();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        final WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }
}