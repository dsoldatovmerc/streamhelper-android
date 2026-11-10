package com.streamhelper.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamhelper.R
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(val isLoggingOut: Boolean = false)

sealed interface ChatEvent {
    data object NavigateToAuth : ChatEvent
}

/** RN `screens/Main/Chat` controller. Step 2 only adds logout; chat itself arrives in step 3. */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val snackbarBus: SnackbarBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = _events.receiveAsFlow()

    /** Revoke + clear credentials + logout hooks. Navigates to Auth on revoke success; otherwise shows an error and stays. */
    fun onLogoutClick() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            try {
                if (authRepository.logout()) {
                    _events.send(ChatEvent.NavigateToAuth)
                } else {
                    snackbarBus.error(R.string.please_try_again)
                }
            } finally {
                _uiState.update { it.copy(isLoggingOut = false) }
            }
        }
    }
}
