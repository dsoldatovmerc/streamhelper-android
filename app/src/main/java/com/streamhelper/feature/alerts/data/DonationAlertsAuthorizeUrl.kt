package com.streamhelper.feature.alerts.data

/** Scopes requested at DonationAlerts login (RN parity; the subscribe scope is requested ahead of real-time alerts). */
val DONATIONALERTS_SCOPES = listOf(
    "oauth-donation-subscribe",
    "oauth-donation-index",
)

/**
 * Builds the DonationAlerts implicit-flow authorize URL like RN `getDonationAlertsUri`, with the
 * scopes joined by a URL-encoded space (`%20`). Pure.
 */
fun buildDonationAlertsAuthorizeUrl(
    authUrl: String,
    clientId: String,
    redirectUri: String,
    scopes: List<String> = DONATIONALERTS_SCOPES,
): String =
    "$authUrl?client_id=$clientId&redirect_uri=$redirectUri&response_type=token&scope=${scopes.joinToString("%20")}"
