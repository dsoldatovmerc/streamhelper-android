package com.streamhelper.feature.auth.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamhelper.R
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.core.startup.StartupGate
import com.streamhelper.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Entry has nothing to show but a spinner; the state exists for UDF symmetry and future retry UI. */
data class EntryUiState(val isChecking: Boolean = true)

sealed interface EntryEvent {
    data object NavigateToLogin : EntryEvent
    data object NavigateToMain : EntryEvent
}

/**
 * RN `screens/Auth/Entry`: stored token → validate → load user → route.
 * If loading the user fails it shows an error and stays on Entry (RN parity).
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val snackbarBus: SnackbarBus,
    private val startupGate: StartupGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _events = Channel<EntryEvent>(Channel.BUFFERED)
    val events: Flow<EntryEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val destination = checkAuth()) {
                null -> {
                    snackbarBus.error(R.string.please_try_again)
                    finishStartup()
                }
                else -> _events.send(destination)
            }
        }
    }

    /** Called by the screen after it has navigated, so the splash hides over the next screen, not Entry. */
    fun onNavigated() = finishStartup()

    private fun finishStartup() {
        _uiState.value = EntryUiState(isChecking = false)
        startupGate.markReady()
    }

    /** Where to go next, or `null` when the token is valid but the user failed to load. */
    private suspend fun checkAuth(): EntryEvent? {
        authRepository.restoreToken() ?: return EntryEvent.NavigateToLogin
        if (!authRepository.validateToken()) return EntryEvent.NavigateToLogin
        return if (authRepository.loadUser().isSuccess) EntryEvent.NavigateToMain else null
    }
}
