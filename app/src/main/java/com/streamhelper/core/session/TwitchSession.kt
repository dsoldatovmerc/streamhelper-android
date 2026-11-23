package com.streamhelper.core.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The signed-in Twitch user (RN `UserModel`). [id] is the broadcaster/moderator id for Helix calls. */
data class TwitchUser(
    val id: String,
    val displayName: String,
    val profileImageUrl: String?,
)

data class SessionState(
    val accessToken: String? = null,
    val user: TwitchUser? = null,
)

/**
 * App-scoped, in-memory current-user session: the Twitch access token and the signed-in user.
 * Written only by the auth flow (`AuthRepository`); everything else reads it.
 * The Twitch OkHttp interceptor reads [accessToken] on every request.
 */
@Singleton
class TwitchSession @Inject constructor() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val accessToken: String? get() = _state.value.accessToken
    val user: TwitchUser? get() = _state.value.user

    /** Convenience for Helix `broadcaster_id` / `moderator_id`; `null` when signed out. */
    val userId: String? get() = user?.id

    fun setAccessToken(token: String?) = _state.update { it.copy(accessToken = token) }

    fun setUser(user: TwitchUser?) = _state.update { it.copy(user = user) }

    fun clear() {
        _state.value = SessionState()
    }
}
