package de.koenidv.sph.networking

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

//  Created by koenidv on 06.12.2020.

//  Stores cookies from network requests

object CookieStore : CookieJar {
    private val cookies: HashMap<String, List<Cookie>> = HashMap()

    /**
     * Save cookies for a domain and account for sph's weird cookie behavior
     * For .schulportal.hessen.de old cookies won't be deleted!
     * @param url HttpUrl to save cookies for
     * @param cookies List of cookies to save
     */
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (url.host().contains("schulportal.hessen.de")) {
            if (this.cookies["schulportal.hessen.de"] != null) {
                val oldAndNew = this.cookies["schulportal.hessen.de"]!!.toMutableList()
                oldAndNew.addAll(cookies)
                //this.cookies["schulportal.hessen.de"] = oldAndNew
                this.cookies["schulportal.hessen.de"] = cookies
            } else this.cookies["schulportal.hessen.de"] = cookies
        } else
            this.cookies[url.host()] = cookies

    }

    /**
     * Load cookies for a domain and account for sph's weird cookie behavior
     * @param url HttpUrl to load cookies for
     * @return List of cookies for the specified domain
     */
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return if (url.host().contains("schulportal.hessen.de"))
            cookies["schulportal.hessen.de"] ?: ArrayList()
        else
            cookies[url.host()] ?: ArrayList()

    }

    /**
     * Clears all previous cookies and sets the provided token as sph session id
     */
    fun setToken(token: String) {
        clearCookies()
        saveFromResponse(
                HttpUrl.parse("https://schulportal.hessen.de")!!,
                listOf(Cookie.Builder()
                        .domain("schulportal.hessen.de")
                        .name("sid")
                        .value(token).build()))
    }

    /**
     * Gets the current sph session id
     */
    fun getToken(): String? =
            cookies["schulportal.hessen.de"]?.find { it.name() == "sid" }?.value()

    /**
     * Deletes all cookies for all domains
     */
    fun clearCookies() {
        cookies.clear()
    }

    /**
     * Get a specific cookie for a domain
     * @param host Domain the cookie is saved for
     * @param key The cookie's name
     * @return The cookie's value or null if no matching cookie was found
     */
    fun getCookie(host: String, key: String): String? {
        return cookies[host]?.firstOrNull { it.name() == key }?.value()
    }
}