package com.streamhelper.feature.auth.data

import com.streamhelper.core.config.AppConfig
import com.streamhelper.core.network.Twitch
import com.streamhelper.core.network.TwitchClientId
import com.streamhelper.core.oauth.OAuthRequest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import retrofit2.Retrofit

/** The Twitch implicit-flow [OAuthRequest] shown by the Login screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchLogin

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    fun twitchAuthApi(@Twitch retrofit: Retrofit): TwitchAuthApi = retrofit.create(TwitchAuthApi::class.java)

    @Provides
    @TwitchLogin
    fun twitchLoginRequest(@TwitchClientId clientId: String): OAuthRequest = OAuthRequest(
        authorizeUrl = buildTwitchAuthorizeUrl(AppConfig.TWITCH_AUTH_URL, clientId, AppConfig.REDIRECT_URI),
        redirectUri = AppConfig.REDIRECT_URI,
    )
}
