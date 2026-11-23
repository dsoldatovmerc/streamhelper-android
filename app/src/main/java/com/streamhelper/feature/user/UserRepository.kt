package com.streamhelper.feature.user

import com.streamhelper.core.session.TwitchUser
import javax.inject.Inject

/** Twitch user lookups (RN `modules/User`). */
class UserRepository @Inject constructor(private val api: UserApi) {
    /** Loads the token owner's profile from Helix `users` (`data[0]`). Throws on HTTP/parse errors or empty data. */
    suspend fun loadCurrentUser(): TwitchUser {
        val dto = api.getUsers().data.firstOrNull() ?: error("Helix /users returned no user")
        return TwitchUser(id = dto.id, displayName = dto.displayName, profileImageUrl = dto.profileImageUrl)
    }
}
