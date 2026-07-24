package com.chilisaft.undroaid.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.ArrayOverview
import com.chilisaft.undroaid.data.models.DiskIdentification
import com.chilisaft.undroaid.data.models.FlashInfo
import com.chilisaft.undroaid.data.models.ParityCheckInfo
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.ArrayRepository
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

data class MainScreenState(
    val overview: WidgetResult<ArrayOverview> = WidgetResult.Loading,
    val parityCheck: WidgetResult<ParityCheckInfo> = WidgetResult.Loading,
    val diskIdentifications: WidgetResult<List<DiskIdentification>> = WidgetResult.Loading,
    val flashInfo: WidgetResult<FlashInfo> = WidgetResult.Loading,
    val actioningArray: Boolean = false,
    val actioningParityCheck: Boolean = false
)

/**
 * Backs the Main tab. [overview] (array/cache/boot devices), [parityCheck] (its own
 * independently-loadable/retryable widget - see `ArrayRepository.getParityCheckStatus`), and
 * [diskIdentifications] (model/vendor/serial, a separate DISK-permission widget) load and retry
 * independently - a parity-specific hiccup no longer blanks the device tables, and vice versa.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val arrayRepository: ArrayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { refreshOverviewOnce() }
        viewModelScope.launch { refreshParityCheckOnce() }
        viewModelScope.launch { refreshDiskIdentificationsOnce() }
        viewModelScope.launch { refreshFlashInfoOnce() }
    }

    fun refreshParityCheck() {
        viewModelScope.launch { refreshParityCheckOnce() }
    }

    fun startArray(password: String?) = runArrayAction { arrayRepository.setArrayState(start = true, password = password) }

    fun stopArray() = runArrayAction { arrayRepository.setArrayState(start = false, password = null) }

    fun startParityCheck() = runParityCheckAction { arrayRepository.startParityCheck() }

    fun pauseParityCheck() = runParityCheckAction { arrayRepository.pauseParityCheck() }

    fun resumeParityCheck() = runParityCheckAction { arrayRepository.resumeParityCheck() }

    fun cancelParityCheck() = runParityCheckAction { arrayRepository.cancelParityCheck() }

    private suspend fun refreshOverviewOnce() {
        _uiState.update { it.copy(overview = WidgetResult.Loading) }
        _uiState.update { it.copy(overview = arrayRepository.getArrayOverview()) }
    }

    private suspend fun refreshParityCheckOnce() {
        _uiState.update { it.copy(parityCheck = WidgetResult.Loading) }
        val result = arrayRepository.getParityCheckStatus()
        _uiState.update { it.copy(parityCheck = result) }
        if (result.isParityCheckRunning()) startPollingIfNeeded() else stopPolling()
    }

    private suspend fun refreshDiskIdentificationsOnce() {
        _uiState.update { it.copy(diskIdentifications = WidgetResult.Loading) }
        _uiState.update { it.copy(diskIdentifications = arrayRepository.getDiskIdentifications()) }
    }

    private suspend fun refreshFlashInfoOnce() {
        _uiState.update { it.copy(flashInfo = WidgetResult.Loading) }
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
        if (pollJob?.isActive == true) return
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

    private fun runArrayAction(action: suspend () -> WidgetResult<Unit>) {
        if (_uiState.value.actioningArray) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningArray = true) }
            val result = action()
            _uiState.update { it.copy(actioningArray = false) }
            if (result is WidgetResult.Success) refresh()
        }
    }

    private fun runParityCheckAction(action: suspend () -> WidgetResult<Unit>) {
        if (_uiState.value.actioningParityCheck) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningParityCheck = true) }
            val result = action()
            _uiState.update { it.copy(actioningParityCheck = false) }
            if (result is WidgetResult.Success) refresh()
        }
    }

    private companion object {
        const val PARITY_POLL_INTERVAL_MS = 5_000L
    }
}
