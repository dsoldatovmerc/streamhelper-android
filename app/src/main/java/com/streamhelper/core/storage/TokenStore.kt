package com.streamhelper.core.storage

/** A named secret kept in [TokenStore]. [storageKey] is the persisted name; never rename it. */
enum class CredentialKey(val storageKey: String) {
    /** Twitch user access token from the implicit OAuth flow. */
    TwitchAccessToken("twitch_access_token"),

    /** DonationAlerts access token (written by the DonationAlerts ticket, cleared on logout). */
    DonationAlertsAccessToken("donationalerts_access_token"),
}

/**
 * Encrypted credential storage. The production binding is [EncryptedTokenStore];
 * tests can bind an in-memory fake instead.
 */
interface TokenStore {
    /** The stored value, or `null` if absent or unreadable. */
    suspend fun get(key: CredentialKey): String?

    suspend fun save(key: CredentialKey, value: String)

    suspend fun clear(key: CredentialKey)
}
