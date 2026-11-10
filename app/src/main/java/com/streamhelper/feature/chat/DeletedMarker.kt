package com.streamhelper.feature.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.streamhelper.R

/**
 * RN appended `" - " + Deleted` to a deleted message's stored text. Here the stored
 * [com.streamhelper.feature.chat.data.ChatMessage.text] stays the raw message (so the emote
 * renderer never parses the marker) and the suffix is added at render time instead.
 */
object DeletedText {
    /** `"- <deletedLabel>"`, the marker on its own. Pure. */
    fun marker(deletedLabel: String): String = "- $deletedLabel"

    /** `"<text> - <deletedLabel>"`. Pure. */
    fun withSuffix(text: String, deletedLabel: String): String = "$text ${marker(deletedLabel)}"
}

/**
 * The "- Deleted" marker shown under a deleted message's body in the chat list. A separate
 * composable after [ChatMessageText], so it works whatever that renderer does with the text.
 */
@Composable
fun DeletedMarker(modifier: Modifier = Modifier) {
    Text(
        text = DeletedText.marker(stringResource(R.string.deleted)),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
    )
}
