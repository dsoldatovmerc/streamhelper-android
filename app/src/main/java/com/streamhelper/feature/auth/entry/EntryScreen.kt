package com.streamhelper.feature.auth.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * RN: screens/Auth/Entry — checks stored token, validates it, loads user, then routes.
 * Step 2 (Twitch auth) adds the ViewModel with that logic; for now it always goes to Login.
 */
@Composable
fun EntryScreen(
    onNavigateToLogin: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToMain: () -> Unit,
) {
    LaunchedEffect(Unit) { onNavigateToLogin() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
