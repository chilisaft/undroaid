package com.chilisaft.undroaid.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.data.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
            runBlocking {
                login()
            }
        }
    }

    fun onServerUrlChange(serverUrl: String) {
        _uiState.value = _uiState.value.copy(serverUrl = serverUrl, error = null)
    }

    fun onApiTokenChange(apiToken: String) {
        _uiState.value = _uiState.value.copy(apiToken = apiToken, error = null)
    }

    fun isLoginEnabled(): Boolean {
        return uiState.value.serverUrl.isNotBlank() && uiState.value.apiToken.isNotBlank()
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = loginRepository.login(Login(_uiState.value.serverUrl, _uiState.value.apiToken))
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = { isLoggedIn ->
                    _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
            )
        }
    }


}
