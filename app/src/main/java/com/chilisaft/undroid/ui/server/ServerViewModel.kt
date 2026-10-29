package com.chilisaft.undroid.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.ConnectInfo
import com.chilisaft.undroid.data.models.NetworkSummary
import com.chilisaft.undroid.data.models.ServerIdentity
import com.chilisaft.undroid.data.models.ServerSystem
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.ServerNetworkRepository
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
    /** Null data = the server reports no primary interface. */
    val network: WidgetResult<NetworkSummary?> = WidgetResult.Loading,
    val connect: WidgetResult<ConnectInfo> = WidgetResult.Loading,
    /** A full refresh is running - drives the pull-to-refresh indicator, while sections keep showing their current data. */
    val isRefreshing: Boolean = false
)

/**
 * Backs the Server screen. Its sections load and fail independently because each needs a
 * different API-key permission (VARS, INFO, SERVERS) - same reasoning as the Dashboard widgets.
 * Network is split from System only because it's slow and stalls the API while it runs (see
 * [ServerNetworkRepository]): it starts from its cache and is refetched only once the other
 * sections are in - on open only when the cache is over [NETWORK_MAX_AGE_ON_OPEN_MILLIS] old,
 * on pull-to-refresh always.
 */
@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val networkRepository: ServerNetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ServerScreenState(network = networkRepository.cached()?.let { WidgetResult.Success(it.network) } ?: WidgetResult.Loading)
    )
    val uiState: StateFlow<ServerScreenState> = _uiState.asStateFlow()

    init {
        refresh(forceNetwork = false)
    }

    fun refresh() = refresh(forceNetwork = true)

    private fun refresh(forceNetwork: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            listOf(
                launch { loadIdentity() },
                launch { loadSystem() },
                launch { loadConnect() }
            ).joinAll()
            _uiState.update { it.copy(isRefreshing = false) }
            // Afterwards, not alongside: the other sections would wait out its ~2 s. It refreshes
            // behind the cached value (or its own loading placeholder), not the pull indicator.
            if (forceNetwork || networkRepository.isStale(NETWORK_MAX_AGE_ON_OPEN_MILLIS)) {
                loadNetwork()
            }
        }
    }

    fun refreshIdentity() {
        viewModelScope.launch { loadIdentity() }
    }

    fun refreshSystem() {
        viewModelScope.launch { loadSystem() }
    }

    fun refreshNetwork() {
        viewModelScope.launch { loadNetwork() }
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

    /** A transient failure keeps the cached interface on screen; only a permission failure replaces it. */
    private suspend fun loadNetwork() {
        _uiState.update { it.copy(network = it.network.reloading()) }
        val result = networkRepository.fetch()
        _uiState.update { state ->
            val keepCurrent = result is WidgetResult.Failure && !result.permissionDenied && state.network is WidgetResult.Success
            if (keepCurrent) state else state.copy(network = result)
        }
    }

    private suspend fun loadConnect() {
        _uiState.update { it.copy(connect = it.connect.reloading()) }
        _uiState.update { it.copy(connect = serverRepository.getConnectInfo()) }
    }

    private companion object {
        const val NETWORK_MAX_AGE_ON_OPEN_MILLIS = 60 * 60 * 1000L
    }
}
