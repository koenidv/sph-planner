package de.koenidv.sph.ui

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
import de.koenidv.sph.networking.TokenManager

// Created by koenidv on 18.12.2020.
class MessagesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        // WebView - Only for demonstration

        val webView = view.findViewById<WebView>(R.id.webview)
        val cookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()

        val domain = "https://start.schulportal.hessen.de"

        webView.webViewClient = WebViewClient()
        webView.settings.domStorageEnabled = true
        webView.settings.builtInZoomControls = true
        cookieManager.removeSessionCookies(null)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        WebView.setWebContentsDebuggingEnabled(true)

        // Generate access token, save as cookie and load once done
        TokenManager().generateAccessToken(object : TokenManager.TokenGeneratedListener {
            override fun onTokenGenerated(success: Int, token: String) {
                cookieManager.setCookie(domain, "sid=$token")
                webView.loadUrl(domain)
                webView.visibility = View.VISIBLE
                view.findViewById<ProgressBar>(R.id.webviewLoading)?.visibility = View.GONE
            }
        })

        // Only update counter if sign-in was successfull
        // prefs.edit().putLong("token_lastuse", Date().time).apply()


        return view
    }


}