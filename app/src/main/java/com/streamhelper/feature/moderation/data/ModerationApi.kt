package com.streamhelper.feature.moderation.data

import com.streamhelper.feature.user.HelixUsersResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Helix user lookup and ban endpoints used by the message sheet (relative to the Helix base URL). */
interface ModerationApi {
    @GET("users")
    suspend fun getUsersById(@Query("id") id: String): HelixUsersResponse

    /** Bans a user, or times them out when [BanUserRequest.Data.duration] is set. */
    @POST("moderation/bans")
    suspend fun banUser(
        @Query("broadcaster_id") broadcasterId: String,
        @Query("moderator_id") moderatorId: String,
        @Body body: BanUserRequest,
    )

    @DELETE("moderation/bans")
    suspend fun unbanUser(
        @Query("broadcaster_id") broadcasterId: String,
        @Query("moderator_id") moderatorId: String,
        @Query("user_id") userId: String,
    )
}

/** `{"data": {"user_id", "reason"?, "duration"?}}`. Null fields are left out of the JSON (`explicitNulls = false`). */
@Serializable
data class BanUserRequest(val data: Data) {
    @Serializable
    data class Data(
        @SerialName("user_id") val userId: String,
        val reason: String? = null,
        /** Timeout length in seconds; `null` for a permanent ban. */
        val duration: Int? = null,
    )
}
