package com.chilisaft.undroaid.ui.docker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.DockerContainer
import com.chilisaft.undroaid.data.models.DockerLogLine
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.DockerRepository
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

data class DockerLogsState(
    val containerId: String,
    val containerName: String,
    val result: WidgetResult<List<DockerLogLine>> = WidgetResult.Loading
)

data class DockerScreenState(
    val containers: WidgetResult<List<DockerContainer>> = WidgetResult.Loading,
    val actioningIds: Set<String> = emptySet(),
    val logs: DockerLogsState? = null
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

    fun restart(id: String) = runAction(id) { dockerRepository.restartContainer(id) }

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

    private var logsJob: Job? = null

    fun openLogs(id: String, name: String) {
        logsJob?.cancel()
        _uiState.update { it.copy(logs = DockerLogsState(containerId = id, containerName = name)) }
        logsJob = viewModelScope.launch {
            var cursor: String? = null
            var lines: List<DockerLogLine> = emptyList()
            while (isActive) {
                when (val result = dockerRepository.getLogs(id = id, tail = if (lines.isEmpty()) LOG_TAIL_LINES else null, since = cursor)) {
                    is WidgetResult.Success -> {
                        lines = (lines + result.data.lines).takeLast(MAX_LOG_LINES)
                        cursor = result.data.cursor ?: cursor
                        _uiState.update { it.copy(logs = it.logs?.copy(result = WidgetResult.Success(lines))) }
                    }
                    // A poll failure once we already have content is likely transient (the daemon
                    // hiccuping, a dropped connection) - keep showing the last good lines and let
                    // the next poll retry, rather than blanking a working log view.
                    is WidgetResult.Failure -> if (lines.isEmpty()) {
                        _uiState.update { it.copy(logs = it.logs?.copy(result = result)) }
                    }
                    WidgetResult.Loading -> Unit
                }
                delay(LOG_POLL_INTERVAL_MS)
            }
        }
    }

    fun retryLogs() {
        val logs = _uiState.value.logs ?: return
        openLogs(logs.containerId, logs.containerName)
    }

    fun closeLogs() {
        logsJob?.cancel()
        logsJob = null
        _uiState.update { it.copy(logs = null) }
    }

    private companion object {
        const val LOG_TAIL_LINES = 300
        const val MAX_LOG_LINES = 2000
        const val LOG_POLL_INTERVAL_MS = 3_000L
    }
}
