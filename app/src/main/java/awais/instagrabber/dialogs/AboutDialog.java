package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import awais.instagrabber.R;
import awais.instagrabber.utils.Utils;

public final class AboutDialog extends BottomSheetDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        final View contentView = View.inflate(getContext(), R.layout.dialog_main_about, null);

        final LinearLayoutCompat infoContainer = contentView.findViewById(R.id.infoContainer);

        final View btnTelegram = infoContainer.getChildAt(1);
        final View btnMatrix = infoContainer.getChildAt(2);
        final View btnProject = infoContainer.getChildAt(3);
        final View.OnClickListener onClickListener = v -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            if (v == btnTelegram) {
                intent.setData(Uri.parse("https://t.me/grabber_app"));
                if (!Utils.isEmpty(Utils.telegramPackage))
                    intent.setPackage(Utils.telegramPackage);
            }
            else if (v == btnMatrix) {
                intent.setData(Uri.parse("https://matrix.to/#/#instagrabber:matrix.org"));
            } else
                intent.setData(Uri.parse("https://instagrabber.austinhuang.me"));
            startActivity(intent);
        };
        btnProject.setOnClickListener(onClickListener);
        btnTelegram.setOnClickListener(onClickListener);
        btnMatrix.setOnClickListener(onClickListener);

        final String description = getString(R.string.description);
        if (!Utils.isEmpty(description)) {
            final SpannableStringBuilder descriptionText = new SpannableStringBuilder(description, 0, description.length());

            int lastIndex = descriptionText.length() / 2;
            for (int i = 0; i < descriptionText.length(); ++i) {
                char c = descriptionText.charAt(i);

                if (c == '[') {
                    final int smallTextStart = i;
                    descriptionText.delete(i, i + 1);

                    do {
                        c = descriptionText.charAt(i);
                        if (c == ']') {
                            descriptionText.delete(i, i + 1);
                            descriptionText.setSpan(new RelativeSizeSpan(0.6f), smallTextStart, i, 0);
                        }
                        ++i;
                    } while (c != ']' || i == descriptionText.length() - 1);
                }
            }

            lastIndex = Utils.indexOfChar(descriptionText, '@', lastIndex) - 12;
            descriptionText.setSpan(new URLSpan("mailto:instagrabber@austinhuang.me"), lastIndex, lastIndex + 27, 0);

            final TextView textView = (TextView) infoContainer.getChildAt(0);
            textView.setMovementMethod(new LinkMovementMethod());
            textView.setText(descriptionText, TextView.BufferType.SPANNABLE);
        }

        dialog.setContentView(contentView);
        return dialog;
    }
}