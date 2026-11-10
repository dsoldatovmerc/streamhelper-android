package com.streamhelper.feature.auth.data

import com.streamhelper.core.config.AppConfig
import com.streamhelper.core.network.Twitch
import com.streamhelper.core.network.TwitchClientId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

/** Twitch OAuth (implicit flow) settings used by the Login screen. */
data class TwitchOAuthConfig(val authorizeUrl: String, val redirectUri: String)

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    fun twitchAuthApi(@Twitch retrofit: Retrofit): TwitchAuthApi = retrofit.create(TwitchAuthApi::class.java)

    @Provides
    fun twitchOAuthConfig(@TwitchClientId clientId: String): TwitchOAuthConfig = TwitchOAuthConfig(
        authorizeUrl = buildTwitchAuthorizeUrl(AppConfig.TWITCH_AUTH_URL, clientId, AppConfig.REDIRECT_URI),
        redirectUri = AppConfig.REDIRECT_URI,
    )
}
