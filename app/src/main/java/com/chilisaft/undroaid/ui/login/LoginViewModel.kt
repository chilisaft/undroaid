package com.chilisaft.undroaid.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.data.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginScreenState(
    val serverUrl: String = "",
    val apiToken: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginScreenState())
    val uiState: StateFlow<LoginScreenState> = _uiState.asStateFlow()

    init {
        val savedLogin = loginRepository.getSavedLogin()
        if (!savedLogin.serverUrl.isNullOrBlank() && !savedLogin.apiToken.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                serverUrl = savedLogin.serverUrl,
                apiToken = savedLogin.apiToken
            )
            // Perform automatic login on a background thread without blocking
            login()
        }
    }

    fun onServerUrlChange(serverUrl: String) {
        _uiState.update { it.copy(serverUrl = serverUrl, error = null) }
    }

    fun onApiTokenChange(apiToken: String) {
        _uiState.update { it.copy(apiToken = apiToken, error = null) }
    }

    fun isLoginEnabled(): Boolean {
        return uiState.value.serverUrl.isNotBlank() && uiState.value.apiToken.isNotBlank() && !uiState.value.isLoading
    }

    fun login() {
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = loginRepository.login(Login(_uiState.value.serverUrl, _uiState.value.apiToken))

            result.fold(
                onSuccess = { isLoggedIn ->
                    _uiState.update { it.copy(isLoggedIn = isLoggedIn, isLoading = false) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message, isLoading = false) }
                }
            )
        }
    }
}