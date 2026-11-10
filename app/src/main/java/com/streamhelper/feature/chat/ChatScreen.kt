package com.streamhelper.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamhelper.R
import com.streamhelper.core.ui.components.PlaceholderScreen

/** RN: screens/Main/Chat. Logout button at the top (RN parity); the message list arrives in step 3. */
@Composable
fun ChatScreen(
    onLoggedOut: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedOut by rememberUpdatedState(onLoggedOut)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChatEvent.NavigateToAuth -> loggedOut()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = viewModel::onLogoutClick,
            enabled = !state.isLoggingOut,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (state.isLoggingOut) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.logout))
            }
        }
        PlaceholderScreen(label = "Chat — step 3", modifier = Modifier.weight(1f))
    }
}
