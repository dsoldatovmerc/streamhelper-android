package com.streamhelper.feature.moderation.data

import com.streamhelper.core.session.TwitchSession
import javax.inject.Inject

/** A chatter's public profile, shown in the message sheet header. */
data class Chatter(
    val id: String,
    val displayName: String,
    val profileImageUrl: String?,
)

/**
 * Chatter lookup, ban, timeout and unban on the streamer's own channel (RN `modules/User`).
 * The signed-in streamer is both broadcaster and moderator. Every call throws on HTTP or parse
 * errors and when signed out. Message deletion lives in `ChatRepository`, which owns the list.
 */
class ModerationRepository @Inject constructor(
    private val api: ModerationApi,
    private val session: TwitchSession,
) {
    /** Helix `users?id=` (`data[0]`). Throws when Twitch returns no user. */
    suspend fun getUserById(id: String): Chatter {
        val dto = api.getUsersById(id).data.firstOrNull() ?: error("Helix /users returned no user for $id")
        return Chatter(id = dto.id, displayName = dto.displayName, profileImageUrl = dto.profileImageUrl)
    }

    /** Permanent ban when [durationSeconds] is `null`, otherwise a timeout. No reason is sent (RN parity). */
    suspend fun ban(userId: String, durationSeconds: Int? = null) {
        val me = requireUserId()
        api.banUser(me, me, BanUserRequest(BanUserRequest.Data(userId = userId, duration = durationSeconds)))
    }

    suspend fun unban(userId: String) {
        val me = requireUserId()
        api.unbanUser(me, me, userId)
    }

    private fun requireUserId(): String = session.userId ?: error("Not signed in")
}
