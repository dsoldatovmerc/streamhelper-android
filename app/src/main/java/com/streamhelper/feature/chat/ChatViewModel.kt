package com.streamhelper.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamhelper.R
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.feature.auth.data.AuthRepository
import com.streamhelper.feature.chat.data.ChatConnectionState
import com.streamhelper.feature.chat.data.ChatMessage
import com.streamhelper.feature.chat.data.ChatRepository
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

/**
 * @property messages Oldest first, straight from [ChatRepository.messages].
 * @property isConnected False only after a drop, until the next welcome ("Can't connect" banner).
 * @property isRefreshing Pull-to-refresh indicator.
 * @property isPaused Auto-scroll paused because the user scrolled away from the bottom.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = true,
    val isRefreshing: Boolean = false,
    val isPaused: Boolean = false,
    val isLoggingOut: Boolean = false,
)

sealed interface ChatEvent {
    data object NavigateToAuth : ChatEvent
}

/**
 * RN `screens/Main/Chat` controller. Starts chat on creation and maps [ChatRepository] to [ChatUiState].
 *
 * Extension point: each source flow is collected in its own `launch` in `init` and folded into
 * [_uiState] with `update { it.copy(...) }`. To expose more state (e.g. emotes, a selected
 * message), add a field to [ChatUiState] and one more collector or intent; no shared `combine`.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val snackbarBus: SnackbarBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = _events.receiveAsFlow()

    init {
        chatRepository.start()
        viewModelScope.launch {
            chatRepository.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            chatRepository.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state != ChatConnectionState.Disconnected) }
            }
        }
        viewModelScope.launch {
            chatRepository.isRefreshing.collect { refreshing -> _uiState.update { it.copy(isRefreshing = refreshing) } }
        }
    }

    /** Pull-to-refresh: reconnect now (no-op while connected). */
    fun onRefresh() = chatRepository.reconnect()

    /** The list reached (true) or the user scrolled away from (false) the bottom. */
    fun onAtBottomChanged(isAtBottom: Boolean) = _uiState.update { it.copy(isPaused = !isAtBottom) }

    /** A message row was tapped. No-op for now; the moderation ticket opens its bottom sheet here. */
    fun onMessageClick(message: ChatMessage) = Unit

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
