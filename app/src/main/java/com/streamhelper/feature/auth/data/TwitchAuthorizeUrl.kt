package com.streamhelper.feature.auth.data

/** Scopes requested at Twitch login (RN parity; polls/predictions/broadcast are requested ahead of those features). */
val TWITCH_SCOPES = listOf(
    "channel:manage:broadcast",
    "channel:manage:polls",
    "channel:manage:predictions",
    "user:read:chat",
    "user:write:chat",
    "moderator:manage:banned_users",
    "moderator:manage:chat_messages",
    "moderation:read",
)

/** Builds the implicit-flow authorize URL exactly like RN `getAuthUri` (scopes joined with `+`). Pure. */
fun buildTwitchAuthorizeUrl(
    authUrl: String,
    clientId: String,
    redirectUri: String,
    scopes: List<String> = TWITCH_SCOPES,
): String =
    "$authUrl?client_id=$clientId&redirect_uri=$redirectUri&response_type=token&scope=${scopes.joinToString("+")}"
