package com.streamhelper.feature.chat.data

/**
 * One Twitch chat message (RN `ChatMessage`), built from a `channel.chat.message` EventSub event.
 *
 * @property id Twitch `message_id`; unique, used as the list key and for moderation (delete).
 * @property authorId `chatter_user_id`; the target of ban/unban/timeout.
 * @property author `chatter_user_name` (display name).
 * @property text Plain message text. The emotes ticket splits it into emote segments at render time.
 * @property color Author's name color as `#RRGGBB`; empty when the chatter hasn't set one.
 * @property isDeleted Set by moderation once the message is deleted
 *   (see [ChatRepository.markDeleted]); rendered faded.
 */
data class ChatMessage(
    val id: String,
    val authorId: String,
    val author: String,
    val text: String,
    val color: String,
    val isDeleted: Boolean = false,
)
