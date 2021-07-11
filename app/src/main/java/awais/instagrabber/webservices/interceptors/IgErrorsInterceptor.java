package awais.instagrabber.webservices.interceptors;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.TextUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class IgErrorsInterceptor implements Interceptor {
    private static final String TAG = IgErrorsInterceptor.class.getSimpleName();

    public IgErrorsInterceptor() { }

    @NonNull
    @Override
    public Response intercept(@NonNull final Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        if (response.isSuccessful()) {
            return response;
        }
        checkError(response);
        return response;
    }

    private void checkError(@NonNull final Response response) {
        final int errorCode = response.code();
        switch (errorCode) {
            case 429: // "429 Too Many Requests"
                // ('Throttled by Instagram because of too many API requests.');
                showErrorDialog(R.string.throttle_error);
                return;
            case 431: // "431 Request Header Fields Too Large"
                // show dialog?
                Log.e(TAG, "Network error: " + getMessage(errorCode, "The request start-line and/or headers are too large to process."));
                return;
            case 404:
                showErrorDialog(R.string.not_found);
                return;
            case 302: // redirect
                final String location = response.header("location");
                if (location != null && location.equals("https://www.instagram.com/accounts/login/")) {
                    // rate limited
                    final String message = MainActivity.getInstance().getString(R.string.rate_limit);
                    final Spanned spanned = Html.fromHtml(message);
                    showErrorDialog(spanned);
                }
                return;
        }
        final ResponseBody body = response.body();
        if (body == null) return;
        try {
            final String bodyString = body.string();
            Log.d(TAG, "checkError: " + bodyString);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(bodyString);
            } catch (JSONException e) {
                Log.e(TAG, "checkError: ", e);
            }
            String message;
            if (jsonObject != null) {
                message = jsonObject.optString("message");
            } else {
                message = bodyString;
            }
            if (!TextUtils.isEmpty(message)) {
                message = message.toLowerCase();
                switch (message) {
                    case "user_has_logged_out":
                        showErrorDialog(R.string.account_logged_out);
                        return;
                    case "login_required":
                        showErrorDialog(R.string.login_required);
                        return;
                    case "execution failure":
                        showSnackbar(message);
                        return;
                    case "not authorized to view user": // Do we handle this in profile view fragment?
                    case "challenge_required": // Since we make users login using browser, we should not be getting this error in api requests
                    default:
                        showSnackbar(message);
                        Log.e(TAG, "checkError: " + bodyString);
                        return;
                }
            }
            final String errorType = jsonObject.optString("error_type");
            if (TextUtils.isEmpty(errorType)) return;
            if (errorType.equals("sentry_block")) {
                showErrorDialog("\"sentry_block\". Please contact developers.");
                return;
            }
            if (errorType.equals("inactive user")) {
                showErrorDialog(R.string.inactive_user);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkError: ", e);
        }
    }

    private void showSnackbar(final String message) {
        final MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity == null) return;
        // final View view = mainActivity.getRootView();
        // if (view == null) return;
        try {
            AppExecutors.INSTANCE
                    .getMainThread()
                    .execute(() -> Toast.makeText(mainActivity.getApplicationContext(), message, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Log.e(TAG, "showSnackbar: ", e);
        }
    }

    @NonNull
    private String getMessage(final int errorCode, final String message) {
        return String.format("code: %s, internalMessage: %s", errorCode, message);
    }

    private void showErrorDialog(@NonNull final CharSequence message) {
        final MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity == null) return;
        final FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) return;
        final ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                Constants.GLOBAL_NETWORK_ERROR_DIALOG_REQUEST_CODE,
                R.string.error,
                message,
                R.string.ok,
                0,
                0
        );
        dialogFragment.show(fragmentManager, "network_error_dialog");
    }

    private void showErrorDialog(@StringRes final int messageResId) {
        showErrorDialog(MainActivity.getInstance().getString(messageResId));
    }

    public void destroy() {
        // mainActivity = null;
    }
}