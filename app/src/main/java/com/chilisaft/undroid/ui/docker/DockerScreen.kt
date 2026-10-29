package com.chilisaft.undroid.ui.docker

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.chilisaft.undroid.data.models.DockerContainer
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.DockerLogLine
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.ConfirmActionDialog
import com.chilisaft.undroid.ui.components.ConfirmDialogBody
import com.chilisaft.undroid.ui.components.ContainerIconBadge
import com.chilisaft.undroid.ui.components.ActionSheet
import com.chilisaft.undroid.ui.components.ActionSheetItem
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.ResourceHeaderCard
import com.chilisaft.undroid.ui.components.ResourceRowCard
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.TabTopBar
import com.chilisaft.undroid.ui.components.tryOpenUri
import com.chilisaft.undroid.ui.components.statusTone
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing

@Composable
fun DockerScreen(viewModel: DockerViewModel = hiltViewModel(), topBarActions: @Composable RowScope.() -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }
    DockerScreenContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        topBarActions = topBarActions,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onPause = viewModel::pause,
        onUnpause = viewModel::unpause,
        onRestart = viewModel::restart,
        onCheckForUpdates = viewModel::checkForUpdates,
        onUpdate = viewModel::update,
        onUpdateAll = viewModel::updateAll,
        onViewLogs = viewModel::openLogs,
        onRetryLogs = viewModel::retryLogs,
        onCloseLogs = viewModel::closeLogs,
        onOpenUrlFailed = viewModel::reportUnopenableUrl
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockerScreenContent(
    uiState: DockerScreenState,
    onRetry: () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    onStart: (String) -> Unit = {},
    onStop: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onUnpause: (String) -> Unit = {},
    onRestart: (String) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onUpdate: (String) -> Unit = {},
    onUpdateAll: (List<String>) -> Unit = {},
    onViewLogs: (String, String) -> Unit = { _, _ -> },
    onRetryLogs: () -> Unit = {},
    onCloseLogs: () -> Unit = {},
    onOpenUrlFailed: (String) -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var selectedContainer by remember { mutableStateOf<DockerContainer?>(null) }
    var confirmUpdate by remember { mutableStateOf<DockerContainer?>(null) }
    var confirmUpdateAll by remember { mutableStateOf<List<DockerContainer>?>(null) }

    Scaffold(
        topBar = { TabTopBar(title = "Docker", icon = Icons.Filled.Layers, actions = topBarActions) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.containers, onRetry = onRetry, minHeight = 200.dp) { containers ->
                if (containers.isEmpty()) {
                    EmptyState("No containers found")
                } else {
                    ScreenList {
                        item {
                            UpdatesCard(
                                updates = containers.filter { it.updateAvailable },
                                isChecking = uiState.isCheckingForUpdates,
                                isUpdating = containers.any { it.updateAvailable && it.id in uiState.actioningIds },
                                onCheck = onCheckForUpdates,
                                onUpdateAll = { confirmUpdateAll = it }
                            )
                        }
                        items(containers, key = { it.id }) { container ->
                            ResourceRowCard(
                                title = container.name,
                                tone = container.state.statusTone,
                                statusText = container.statusText,
                                isActioning = container.id in uiState.actioningIds,
                                onClick = { selectedContainer = container },
                                tag = if (container.updateAvailable) "Update" else null,
                                leading = { size ->
                                    ContainerIconBadge(container.iconUrl, isRunning = container.state == DockerContainerState.RUNNING, size = size)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val container = selectedContainer
    if (container != null) {
        ContainerActionSheet(
            container = container,
            onDismiss = { selectedContainer = null },
            onStart = { onStart(container.id); selectedContainer = null },
            onStop = { onStop(container.id); selectedContainer = null },
            onPause = { onPause(container.id); selectedContainer = null },
            onUnpause = { onUnpause(container.id); selectedContainer = null },
            onRestart = { onRestart(container.id); selectedContainer = null },
            onUpdate = { confirmUpdate = container; selectedContainer = null },
            onViewLogs = { onViewLogs(container.id, container.name); selectedContainer = null },
            onOpenUrlFailed = onOpenUrlFailed
        )
    }

    confirmUpdate?.let { target ->
        ConfirmActionDialog(
            title = "Update ${target.name}?",
            text = "Pulls the latest image and recreates the container. It's unavailable while that happens.",
            confirmLabel = "Update",
            onConfirm = { onUpdate(target.id); confirmUpdate = null },
            onDismiss = { confirmUpdate = null }
        )
    }

    confirmUpdateAll?.let { targets ->
        ConfirmActionDialog(
            title = "Update ${targets.size} ${if (targets.size == 1) "container" else "containers"}?",
            text = "Pulls the latest images and recreates these containers. Each is unavailable while it's updated.",
            confirmLabel = "Update All",
            onConfirm = { onUpdateAll(targets.map { it.id }); confirmUpdateAll = null },
            onDismiss = { confirmUpdateAll = null }
        ) {
            ContainerNameList(targets)
        }
    }

    val logs = uiState.logs
    if (logs != null) {
        ContainerLogsDialog(state = logs, onRetry = onRetryLogs, onDismiss = onCloseLogs)
    }
}

@Composable
private fun ContainerActionSheet(
    container: DockerContainer,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onUnpause: () -> Unit,
    onRestart: () -> Unit,
    onUpdate: () -> Unit,
    onViewLogs: () -> Unit,
    onOpenUrlFailed: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    ActionSheet(
        onDismissRequest = onDismiss,
        header = {
            ResourceHeaderCard(
                title = container.name,
                subtitle = container.image,
                tone = container.state.statusTone,
                statusText = container.statusText,
                leading = { size ->
                    ContainerIconBadge(container.iconUrl, isRunning = container.state == DockerContainerState.RUNNING, size = size)
                }
            )
        }
    ) {
        when (container.state) {
            DockerContainerState.RUNNING -> {
                ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onRestart)
                ActionSheetItem(Icons.Filled.Pause, "Pause", onPause)
                ActionSheetItem(Icons.Filled.Stop, "Stop", onStop, destructive = true)
            }
            DockerContainerState.PAUSED -> {
                ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onRestart)
                ActionSheetItem(Icons.Filled.PlayArrow, "Resume", onUnpause)
                ActionSheetItem(Icons.Filled.Stop, "Stop", onStop, destructive = true)
            }
            DockerContainerState.EXITED -> {
                ActionSheetItem(Icons.Filled.PlayArrow, "Start", onStart)
            }
        }
        if (container.updateAvailable) {
            ActionSheetItem(Icons.Filled.SystemUpdateAlt, "Update", onUpdate)
        }
        ActionSheetItem(Icons.AutoMirrored.Filled.Article, "View Logs", onViewLogs)
        if (!container.webUiUrl.isNullOrBlank()) {
            ActionSheetItem(Icons.AutoMirrored.Filled.OpenInNew, "Open Web UI", {
                if (!uriHandler.tryOpenUri(container.webUiUrl)) onOpenUrlFailed(container.webUiUrl)
                onDismiss()
            })
        }
    }
}

/**
 * The top of the Docker list: what Unraid reports as updatable, with "Update all", and a manual
 * "Check for updates" (the registries are only asked when the user taps it - it takes seconds).
 */
@Composable
private fun UpdatesCard(
    updates: List<DockerContainer>,
    isChecking: Boolean,
    isUpdating: Boolean,
    onCheck: () -> Unit,
    onUpdateAll: (List<DockerContainer>) -> Unit
) {
    val spacing = MaterialTheme.spacing
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SystemUpdateAlt,
                    contentDescription = null,
                    tint = if (updates.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(spacing.small))
                Text(
                    when {
                        isChecking -> "Checking for updates…"
                        updates.isEmpty() -> "No updates available"
                        else -> "${updates.size} ${if (updates.size == 1) "update" else "updates"} available"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onCheck, enabled = !isUpdating) { Text(if (updates.isEmpty()) "Check" else "Check Again") }
                }
            }
            if (updates.isNotEmpty()) {
                Text(
                    updates.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { onUpdateAll(updates) }, enabled = !isUpdating && !isChecking, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isUpdating) "Updating…" else "Update All")
                }
            }
        }
    }
}

