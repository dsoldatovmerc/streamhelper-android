package com.streamhelper.feature.alerts.data

import android.util.Log
import com.streamhelper.core.storage.CredentialKey
import com.streamhelper.core.storage.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * DonationAlerts auth (RN `modules/DonationAlertsAuth`): the stored token and the in-memory session.
 * The only writer of [DonationAlertsSession]. Clearing on Twitch logout happens in `AuthRepository`
 * (storage) and [DonationAlertsSession.onLoggedOut] (memory).
 */
@Singleton
class DonationAlertsAuthRepository @Inject constructor(
    private val tokenStore: TokenStore,
    private val session: DonationAlertsSession,
) {
    /** Whether a DonationAlerts token is held in memory. Turns `false` on Twitch logout. */
    val isLoggedIn: Flow<Boolean> = session.accessToken.map { it != null }.distinctUntilChanged()

    /** Reads the stored DonationAlerts token into the session. Returns `null` if missing, blank or unreadable. */
    suspend fun restoreToken(): String? {
        val token = try {
            tokenStore.get(CredentialKey.DonationAlertsAccessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Reading stored DonationAlerts token failed", e)
            null
        }?.takeIf { it.isNotBlank() }
        session.setAccessToken(token)
        return token
    }

    /** Stores a freshly obtained token and puts it in the session. Throws if storing fails. */
    suspend fun saveToken(token: String) {
        tokenStore.save(CredentialKey.DonationAlertsAccessToken, token)
        session.setAccessToken(token)
    }

    private companion object {
        const val TAG = "DonationAlertsAuth"
    }
}
