package com.streamhelper.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamhelper.R
import com.streamhelper.core.oauth.OAuthResult
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.feature.auth.data.AuthRepository
import com.streamhelper.feature.auth.data.TwitchOAuthConfig
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

/** Request to show the OAuth WebView overlay. */
data class OAuthRequest(val authorizeUrl: String, val redirectUri: String)

data class LoginUiState(
    val isLoading: Boolean = false,
    /** Non-null while the Twitch login page is shown. */
    val oauthRequest: OAuthRequest? = null,
)

sealed interface LoginEvent {
    data object NavigateToMain : LoginEvent
}

/** RN `screens/Auth/Login`: Twitch login in the in-app OAuth WebView, then save token and load the user. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val snackbarBus: SnackbarBus,
    private val oauthConfig: TwitchOAuthConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onLoginClick() {
        if (_uiState.value.isLoading || _uiState.value.oauthRequest != null) return
        _uiState.update {
            it.copy(oauthRequest = OAuthRequest(oauthConfig.authorizeUrl, oauthConfig.redirectUri))
        }
    }

    fun onOAuthResult(result: OAuthResult) {
        _uiState.update { it.copy(oauthRequest = null) }
        when (result) {
            is OAuthResult.Success -> signIn(result.accessToken)
            OAuthResult.Failed -> snackbarBus.error(R.string.please_try_again)
            OAuthResult.Cancelled -> Unit
        }
    }

    private fun signIn(token: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                authRepository.saveToken(token)
                authRepository.loadUser()
                    .onSuccess { _events.send(LoginEvent.NavigateToMain) }
                    .onFailure { snackbarBus.error(R.string.please_try_again) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