/** Icon + name per container, for dialogs that act on several at once. */
@Composable
private fun ContainerNameList(containers: List<DockerContainer>) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        containers.forEach { container ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContainerIconBadge(container.iconUrl, isRunning = container.state == DockerContainerState.RUNNING)
                Spacer(Modifier.width(spacing.small))
                Text(
                    container.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Large near-fullscreen modal (not a [ModalBottomSheet], unlike the rest of this screen - a
 * log tail needs the vertical space) showing a live-tailed log for one container. Polling
 * lives in [DockerViewModel.openLogs]; this just renders whatever [DockerLogsState.result]
 * currently holds and auto-scrolls to the newest line as it grows.
 */
@Composable
private fun ContainerLogsDialog(state: DockerLogsState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Logs", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.containerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()
                Box(modifier = Modifier.weight(1f)) {
                    WidgetSection(result = state.result, onRetry = onRetry, minHeight = 200.dp) { lines ->
                        if (lines.isEmpty()) {
                            EmptyState("No log output yet")
                        } else {
                            LogLines(lines)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogLines(lines: List<DockerLogLine>) {
    val spacing = MaterialTheme.spacing
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1)
    }
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.medium, vertical = spacing.small)
        ) {
            items(lines) { line ->
                Text(
                    text = line.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private val previewContainers = listOf(
    DockerContainer(id = "1", name = "plex", image = "plexinc/pms-docker:latest", state = DockerContainerState.RUNNING, statusText = "Up 3 hours", iconUrl = null, webUiUrl = "http://tower:32400/web", updateAvailable = true),
    DockerContainer(id = "2", name = "sonarr", image = "linuxserver/sonarr:latest", state = DockerContainerState.EXITED, statusText = "Exited (0) 2 days ago", iconUrl = null, webUiUrl = null),
    DockerContainer(id = "3", name = "qbittorrent", image = "linuxserver/qbittorrent:latest", state = DockerContainerState.PAUSED, statusText = "Paused", iconUrl = null, webUiUrl = "http://tower:8080")
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DockerScreenPreview() {
    AppTheme {
        DockerScreenContent(uiState = DockerScreenState(containers = WidgetResult.Success(previewContainers)))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
fun DockerScreenEmptyPreview() {
    AppTheme {
        DockerScreenContent(uiState = DockerScreenState(containers = WidgetResult.Success(emptyList())))
    }
}

// The static preview can't draw a dialog window, so this shows the dialog's body on a dialog-styled surface.
@Preview(name = "Update all dialog", showBackground = true)
@Preview(name = "Update all dialog (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DockerUpdateAllDialogPreview() {
    AppTheme {
        Surface(shape = AlertDialogDefaults.shape, color = AlertDialogDefaults.containerColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Update ${previewContainers.size} containers?", style = MaterialTheme.typography.headlineSmall)
                ConfirmDialogBody("Pulls the latest images and recreates these containers. Each is unavailable while it's updated.") {
                    ContainerNameList(previewContainers)
                }
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = {}) { Text("Cancel") }
                    TextButton(onClick = {}) { Text("Update All") }
                }
            }
        }
    }
}
