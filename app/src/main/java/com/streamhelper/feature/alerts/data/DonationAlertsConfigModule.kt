package com.streamhelper.feature.alerts.data

import com.streamhelper.core.config.AppConfig
import com.streamhelper.core.oauth.OAuthRequest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

/** Base URL of the DonationAlerts API v1, ending in `/` (Retrofit base URL of the DonationAlerts client). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DonationAlertsBaseUrl

/** The DonationAlerts implicit-flow [OAuthRequest] shown from the Alerts tab. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DonationAlertsLogin

/**
 * DonationAlerts endpoints and login. Kept separate from [DonationAlertsApiModule] so tests can
 * replace just this module (`@TestInstallIn`) and point DonationAlerts calls at MockWebServer.
 */
@Module
@InstallIn(SingletonComponent::class)
object DonationAlertsConfigModule {
    @Provides
    @DonationAlertsBaseUrl
    fun baseUrl(): String = "https://www.donationalerts.com/api/v1/"

    @Provides
    @DonationAlertsLogin
    fun loginRequest(): OAuthRequest = OAuthRequest(
        authorizeUrl = buildDonationAlertsAuthorizeUrl(
            authUrl = AppConfig.DONATIONALERTS_AUTH_URL,
            clientId = AppConfig.DONATIONALERTS_CLIENT_ID,
            redirectUri = AppConfig.REDIRECT_URI,
        ),
        redirectUri = AppConfig.REDIRECT_URI,
    )
}
