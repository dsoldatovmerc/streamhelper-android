package com.streamhelper.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamhelper.R
import com.streamhelper.core.ui.components.LoadingButton
import com.streamhelper.core.ui.theme.AppColors
import com.streamhelper.feature.chat.data.ChatMessage
import kotlinx.coroutines.flow.distinctUntilChanged

/** RN `screens/Main/Chat`: Logout button, connection/paused banners, pull-to-refresh message list. */
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
        LoadingButton(
            text = stringResource(R.string.logout),
            isLoading = state.isLoggingOut,
            onClick = viewModel::onLogoutClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (!state.isConnected) {
            ChatBanner(
                text = stringResource(R.string.chat_connection_error),
                color = AppColors.Danger,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            ChatMessageList(
                messages = state.messages,
                isRefreshing = state.isRefreshing,
                isPaused = state.isPaused,
                onRefresh = viewModel::onRefresh,
                onAtBottomChanged = viewModel::onAtBottomChanged,
                onMessageClick = viewModel::onMessageClick,
            )
            if (state.isPaused) {
                ChatBanner(
                    text = stringResource(R.string.chat_paused),
                    color = AppColors.Gray,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Follows new messages while not paused. A user scroll away from the bottom reports
 * `onAtBottomChanged(false)`; reaching the bottom (within [BOTTOM_THRESHOLD]) reports `true`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    isRefreshing: Boolean,
    isPaused: Boolean,
    onRefresh: () -> Unit,
    onAtBottomChanged: (Boolean) -> Unit,
    onMessageClick: (ChatMessage) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val thresholdPx = with(LocalDensity.current) { BOTTOM_THRESHOLD.roundToPx() }
    val isAtBottom by remember(listState, thresholdPx) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index == info.totalItemsCount - 1 &&
                last.offset + last.size <= info.viewportEndOffset + thresholdPx
        }
    }
    // True while we animate to the newest message, so that scroll isn't mistaken for the user's.
    var isAutoScrolling by remember { mutableStateOf(false) }
    val atBottomChanged by rememberUpdatedState(onAtBottomChanged)

    LaunchedEffect(listState) {
        snapshotFlow { isAtBottom to (listState.isScrollInProgress && !isAutoScrolling) }
            .distinctUntilChanged()
            .collect { (atBottom, userScrolling) ->
                if (atBottom) {
                    atBottomChanged(true)
                } else if (userScrolling) {
                    atBottomChanged(false)
                }
            }
    }

    LaunchedEffect(messages.size) {
        if (isPaused || messages.isEmpty()) return@LaunchedEffect
        isAutoScrolling = true
        try {
            listState.animateScrollToItem(messages.lastIndex)
        } finally {
            isAutoScrolling = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = SeparatorColor)
                ChatMessageRow(message = message, onMessageClick = onMessageClick)
            }
        }
    }
}

@Composable
private fun ChatBanner(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().background(color).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.White, style = MaterialTheme.typography.bodyMedium)
    }
}

private val BOTTOM_THRESHOLD = 16.dp
private val SeparatorColor = Color.Black.copy(alpha = 0.1f)
