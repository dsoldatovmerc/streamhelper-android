package com.streamhelper.core.snackbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamhelper.R
import com.streamhelper.core.ui.theme.AppColors

private class AppSnackbarVisuals(val appMessage: SnackbarMessage) : SnackbarVisuals {
    // Rendered by AppSnackbar from appMessage; the host only needs these for timing/accessibility.
    override val message: String = appMessage.kind.name
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * The single snackbar host for the whole app. Place it once at the root of the activity;
 * it collects [SnackbarBus.messages] and shows them one after another.
 */
@Composable
fun AppSnackbarHost(bus: SnackbarBus, modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(bus) {
        bus.messages.collect { hostState.showSnackbar(AppSnackbarVisuals(it)) }
    }
    SnackbarHost(hostState = hostState, modifier = modifier.safeDrawingPadding()) { data ->
        AppSnackbar(data)
    }
}

@Composable
private fun AppSnackbar(data: SnackbarData) {
    val message = (data.visuals as AppSnackbarVisuals).appMessage
    val (title, accent) = when (message.kind) {
        SnackbarMessage.Kind.Success -> stringResource(R.string.success) to Color(0xFF2E7D32)
        SnackbarMessage.Kind.Error -> stringResource(R.string.error) to AppColors.Danger
    }
    Snackbar(
        modifier = Modifier.padding(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Coloured left edge, like RN's toast borderLeftColor.
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                message.description?.let { Text(text = it.asString(), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
