package com.streamhelper.feature.alerts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamhelper.R
import com.streamhelper.core.oauth.OAuthWebView
import com.streamhelper.core.ui.components.LoadingButton
import com.streamhelper.core.ui.theme.AppColors
import com.streamhelper.feature.alerts.data.Donation
import com.streamhelper.feature.alerts.data.amountText

/**
 * RN: screens/Main/Alerts. "Log in via DonationAlerts" opens DonationAlerts' login page in the shared
 * in-app OAuth WebView; once connected, a pull-to-refresh list of the latest text donations.
 */
@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoggedIn) {
            DonationList(
                donations = state.donations,
                isRefreshing = state.isLoading,
                onRefresh = viewModel::onRefresh,
            )
        } else {
            LoadingButton(
                text = stringResource(R.string.donationalerts_login),
                isLoading = state.isLoading,
                onClick = viewModel::onLoginClick,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        state.oauthRequest?.let { request ->
            OAuthWebView(
                request = request,
                onResult = viewModel::onOAuthResult,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationList(
    donations: List<Donation>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(donations, key = { _, donation -> donation.id }) { index, donation ->
                // RN ItemSeparatorComponent: 1px black at 10% opacity, between rows only.
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = AppColors.Black.copy(alpha = 0.1f))
                }
                DonationRow(donation)
            }
        }
    }
}

/** RN `AlertListItem`: donor name (one line) and amount on the header, message below if present. */
@Composable
private fun DonationRow(donation: Donation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = donation.userName ?: stringResource(R.string.anonymous),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = donation.amountText(),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.Donation,
            )
        }
        donation.message?.takeIf { it.isNotEmpty() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
