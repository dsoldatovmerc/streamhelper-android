package com.streamhelper.feature.emotes.data

import com.streamhelper.feature.emotes.Emote

/** Twitch emotes are always drawn at this size (dp), RN parity. */
internal const val TWITCH_EMOTE_SIZE = 32

/**
 * Maps a 7TV user to `name → Emote`, using each emote's first file:
 * `"https:" + host.url + "/" + files[0].name`, sized by that file. Emotes without host data or files are skipped.
 */
internal fun SevenTvUserDto.toEmoteMap(): Map<String, Emote> =
    emoteSet?.emotes.orEmpty().mapNotNull { emote ->
        val host = emote.data?.host ?: return@mapNotNull null
        val file = host.files.firstOrNull() ?: return@mapNotNull null
        emote.name to Emote(url = "https:${host.url}/${file.name}", width = file.width, height = file.height)
    }.toMap()

/**
 * Maps Twitch global and channel emotes to `name → Emote` (channel overrides global on name
 * clashes). URLs come from the global response's template.
 */
internal fun twitchEmoteMap(global: HelixEmotesResponse, channel: HelixEmotesResponse): Map<String, Emote> {
    val template = global.template
    require(template.isNotBlank()) { "Helix global emotes response has no template" }
    return (global.data + channel.data).associate { emote ->
        emote.name to Emote(
            url = twitchEmoteUrl(template, emote),
            width = TWITCH_EMOTE_SIZE,
            height = TWITCH_EMOTE_SIZE,
        )
    }
}

internal fun twitchEmoteUrl(template: String, emote: HelixEmoteDto): String =
    template
        .replace("{{id}}", emote.id)
        .replace("{{format}}", if ("animated" in emote.format) "animated" else "static")
        .replace("{{theme_mode}}", "light")
        .replace("{{scale}}", "1.0")
