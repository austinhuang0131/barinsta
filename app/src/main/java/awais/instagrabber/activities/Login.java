package awais.instagrabber.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ActivityLoginBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class Login extends BaseLanguageActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            webViewUrl = url;
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            webViewUrl = url;
            final String mainCookie = Utils.getCookie(url);
            if (Utils.isEmpty(mainCookie) || !mainCookie.contains("; ds_user_id=")) ready = true;
            else if (mainCookie.contains("; ds_user_id=") && ready) {
                Utils.setupCookies(mainCookie);
                settingsHelper.putString(Constants.COOKIE, mainCookie);
                Toast.makeText(getApplicationContext(), R.string.login_success_loading_cookies, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };
    private final WebChromeClient webChromeClient = new WebChromeClient();
    private String webViewUrl, defaultUserAgent;
    private boolean ready = false;
    private ActivityLoginBinding loginBinding;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(loginBinding.getRoot());

        initWebView();

        loginBinding.desktopMode.setOnCheckedChangeListener(this);
        loginBinding.cookies.setOnClickListener(this);
        loginBinding.refresh.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        if (v == loginBinding.refresh) {
            loginBinding.webView.loadUrl("https://instagram.com/");
        } else if (v == loginBinding.cookies) {
            final String mainCookie = Utils.getCookie(webViewUrl);
            if (Utils.isEmpty(mainCookie))
                Toast.makeText(this, R.string.login_error_loading_cookies, Toast.LENGTH_SHORT).show();
            else {
                Utils.setupCookies(mainCookie);
                settingsHelper.putString(Constants.COOKIE, mainCookie);
                Toast.makeText(this, R.string.login_success_loading_cookies, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), Main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        final WebSettings webSettings = loginBinding.webView.getSettings();

        final String newUserAgent = isChecked ? "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"
                : defaultUserAgent;

        webSettings.setUserAgentString(newUserAgent);
        webSettings.setUseWideViewPort(isChecked);
        webSettings.setLoadWithOverviewMode(isChecked);
        webSettings.setSupportZoom(isChecked);
        webSettings.setBuiltInZoomControls(isChecked);

        loginBinding.webView.loadUrl("https://instagram.com/");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        if (loginBinding != null) {
            loginBinding.webView.setWebChromeClient(webChromeClient);
            loginBinding.webView.setWebViewClient(webViewClient);
            final WebSettings webSettings = loginBinding.webView.getSettings();
            if (webSettings != null) {
                if (defaultUserAgent == null) defaultUserAgent = webSettings.getUserAgentString();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setSupportZoom(true);
                webSettings.setBuiltInZoomControls(true);
                webSettings.setDisplayZoomControls(false);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    webSettings.setAllowFileAccessFromFileURLs(true);
                    webSettings.setAllowUniversalAccessFromFileURLs(true);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }

            CookieManager.getInstance().removeAllCookies(null);
            loginBinding.webView.loadUrl("https://instagram.com/");
        }
    }

    @Override
    protected void onPause() {
        if (loginBinding != null) loginBinding.webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loginBinding != null) loginBinding.webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (loginBinding != null) loginBinding.webView.destroy();
        super.onDestroy();
    }
}