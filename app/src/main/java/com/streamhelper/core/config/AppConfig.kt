package com.streamhelper.core.config

import com.streamhelper.BuildConfig

/** Build-time config from local.properties (replaces react-native-config's CONFIG). */
object AppConfig {
    const val TWITCH_CLIENT_ID = BuildConfig.TWITCH_CLIENT_ID
    const val TWITCH_AUTH_URL = BuildConfig.TWITCH_AUTH_URL
    const val REDIRECT_URI = BuildConfig.REDIRECT_URI
    const val TWITCH_WS_URL = BuildConfig.TWITCH_WS_URL
    const val DONATIONALERTS_AUTH_URL = BuildConfig.DONATIONALERTS_AUTH_URL
    const val DONATIONALERTS_CLIENT_ID = BuildConfig.DONATIONALERTS_CLIENT_ID
}
