package com.streamhelper.core.oauth

/** Outcome of an in-app implicit OAuth flow shown by [OAuthWebView]. */
sealed interface OAuthResult {
    /** The redirect carried `access_token` in its URL fragment. */
    data class Success(val accessToken: String) : OAuthResult

    /** The user pressed system Back on the login page. */
    data object Cancelled : OAuthResult

    /** The redirect had no `access_token` (e.g. `?error=access_denied`). */
    data object Failed : OAuthResult
}
