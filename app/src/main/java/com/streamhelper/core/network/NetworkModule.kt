package com.streamhelper.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/** Backend-agnostic networking: one [Json] and one base [OkHttpClient] that per-backend clients derive from. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Shared connection pool / dispatcher. Derive backend clients with `newBuilder()`. */
    @Provides
    @Singleton
    fun baseOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}
