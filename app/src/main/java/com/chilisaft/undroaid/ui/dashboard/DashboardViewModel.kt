package com.chilisaft.undroaid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.DockerContainerSummary
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.NotificationsRepository
import com.chilisaft.undroaid.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun DashboardScreenState.mapSystemMetrics(transform: (SystemMetrics) -> SystemMetrics): DashboardScreenState {
    val current = systemMetrics
    return if (current is WidgetResult.Success) copy(systemMetrics = WidgetResult.Success(transform(current.data))) else this
}


data class DashboardScreenState(
    val serverName: String? = null,
    val arrayStatus: WidgetResult<ArrayStatus> = WidgetResult.Loading,
    val systemMetrics: WidgetResult<SystemMetrics> = WidgetResult.Loading,
    val containers: WidgetResult<List<DockerContainerSummary>> = WidgetResult.Loading,
    val unreadNotificationCount: WidgetResult<Int> = WidgetResult.Loading
)

/**
 * Each widget is loaded and retried independently - a failure (or missing permission) on
 * one doesn't block or clear the others. See [ServerRepository] for why the queries are
 * split the way they are.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardScreenState())
    val uiState: StateFlow<DashboardScreenState> = _uiState.asStateFlow()

    init {
        refresh()
        observeLiveMetrics()
    }

    /**
     * Polls CPU/memory once a second, merging each reading into the system metrics widget
     * once it has an initial value to update. Runs for the lifetime of the ViewModel - no
     * need to restart on [refreshSystemMetrics].
     */
    private fun observeLiveMetrics() {
        viewModelScope.launch {
            serverRepository.observeSystemMetricsPoll().collect { sample ->
                _uiState.update {
                    it.mapSystemMetrics { metrics ->
                        metrics.copy(cpuLoadPercent = sample.cpuLoadPercent, memoryLoadPercent = sample.memoryLoadPercent)
                    }
                }
            }
        }
    }

    fun refresh() {
        refreshServerName()
        refreshArrayStatus()
        refreshSystemMetrics()
        refreshContainers()
        refreshUnreadCount()
    }

    fun refreshServerName() {
        viewModelScope.launch {
            val result = serverRepository.getServerName()
            if (result is WidgetResult.Success) {
                _uiState.update { it.copy(serverName = result.data) }
            }
        }
    }

    fun refreshArrayStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(arrayStatus = WidgetResult.Loading) }
            _uiState.update { it.copy(arrayStatus = serverRepository.getArrayStatus()) }
        }
    }

    fun refreshSystemMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(systemMetrics = WidgetResult.Loading) }
            _uiState.update { it.copy(systemMetrics = serverRepository.getSystemMetrics()) }
        }
    }

    fun refreshContainers() {
        viewModelScope.launch {
            _uiState.update { it.copy(containers = WidgetResult.Loading) }
            _uiState.update { it.copy(containers = serverRepository.getDockerContainers()) }
        }
    }

    /** Called on load and whenever the user returns from the Notifications screen. */
    fun refreshUnreadCount() {
        viewModelScope.launch {
            _uiState.update { it.copy(unreadNotificationCount = notificationsRepository.getUnreadCount()) }
        }
    }
}
