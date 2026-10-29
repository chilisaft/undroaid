package com.chilisaft.undroid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.ArrayStatus
import com.chilisaft.undroid.data.models.DashboardCards
import com.chilisaft.undroid.data.models.DockerContainerSummary
import com.chilisaft.undroid.data.models.NetworkRate
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.SystemMetrics
import com.chilisaft.undroid.data.models.TemperatureOverview
import com.chilisaft.undroid.data.models.VmDomain
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.NotificationsRepository
import com.chilisaft.undroid.data.repository.ServerNetworkRepository
import com.chilisaft.undroid.data.repository.ServerRepository
import com.chilisaft.undroid.data.repository.SettingsRepository
import com.chilisaft.undroid.data.repository.VmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A polled reading replaces the current one, except a failure replacing data already shown. */
private fun <T> WidgetResult<T>.keepOnFailure(next: WidgetResult<T>): WidgetResult<T> =
    if (next is WidgetResult.Failure && this is WidgetResult.Success) this else next

private fun DashboardScreenState.mapSystemMetrics(transform: (SystemMetrics) -> SystemMetrics): DashboardScreenState {
    val current = systemMetrics
    return if (current is WidgetResult.Success) copy(systemMetrics = WidgetResult.Success(transform(current.data))) else this
}


data class DashboardScreenState(
    val serverName: String? = null,
    val arrayStatus: WidgetResult<ArrayStatus> = WidgetResult.Loading,
    val parityCheck: WidgetResult<ParityCheckInfo> = WidgetResult.Loading,
    val systemMetrics: WidgetResult<SystemMetrics> = WidgetResult.Loading,
    val temperatures: WidgetResult<TemperatureOverview> = WidgetResult.Loading,
    /** Null inside a Success when the server lists no usable interface. */
    val network: WidgetResult<NetworkRate?> = WidgetResult.Loading,
    val containers: WidgetResult<List<DockerContainerSummary>> = WidgetResult.Loading,
    val vms: WidgetResult<List<VmDomain>> = WidgetResult.Loading,
    val unreadNotificationCount: WidgetResult<Int> = WidgetResult.Loading,
    /** Which optional cards Settings shows - hidden ones aren't fetched. */
    val cards: DashboardCards = DashboardCards(),
    /** Live updates are off in Settings - the network card has no rate to show (see [network]). */
    val liveUpdatesOff: Boolean = false,
    /** A full [DashboardViewModel.refresh] is running - drives the pull-to-refresh indicator, while widgets keep showing their current data. */
    val isRefreshing: Boolean = false
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
    private val notificationsRepository: NotificationsRepository,
    private val vmRepository: VmRepository,
    private val serverNetworkRepository: ServerNetworkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardScreenState())
    val uiState: StateFlow<DashboardScreenState> = _uiState.asStateFlow()

    private var parityCheckPollJob: Job? = null
    private var liveMetricsJob: Job? = null
    private var sensorsJob: Job? = null

    /** Whether the Dashboard is currently on screen - polling only runs while it is. */
    private var isVisible = false
    private var hasBeenVisible = false

    init {
        applySettings()
        refresh()
    }

    /** Settings can only change while the Dashboard is hidden, so they're re-read on each visit. */
    private fun applySettings() {
        _uiState.update {
            it.copy(cards = settingsRepository.dashboardCards.value, liveUpdatesOff = settingsRepository.liveUpdateSpeed.value.metricsMillis == null)
        }
    }

    /**
     * The Dashboard became visible (tab selected, returned from a drill-in screen, or the app came
     * back to the foreground). Starts the live polls and - on every visit after the first, which
     * `init` already covers - refreshes everything, since this ViewModel outlives tab switches and
     * would otherwise keep showing whatever it last fetched.
     */
    fun onScreenVisible() {
        isVisible = true
        if (hasBeenVisible) {
            applySettings()
            refresh()
        } else {
            hasBeenVisible = true
        }
        startLiveMetrics()
        startSensorsPoll()
        if (_uiState.value.parityCheck.isParityCheckRunning()) startParityCheckPollingIfNeeded()
    }

    /**
     * The Dashboard left the screen (another tab, a drill-in screen, or the app went to the
     * background). Stops every poll - they used to run for the ViewModel's whole lifetime,
     * hitting the server once a second even with the app in the background.
     */
    fun onScreenHidden() {
        isVisible = false
        liveMetricsJob?.cancel()
        liveMetricsJob = null
        sensorsJob?.cancel()
        sensorsJob = null
        stopParityCheckPolling()
    }

    /**
     * Polls CPU/memory once a second while the Dashboard is visible, merging each reading into the
     * system metrics widget once it has an initial value to update. No need to restart on
     * [refreshSystemMetrics].
     */
    private fun startLiveMetrics() {
        if (liveMetricsJob?.isActive == true) return
        val intervalMillis = settingsRepository.liveUpdateSpeed.value.metricsMillis ?: return
        liveMetricsJob = viewModelScope.launch {
            serverRepository.observeSystemMetricsPoll(intervalMillis).collect { sample ->
                _uiState.update {
                    it.mapSystemMetrics { metrics ->
                        metrics.copy(cpuLoadPercent = sample.cpuLoadPercent, memoryLoadPercent = sample.memoryLoadPercent)
                    }
                }
            }
        }
    }

    /**
     * Temperatures and network throughput while visible (see [ServerRepository.observeSensorsPoll]).
     * A failed tick keeps the last reading, unless there's none yet - so a permission failure
     * still shows. The server computes network rates since its previous call, so the first
     * reading after a pause can be 0.
     */
    private fun startSensorsPoll() {
        if (sensorsJob?.isActive == true || !_uiState.value.cards.sensors) return
        val intervalMillis = settingsRepository.liveUpdateSpeed.value.sensorsMillis ?: return
        sensorsJob = viewModelScope.launch {
            serverRepository.observeSensorsPoll({ serverNetworkRepository.cached()?.network?.interfaceName }, intervalMillis).collect { sample ->
                _uiState.update {
                    it.copy(
                        temperatures = it.temperatures.keepOnFailure(sample.temperatures),
                        network = it.network.keepOnFailure(sample.network)
                    )
                }
            }
        }
    }

    /** Reloads every widget at once (pull-to-refresh, revisiting the tab), keeping current data on screen meanwhile. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            listOf(
                launch { loadServerName() },
                launch { loadArrayStatus() },
                launch { loadParityCheck() },
                launch { loadSystemMetrics() },
                launch { if (_uiState.value.cards.docker) loadContainers() },
                launch { if (_uiState.value.cards.vms) loadVms() },
                // With live updates on, the sensors poll loads these; off, only a refresh does.
                launch { if (_uiState.value.cards.sensors && _uiState.value.liveUpdatesOff) loadTemperatures() },
                launch { loadUnreadCount() }
            ).joinAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshArrayStatus() {
        viewModelScope.launch { loadArrayStatus() }
    }

    fun refreshParityCheck() {
        viewModelScope.launch { loadParityCheck() }
    }

    fun refreshSystemMetrics() {
        viewModelScope.launch { loadSystemMetrics() }
    }

    fun refreshTemperatures() {
        viewModelScope.launch {
            _uiState.update { it.copy(temperatures = WidgetResult.Loading) }
            loadTemperatures()
        }
    }

    fun refreshNetwork() {
        viewModelScope.launch {
            _uiState.update { it.copy(network = WidgetResult.Loading) }
            loadNetwork()
        }
    }

    fun refreshContainers() {
        viewModelScope.launch { loadContainers() }
    }

    /** Reuses [VmRepository.getVms] - the VMS resource is already its own permission group and the schema has no separate trimmed-summary shape to query, unlike Docker's full container list. */
    fun refreshVms() {
        viewModelScope.launch { loadVms() }
    }

    /** Called on load and whenever the user returns from the Notifications screen. */
    fun refreshUnreadCount() {
        viewModelScope.launch { loadUnreadCount() }
    }

    private suspend fun loadServerName() {
        val result = serverRepository.getServerName()
        if (result is WidgetResult.Success) {
            _uiState.update { it.copy(serverName = result.data) }
        }
    }

    private suspend fun loadArrayStatus() {
        _uiState.update { it.copy(arrayStatus = it.arrayStatus.reloading()) }
        _uiState.update { it.copy(arrayStatus = serverRepository.getArrayStatus()) }
    }

    private suspend fun loadParityCheck() {
        _uiState.update { it.copy(parityCheck = it.parityCheck.reloading()) }
        val result = serverRepository.getParityCheckStatus()
        _uiState.update { it.copy(parityCheck = result) }
        if (result.isParityCheckRunning()) startParityCheckPollingIfNeeded() else stopParityCheckPolling()
    }

    private suspend fun loadSystemMetrics() {
        _uiState.update { it.copy(systemMetrics = it.systemMetrics.reloading()) }
        _uiState.update { it.copy(systemMetrics = serverRepository.getSystemMetrics()) }
    }

    private suspend fun loadTemperatures() {
        _uiState.update { it.copy(temperatures = serverRepository.getTemperatures()) }
    }

    private suspend fun loadNetwork() {
        _uiState.update { it.copy(network = serverRepository.getNetworkRate(serverNetworkRepository.cached()?.network?.interfaceName)) }
    }

    private suspend fun loadContainers() {
        _uiState.update { it.copy(containers = it.containers.reloading()) }
        _uiState.update { it.copy(containers = serverRepository.getDockerContainers()) }
    }

    private suspend fun loadVms() {
        _uiState.update { it.copy(vms = it.vms.reloading()) }
        _uiState.update { it.copy(vms = vmRepository.getVms()) }
    }

    private suspend fun loadUnreadCount() {
        _uiState.update { it.copy(unreadNotificationCount = notificationsRepository.getUnreadCount()) }
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
        if (!isVisible || parityCheckPollJob?.isActive == true) return
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

    private companion object {
        const val PARITY_POLL_INTERVAL_MS = 5_000L
    }
}
