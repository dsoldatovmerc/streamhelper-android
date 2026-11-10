package com.streamhelper.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SystemSecurityUpdateWarning
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.streamhelper.R
import com.streamhelper.feature.alerts.AlertsScreen
import com.streamhelper.feature.chat.ChatScreen
import com.streamhelper.feature.raid.RaidScreen
import com.streamhelper.feature.streaminfo.StreamInfoScreen
import com.streamhelper.navigation.AlertsRoute
import com.streamhelper.navigation.ChatRoute
import com.streamhelper.navigation.RaidRoute
import com.streamhelper.navigation.StreamInfoRoute
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val labelRes: Int, val icon: ImageVector)

// Same order and icons as RN navigation/Main.
private val tabs = listOf(
    Tab(ChatRoute, ChatRoute::class, R.string.tab_chat, Icons.AutoMirrored.Filled.Message),
    Tab(AlertsRoute, AlertsRoute::class, R.string.tab_alerts, Icons.Filled.SystemSecurityUpdateWarning),
    Tab(RaidRoute, RaidRoute::class, R.string.tab_raid, Icons.Filled.NorthEast),
    Tab(StreamInfoRoute, StreamInfoRoute::class, R.string.tab_stream_info, Icons.Filled.VideoSettings),
)

@Composable
fun MainScreen(@Suppress("UNUSED_PARAMETER") onOpenWebView: (String) -> Unit) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = ChatRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<ChatRoute> { ChatScreen() }
            composable<AlertsRoute> { AlertsScreen() }
            composable<RaidRoute> { RaidScreen() }
            composable<StreamInfoRoute> { StreamInfoScreen() }
        }
    }
}
