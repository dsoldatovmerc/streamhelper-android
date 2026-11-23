package com.streamhelper.feature.alerts.data

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <token>` to every DonationAlerts request while a token is in
 * [DonationAlertsSession]. Requests without a token pass through unchanged. Separate from the Twitch token.
 */
class DonationAlertsAuthInterceptor @Inject constructor(
    private val session: DonationAlertsSession,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.accessToken.value
        if (token.isNullOrBlank()) return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
