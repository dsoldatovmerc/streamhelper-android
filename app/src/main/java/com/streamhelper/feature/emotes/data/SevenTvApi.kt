package com.streamhelper.feature.emotes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/** 7TV v3 API (relative to [SevenTvBaseUrl]). No auth. */
interface SevenTvApi {
    /** The 7TV user linked to a Twitch account, with their active emote set. 404 if there is none. */
    @GET("users/twitch/{twitchUserId}")
    suspend fun getUserByTwitchId(@Path("twitchUserId") twitchUserId: String): SevenTvUserDto
}

@Serializable
data class SevenTvUserDto(@SerialName("emote_set") val emoteSet: SevenTvEmoteSetDto? = null)

@Serializable
data class SevenTvEmoteSetDto(val emotes: List<SevenTvEmoteDto>? = null)

@Serializable
data class SevenTvEmoteDto(val name: String, val data: SevenTvEmoteDataDto? = null)

@Serializable
data class SevenTvEmoteDataDto(val host: SevenTvHostDto? = null)

/** [url] is protocol-relative (`//cdn.7tv.app/emote/<id>`). */
@Serializable
data class SevenTvHostDto(val url: String, val files: List<SevenTvFileDto> = emptyList())

@Serializable
data class SevenTvFileDto(val name: String, val width: Int, val height: Int)
