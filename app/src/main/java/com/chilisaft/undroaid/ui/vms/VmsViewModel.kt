package com.chilisaft.undroaid.ui.vms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.VmDomain
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.VmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VmsScreenState(
    val vms: WidgetResult<List<VmDomain>> = WidgetResult.Loading,
    val actioningIds: Set<String> = emptySet()
)

/** Backs the VMs tab - same shape as [com.chilisaft.undroaid.ui.docker.DockerViewModel] for the Docker tab. */
@HiltViewModel
class VmsViewModel @Inject constructor(
    private val vmRepository: VmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VmsScreenState())
    val uiState: StateFlow<VmsScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(vms = WidgetResult.Loading) }
            _uiState.update { it.copy(vms = vmRepository.getVms()) }
        }
    }

    fun start(id: String) = runAction(id) { vmRepository.startVm(id) }

    fun stop(id: String) = runAction(id) { vmRepository.stopVm(id) }

    fun forceStop(id: String) = runAction(id) { vmRepository.forceStopVm(id) }

    fun pause(id: String) = runAction(id) { vmRepository.pauseVm(id) }

    fun resume(id: String) = runAction(id) { vmRepository.resumeVm(id) }

    fun reboot(id: String) = runAction(id) { vmRepository.rebootVm(id) }

    private fun runAction(id: String, action: suspend () -> WidgetResult<Unit>) {
        if (id in _uiState.value.actioningIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningIds = it.actioningIds + id) }
            val result = action()
            _uiState.update { it.copy(actioningIds = it.actioningIds - id) }
            if (result is WidgetResult.Success) refresh()
        }
    }
}
