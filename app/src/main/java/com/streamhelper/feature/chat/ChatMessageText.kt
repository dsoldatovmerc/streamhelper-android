package com.streamhelper.feature.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.streamhelper.feature.chat.data.ChatMessage

/**
 * Renders a message's body under the author name. Plain text for now.
 *
 * Extension point: the emotes ticket replaces this composable's body with an emote-segment
 * renderer (RN `MessageContent`). It is called only from [ChatMessageRow].
 */
@Composable
fun ChatMessageText(message: ChatMessage, modifier: Modifier = Modifier) {
    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, modifier = modifier)
}
