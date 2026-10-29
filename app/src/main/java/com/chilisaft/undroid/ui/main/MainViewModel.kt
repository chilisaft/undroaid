package com.chilisaft.undroid.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.ArrayOverview
import com.chilisaft.undroid.data.models.ArrayRunState
import com.chilisaft.undroid.data.models.DiskIdentification
import com.chilisaft.undroid.data.models.FlashInfo
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.ParityHistoryEntry
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.ArrayRepository
import com.chilisaft.undroid.ui.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainScreenState(
    val overview: WidgetResult<ArrayOverview> = WidgetResult.Loading,
    val parityCheck: WidgetResult<ParityCheckInfo> = WidgetResult.Loading,
    val diskIdentifications: WidgetResult<List<DiskIdentification>> = WidgetResult.Loading,
    val flashInfo: WidgetResult<FlashInfo> = WidgetResult.Loading,
    val parityHistory: WidgetResult<List<ParityHistoryEntry>> = WidgetResult.Loading,
    val actioningArray: Boolean = false,
    val actioningParityCheck: Boolean = false,
    /** A full [MainViewModel.refresh] is running - drives the pull-to-refresh indicator, while sections keep showing their current data. */
    val isRefreshing: Boolean = false
)

/**
 * Backs the Main tab. [overview] (array/cache/boot devices), [parityCheck] (its own
 * independently-loadable/retryable widget - see `ArrayRepository.getParityCheckStatus`), and
 * [diskIdentifications] (model/vendor/serial, a separate DISK-permission widget) load and retry
 * independently - a parity-specific hiccup no longer blanks the device tables, and vice versa.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val arrayRepository: ArrayRepository,
    private val userMessages: UserMessages
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    /** Whether the Main tab is currently on screen - the parity-check poll only runs while it is. */
    private var isVisible = false
    private var hasBeenVisible = false

    init {
        refresh()
    }

    /**
     * Main tab became visible. Every visit after the first refreshes: this ViewModel survives tab
     * switches, so without it the tab kept showing whatever it fetched when first opened (e.g. a
     * parity check started meanwhile never appeared). Also resumes the parity poll if one's running.
     */
    fun onScreenVisible() {
        isVisible = true
        if (hasBeenVisible) refresh() else hasBeenVisible = true
        if (_uiState.value.parityCheck.isParityCheckRunning()) startPollingIfNeeded()
    }

    /** Main tab left the screen (other tab, or app in background) - stop polling the server. */
    fun onScreenHidden() {
        isVisible = false
        stopPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            listOf(
                launch { refreshOverviewOnce() },
                launch { refreshParityCheckOnce() },
                launch { refreshDiskIdentificationsOnce() },
                launch { refreshFlashInfoOnce() },
                launch { refreshParityHistoryOnce() }
            ).joinAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshParityCheck() {
        viewModelScope.launch { refreshParityCheckOnce() }
    }

    fun refreshParityHistory() {
        viewModelScope.launch { refreshParityHistoryOnce() }
    }

    fun startArray(password: String?) =
        runArrayAction("start the array", ArrayRunState.STARTED) { arrayRepository.setArrayState(start = true, password = password) }

    fun stopArray() =
        runArrayAction("stop the array", ArrayRunState.STOPPED) { arrayRepository.setArrayState(start = false, password = null) }

    fun startParityCheck() = runParityCheckAction("start the parity check") { arrayRepository.startParityCheck() }

    fun pauseParityCheck() = runParityCheckAction("pause the parity check") { arrayRepository.pauseParityCheck() }

    fun resumeParityCheck() = runParityCheckAction("resume the parity check") { arrayRepository.resumeParityCheck() }

    fun cancelParityCheck() = runParityCheckAction("stop the parity check") { arrayRepository.cancelParityCheck() }

    private suspend fun refreshOverviewOnce() {
        _uiState.update { it.copy(overview = it.overview.reloading()) }
        _uiState.update { it.copy(overview = arrayRepository.getArrayOverview()) }
    }

    private suspend fun refreshParityCheckOnce() {
        _uiState.update { it.copy(parityCheck = it.parityCheck.reloading()) }
        val result = arrayRepository.getParityCheckStatus()
        _uiState.update { it.copy(parityCheck = result) }
        if (result.isParityCheckRunning()) startPollingIfNeeded() else stopPolling()
    }

    private suspend fun refreshDiskIdentificationsOnce() {
        _uiState.update { it.copy(diskIdentifications = it.diskIdentifications.reloading()) }
        _uiState.update { it.copy(diskIdentifications = arrayRepository.getDiskIdentifications()) }
    }

    private suspend fun refreshParityHistoryOnce() {
        _uiState.update { it.copy(parityHistory = it.parityHistory.reloading()) }
        _uiState.update { it.copy(parityHistory = arrayRepository.getParityHistory()) }
    }

    private suspend fun refreshFlashInfoOnce() {
        _uiState.update { it.copy(flashInfo = it.flashInfo.reloading()) }
        _uiState.update { it.copy(flashInfo = arrayRepository.getFlashInfo()) }
    }

    /**
     * A running parity check's progress needs to move without the user manually pulling to
     * refresh - same reasoning as the Docker log viewer's live tail. Polls only the parity-check
     * widget (not the overview or disk identifications, which don't change mid-check) and stops
     * itself once a *successful* fetch confirms the check is no longer running. A failed tick is
     * skipped (keeping whatever was last shown) rather than stopping the loop - same reasoning as
     * `ServerRepository.observeSystemMetricsPoll`. Previously this broke the loop on ANY failed
     * tick (a `Failure` result made `isParityCheckRunning()` return `false`, indistinguishable
     * from "the check actually finished"), so a single transient network hiccup - e.g. Wi-Fi
     * reconnecting after the device was locked for a while - would silently and permanently stop
     * polling, leaving the last error on screen with no way to recover short of a manual retry.
     */
    private fun startPollingIfNeeded() {
        if (!isVisible || pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(PARITY_POLL_INTERVAL_MS)
                when (val result = arrayRepository.getParityCheckStatus()) {
                    is WidgetResult.Success -> {
                        _uiState.update { it.copy(parityCheck = result) }
                        if (!result.data.running) break
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun WidgetResult<ParityCheckInfo>.isParityCheckRunning(): Boolean =
        (this as? WidgetResult.Success)?.data?.running == true

    /**
     * Starting/stopping the array takes a while server-side (mounting/unmounting every disk,
     * starting/stopping Docker and VMs), so - same as Docker/VM actions - the mutation resolving
     * doesn't mean the new state is visible yet. Keeps the button's spinner up and re-polls the
     * overview until [expectedState] is observed (or gives up after a bounded number of attempts),
     * then refreshes the rest of the tab. A failure is reported via [UserMessages].
     */
    private fun runArrayAction(description: String, expectedState: ArrayRunState, action: suspend () -> WidgetResult<Unit>) {
        if (_uiState.value.actioningArray) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningArray = true) }
            val result = action()
            if (result is WidgetResult.Success) {
                awaitArrayState(expectedState)
                launch { refreshParityCheckOnce() }
                launch { refreshDiskIdentificationsOnce() }
                launch { refreshFlashInfoOnce() }
            } else if (result is WidgetResult.Failure) {
                userMessages.showFailure(description, result)
            }
            _uiState.update { it.copy(actioningArray = false) }
        }
    }

    private suspend fun awaitArrayState(expectedState: ArrayRunState) {
        repeat(ARRAY_STATE_POLL_ATTEMPTS) { attempt ->
            val result = arrayRepository.getArrayOverview()
            if (result is WidgetResult.Success) {
                _uiState.update { it.copy(overview = result) }
                if (result.data.state == expectedState) return
            }
            if (attempt < ARRAY_STATE_POLL_ATTEMPTS - 1) delay(ARRAY_STATE_POLL_INTERVAL_MS)
        }
    }

    private fun runParityCheckAction(description: String, action: suspend () -> WidgetResult<Unit>) {
        if (_uiState.value.actioningParityCheck) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningParityCheck = true) }
            val result = action()
            _uiState.update { it.copy(actioningParityCheck = false) }
            when (result) {
                is WidgetResult.Success -> refresh()
                is WidgetResult.Failure -> userMessages.showFailure(description, result)
                WidgetResult.Loading -> Unit
            }
        }
    }

    private companion object {
        const val PARITY_POLL_INTERVAL_MS = 5_000L
        const val ARRAY_STATE_POLL_ATTEMPTS = 15
        const val ARRAY_STATE_POLL_INTERVAL_MS = 2_000L
    }
}
