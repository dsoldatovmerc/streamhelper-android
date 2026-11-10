package com.streamhelper.feature.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.streamhelper.feature.chat.data.ChatMessage
import com.streamhelper.feature.emotes.EmotesViewModel
import com.streamhelper.feature.emotes.MessageSegment
import com.streamhelper.feature.emotes.segmentMessage

/**
 * Renders a message's body under the author name (RN `MessageContent`): words that match a
 * Twitch or 7TV emote are drawn as the emote image, inline with the surrounding text.
 * Re-renders when the emote sets load, so earlier messages pick up emotes too.
 * Called only from [ChatMessageRow].
 */
@Composable
fun ChatMessageText(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    emotesViewModel: EmotesViewModel = hiltViewModel(),
) {
    val emotes by emotesViewModel.emotes.collectAsStateWithLifecycle()
    val segments = remember(message.text, emotes) { segmentMessage(message.text, emotes) }
    SegmentedText(segments, MaterialTheme.typography.bodyLarge, modifier)
}

/** Text segments and emotes in one flowing, wrapping line; emotes are `InlineTextContent` placeholders. */
@Composable
private fun SegmentedText(segments: List<MessageSegment>, style: TextStyle, modifier: Modifier) {
    val density = LocalDensity.current
    val content = remember(segments, density) { buildInlineContent(segments, density) }
    Text(
        text = content.text,
        inlineContent = content.inlineContent,
        // A fixed line height would let emotes taller than it overlap the neighbouring lines;
        // unspecified lets each line grow to fit its emotes.
        style = if (content.inlineContent.isEmpty()) style else style.copy(lineHeight = TextUnit.Unspecified),
        modifier = modifier,
    )
}

private class InlineContent(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>,
)

private fun buildInlineContent(segments: List<MessageSegment>, density: Density): InlineContent {
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val text = buildAnnotatedString {
        segments.forEachIndexed { index, segment ->
            if (index > 0) append(' ')
            when (segment) {
                is MessageSegment.Text -> append(segment.text)
                is MessageSegment.Emote -> {
                    val id = "emote_$index"
                    appendInlineContent(id, alternateText = segment.name)
                    inlineContent[id] = emoteContent(segment, density)
                }
            }
        }
    }
    return InlineContent(text, inlineContent)
}

/** Placeholder sized in dp (converted to sp at the current density, so font scale doesn't stretch it). */
private fun emoteContent(segment: MessageSegment.Emote, density: Density): InlineTextContent {
    val placeholder = with(density) {
        Placeholder(
            width = segment.emote.width.dp.toSp(),
            height = segment.emote.height.dp.toSp(),
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        )
    }
    return InlineTextContent(placeholder) {
        AsyncImage(
            model = segment.emote.url,
            contentDescription = segment.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
