package com.streamhelper.core.snackbar

import androidx.annotation.StringRes
import com.streamhelper.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** One app-wide snackbar: a "Success"/"Error" title plus an optional description (RN toast parity). */
data class SnackbarMessage(val kind: Kind, val description: UiText? = null) {
    enum class Kind(@StringRes val titleRes: Int) {
        Success(R.string.success),
        Error(R.string.error),
    }
}

/**
 * App-scoped message bus for snackbars (RN `ToastService`). Inject it into any ViewModel or
 * repository and call [success] / [error]; the single `AppSnackbarHost` at the root of
 * `MainActivity` shows them one at a time at the bottom of the screen.
 */
@Singleton
class SnackbarBus @Inject constructor() {
    private val channel = Channel<SnackbarMessage>(Channel.BUFFERED)

    /** Consumed by the root snackbar host only. Each message is delivered once. */
    val messages: Flow<SnackbarMessage> = channel.receiveAsFlow()

    fun success(description: UiText? = null) = send(SnackbarMessage(SnackbarMessage.Kind.Success, description))

    fun success(@StringRes description: Int) = success(UiText.Resource(description))

    fun error(description: UiText? = null) = send(SnackbarMessage(SnackbarMessage.Kind.Error, description))

    fun error(@StringRes description: Int) = error(UiText.Resource(description))

    private fun send(message: SnackbarMessage) {
        channel.trySend(message)
    }
}
