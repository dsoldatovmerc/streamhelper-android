package com.streamhelper.feature.emotes.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Helix chat emote endpoints (relative to the Helix base URL). */
interface TwitchEmoteApi {
    @GET("chat/emotes/global")
    suspend fun getGlobalEmotes(): HelixEmotesResponse

    @GET("chat/emotes")
    suspend fun getChannelEmotes(@Query("broadcaster_id") broadcasterId: String): HelixEmotesResponse
}

/**
 * [template] is the CDN URL template, e.g.
 * `https://static-cdn.jtvnw.net/emoticons/v2/{{id}}/{{format}}/{{theme_mode}}/{{scale}}`.
 */
@Serializable
data class HelixEmotesResponse(
    val data: List<HelixEmoteDto> = emptyList(),
    val template: String = "",
)

/** [format] lists the available formats: `static`, and `animated` for animated emotes. */
@Serializable
data class HelixEmoteDto(
    val id: String,
    val name: String,
    val format: List<String> = emptyList(),
)
