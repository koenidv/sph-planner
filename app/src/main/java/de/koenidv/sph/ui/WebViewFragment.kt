package de.koenidv.sph.ui

import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import java.util.*


// Created by koenidv on 18.12.2020.
class WebViewFragment : Fragment() {

    lateinit var webView: WebView

    // Reload webvoew whenever the broadcast "uichange" with content=webview is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            webView.reload()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_webview, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        // Set up WebView to show a page
        webView = view.findViewById(R.id.webview)
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
                // Set action bar title
                if (activity != null) (activity as AppCompatActivity).supportActionBar?.title = webView.title
                // Update open in browser url
                SphPlanner.openInBrowserUrl = webView.url
                // Check if login was successful on page load
                webView.evaluateJavascript(
                        "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
                ) { html ->
                    if (!html.contains("Login - Schulportal Hessen")
                            && !html.contains("Schulauswahl")) {
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
        // Remove previous sph cookies as they might lead to problems
        /*listOf("https://schulportal.hessen.de",
                "schulportal.hessen.de",
                "https://connect.schulportal.hessen.de",
                "connect.schulportal.hessen.de",
                "https://www.schulportal.hessen.de",
                "www.schulportal.hessen.de",
                "https://sync.schulportal.hessen.de",
                "sync.schulportal.hessen.de",
                "https://login.schulportal.hessen.de",
                "login.schulportal.hessen.de",
                "https://start.schulportal.hessen.de",
                "start.schulportal.hessen.de",
                ".schulportal.hessen.de").forEach {
            cookieManager.setCookie(it, "")
        }*/
        // todo Removing only sph's cookies somehow doesn't work, so we'll remove all session cookies
        // This will also remove any login to other sites..
        cookieManager.removeSessionCookies {}
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Enable force dark mode above Android 10 if dark theme is selected
        if ((!prefs.contains("forceDark") || prefs.getBoolean("forceDark", true)
                        && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(webView.settings, WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING)
            }
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
            TokenManager.authenticate { success: Int, token: String? ->
                if (success == NetworkManager.SUCCESS) {
                    Log.d(SphPlanner.TAG, token!!)
                    cookieManager.setCookie(domain, "sid=$token")
                    webView.loadUrl(domain)
                }
                // todo handle errors
            }
        } else {
            webView.loadUrl(domain)
        }

        return view
    }

    override fun onStop() {
        // Stop loading any page
        webView.stopLoading()
        super.onStop()
    }

    override fun onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
        super.onDestroy()
        // Recreate activity if this was the first time using webview during this application lifecycle
        // This is to work around a weird bug in WebView causing the night theme configuration to be changed
        if (SphPlanner.applicationContext()
                        .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                        .getBoolean("force_webview_theme_fix", true)
                && !SphPlanner.webViewFixed) {
            SphPlanner.webViewFixed = true
            Log.d(SphPlanner.TAG, "Activity recreated to force fix webview theme bug")
            requireActivity().recreate()
        }
    }
}