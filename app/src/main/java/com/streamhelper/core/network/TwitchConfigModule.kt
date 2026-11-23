package com.streamhelper.core.network

import com.streamhelper.core.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

/** Base URL of Twitch Helix, ending in `/` (Retrofit base URL of the Twitch client). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchHelixBaseUrl

/** Base URL of Twitch's `id.twitch.tv/oauth2/`, ending in `/` (validate, revoke). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchIdBaseUrl

/** Twitch application client id, sent as `Client-Id` and used in authorize/revoke URLs. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchClientId

/**
 * Twitch endpoints and identity. Kept separate from [TwitchApiModule] so tests can replace just
 * this module (`@TestInstallIn`) and point every Twitch call at MockWebServer.
 */
@Module
@InstallIn(SingletonComponent::class)
object TwitchConfigModule {
    @Provides
    @TwitchHelixBaseUrl
    fun helixBaseUrl(): String = "https://api.twitch.tv/helix/"

    @Provides
    @TwitchIdBaseUrl
    fun idBaseUrl(): String = "https://id.twitch.tv/oauth2/"

    @Provides
    @TwitchClientId
    fun clientId(): String = AppConfig.TWITCH_CLIENT_ID
}
