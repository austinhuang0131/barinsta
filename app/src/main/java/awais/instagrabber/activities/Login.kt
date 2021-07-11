package awais.instagrabber.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import android.widget.Toast
import awais.instagrabber.R
import awais.instagrabber.databinding.ActivityLoginBinding
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.getCookie

class Login : BaseLanguageActivity(), View.OnClickListener {
    private var webViewUrl: String? = null
    private var ready = false
    private lateinit var loginBinding: ActivityLoginBinding

    private val webChromeClient = WebChromeClient()
    private val webViewClient: WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            webViewUrl = url
        }

        override fun onPageFinished(view: WebView, url: String) {
            webViewUrl = url
            val mainCookie = getCookie(url)
            if (mainCookie.isNullOrBlank() || !mainCookie.contains("; ds_user_id=")) {
                ready = true
                return
            }
            if (mainCookie.contains("; ds_user_id=") && ready) {
                returnCookieResult(mainCookie)
            }
        }
    }

    private fun returnCookieResult(mainCookie: String?) {
        val intent = Intent()
        intent.putExtra("cookie", mainCookie)
        setResult(Constants.LOGIN_RESULT_CODE, intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginBinding = ActivityLoginBinding.inflate(LayoutInflater.from(applicationContext))
        setContentView(loginBinding.root)
        initWebView()
        loginBinding.cookies.setOnClickListener(this)
        loginBinding.refresh.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v === loginBinding.refresh) {
            loginBinding.webView.loadUrl("https://instagram.com/")
            return
        }
        if (v === loginBinding.cookies) {
            val mainCookie = getCookie(webViewUrl)
            if (mainCookie.isNullOrBlank() || !mainCookie.contains("; ds_user_id=")) {
                Toast.makeText(this, R.string.login_error_loading_cookies, Toast.LENGTH_SHORT).show()
                return
            }
            returnCookieResult(mainCookie)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        loginBinding.webView.webChromeClient = webChromeClient
        loginBinding.webView.webViewClient = webViewClient
        val webSettings = loginBinding.webView.settings
        webSettings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.105 Mobile Safari/537.36"
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } else {
            val cookieSyncMngr = CookieSyncManager.createInstance(applicationContext)
            cookieSyncMngr.startSync()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncMngr.stopSync()
            cookieSyncMngr.sync()
        }
        loginBinding.webView.loadUrl("https://instagram.com/")
    }

    override fun onPause() {
        loginBinding.webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        loginBinding.webView.onResume()
    }

    override fun onDestroy() {
        loginBinding.webView.destroy()
        super.onDestroy()
    }
}