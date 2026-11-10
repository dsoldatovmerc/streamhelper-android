package com.streamhelper.feature.auth.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/** `id.twitch.tv/oauth2` endpoints. URLs are absolute, built from `@TwitchIdBaseUrl`. */
interface TwitchAuthApi {
    /** Any 2xx means the token (sent by the interceptor) is valid. */
    @GET
    suspend fun validate(@Url url: String): Response<Unit>

    @POST
    suspend fun revoke(
        @Url url: String,
        @Query("client_id") clientId: String,
        @Query("token") token: String,
    ): Response<Unit>
}
