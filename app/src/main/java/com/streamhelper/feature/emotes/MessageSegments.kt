package com.streamhelper.feature.emotes

/** A piece of a chat message: a run of plain words or a single emote. */
sealed interface MessageSegment {
    data class Text(val text: String) : MessageSegment

    /** [name] is the word that matched; used as the emote's alternate text. */
    data class Emote(val name: String, val emote: com.streamhelper.feature.emotes.Emote) : MessageSegment
}

/**
 * Splits [text] into segments (RN `formatChatMessage`). The text is split on single spaces;
 * each word that exactly matches an emote name (7TV first, then Twitch) becomes an
 * [MessageSegment.Emote], and each run of consecutive non-emote words becomes one
 * [MessageSegment.Text], joined with spaces. Whitespace-only text runs are dropped.
 *
 * Pure Kotlin, no Android dependencies.
 */
fun segmentMessage(text: String, emotes: EmoteSets): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val words = mutableListOf<String>()

    fun flushText() {
        if (words.isEmpty()) return
        val run = words.joinToString(" ")
        if (run.isNotBlank()) segments += MessageSegment.Text(run)
        words.clear()
    }

    for (word in text.split(' ')) {
        val emote = emotes.find(word)
        if (emote == null) {
            words += word
        } else {
            flushText()
            segments += MessageSegment.Emote(word, emote)
        }
    }
    flushText()
    return segments
}
