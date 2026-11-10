package com.streamhelper.feature.auth.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** RN: screens/Auth/Entry — checks the stored token, validates it, loads the user, then routes. */
@Composable
fun EntryScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val toLogin by rememberUpdatedState(onNavigateToLogin)
    val toMain by rememberUpdatedState(onNavigateToMain)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EntryEvent.NavigateToLogin -> toLogin()
                EntryEvent.NavigateToMain -> toMain()
            }
            viewModel.onNavigated()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
