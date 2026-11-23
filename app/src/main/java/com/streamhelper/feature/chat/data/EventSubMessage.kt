package com.streamhelper.feature.chat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A parsed EventSub WebSocket frame, keyed on `metadata.message_type`. */
sealed interface EventSubMessage {
    /** `session_welcome`: the session is ready; subscriptions must name [sessionId]. */
    data class Welcome(val sessionId: String, val keepaliveTimeoutSeconds: Int?) : EventSubMessage

    /** `notification` of type `channel.chat.message`. */
    data class ChatMessageReceived(val message: ChatMessage) : EventSubMessage

    /** `session_keepalive`: nothing to do except reset the keepalive timer. */
    data object Keepalive : EventSubMessage

    /** `session_reconnect`: Twitch is migrating the session; connect to [reconnectUrl]. */
    data class Reconnect(val reconnectUrl: String) : EventSubMessage

    /** `revocation`: Twitch revoked a subscription (e.g. the token lost a scope). */
    data class Revocation(val subscriptionType: String?, val status: String?) : EventSubMessage

    /** Any other message type, or a notification we don't subscribe to. */
    data class Other(val messageType: String) : EventSubMessage
}

/** Pure EventSub frame parsing (no Android dependencies). */
object EventSubParser {
    const val CHAT_MESSAGE_TYPE = "channel.chat.message"

    /**
     * Parses one text frame. Returns `null` for malformed JSON or frames missing required fields
     * (e.g. a welcome without a session id). [json] must ignore unknown keys.
     */
    fun parse(json: Json, text: String): EventSubMessage? {
        val frame = try {
            json.decodeFromString(FrameDto.serializer(), text)
        } catch (e: SerializationException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
        val payload = frame.payload
        return when (val type = frame.metadata.messageType) {
            "session_welcome" -> payload?.session?.id?.let {
                EventSubMessage.Welcome(it, payload.session.keepaliveTimeoutSeconds)
            }
            "session_keepalive" -> EventSubMessage.Keepalive
            "session_reconnect" -> payload?.session?.reconnectUrl?.let { EventSubMessage.Reconnect(it) }
            "revocation" -> EventSubMessage.Revocation(payload?.subscription?.type, payload?.subscription?.status)
            "notification" -> {
                val subscriptionType = frame.metadata.subscriptionType ?: payload?.subscription?.type
                val event = payload?.event
                if (subscriptionType == CHAT_MESSAGE_TYPE && event != null) {
                    event.toChatMessage()?.let { EventSubMessage.ChatMessageReceived(it) }
                } else {
                    EventSubMessage.Other(type)
                }
            }
            else -> EventSubMessage.Other(type)
        }
    }

    private fun ChatEventDto.toChatMessage(): ChatMessage? {
        return ChatMessage(
            id = messageId ?: return null,
            authorId = chatterUserId.orEmpty(),
            author = chatterUserName.orEmpty(),
            text = message?.text.orEmpty(),
            color = color.orEmpty(),
            isDeleted = false,
        )
    }
}

@Serializable
private data class FrameDto(
    val metadata: MetadataDto,
    val payload: PayloadDto? = null,
)

@Serializable
private data class MetadataDto(
    @SerialName("message_type") val messageType: String,
    @SerialName("subscription_type") val subscriptionType: String? = null,
)

@Serializable
private data class PayloadDto(
    val session: SessionDto? = null,
    val subscription: SubscriptionDto? = null,
    val event: ChatEventDto? = null,
)

@Serializable
private data class SessionDto(
    val id: String? = null,
    @SerialName("keepalive_timeout_seconds") val keepaliveTimeoutSeconds: Int? = null,
    @SerialName("reconnect_url") val reconnectUrl: String? = null,
)

@Serializable
private data class SubscriptionDto(
    val type: String? = null,
    val status: String? = null,
)

/** `channel.chat.message` event; all optional so other notification types still decode. */
@Serializable
private data class ChatEventDto(
    @SerialName("chatter_user_id") val chatterUserId: String? = null,
    @SerialName("chatter_user_name") val chatterUserName: String? = null,
    @SerialName("message_id") val messageId: String? = null,
    val message: ChatTextDto? = null,
    val color: String? = null,
)

@Serializable
private data class ChatTextDto(val text: String? = null)
