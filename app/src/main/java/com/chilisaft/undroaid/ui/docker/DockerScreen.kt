package com.chilisaft.undroaid.ui.docker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.DockerContainer
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.components.ContainerIconBadge
import com.chilisaft.undroaid.ui.components.WidgetSection
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun DockerScreen(viewModel: DockerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    DockerScreenContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onPause = viewModel::pause,
        onUnpause = viewModel::unpause,
        onRestart = viewModel::restart
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockerScreenContent(
    uiState: DockerScreenState,
    onRetry: () -> Unit = {},
    onStart: (String) -> Unit = {},
    onStop: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onUnpause: (String) -> Unit = {},
    onRestart: (String) -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var selectedContainer by remember { mutableStateOf<DockerContainer?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Docker") }) }
    ) { padding ->
        Box(
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
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
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
            onRestart = { onRestart(container.id); selectedContainer = null }
        )
    }
}

@Composable
private fun ContainerRow(container: DockerContainer, isActioning: Boolean, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val isRunning = container.state == DockerContainerState.RUNNING
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
                            .background(
                                color = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(50)
                            )
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
    onRestart: () -> Unit
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
            if (!container.webUiUrl.isNullOrBlank()) {
                ActionSheetItem(Icons.AutoMirrored.Filled.OpenInNew, "Open Web UI") {
                    uriHandler.openUri(container.webUiUrl)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ContainerInfoCard(container: DockerContainer, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val isRunning = container.state == DockerContainerState.RUNNING
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
                            .background(
                                color = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(50)
                            )
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
