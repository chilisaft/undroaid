package com.chilisaft.undroid.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.ConnectInfo
import com.chilisaft.undroid.data.models.ServerIdentity
import com.chilisaft.undroid.data.models.ServerSystem
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerScreenState(
    val identity: WidgetResult<ServerIdentity> = WidgetResult.Loading,
    val system: WidgetResult<ServerSystem> = WidgetResult.Loading,
    val connect: WidgetResult<ConnectInfo> = WidgetResult.Loading,
    /** A full refresh is running - drives the pull-to-refresh indicator, while sections keep showing their current data. */
    val isRefreshing: Boolean = false
)

/**
 * Backs the Server screen. Its three sections load and fail independently because each needs a
 * different API-key permission (VARS, INFO, SERVERS) - same reasoning as the Dashboard widgets.
 */
@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerScreenState())
    val uiState: StateFlow<ServerScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            listOf(
                launch { loadIdentity() },
                launch { loadSystem() },
                launch { loadConnect() }
            ).joinAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshIdentity() {
        viewModelScope.launch { loadIdentity() }
    }

    fun refreshSystem() {
        viewModelScope.launch { loadSystem() }
    }

    fun refreshConnect() {
        viewModelScope.launch { loadConnect() }
    }

    private suspend fun loadIdentity() {
        _uiState.update { it.copy(identity = it.identity.reloading()) }
        _uiState.update { it.copy(identity = serverRepository.getServerIdentity()) }
    }

    private suspend fun loadSystem() {
        _uiState.update { it.copy(system = it.system.reloading()) }
        _uiState.update { it.copy(system = serverRepository.getServerSystem()) }
    }

    private suspend fun loadConnect() {
        _uiState.update { it.copy(connect = it.connect.reloading()) }
        _uiState.update { it.copy(connect = serverRepository.getConnectInfo()) }
    }
}
