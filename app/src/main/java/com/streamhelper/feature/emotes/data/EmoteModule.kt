package com.streamhelper.feature.emotes.data

import com.streamhelper.core.network.Twitch
import com.streamhelper.core.session.LogoutHook
import com.streamhelper.core.session.PostLoginHook
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Base URL of the 7TV v3 API, ending in `/`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SevenTvBaseUrl

/** Marks the unauthenticated 7TV [Retrofit]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SevenTv

/**
 * 7TV endpoint. Kept separate from [SevenTvApiModule] so tests can replace just this module
 * (`@TestInstallIn`) and point 7TV at MockWebServer.
 */
@Module
@InstallIn(SingletonComponent::class)
object SevenTvConfigModule {
    @Provides
    @SevenTvBaseUrl
    fun sevenTvBaseUrl(): String = "https://7tv.io/v3/"
}

/** The 7TV Retrofit instance: shared base [OkHttpClient], no auth. */
@Module
@InstallIn(SingletonComponent::class)
object SevenTvApiModule {
    @Provides
    @Singleton
    @SevenTv
    fun sevenTvRetrofit(client: OkHttpClient, @SevenTvBaseUrl baseUrl: String, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    fun sevenTvApi(@SevenTv retrofit: Retrofit): SevenTvApi = retrofit.create(SevenTvApi::class.java)

    @Provides
    fun twitchEmoteApi(@Twitch retrofit: Retrofit): TwitchEmoteApi = retrofit.create(TwitchEmoteApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class EmoteHooksModule {
    @Binds
    @IntoSet
    abstract fun bindEmotePostLoginHook(impl: EmoteRepository): PostLoginHook

    @Binds
    @IntoSet
    abstract fun bindEmoteLogoutHook(impl: EmoteRepository): LogoutHook
}
