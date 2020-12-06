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
     * @param url HttpUrl to save cookies for
     * @param cookies List of cookies to save
     */
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (url.host().contains("schulportal.hessen.de"))
            this.cookies[url.host().substring(url.host().indexOf(".") + 1)] = cookies
        else
            this.cookies[url.host()] = cookies

    }

    /**
     * Load cookies for a domain and account for sph's weird cookie behavior
     * @param url HttpUrl to load cookies for
     * @return List of cookies for the specified domain
     */
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (url.host().contains("schulportal.hessen.de"))
            return cookies[url.host().substring(url.host().indexOf(".") + 1)] ?: ArrayList()
        else
            return cookies[url.host()] ?: ArrayList()

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