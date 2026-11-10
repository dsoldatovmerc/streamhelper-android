package com.streamhelper.feature.auth.data

import android.util.Log
import com.streamhelper.core.network.TwitchClientId
import com.streamhelper.core.network.TwitchIdBaseUrl
import com.streamhelper.core.session.SessionHooks
import com.streamhelper.core.session.TwitchSession
import com.streamhelper.core.session.TwitchUser
import com.streamhelper.core.storage.CredentialKey
import com.streamhelper.core.storage.TokenStore
import com.streamhelper.feature.user.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Twitch auth (RN `modules/TwitchAuth`): stored token, validation, sign-in and logout.
 * The only writer of [TwitchSession].
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: TwitchAuthApi,
    private val tokenStore: TokenStore,
    private val session: TwitchSession,
    private val userRepository: UserRepository,
    private val hooks: SessionHooks,
    @TwitchIdBaseUrl private val idBaseUrl: String,
    @TwitchClientId private val clientId: String,
) {
    /** Reads the stored Twitch token into the session. Returns `null` if missing or blank. */
    suspend fun restoreToken(): String? {
        val token = tokenStore.get(CredentialKey.TwitchAccessToken)?.takeIf { it.isNotBlank() }
        session.setAccessToken(token)
        return token
    }

    /**
     * Validates the session token with Twitch. Any 2xx is valid; any error or non-2xx is not,
     * and the dead token is dropped from the session (storage is left as is, like RN).
     */
    suspend fun validateToken(): Boolean {
        val valid = runCatchingNonCancel {
            api.validate("${idBaseUrl}validate").isSuccessful
        }.getOrDefault(false)
        if (!valid) session.setAccessToken(null)
        return valid
    }

    /** Stores a freshly obtained token and puts it in the session. */
    suspend fun saveToken(token: String) {
        tokenStore.save(CredentialKey.TwitchAccessToken, token)
        session.setAccessToken(token)
    }

    /**
     * Loads the current user into the session and, on success, fires the post-login hooks.
     * Requires a token in the session.
     */
    suspend fun loadUser(): Result<TwitchUser> = runCatchingNonCancel {
        userRepository.loadCurrentUser()
    }.onSuccess { user ->
        session.setUser(user)
        hooks.dispatchPostLogin(user)
    }.onFailure { Log.w(TAG, "Loading current user failed", it) }

    /**
     * Revokes the token with Twitch, then — regardless of the result — clears the stored Twitch and
     * DonationAlerts tokens and the session, and runs the logout hooks.
     * Returns whether revoke succeeded.
     */
    suspend fun logout(): Boolean {
        val token = session.accessToken.orEmpty()
        val revoked = runCatchingNonCancel {
            api.revoke("${idBaseUrl}revoke", clientId, token).isSuccessful
        }.getOrDefault(false)

        // Local cleanup must finish even if the caller is cancelled mid-logout.
        withContext(NonCancellable) {
            tokenStore.clear(CredentialKey.TwitchAccessToken)
            tokenStore.clear(CredentialKey.DonationAlertsAccessToken)
            session.clear()
            hooks.runLogoutHooks()
        }
        return revoked
    }

    private inline fun <T> runCatchingNonCancel(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
