package de.koenidv.sph.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
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

        val domain = arguments?.getString("url")
                ?: "https://start.schulportal.hessen.de"


        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(webview: WebView?, url: String?) {
                // Check if login was successfull on page load
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
        // todo store aes key
        cookieManager.removeSessionCookies(null)
        cookieManager.setAcceptThirdPartyCookies(webView, true)


        // Generate access token, save as cookie and load once done
        TokenManager().generateAccessToken(object : TokenManager.TokenGeneratedListener {
            override fun onTokenGenerated(success: Int, token: String) {
                cookieManager.setCookie(domain, "sid=$token")
                webView.loadUrl(domain)
                // Show WebView and hide token loading ProgressBar
                webView.visibility = View.VISIBLE
                view.findViewById<ProgressBar>(R.id.webviewLoading).visibility = View.GONE
            }
        })


        // Only update counter if sign-in was successfull
        // prefs.edit().putLong("token_lastuse", Date().time).apply()


        return view
    }
}