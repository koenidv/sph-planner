package de.koenidv.sph.ui.links

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.*

class LinksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_links, container, false)



        val webView = view.findViewById<WebView>(R.id.webview)
        val cookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()

        val domain = "https://start.schulportal.hessen.de"

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        cookieManager.removeSessionCookies(null)
        cookieManager.setCookie(domain,"sid=" + prefs.getString("token", ""))
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        Log.d(SphPlanner.TAG, cookieManager.getCookie(domain))

        WebView.setWebContentsDebuggingEnabled(true)
        webView.loadUrl(domain)

        prefs.edit().putLong("token_lastuse", Date().time).apply()


        return view
    }


}