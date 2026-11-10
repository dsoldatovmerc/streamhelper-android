package com.streamhelper.feature.moderation

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamhelper.R
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.feature.chat.data.ChatMessage
import com.streamhelper.feature.chat.data.ChatRepository
import com.streamhelper.feature.moderation.data.Chatter
import com.streamhelper.feature.moderation.data.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The tapped message and its author.
 *
 * @property user `null` while the chatter's profile loads (skeletons, no actions).
 * @property message Kept in sync with the live chat list by id, so a delete shows immediately.
 */
data class SelectedMessage(
    val user: Chatter?,
    val message: ChatMessage,
)

/**
 * @property isOpen Whether the sheet is shown. Opens on tap, before the profile has loaded.
 * @property selected `null` while closed.
 * @property isActionInFlight A delete/timeout/ban/unban request is running; all actions are disabled.
 */
data class MessageSheetUiState(
    val isOpen: Boolean = false,
    val selected: SelectedMessage? = null,
    val isActionInFlight: Boolean = false,
)

/**
 * RN `MessageSheet` + `useMessageSheetController`: the moderation bottom sheet on the Chat tab.
 * Tapping a message opens it at once and loads the author's profile; the actions run as the
 * streamer on their own channel and report the outcome through [SnackbarBus].
 */
@HiltViewModel
class MessageSheetViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository,
    private val chatRepository: ChatRepository,
    private val snackbarBus: SnackbarBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageSheetUiState())
    val uiState: StateFlow<MessageSheetUiState> = _uiState.asStateFlow()

    private var loadUserJob: Job? = null

    init {
        viewModelScope.launch {
            chatRepository.messages.collect { messages ->
                _uiState.update { state ->
                    val selected = state.selected ?: return@update state
                    val live = messages.firstOrNull { it.id == selected.message.id }
                    if (live == null || live == selected.message) state else state.copy(selected = selected.copy(message = live))
                }
            }
        }
    }

    /** A chat row was tapped: open the sheet in the loading state and fetch the author. */
    fun onMessageClick(message: ChatMessage) {
        if (message.authorId.isBlank()) return
        loadUserJob?.cancel()
        val live = chatRepository.messages.value.firstOrNull { it.id == message.id } ?: message
        _uiState.update { it.copy(isOpen = true, selected = SelectedMessage(user = null, message = live)) }
        loadUserJob = viewModelScope.launch {
            try {
                val user = moderationRepository.getUserById(message.authorId)
                _uiState.update { state ->
                    val selected = state.selected
                    if (selected?.message?.id != message.id) state else state.copy(selected = selected.copy(user = user))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Loading chatter ${message.authorId} failed", e)
                snackbarBus.error()
                close()
            }
        }
    }

    /** Swiped down or tapped outside. */
    fun onDismiss() {
        loadUserJob?.cancel()
        close()
    }

    /** Failure is silent (RN parity); success marks the message deleted in the chat list. */
    fun onDeleteClick() = runAction { selected ->
        if (selected.message.isDeleted) return@runAction
        try {
            chatRepository.deleteMessage(selected.message.id)
            snackbarBus.success(R.string.successfully_deleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deleting message ${selected.message.id} failed", e)
        }
    }

    fun onTimeoutClick() = moderate(R.string.successfully_timed_out) { user ->
        moderationRepository.ban(user.id, durationSeconds = TIMEOUT_SECONDS)
    }

    fun onBanClick() = moderate(R.string.successfully_banned) { user -> moderationRepository.ban(user.id) }

    fun onUnbanClick() = moderate(R.string.successfully_unbanned) { user -> moderationRepository.unban(user.id) }

    /** Ban/timeout/unban: success snackbar, or the generic error snackbar. The message list is untouched. */
    private fun moderate(@StringRes successMessage: Int, request: suspend (Chatter) -> Unit) = runAction { selected ->
        val user = selected.user ?: return@runAction
        try {
            request(user)
            snackbarBus.success(successMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Moderating ${user.id} failed", e)
            snackbarBus.error()
        }
    }

    /** Runs one action for the loaded selection; ignored while another is in flight. The sheet stays open. */
    private fun runAction(action: suspend (SelectedMessage) -> Unit) {
        val state = _uiState.value
        val selected = state.selected
        if (state.isActionInFlight || selected?.user == null) return
        _uiState.update { it.copy(isActionInFlight = true) }
        viewModelScope.launch {
            try {
                action(selected)
            } finally {
                _uiState.update { it.copy(isActionInFlight = false) }
            }
        }
    }

    private fun close() {
        _uiState.update { it.copy(isOpen = false, selected = null) }
    }

    private companion object {
        const val TAG = "MessageSheetViewModel"

        /** RN parity: Timeout is fixed at 30 seconds. */
        const val TIMEOUT_SECONDS = 30
    }
}
