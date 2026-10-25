package com.chilisaft.undroid.ui.vms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.VmDomain
import com.chilisaft.undroid.data.models.VmRunState
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.VmRepository
import com.chilisaft.undroid.ui.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VmsScreenState(
    val vms: WidgetResult<List<VmDomain>> = WidgetResult.Loading,
    val actioningIds: Set<String> = emptySet(),
    /** A refresh is running - drives the pull-to-refresh indicator, while the list keeps showing its current data. */
    val isRefreshing: Boolean = false
)

/** Backs the VMs tab - same shape as [com.chilisaft.undroid.ui.docker.DockerViewModel] for the Docker tab. */
@HiltViewModel
class VmsViewModel @Inject constructor(
    private val vmRepository: VmRepository,
    private val userMessages: UserMessages
) : ViewModel() {

    private val _uiState = MutableStateFlow(VmsScreenState())
    val uiState: StateFlow<VmsScreenState> = _uiState.asStateFlow()

    private var hasBeenVisible = false

    init {
        refresh()
    }

    /** Refreshes on every visit after the first - this ViewModel survives tab switches, so the list would otherwise go stale. */
    fun onScreenVisible() {
        if (hasBeenVisible) refresh() else hasBeenVisible = true
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(vms = it.vms.reloading(), isRefreshing = true) }
            val result = vmRepository.getVms()
            _uiState.update { it.copy(vms = result, isRefreshing = false) }
        }
    }

    fun start(id: String) = runAction(id, "start", VmRunState.RUNNING) { vmRepository.startVm(id) }

    fun stop(id: String) = runAction(id, "stop", VmRunState.STOPPED) { vmRepository.stopVm(id) }

    fun forceStop(id: String) = runAction(id, "force stop", VmRunState.STOPPED) { vmRepository.forceStopVm(id) }

    fun pause(id: String) = runAction(id, "pause", VmRunState.PAUSED) { vmRepository.pauseVm(id) }

    fun resume(id: String) = runAction(id, "resume", VmRunState.RUNNING) { vmRepository.resumeVm(id) }

    fun reboot(id: String) = runAction(id, "restart", VmRunState.RUNNING) { vmRepository.rebootVm(id) }

    /**
     * Same reasoning as [com.chilisaft.undroid.ui.docker.DockerViewModel]'s equivalent - the
     * mutation resolving doesn't mean libvirt has actually finished the transition yet, so a
     * single immediate [refresh] can still observe the pre-action state. Keeps the row's spinner
     * up and re-polls [expectedState] until it's actually observed (or gives up after a bounded
     * number of attempts and leaves whatever was last fetched, same as any other widget).
     * A failure is reported via [UserMessages] as "Couldn't [verb] <name>: <reason>".
     */
    private fun runAction(id: String, verb: String, expectedState: VmRunState, action: suspend () -> WidgetResult<Unit>) {
        if (id in _uiState.value.actioningIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningIds = it.actioningIds + id) }
            when (val result = action()) {
                is WidgetResult.Success -> awaitVmState(id, expectedState)
                is WidgetResult.Failure -> userMessages.showFailure("$verb ${vmName(id)}", result)
                WidgetResult.Loading -> Unit
            }
            _uiState.update { it.copy(actioningIds = it.actioningIds - id) }
        }
    }

    private fun vmName(id: String): String =
        (_uiState.value.vms as? WidgetResult.Success)?.data?.firstOrNull { it.id == id }?.name ?: "the VM"

    private suspend fun awaitVmState(id: String, expectedState: VmRunState) {
        repeat(STATE_POLL_ATTEMPTS) { attempt ->
            val result = vmRepository.getVms()
            if (result is WidgetResult.Success) {
                _uiState.update { it.copy(vms = result) }
                if (result.data.firstOrNull { it.id == id }?.state == expectedState) return
            }
            if (attempt < STATE_POLL_ATTEMPTS - 1) delay(STATE_POLL_INTERVAL_MS)
        }
    }

    private companion object {
        const val STATE_POLL_ATTEMPTS = 10
        const val STATE_POLL_INTERVAL_MS = 1_000L
    }
}
