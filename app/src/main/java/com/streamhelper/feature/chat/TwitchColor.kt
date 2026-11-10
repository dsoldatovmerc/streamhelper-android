package com.streamhelper.feature.chat

/** Pure parsing of Twitch name colors (no Android dependencies). */
object TwitchColor {
    /** `#RRGGBB` (the `#` is optional) to an opaque ARGB int; `null` when empty or invalid. */
    fun parseArgb(value: String?): Int? {
        val hex = value?.trim()?.removePrefix("#") ?: return null
        if (hex.length != 6 || hex.any { Character.digit(it, 16) < 0 }) return null
        return (0xFF shl 24) or hex.toInt(16)
    }
}
