package com.streamhelper.feature.chat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Helix EventSub subscription endpoints (relative to the Helix base URL). */
interface EventSubApi {
    @GET("eventsub/subscriptions")
    suspend fun getSubscriptions(): EventSubSubscriptionsResponse

    @DELETE("eventsub/subscriptions")
    suspend fun deleteSubscription(@Query("id") id: String)

    @POST("eventsub/subscriptions")
    suspend fun createSubscription(@Body body: CreateSubscriptionRequest)
}

@Serializable
data class EventSubSubscriptionsResponse(val data: List<EventSubSubscriptionDto> = emptyList())

@Serializable
data class EventSubSubscriptionDto(val id: String)

@Serializable
data class CreateSubscriptionRequest(
    val type: String,
    val version: String,
    val condition: Condition,
    val transport: Transport,
) {
    @Serializable
    data class Condition(
        @SerialName("broadcaster_user_id") val broadcasterUserId: String,
        @SerialName("user_id") val userId: String,
    )

    @Serializable
    data class Transport(
        val method: String,
        @SerialName("session_id") val sessionId: String,
    )

    companion object {
        /** `channel.chat.message` v1 for the streamer's own channel, read as the streamer. */
        fun chatMessages(userId: String, sessionId: String) = CreateSubscriptionRequest(
            type = EventSubParser.CHAT_MESSAGE_TYPE,
            version = "1",
            condition = Condition(broadcasterUserId = userId, userId = userId),
            transport = Transport(method = "websocket", sessionId = sessionId),
        )
    }
}
