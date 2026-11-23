package com.streamhelper.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamhelper.R
import com.streamhelper.core.oauth.OAuthWebView
import com.streamhelper.core.ui.components.LoadingButton

/** RN: screens/Auth/Login — "Log In via Twitch" opens Twitch's login page in an in-app WebView overlay. */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedIn by rememberUpdatedState(onLoggedIn)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateToMain -> loggedIn()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineMedium)
            LoadingButton(
                text = stringResource(R.string.login),
                isLoading = state.isLoading,
                onClick = viewModel::onLoginClick,
            )
        }

        state.oauthRequest?.let { request ->
            OAuthWebView(
                request = request,
                onResult = viewModel::onOAuthResult,
            )
        }
    }
}
