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

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import awais.instagrabber.databinding.DialogTimeSettingsBinding;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.TextUtils;

public final class TimeSettingsDialog extends DialogFragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, TextWatcher {
    private DialogTimeSettingsBinding timeSettingsBinding;
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
        timeSettingsBinding = DialogTimeSettingsBinding.inflate(inflater, container, false);

        timeSettingsBinding.cbCustomFormat.setOnCheckedChangeListener(this);
        timeSettingsBinding.cbCustomFormat.setChecked(customDateTimeFormatEnabled);
        timeSettingsBinding.cbSwapTimeDate.setChecked(swapDateTimeEnabled);
        timeSettingsBinding.etCustomFormat.setText(customDateTimeFormat);

        final String[] dateTimeFormat = dateTimeSelection.split(";"); // output = time;separator;date
        timeSettingsBinding.spTimeFormat.setSelection(Integer.parseInt(dateTimeFormat[0]));
        timeSettingsBinding.spSeparator.setSelection(Integer.parseInt(dateTimeFormat[1]));
        timeSettingsBinding.spDateFormat.setSelection(Integer.parseInt(dateTimeFormat[2]));

        timeSettingsBinding.cbSwapTimeDate.setOnCheckedChangeListener(this);

        refreshTimeFormat();

        timeSettingsBinding.spTimeFormat.setOnItemSelectedListener(this);
        timeSettingsBinding.spDateFormat.setOnItemSelectedListener(this);
        timeSettingsBinding.spSeparator.setOnItemSelectedListener(this);

        timeSettingsBinding.etCustomFormat.addTextChangedListener(this);
        timeSettingsBinding.btnConfirm.setOnClickListener(this);
        timeSettingsBinding.btnInfo.setOnClickListener(this);

        return timeSettingsBinding.getRoot();
    }

    private void refreshTimeFormat() {
        if (timeSettingsBinding.cbCustomFormat.isChecked())
            selectedFormat = timeSettingsBinding.etCustomFormat.getText().toString();
        else {
            final String sepStr = String.valueOf(timeSettingsBinding.spSeparator.getSelectedItem());
            final String timeStr = String.valueOf(timeSettingsBinding.spTimeFormat.getSelectedItem());
            final String dateStr = String.valueOf(timeSettingsBinding.spDateFormat.getSelectedItem());

            final boolean isSwapTime = timeSettingsBinding.cbSwapTimeDate.isChecked();
            final boolean isBlankSeparator = timeSettingsBinding.spSeparator.getSelectedItemPosition() <= 0;

            selectedFormat = (isSwapTime ? dateStr : timeStr)
                    + (isBlankSeparator ? " " : " '" + sepStr + "' ")
                    + (isSwapTime ? timeStr : dateStr);
        }

        timeSettingsBinding.btnConfirm.setEnabled(true);
        try {
            currentFormat = DateTimeFormatter.ofPattern(selectedFormat, LocaleUtils.getCurrentLocale());
            timeSettingsBinding.timePreview.setText(magicDate.format(currentFormat));
        }
        catch (Exception e) {
            timeSettingsBinding.btnConfirm.setEnabled(false);
            timeSettingsBinding.timePreview.setText(null);
        }
    }

    @Override
    public void onItemSelected(final AdapterView<?> p, final View v, final int pos, final long id) {
        refreshTimeFormat();
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if (buttonView == timeSettingsBinding.cbCustomFormat) {
            final View parent = (View) timeSettingsBinding.etCustomFormat.getParent();
            parent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            timeSettingsBinding.etCustomFormat.setEnabled(isChecked);
            timeSettingsBinding.btnInfo.setEnabled(isChecked);

            timeSettingsBinding.spTimeFormat.setEnabled(!isChecked);
            timeSettingsBinding.spDateFormat.setEnabled(!isChecked);
            timeSettingsBinding.spSeparator.setEnabled(!isChecked);
            timeSettingsBinding.cbSwapTimeDate.setEnabled(!isChecked);
        }
        refreshTimeFormat();
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        refreshTimeFormat();
    }

    @Override
    public void onClick(final View v) {
        if (v == timeSettingsBinding.btnConfirm) {
            if (onConfirmListener != null) {
                onConfirmListener.onConfirm(
                        timeSettingsBinding.cbCustomFormat.isChecked(),
                        timeSettingsBinding.spTimeFormat.getSelectedItemPosition(),
                        timeSettingsBinding.spSeparator.getSelectedItemPosition(),
                        timeSettingsBinding.spDateFormat.getSelectedItemPosition(),
                        selectedFormat,
                        timeSettingsBinding.cbSwapTimeDate.isChecked());
            }
            dismiss();
        } else if (v == timeSettingsBinding.btnInfo) {
            timeSettingsBinding.customPanel.setVisibility(timeSettingsBinding.customPanel
                    .getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

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