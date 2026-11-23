package com.streamhelper.feature.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** Helix users endpoints (relative to the Helix base URL). */
interface UserApi {
    /** Without query params, returns the user that owns the access token. */
    @GET("users")
    suspend fun getUsers(): HelixUsersResponse
}

@Serializable
data class HelixUsersResponse(val data: List<HelixUserDto> = emptyList())

@Serializable
data class HelixUserDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
)
