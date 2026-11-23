package com.streamhelper.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamhelper.feature.chat.data.ChatMessage

/**
 * One chat message (RN `Message`): the author in bold in their Twitch color, then the text.
 * Deleted messages are faded (RN opacity 0.4).
 *
 * @param onMessageClick Tap on the row; the moderation ticket opens its bottom sheet from it.
 */
@Composable
fun ChatMessageRow(
    message: ChatMessage,
    onMessageClick: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackColor = MaterialTheme.colorScheme.onSurface
    val authorColor = remember(message.color, fallbackColor) {
        TwitchColor.parseArgb(message.color)?.let(::Color) ?: fallbackColor
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onMessageClick(message) }
            .alpha(if (message.isDeleted) DELETED_ALPHA else 1f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = message.author,
            color = authorColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ChatMessageText(message)
    }
}

private const val DELETED_ALPHA = 0.4f
