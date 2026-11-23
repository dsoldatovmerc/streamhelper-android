package com.streamhelper.feature.emotes

/** One emote image (RN `Emoji`). [width] and [height] are in dp. */
data class Emote(val url: String, val width: Int, val height: Int)

/**
 * The streamer's emote sets, keyed by emote name: [sevenTv] (the 7TV user set) and [twitch]
 * (Twitch global plus channel, channel winning on name clashes).
 */
data class EmoteSets(
    val sevenTv: Map<String, Emote> = emptyMap(),
    val twitch: Map<String, Emote> = emptyMap(),
) {
    /** The emote named exactly [word]; 7TV wins when both sets have it. */
    fun find(word: String): Emote? = sevenTv[word] ?: twitch[word]

    companion object {
        val Empty = EmoteSets()
    }
}
