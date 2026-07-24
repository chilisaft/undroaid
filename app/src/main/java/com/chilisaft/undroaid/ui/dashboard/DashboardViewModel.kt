package com.chilisaft.undroaid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.DockerContainerSummary
import com.chilisaft.undroaid.data.models.ParityCheckInfo
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.NotificationsRepository
import com.chilisaft.undroaid.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun DashboardScreenState.mapSystemMetrics(transform: (SystemMetrics) -> SystemMetrics): DashboardScreenState {
    val current = systemMetrics
    return if (current is WidgetResult.Success) copy(systemMetrics = WidgetResult.Success(transform(current.data))) else this
}


data class DashboardScreenState(
    val serverName: String? = null,
    val arrayStatus: WidgetResult<ArrayStatus> = WidgetResult.Loading,
    val parityCheck: WidgetResult<ParityCheckInfo> = WidgetResult.Loading,
    val systemMetrics: WidgetResult<SystemMetrics> = WidgetResult.Loading,
    val containers: WidgetResult<List<DockerContainerSummary>> = WidgetResult.Loading,
    val unreadNotificationCount: WidgetResult<Int> = WidgetResult.Loading
)

/**
 * Each widget is loaded and retried independently - a failure (or missing permission) on
 * one doesn't block or clear the others. See [ServerRepository] for why the queries are
 * split the way they are. [parityCheck] is its own widget (not folded into [arrayStatus]) so
 * a parity-specific hiccup never blanks the Array Health card, and vice versa.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardScreenState())
    val uiState: StateFlow<DashboardScreenState> = _uiState.asStateFlow()

    private var parityCheckPollJob: Job? = null

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
        refreshParityCheck()
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

    fun refreshParityCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(parityCheck = WidgetResult.Loading) }
            val result = serverRepository.getParityCheckStatus()
            _uiState.update { it.copy(parityCheck = result) }
            if (result.isParityCheckRunning()) startParityCheckPollingIfNeeded() else stopParityCheckPolling()
        }
    }

    /**
     * A running parity check's progress needs to move without the user manually pulling to
     * refresh - same reasoning as Main's parity-check poll (`MainViewModel`). Only re-fetches
     * the parity-check widget (not array status or any other widget) and stops itself once a
     * *successful* fetch confirms the check is no longer running. A failed tick is skipped
     * (keeping whatever was last shown) rather than stopping the loop - same reasoning as
     * `observeSystemMetricsPoll`. Previously this broke the loop on ANY failed tick (a `Failure`
     * result made `isParityCheckRunning()` return `false`, indistinguishable from "the check
     * actually finished"), so a single transient network hiccup - e.g. Wi-Fi reconnecting after
     * the device was locked for a while - would silently and permanently stop polling, leaving
     * the last error on screen with no way to recover short of a manual retry.
     */
    private fun startParityCheckPollingIfNeeded() {
        if (parityCheckPollJob?.isActive == true) return
        parityCheckPollJob = viewModelScope.launch {
            while (isActive) {
                delay(PARITY_POLL_INTERVAL_MS)
                when (val result = serverRepository.getParityCheckStatus()) {
                    is WidgetResult.Success -> {
                        _uiState.update { it.copy(parityCheck = result) }
                        if (!result.data.running) break
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun stopParityCheckPolling() {
        parityCheckPollJob?.cancel()
        parityCheckPollJob = null
    }

    private fun WidgetResult<ParityCheckInfo>.isParityCheckRunning(): Boolean =
        (this as? WidgetResult.Success)?.data?.running == true

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

    private companion object {
        const val PARITY_POLL_INTERVAL_MS = 5_000L
    }
}
