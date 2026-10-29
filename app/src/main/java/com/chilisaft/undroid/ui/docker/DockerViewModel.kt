package com.chilisaft.undroid.ui.docker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.DockerContainer
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.DockerLogLine
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.DockerRepository
import com.chilisaft.undroid.ui.UserMessages
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
    val logs: DockerLogsState? = null,
    /** "Check for updates" is running (it takes several seconds on a real server). */
    val isCheckingForUpdates: Boolean = false,
    /** A [DockerViewModel.refresh] is running - drives the pull-to-refresh indicator, while the list keeps showing its current data. */
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DockerViewModel @Inject constructor(
    private val dockerRepository: DockerRepository,
    private val userMessages: UserMessages
) : ViewModel() {

    private val _uiState = MutableStateFlow(DockerScreenState())
    val uiState: StateFlow<DockerScreenState> = _uiState.asStateFlow()

    private var isVisible = false
    private var hasBeenVisible = false

    init {
        refresh()
    }

    /**
     * Docker tab became visible. Refreshes on every visit after the first (this ViewModel survives
     * tab switches) and resumes an open log viewer's live tail where it left off.
     */
    fun onScreenVisible() {
        isVisible = true
        if (hasBeenVisible) refresh() else hasBeenVisible = true
        if (_uiState.value.logs != null) startLogPolling()
    }

    /** Docker tab left the screen (app in background) - pause the log tail rather than polling every 3s unseen. */
    fun onScreenHidden() {
        isVisible = false
        logsJob?.cancel()
        logsJob = null
    }

    /** The container's web UI link couldn't be opened (no app for it, or not a valid URL). */
    fun reportUnopenableUrl(url: String) {
        userMessages.show("Couldn't open $url")
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(containers = it.containers.reloading(), isRefreshing = true) }
            val result = dockerRepository.getContainers()
            _uiState.update { it.copy(containers = result, isRefreshing = false) }
        }
    }

    fun start(id: String) = runAction(id, "start", DockerContainerState.RUNNING) { dockerRepository.startContainer(id) }

    fun stop(id: String) = runAction(id, "stop", DockerContainerState.EXITED) { dockerRepository.stopContainer(id) }

    fun pause(id: String) = runAction(id, "pause", DockerContainerState.PAUSED) { dockerRepository.pauseContainer(id) }

    fun unpause(id: String) = runAction(id, "resume", DockerContainerState.RUNNING) { dockerRepository.unpauseContainer(id) }

    fun restart(id: String) = runAction(id, "restart", DockerContainerState.RUNNING) { dockerRepository.restartContainer(id) }

    /** Asks the registries for new images, then reloads the list with the fresh update flags. */
    fun checkForUpdates() {
        if (_uiState.value.isCheckingForUpdates) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdates = true) }
            when (val result = dockerRepository.checkForUpdates()) {
                is WidgetResult.Success -> {
                    val containers = dockerRepository.getContainers()
                    _uiState.update { it.copy(containers = containers) }
                    if (containers is WidgetResult.Success) {
                        val count = containers.data.count { it.updateAvailable }
                        userMessages.show(if (count == 0) "No updates available" else "$count ${if (count == 1) "update" else "updates"} available")
                    }
                }
                is WidgetResult.Failure -> userMessages.showFailure("check for updates", result)
                WidgetResult.Loading -> Unit
            }
            _uiState.update { it.copy(isCheckingForUpdates = false) }
        }
    }

    fun update(id: String) = runUpdate(listOf(id)) { dockerRepository.updateContainer(id) }

    /** Updates exactly [ids] - the containers the user saw and confirmed, not whatever has an update by now. */
    fun updateAll(ids: List<String>) = runUpdate(ids) { dockerRepository.updateContainers(ids) }

    /**
     * Updating recreates each container, which gives it a new id - so instead of polling for the
     * old id's state like [runAction], it reloads the list once the server reports it's done.
     */
    private fun runUpdate(ids: List<String>, action: suspend () -> WidgetResult<Unit>) {
        if (ids.isEmpty() || ids.any { it in _uiState.value.actioningIds }) return
        val names = ids.map { containerName(it) }
        viewModelScope.launch {
            _uiState.update { it.copy(actioningIds = it.actioningIds + ids) }
            val result = action()
            val containers = dockerRepository.getContainers()
            _uiState.update { state ->
                state.copy(containers = if (containers is WidgetResult.Success) containers else state.containers, actioningIds = state.actioningIds - ids.toSet())
            }
            val what = names.singleOrNull() ?: "${names.size} containers"
            when (result) {
                is WidgetResult.Success -> userMessages.show("Updated $what")
                is WidgetResult.Failure -> userMessages.showFailure("update $what", result)
                WidgetResult.Loading -> Unit
            }
        }
    }

    /**
     * The mutation resolving doesn't mean the daemon has actually finished the transition yet
     * (a `stop` can take several seconds to go from a graceful SIGTERM to actually exited) - a
     * single immediate [refresh] can still observe the pre-action state. Keeps the row's spinner
     * up and re-polls [expectedState] until it's actually observed (or gives up after a bounded
     * number of attempts and leaves whatever was last fetched, same as any other widget).
     * A failure is reported via [UserMessages] as "Couldn't [verb] <name>: <reason>".
     */
    private fun runAction(id: String, verb: String, expectedState: DockerContainerState, action: suspend () -> WidgetResult<Unit>) {
        if (id in _uiState.value.actioningIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(actioningIds = it.actioningIds + id) }
            when (val result = action()) {
                is WidgetResult.Success -> awaitContainerState(id, expectedState)
                is WidgetResult.Failure -> userMessages.showFailure("$verb ${containerName(id)}", result)
                WidgetResult.Loading -> Unit
            }
            _uiState.update { it.copy(actioningIds = it.actioningIds - id) }
        }
    }

    private fun containerName(id: String): String =
        (_uiState.value.containers as? WidgetResult.Success)?.data?.firstOrNull { it.id == id }?.name ?: "the container"

    private suspend fun awaitContainerState(id: String, expectedState: DockerContainerState) {
        repeat(STATE_POLL_ATTEMPTS) { attempt ->
            val result = dockerRepository.getContainers()
            if (result is WidgetResult.Success) {
                _uiState.update { it.copy(containers = result) }
                if (result.data.firstOrNull { it.id == id }?.state == expectedState) return
            }
            if (attempt < STATE_POLL_ATTEMPTS - 1) delay(STATE_POLL_INTERVAL_MS)
        }
    }

    private var logsJob: Job? = null

    // The open log viewer's tail so far - kept outside the polling job so pausing (screen hidden)
    // and resuming continues from the last cursor instead of re-fetching the whole tail.
    private var logLines: List<DockerLogLine> = emptyList()
    private var logCursor: String? = null

    fun openLogs(id: String, name: String) {
        logsJob?.cancel()
        logLines = emptyList()
        logCursor = null
        _uiState.update { it.copy(logs = DockerLogsState(containerId = id, containerName = name)) }
        startLogPolling()
    }

    fun retryLogs() {
        val logs = _uiState.value.logs ?: return
        openLogs(logs.containerId, logs.containerName)
    }

    fun closeLogs() {
        logsJob?.cancel()
        logsJob = null
        logLines = emptyList()
        logCursor = null
        _uiState.update { it.copy(logs = null) }
    }

    /**
     * First fetch grabs the last [LOG_TAIL_LINES] lines, then polls every [LOG_POLL_INTERVAL_MS]
     * using the previous response's cursor to fetch only new lines. Only runs while the screen is
     * visible - see [onScreenHidden].
     */
    private fun startLogPolling() {
        val id = _uiState.value.logs?.containerId ?: return
        if (!isVisible || logsJob?.isActive == true) return
        logsJob = viewModelScope.launch {
            while (isActive) {
                when (val result = dockerRepository.getLogs(id = id, tail = if (logLines.isEmpty()) LOG_TAIL_LINES else null, since = logCursor)) {
                    is WidgetResult.Success -> {
                        logLines = (logLines + result.data.lines).takeLast(MAX_LOG_LINES)
                        logCursor = result.data.cursor ?: logCursor
                        _uiState.update { it.copy(logs = it.logs?.copy(result = WidgetResult.Success(logLines))) }
                    }
                    // A poll failure once we already have content is likely transient (the daemon
                    // hiccuping, a dropped connection) - keep showing the last good lines and let
                    // the next poll retry, rather than blanking a working log view.
                    is WidgetResult.Failure -> if (logLines.isEmpty()) {
                        _uiState.update { it.copy(logs = it.logs?.copy(result = result)) }
                    }
                    WidgetResult.Loading -> Unit
                }
                delay(LOG_POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val LOG_TAIL_LINES = 300
        const val MAX_LOG_LINES = 2000
        const val LOG_POLL_INTERVAL_MS = 3_000L
        const val STATE_POLL_ATTEMPTS = 10
        const val STATE_POLL_INTERVAL_MS = 1_000L
    }
}
