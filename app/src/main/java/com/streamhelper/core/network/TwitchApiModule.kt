package com.streamhelper.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Marks the authenticated Twitch [OkHttpClient] and [Retrofit] (Helix + `id.twitch.tv/oauth2`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Twitch

/**
 * The single authenticated Twitch Retrofit instance. Its base URL is [TwitchHelixBaseUrl], so
 * Helix endpoints use relative paths (`@GET("users")`); `id.twitch.tv` endpoints pass an absolute
 * `@Url` built from [TwitchIdBaseUrl].
 *
 * To add endpoints: declare a Retrofit interface and provide it from `@Twitch Retrofit`, e.g.
 * `@Provides fun chatApi(@Twitch retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)`.
 */
@Module
@InstallIn(SingletonComponent::class)
object TwitchApiModule {
    @Provides
    @Singleton
    @Twitch
    fun twitchOkHttpClient(base: OkHttpClient, authInterceptor: TwitchAuthInterceptor): OkHttpClient =
        base.newBuilder().addInterceptor(authInterceptor).build()

    @Provides
    @Singleton
    @Twitch
    fun twitchRetrofit(
        @Twitch client: OkHttpClient,
        @TwitchHelixBaseUrl baseUrl: String,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
