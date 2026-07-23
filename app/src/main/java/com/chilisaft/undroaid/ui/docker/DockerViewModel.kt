package com.chilisaft.undroaid.ui.docker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.DockerContainer
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.DockerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DockerScreenState(
    val containers: WidgetResult<List<DockerContainer>> = WidgetResult.Loading,
    val actioningIds: Set<String> = emptySet()
)

@HiltViewModel
class DockerViewModel @Inject constructor(
    private val dockerRepository: DockerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DockerScreenState())
    val uiState: StateFlow<DockerScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(containers = WidgetResult.Loading) }
            _uiState.update { it.copy(containers = dockerRepository.getContainers()) }
        }
    }

    fun start(id: String) = runAction(id) { dockerRepository.startContainer(id) }

    fun stop(id: String) = runAction(id) { dockerRepository.stopContainer(id) }

    fun pause(id: String) = runAction(id) { dockerRepository.pauseContainer(id) }

    fun unpause(id: String) = runAction(id) { dockerRepository.unpauseContainer(id) }

    /**
     * There's no "restart" mutation in the schema, only start/stop/pause/unpause - so this
     * chains stop then start. Always refreshes afterwards regardless of outcome (unlike the
     * single-mutation actions above, which only refresh on success): if stop succeeds but the
     * following start fails, the container is still left stopped, and the list needs to reflect
     * that rather than silently keeping stale "running" state.
     */
    fun restart(id: String) {
        if (id in _uiState.value.actioningIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningIds = it.actioningIds + id) }
            val stopResult = dockerRepository.stopContainer(id)
            if (stopResult is WidgetResult.Success) {
                dockerRepository.startContainer(id)
            }
            _uiState.update { it.copy(actioningIds = it.actioningIds - id) }
            refresh()
        }
    }

    private fun runAction(id: String, action: suspend () -> WidgetResult<Unit>) {
        if (id in _uiState.value.actioningIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningIds = it.actioningIds + id) }
            val result = action()
            _uiState.update { it.copy(actioningIds = it.actioningIds - id) }
            if (result is WidgetResult.Success) {
                refresh()
            }
        }
    }
}
