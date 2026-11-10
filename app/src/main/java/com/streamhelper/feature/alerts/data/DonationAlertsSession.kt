package com.streamhelper.feature.alerts.data

import com.streamhelper.core.session.LogoutHook
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped, in-memory DonationAlerts access token. Written only by [DonationAlertsAuthRepository];
 * read by [DonationAlertsAuthInterceptor] on every request (no DataStore read per request).
 *
 * Registered as a [LogoutHook]: Twitch logout clears the stored token in `AuthRepository` and this
 * clears the in-memory copy, so an open Alerts tab falls back to "Log in".
 */
@Singleton
class DonationAlertsSession @Inject constructor() : LogoutHook {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun setAccessToken(token: String?) {
        _accessToken.value = token
    }

    override suspend fun onLoggedOut() = setAccessToken(null)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DonationAlertsHooksModule {
    /** Twitch logout also drops the in-memory DonationAlerts token. */
    @Binds
    @IntoSet
    abstract fun bindSessionLogoutHook(impl: DonationAlertsSession): LogoutHook
}
