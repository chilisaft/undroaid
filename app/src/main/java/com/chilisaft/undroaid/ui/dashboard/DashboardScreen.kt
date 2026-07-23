package com.chilisaft.undroaid.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.DockerContainerSummary
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.components.WidgetSection
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing
import com.chilisaft.undroaid.utils.kilobytesToHumanReadable
import com.chilisaft.undroaid.utils.toUptimeLabel
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNotificationsClick: () -> Unit = {},
    onShowAllContainers: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardScreenContent(
        uiState = uiState,
        onNotificationsClick = onNotificationsClick,
        onShowAllContainers = onShowAllContainers,
        onRetryArrayStatus = viewModel::refreshArrayStatus,
        onRetrySystemMetrics = viewModel::refreshSystemMetrics,
        onRetryContainers = viewModel::refreshContainers
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    uiState: DashboardScreenState,
    onNotificationsClick: () -> Unit = {},
    onShowAllContainers: () -> Unit = {},
    onRetryArrayStatus: () -> Unit = {},
    onRetrySystemMetrics: () -> Unit = {},
    onRetryContainers: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(spacing.small))
                        Text(
                            text = uiState.serverName ?: "Tower Command",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                },
                actions = {
                    val unreadCount = (uiState.unreadNotificationCount as? WidgetResult.Success)?.data ?: 0
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(badge = {
                            if (unreadCount > 0) {
                                Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {

            item {
                // Boot time only changes on a reboot, so recompute the elapsed label
                // independently of the systemMetrics widget's own load/poll cadence.
                val now by rememberTickingNow()
                val uptimeLabel = (uiState.systemMetrics as? WidgetResult.Success)?.data?.bootTimeIso?.toUptimeLabel(now)
                WidgetSection(result = uiState.arrayStatus, onRetry = onRetryArrayStatus, minHeight = 140.dp) { status ->
                    ArrayStatusCard(status = status, uptimeLabel = uptimeLabel)
                }
            }

            item {
                WidgetSection(result = uiState.systemMetrics, onRetry = onRetrySystemMetrics, minHeight = 110.dp) { metrics ->
                    SystemMetricsRow(metrics)
                }
            }

            item {
                SectionHeader(title = "Docker")
                WidgetSection(result = uiState.containers, onRetry = onRetryContainers, minHeight = 80.dp) { containers ->
                    DockerCard(containers = containers, onShowAll = onShowAllContainers)
                }
            }

            item { Spacer(Modifier.height(spacing.extraLarge)) }
        }
    }
}

@Composable
fun ArrayStatusCard(status: ArrayStatus, uptimeLabel: String?) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryFixed
                        )
                    )
                )
                .padding(spacing.large)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("ARRAY STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text(status.statusLabel, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = spacing.small, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(50)))
                            Spacer(Modifier.width(spacing.extraSmall))
                            Text(
                                if (status.healthy) "HEALTH: OK" else "HEALTH: ISSUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(spacing.small))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoColumn("UPTIME", uptimeLabel ?: "—")
                    Box(modifier = Modifier.width(1.dp).height(spacing.extraLarge).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)))
                    InfoColumn("STORAGE USED", formatStorage(status.usedKb, status.totalKb))
                }
            }
        }
    }
}

/** A [State] holding the current time, refreshed every [intervalMillis] while composed. */
@Composable
private fun rememberTickingNow(intervalMillis: Long = 30_000): State<Instant> {
    return produceState(initialValue = Instant.now()) {
        while (true) {
            delay(intervalMillis)
            value = Instant.now()
        }
    }
}

@Composable
private fun SystemMetricsRow(metrics: SystemMetrics) {
    val spacing = MaterialTheme.spacing
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
        MetricCard(
            modifier = Modifier.weight(1f),
            label = "CPU LOAD",
            percent = metrics.cpuLoadPercent,
            icon = Icons.Filled.Memory,
            color = MaterialTheme.colorScheme.primary
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            label = "MEMORY",
            percent = metrics.memoryLoadPercent,
            icon = Icons.Filled.Reorder,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private const val MAX_VISIBLE_CONTAINERS = 3

@Composable
private fun DockerCard(containers: List<DockerContainerSummary>, onShowAll: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(spacing.mediumLarge)
    ) {
        if (containers.isEmpty()) {
            Text(
                text = "No containers configured",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(spacing.medium)
            )
        } else {
            Column(modifier = Modifier.padding(vertical = spacing.small)) {
                containers.take(MAX_VISIBLE_CONTAINERS).forEach { container ->
                    DockerRow(container.name, container.isRunning)
                }
                if (containers.size > MAX_VISIBLE_CONTAINERS) {
                    TextButton(onClick = onShowAll, modifier = Modifier.fillMaxWidth()) {
                        Text("SHOW ALL (${containers.size})", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun formatStorage(usedKb: Long?, totalKb: Long?): String {
    if (usedKb == null || totalKb == null) return "—"
    return "${usedKb.kilobytesToHumanReadable()} / ${totalKb.kilobytesToHumanReadable()}"
}

@Composable
fun MetricCard(modifier: Modifier, label: String, percent: Double?, icon: ImageVector, color: Color) {
    val spacing = MaterialTheme.spacing
    val roundedPercent = percent?.let { Math.round(it).toInt() }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(spacing.small))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(roundedPercent?.toString() ?: "—", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                if (roundedPercent != null) {
                    Text("%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                }
            }
            Spacer(Modifier.height(spacing.itemSpacing))
            LinearProgressIndicator(
                progress = { (roundedPercent ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth().height(spacing.itemSpacing),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun DockerRow(name: String, isRunning: Boolean) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = null,
                        tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(spacing.medium))
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.width(spacing.extraSmall))
                    Text(
                        text = if (isRunning) "RUNNING" else "STOPPED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private val previewArrayStatus = ArrayStatus(
    statusLabel = "Array Started",
    healthy = true,
    usedKb = 130_589_261_824L / 1024,
    totalKb = 268_435_456_000L / 1024
)

private val previewSystemMetrics = SystemMetrics(
    bootTimeIso = "2024-01-01T00:00:00Z",
    cpuLoadPercent = 24.0,
    memoryLoadPercent = 58.0
)

private val previewContainers = listOf(
    DockerContainerSummary("Plex Media Server", isRunning = true),
    DockerContainerSummary("Nextcloud", isRunning = true),
    DockerContainerSummary("Home Assistant", isRunning = false),
    DockerContainerSummary("Pi-hole", isRunning = true)
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardPreview() {
    AppTheme {
        DashboardScreenContent(
            uiState = DashboardScreenState(
                serverName = "TOWER",
                arrayStatus = WidgetResult.Success(previewArrayStatus),
                systemMetrics = WidgetResult.Success(previewSystemMetrics),
                containers = WidgetResult.Success(previewContainers),
                unreadNotificationCount = WidgetResult.Success(3)
            )
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
fun DashboardLoadingPreview() {
    AppTheme {
        DashboardScreenContent(uiState = DashboardScreenState())
    }
}

@Preview(name = "Mixed widget failures", showBackground = true)
@Composable
fun DashboardPartialFailurePreview() {
    AppTheme {
        DashboardScreenContent(
            uiState = DashboardScreenState(
                serverName = "TOWER",
                arrayStatus = WidgetResult.Success(previewArrayStatus),
                systemMetrics = WidgetResult.Failure(permissionDenied = true, message = null),
                containers = WidgetResult.Failure(permissionDenied = false, message = "Network error")
            )
        )
    }
}
