package com.streamhelper.feature.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.streamhelper.R
import com.streamhelper.core.ui.components.shimmer
import com.streamhelper.core.ui.theme.AppColors
import com.streamhelper.feature.chat.DeletedText

/**
 * The moderation bottom sheet (RN `MessageSheet`). Shown while [MessageSheetViewModel] is open;
 * closing from the ViewModel (e.g. the profile failed to load) animates the sheet out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSheet(viewModel: MessageSheetViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Kept composed after isOpen turns false, until the hide animation ends.
    var isComposed by remember { mutableStateOf(false) }
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            if (isComposed) sheetState.show() else isComposed = true
        } else if (isComposed) {
            sheetState.hide()
            isComposed = false
        }
    }
    // The selection is cleared on close; keep showing the last one while the sheet slides away.
    val lastSelected = remember { LastValue<SelectedMessage>() }
    state.selected?.let { lastSelected.value = it }
    val selected = state.selected ?: lastSelected.value

    if (isComposed && selected != null) {
        ModalBottomSheet(onDismissRequest = viewModel::onDismiss, sheetState = sheetState) {
            MessageSheetContent(
                selected = selected,
                actionsEnabled = !state.isActionInFlight,
                onDeleteClick = viewModel::onDeleteClick,
                onTimeoutClick = viewModel::onTimeoutClick,
                onBanClick = viewModel::onBanClick,
                onUnbanClick = viewModel::onUnbanClick,
            )
        }
    }
}

@Composable
private fun MessageSheetContent(
    selected: SelectedMessage,
    actionsEnabled: Boolean,
    onDeleteClick: () -> Unit,
    onTimeoutClick: () -> Unit,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit,
) {
    val user = selected.user
    val message = selected.message
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row {
            val avatarModifier = Modifier.padding(end = 16.dp).size(AvatarSize)
            if (user == null) {
                Box(modifier = avatarModifier.shimmer(CircleShape))
                Box(modifier = Modifier.size(width = 128.dp, height = 26.dp).shimmer())
            } else {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    modifier = avatarModifier.clip(CircleShape).background(AppColors.Gray),
                )
                Text(text = user.displayName, style = MaterialTheme.typography.titleLarge)
            }
        }
        if (user == null) {
            Box(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth().height(24.dp).shimmer())
        } else {
            val text = if (message.isDeleted) {
                DeletedText.withSuffix(message.text, stringResource(R.string.deleted))
            } else {
                message.text
            }
            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val actionModifier = Modifier.weight(1f)
                if (message.isDeleted) {
                    SheetAction(
                        label = stringResource(R.string.deleted),
                        icon = Icons.Filled.Check,
                        onClick = {},
                        enabled = false,
                        modifier = actionModifier,
                    )
                } else {
                    SheetAction(
                        label = stringResource(R.string.delete),
                        icon = Icons.Filled.Delete,
                        onClick = onDeleteClick,
                        enabled = actionsEnabled,
                        modifier = actionModifier,
                    )
                }
                SheetAction(
                    label = stringResource(R.string.timeout),
                    icon = Icons.Outlined.Timer,
                    onClick = onTimeoutClick,
                    enabled = actionsEnabled,
                    modifier = actionModifier,
                )
                SheetAction(
                    label = stringResource(R.string.ban),
                    icon = Icons.Filled.Block,
                    onClick = onBanClick,
                    enabled = actionsEnabled,
                    color = AppColors.Danger,
                    modifier = actionModifier,
                )
                SheetAction(
                    label = stringResource(R.string.unban),
                    icon = Icons.Filled.Block,
                    onClick = onUnbanClick,
                    enabled = actionsEnabled,
                    color = AppColors.Danger,
                    modifier = actionModifier,
                )
            }
        }
    }
}

/** RN sheet `Action`: a gray button with a 32dp icon above its label. */
@Composable
private fun SheetAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Black,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gray, contentColor = color),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Four labels share a phone-width row; long ones ("Отстранить") shrink to fit.
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
            )
        }
    }
}

/** A plain holder written during composition; reading it never triggers recomposition. */
private class LastValue<T : Any> {
    var value: T? = null
}

private val AvatarSize = 64.dp
