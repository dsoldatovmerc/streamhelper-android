package com.streamhelper.feature.alerts.data

import com.streamhelper.core.session.LogoutHook
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

/** Marks the authenticated DonationAlerts [OkHttpClient] and [Retrofit], separate from the Twitch ones. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DonationAlerts

/** The DonationAlerts Retrofit instance (base URL from [DonationAlertsBaseUrl]) and its endpoints. */
@Module
@InstallIn(SingletonComponent::class)
object DonationAlertsApiModule {
    @Provides
    @Singleton
    @DonationAlerts
    fun donationAlertsOkHttpClient(base: OkHttpClient, authInterceptor: DonationAlertsAuthInterceptor): OkHttpClient =
        base.newBuilder().addInterceptor(authInterceptor).build()

    @Provides
    @Singleton
    @DonationAlerts
    fun donationAlertsRetrofit(
        @DonationAlerts client: OkHttpClient,
        @DonationAlertsBaseUrl baseUrl: String,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    fun alertsApi(@DonationAlerts retrofit: Retrofit): AlertsApi = retrofit.create(AlertsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DonationAlertsHooksModule {
    /** Twitch logout also drops the in-memory DonationAlerts token. */
    @Binds
    @IntoSet
    abstract fun bindSessionLogoutHook(impl: DonationAlertsSession): LogoutHook
}
