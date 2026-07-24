package com.chilisaft.undroaid.ui.usermenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.BuildConfig
import com.chilisaft.undroaid.data.models.ApiKeyInfo
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.LoginRepository
import com.chilisaft.undroaid.data.repository.ServerRepository
import com.chilisaft.undroaid.utils.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserMenuState(
    val serverUrl: String? = null,
    val appVersion: String = "",
    val serverVersion: WidgetResult<String?> = WidgetResult.Loading,
    val apiKeyInfo: WidgetResult<ApiKeyInfo> = WidgetResult.Loading,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class UserMenuViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val loginRepository: LoginRepository,
    storage: Storage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UserMenuState(serverUrl = storage.serverUrl, appVersion = BuildConfig.VERSION_NAME)
    )
    val uiState: StateFlow<UserMenuState> = _uiState.asStateFlow()

    init {
        refreshServerVersion()
        refreshApiKeyInfo()
    }

    fun refreshServerVersion() {
        viewModelScope.launch {
            _uiState.update { it.copy(serverVersion = WidgetResult.Loading) }
            _uiState.update { it.copy(serverVersion = serverRepository.getServerVersion()) }
        }
    }

    fun refreshApiKeyInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(apiKeyInfo = WidgetResult.Loading) }
            _uiState.update { it.copy(apiKeyInfo = serverRepository.getApiKeyInfo()) }
        }
    }

    fun logout() {
        loginRepository.logout()
        _uiState.update { it.copy(isLoggedOut = true) }
    }
}
