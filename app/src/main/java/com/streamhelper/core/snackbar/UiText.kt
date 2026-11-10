package com.streamhelper.core.snackbar

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text a ViewModel can emit without holding a `Context`: either a string resource or a literal. */
sealed interface UiText {
    data class Resource(@StringRes val id: Int) : UiText
    data class Literal(val value: String) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Resource -> stringResource(id)
        is Literal -> value
    }
}
