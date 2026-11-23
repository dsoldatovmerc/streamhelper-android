package com.streamhelper.feature.alerts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamhelper.R
import com.streamhelper.core.oauth.OAuthRequest
import com.streamhelper.core.oauth.OAuthResult
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.feature.alerts.data.AlertsRepository
import com.streamhelper.feature.alerts.data.Donation
import com.streamhelper.feature.alerts.data.DonationAlertsAuthRepository
import com.streamhelper.feature.alerts.data.DonationAlertsLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoggedIn: Boolean = false,
    /** Login completing (token being saved) or donations loading. */
    val isLoading: Boolean = false,
    /** Text donations only, newest first. */
    val donations: List<Donation> = emptyList(),
    /** Non-null while the DonationAlerts login page is shown. */
    val oauthRequest: OAuthRequest? = null,
)

/** RN `screens/Main/Alerts` + `DonationAlertsAuthStore` + `AlertsStore`. */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val authRepository: DonationAlertsAuthRepository,
    private val alertsRepository: AlertsRepository,
    private val snackbarBus: SnackbarBus,
    @DonationAlertsLogin private val loginRequest: OAuthRequest,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            if (authRepository.restoreToken() != null) {
                _uiState.update { it.copy(isLoggedIn = true) }
                loadDonations()
            }
            // Twitch logout clears the DonationAlerts session: drop the list and show "Log in" again.
            authRepository.isLoggedIn.collect { loggedIn ->
                if (!loggedIn && _uiState.value.isLoggedIn) {
                    loadJob?.cancel()
                    _uiState.value = AlertsUiState()
                }
            }
        }
    }

    fun onLoginClick() {
        if (_uiState.value.isLoading || _uiState.value.oauthRequest != null) return
        _uiState.update { it.copy(oauthRequest = loginRequest) }
    }

    fun onOAuthResult(result: OAuthResult) {
        _uiState.update { it.copy(oauthRequest = null) }
        when (result) {
            is OAuthResult.Success -> signIn(result.accessToken)
            OAuthResult.Failed -> snackbarBus.error(R.string.please_try_again)
            OAuthResult.Cancelled -> Unit
        }
    }

    fun onRefresh() = loadDonations()

    private fun signIn(token: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                authRepository.saveToken(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Saving DonationAlerts token failed", e)
                _uiState.update { it.copy(isLoading = false) }
                snackbarBus.error(R.string.please_try_again)
                return@launch
            }
            _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
            loadDonations()
        }
    }

    /** Reloads donations; ignored while a load is running. On failure keeps the previous list. */
    private fun loadDonations() {
        if (loadJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            try {
                val donations = alertsRepository.loadDonations()
                _uiState.update { it.copy(donations = donations) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Loading donations failed", e)
                snackbarBus.error(R.string.alerts_load_error)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private companion object {
        const val TAG = "AlertsViewModel"
    }
}
