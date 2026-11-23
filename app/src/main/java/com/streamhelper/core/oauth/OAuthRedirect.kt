package com.streamhelper.core.oauth

import java.net.URLDecoder

/**
 * Pure (no Android) helpers for the implicit OAuth flow, shared by Twitch and DonationAlerts.
 */
object OAuthRedirect {

    /** True when [url] is a navigation to [redirectUri] that must be intercepted. A blank [redirectUri] never matches. */
    fun isRedirect(url: String, redirectUri: String): Boolean =
        redirectUri.isNotBlank() && url.startsWith(redirectUri)

    /**
     * Parses a navigation [url]. Returns `null` if it isn't the redirect (let the WebView load it),
     * [OAuthResult.Success] if the URL fragment has a non-blank `access_token`, otherwise [OAuthResult.Failed].
     */
    fun parse(url: String, redirectUri: String): OAuthResult? {
        if (!isRedirect(url, redirectUri)) return null
        val token = fragmentParam(url, "access_token")
        return if (token.isNullOrBlank()) OAuthResult.Failed else OAuthResult.Success(token)
    }

    /** Value of [name] in the `#a=1&b=2` fragment of [url], URL-decoded, or `null`. */
    fun fragmentParam(url: String, name: String): String? {
        val fragment = url.substringAfter('#', missingDelimiterValue = "")
        if (fragment.isEmpty()) return null
        return fragment.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it[0] == name }
            ?.getOrNull(1)
            ?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) }
    }
}
