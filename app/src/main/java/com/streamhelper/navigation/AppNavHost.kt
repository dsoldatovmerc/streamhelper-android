package com.streamhelper.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.streamhelper.feature.auth.entry.EntryScreen
import com.streamhelper.feature.auth.login.LoginScreen
import com.streamhelper.feature.main.MainScreen
import com.streamhelper.feature.webview.WebViewScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthGraph) {
        navigation<AuthGraph>(startDestination = EntryRoute) {
            composable<EntryRoute> {
                EntryScreen(
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo<EntryRoute> { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(MainRoute) {
                            popUpTo<AuthGraph> { inclusive = true }
                        }
                    },
                )
            }
            composable<LoginRoute> {
                LoginScreen(
                    onLoggedIn = {
                        navController.navigate(MainRoute) {
                            popUpTo<AuthGraph> { inclusive = true }
                        }
                    },
                )
            }
        }

        composable<MainRoute> {
            MainScreen(
                onOpenWebView = { uri -> navController.navigate(WebViewRoute(uri)) },
            )
        }

        composable<WebViewRoute> { entry ->
            WebViewScreen(uri = entry.toRoute<WebViewRoute>().uri)
        }
    }
}
