package com.streamhelper.feature.auth.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.streamhelper.R

/**
 * RN: screens/Auth/Login — Twitch OAuth in a WebView.
 * Step 2 replaces this with the WebView flow; for now the button just enters Main so the shell can be reviewed.
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onLoggedIn) {
            Text(stringResource(R.string.login))
        }
    }
}
