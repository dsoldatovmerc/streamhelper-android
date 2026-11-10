package com.streamhelper.navigation

import kotlinx.serialization.Serializable

// Root graph (RN: navigation/Root)
@Serializable data object AuthGraph
@Serializable data object MainRoute
@Serializable data class WebViewRoute(val uri: String)

// Auth graph (RN: navigation/Auth)
@Serializable data object EntryRoute
@Serializable data object LoginRoute

// Main tabs (RN: navigation/Main)
@Serializable data object ChatRoute
@Serializable data object AlertsRoute
@Serializable data object RaidRoute
@Serializable data object StreamInfoRoute
