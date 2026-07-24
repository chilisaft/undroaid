package com.chilisaft.undroaid.ui.docker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.DockerContainer
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.DockerLogLine
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.components.ContainerIconBadge
import com.chilisaft.undroaid.ui.components.ScreenTitle
import com.chilisaft.undroaid.ui.components.WidgetSection
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun DockerScreen(viewModel: DockerViewModel = hiltViewModel(), onUserClick: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    DockerScreenContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onUserClick = onUserClick,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onPause = viewModel::pause,
        onUnpause = viewModel::unpause,
        onRestart = viewModel::restart,
        onViewLogs = viewModel::openLogs,
        onRetryLogs = viewModel::retryLogs,
        onCloseLogs = viewModel::closeLogs
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockerScreenContent(
    uiState: DockerScreenState,
    onRetry: () -> Unit = {},
    onUserClick: () -> Unit = {},
    onStart: (String) -> Unit = {},
    onStop: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onUnpause: (String) -> Unit = {},
    onRestart: (String) -> Unit = {},
    onViewLogs: (String, String) -> Unit = { _, _ -> },
    onRetryLogs: () -> Unit = {},
    onCloseLogs: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var selectedContainer by remember { mutableStateOf<DockerContainer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(Icons.Filled.SmartToy, "Docker Containers") },
                actions = {
                    IconButton(onClick = onUserClick) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.containers is WidgetResult.Loading,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.containers, onRetry = onRetry, minHeight = 200.dp) { containers ->
                if (containers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No containers found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing),
                        contentPadding = PaddingValues(vertical = spacing.medium)
                    ) {
                        items(containers, key = { it.id }) { container ->
                            ContainerRow(
                                container = container,
                                isActioning = container.id in uiState.actioningIds,
                                onClick = { selectedContainer = container }
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
            onViewLogs = { onViewLogs(container.id, container.name); selectedContainer = null }
        )
    }

    val logs = uiState.logs
    if (logs != null) {
        ContainerLogsDialog(state = logs, onRetry = onRetryLogs, onDismiss = onCloseLogs)
    }
}

@Composable
private fun ContainerRow(container: DockerContainer, isActioning: Boolean, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val isRunning = container.state == DockerContainerState.RUNNING
    val statusDotColor = when (container.state) {
        DockerContainerState.RUNNING -> Color(0xFF4CAF50)
        DockerContainerState.PAUSED -> MaterialTheme.colorScheme.tertiary
        DockerContainerState.EXITED -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActioning, onClick = onClick),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContainerIconBadge(iconUrl = container.iconUrl, isRunning = isRunning, size = 52.dp)
            Spacer(Modifier.width(spacing.mediumLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    container.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(spacing.extraSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = statusDotColor, shape = RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(spacing.extraSmall))
                    Text(
                        text = container.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(spacing.medium))
            if (isActioning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerActionSheet(
    container: DockerContainer,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onUnpause: () -> Unit,
    onRestart: () -> Unit,
    onViewLogs: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = spacing.large)) {
            ContainerInfoCard(container = container, modifier = Modifier.padding(horizontal = spacing.medium))
            Spacer(Modifier.height(spacing.small))
            when (container.state) {
                DockerContainerState.RUNNING -> {
                    ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onRestart)
                    ActionSheetItem(Icons.Filled.Pause, "Pause", onPause)
                    ActionSheetItem(Icons.Filled.Stop, "Stop", onStop)
                }
                DockerContainerState.PAUSED -> {
                    ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onRestart)
                    ActionSheetItem(Icons.Filled.PlayArrow, "Resume", onUnpause)
                    ActionSheetItem(Icons.Filled.Stop, "Stop", onStop)
                }
                DockerContainerState.EXITED -> {
                    ActionSheetItem(Icons.Filled.PlayArrow, "Start", onStart)
                }
            }
            ActionSheetItem(Icons.AutoMirrored.Filled.Article, "View Logs", onViewLogs)
            if (!container.webUiUrl.isNullOrBlank()) {
                ActionSheetItem(Icons.AutoMirrored.Filled.OpenInNew, "Open Web UI") {
                    uriHandler.openUri(container.webUiUrl)
                    onDismiss()
                }
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
            shape = RoundedCornerShape(spacing.mediumLarge),
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
                        Text("Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No log output yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

@Composable
private fun ContainerInfoCard(container: DockerContainer, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val isRunning = container.state == DockerContainerState.RUNNING
    val statusDotColor = when (container.state) {
        DockerContainerState.RUNNING -> Color(0xFF4CAF50)
        DockerContainerState.PAUSED -> MaterialTheme.colorScheme.tertiary
        DockerContainerState.EXITED -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContainerIconBadge(iconUrl = container.iconUrl, isRunning = isRunning, size = 56.dp)
            Spacer(Modifier.width(spacing.mediumLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    container.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    container.image,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(spacing.extraSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = statusDotColor, shape = RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(spacing.extraSmall))
                    Text(
                        text = container.statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionSheetItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(spacing.medium))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private val previewContainers = listOf(
    DockerContainer(id = "1", name = "plex", image = "plexinc/pms-docker:latest", state = DockerContainerState.RUNNING, statusText = "Up 3 hours", iconUrl = null, webUiUrl = "http://tower:32400/web"),
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
