package com.streamhelper.core.network

import com.streamhelper.core.session.TwitchSession
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Client-Id` and `Authorization: Bearer <token>` to every Twitch request while a token is
 * in [TwitchSession] (in memory, so no DataStore read per request). Requests without a token pass through unchanged.
 */
class TwitchAuthInterceptor @Inject constructor(
    private val session: TwitchSession,
    @TwitchClientId private val clientId: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.accessToken
        if (token.isNullOrBlank()) return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Client-Id", clientId)
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
