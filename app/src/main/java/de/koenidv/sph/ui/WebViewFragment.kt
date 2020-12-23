package de.koenidv.sph.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.*
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import java.util.*


// Created by koenidv on 18.12.2020.
class WebViewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_webview, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        // Set up WebView to show a page
        val webView = view.findViewById<WebView>(R.id.webview)
        val cookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()

        // Get passed url argument
        val domain = arguments?.getString("url")
                ?: "https://start.schulportal.hessen.de"

        // Set a client for the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(webview: WebView?, url: String?) {
                // Don't show WebView until page is loaded
                webView.visibility = View.VISIBLE
                view.findViewById<ProgressBar>(R.id.webviewLoading)?.visibility = View.GONE
                // Check if login was successful on page load
                webView.evaluateJavascript(
                        "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
                ) { html ->
                    if (!html.contains("Login - Schulportal Hessen")) {
                        // Login was successful, save last token usage
                        prefs.edit().putLong("token_last_success", Date().time).apply()
                    }
                }
            }
        }
        /* js might introduce xss vulnerabilities
         * We need it to display some sites
         * Site can access all @JavascriptInterface public methods
         * todo Whitelist for js sites
         */
        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        // Fix zoom issue
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.setInitialScale(1)
        // todo store aes key
        // Remove previous sph cookies as they might lead to problems
        cookieManager.setCookie(".schulportal.hessen.de", "")
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Enable force dark mode above Android 10 if dark theme is selected
        if (VERSION.SDK_INT >= VERSION_CODES.Q
                && webView.isForceDarkAllowed
                && ((prefs.contains("forceDark") && prefs.getBoolean("forceDark", true))
                        || resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)) {
            webView.settings.forceDark = WebSettings.FORCE_DARK_ON
        }

        // Enable using back button for webView
        webView.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK && !webView.canGoBack()) {
                // Ignore this listener if webView can't go back
                false
            } else if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == MotionEvent.ACTION_UP) {
                // Go back in webView and ignore button press
                webView.goBack()
                true
            } else true // Ignore this listener if any other button was pressed
        }

        // If target is a sph page, generate access token, save as cookie and load once done
        // else just load the page
        if (domain.contains("schulportal.hessen.de")) {
            TokenManager().generateAccessToken { success: Int, token: String? ->
                if (success == NetworkManager().SUCCESS) {
                    cookieManager.setCookie(domain, "sid=$token")
                    webView.loadUrl(domain)
                }
            }
        } else {
            webView.loadUrl(domain)
        }

        return view
    }
}