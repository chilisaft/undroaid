package com.chilisaft.undroid.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.chilisaft.undroid.data.models.ArrayStatus
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.DockerContainerSummary
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.SystemMetrics
import com.chilisaft.undroid.data.models.VmDomain
import com.chilisaft.undroid.data.models.VmRunState
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.ContainerIconBadge
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.ParityCheckProgress
import com.chilisaft.undroid.ui.components.ResourceItemSize
import com.chilisaft.undroid.ui.components.ResourceListItem
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.TabTopBar
import com.chilisaft.undroid.ui.components.VmIconBadge
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.components.label
import com.chilisaft.undroid.ui.components.statusTone
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.kilobytesToHumanReadable
import com.chilisaft.undroid.utils.toUptimeLabel
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    topBarActions: @Composable RowScope.() -> Unit = {},
    onShowAllContainers: () -> Unit = {},
    onShowAllVms: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    // Live polling only while this screen is actually showing - see DashboardViewModel.onScreenVisible.
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }
    DashboardScreenContent(
        uiState = uiState,
        topBarActions = topBarActions,
        onShowAllContainers = onShowAllContainers,
        onShowAllVms = onShowAllVms,
        onRefresh = viewModel::refresh,
        onRetryArrayStatus = viewModel::refreshArrayStatus,
        onRetrySystemMetrics = viewModel::refreshSystemMetrics,
        onRetryContainers = viewModel::refreshContainers,
        onRetryVms = viewModel::refreshVms
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    uiState: DashboardScreenState,
    topBarActions: @Composable RowScope.() -> Unit = {},
    onShowAllContainers: () -> Unit = {},
    onShowAllVms: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryArrayStatus: () -> Unit = {},
    onRetrySystemMetrics: () -> Unit = {},
    onRetryContainers: () -> Unit = {},
    onRetryVms: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = { TabTopBar(title = uiState.serverName ?: "Dashboard", icon = Icons.Filled.Dns, actions = topBarActions) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            ScreenList {
                item {
                    // Boot time only changes on a reboot, so recompute the elapsed label
                    // independently of the systemMetrics widget's own load/poll cadence.
                    val now by rememberTickingNow()
                    val uptimeLabel = (uiState.systemMetrics as? WidgetResult.Success)?.data?.bootTimeIso?.toUptimeLabel(now)
                    WidgetSection(result = uiState.arrayStatus, onRetry = onRetryArrayStatus, minHeight = 140.dp) { status ->
                        ArrayStatusCard(status = status, uptimeLabel = uptimeLabel)
                    }
                }

                // Its own independently loadable/retryable widget (see
                // ServerRepository.getParityCheckStatus), separate from arrayStatus above - a
                // parity-specific hiccup no longer blanks the Array Health card, and vice versa.
                // Only rendered at all while a check is actually running/paused - this is a
                // glanceable "one's active right now" surface, not a permanent fixture, so
                // loading/error/idle states show nothing rather than an empty placeholder card.
                val activeParityCheck = (uiState.parityCheck as? WidgetResult.Success)?.data?.takeIf { it.running || it.paused }
                if (activeParityCheck != null) {
                    item {
                        ParityCheckBanner(activeParityCheck, (uiState.arrayStatus as? WidgetResult.Success)?.data?.paritySizeKb)
                    }
                }

                item {
                    WidgetSection(result = uiState.systemMetrics, onRetry = onRetrySystemMetrics, minHeight = 110.dp) { metrics ->
                        SystemMetricsRow(metrics)
                    }
                }

                item {
                    WidgetSection(result = uiState.containers, onRetry = onRetryContainers, minHeight = 80.dp) { containers ->
                        ResourcePreviewCard(
                            title = "Docker",
                            items = containers,
                            countsLabel = stateCountsLabel(
                                running = containers.count { it.state == DockerContainerState.RUNNING },
                                paused = containers.count { it.state == DockerContainerState.PAUSED },
                                stopped = containers.count { it.state == DockerContainerState.EXITED }
                            ),
                            emptyText = "No containers found",
                            onShowAll = onShowAllContainers
                        ) { container ->
                            ResourceListItem(
                                title = container.name,
                                tone = container.state.statusTone,
                                statusText = container.state.label,
                                size = ResourceItemSize.Compact,
                                leading = { size ->
                                    ContainerIconBadge(container.iconUrl, isRunning = container.state == DockerContainerState.RUNNING, size = size)
                                },
                                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)
                            )
                        }
                    }
                }

                item {
                    WidgetSection(result = uiState.vms, onRetry = onRetryVms, minHeight = 80.dp) { vms ->
                        ResourcePreviewCard(
                            title = "Virtual Machines",
                            items = vms,
                            countsLabel = stateCountsLabel(
                                running = vms.count { it.state == VmRunState.RUNNING },
                                paused = vms.count { it.state == VmRunState.PAUSED },
                                stopped = vms.count { it.state == VmRunState.STOPPED }
                            ),
                            emptyText = "No virtual machines found",
                            onShowAll = onShowAllVms
                        ) { vm ->
                            ResourceListItem(
                                title = vm.name,
                                tone = vm.statusTone,
                                statusText = vm.statusLabel,
                                size = ResourceItemSize.Compact,
                                leading = { size -> VmIconBadge(state = vm.state, size = size) },
                                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Dashboard's hero card, on a brand gradient. The gradient runs from `primary` a third of the
 * way towards `onPrimaryContainer` - darker in light mode, lighter in dark mode, i.e. always
 * *away* from `onPrimary` - so the white/dark text on top keeps its contrast across the whole
 * card. (It used to fade into the very pale `primaryFixed`, which washed out the right-hand
 * column's text in light mode.)
 */
@Composable
private fun ArrayStatusCard(status: ArrayStatus, uptimeLabel: String?) {
    val spacing = MaterialTheme.spacing
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(colors.primary, lerp(colors.primary, colors.onPrimaryContainer, 0.35f))))
                .padding(spacing.large)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("ARRAY STATUS", style = MaterialTheme.typography.labelSmall, color = colors.onPrimary.copy(alpha = 0.7f))
                        Text(status.statusLabel, style = MaterialTheme.typography.titleLarge, color = colors.onPrimary)
                    }
                    Surface(color = colors.onPrimary.copy(alpha = 0.15f), shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(colors.onPrimary, CircleShape))
                            Spacer(Modifier.width(spacing.extraSmall))
                            Text(
                                if (status.healthy) "HEALTH: OK" else "HEALTH: ISSUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(spacing.small))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoColumn("UPTIME", uptimeLabel ?: "—")
                    Box(modifier = Modifier.width(1.dp).height(spacing.extraLarge).background(colors.onPrimary.copy(alpha = 0.2f)))
                    InfoColumn("STORAGE USED", formatStorage(status.usedKb, status.totalKb))
                }
            }
        }
    }
}

/**
 * Only shown while a parity check is actually running/paused (see the caller) - the Main tab
 * covers the full array/parity-check management story, this is just a glanceable "one's active
 * right now" surface, so it disappears entirely the rest of the time rather than always taking
 * up space. Polled every 5s while running - see `DashboardViewModel.startParityCheckPollingIfNeeded`.
 */
@Composable
private fun ParityCheckBanner(parityCheck: ParityCheckInfo, paritySizeKb: Long?) {
    AppCard {
        ParityCheckProgress(parityCheck, paritySizeKb, modifier = Modifier.padding(MaterialTheme.spacing.medium))
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
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

private const val MAX_PREVIEW_ITEMS = 3

/**
 * A Dashboard summary of one resource list (Docker, VMs): title and per-state counts sharing the
 * header row, the first [MAX_PREVIEW_ITEMS] items, and a "Show All" button when there are more.
 * The whole card jumps to the full tab.
 */
@Composable
private fun <T> ResourcePreviewCard(
    title: String,
    items: List<T>,
    countsLabel: String?,
    emptyText: String,
    onShowAll: () -> Unit,
    row: @Composable (T) -> Unit
) {
    val spacing = MaterialTheme.spacing
    AppCard(onClick = onShowAll) {
        Column(modifier = Modifier.padding(vertical = spacing.small)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (countsLabel != null) {
                    Text(countsLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.medium))
            if (items.isEmpty()) {
                EmptyState(emptyText, modifier = Modifier.fillMaxWidth().padding(spacing.medium))
            } else {
                items.take(MAX_PREVIEW_ITEMS).forEach { row(it) }
                if (items.size > MAX_PREVIEW_ITEMS) {
                    TextButton(onClick = onShowAll, modifier = Modifier.fillMaxWidth()) {
                        Text("Show All (${items.size})")
                    }
                }
            }
        }
    }
}

/** Omits any state with a zero count, so an all-running (the common case) list just reads "N Running". Null when there's nothing at all. */
private fun stateCountsLabel(running: Int, paused: Int, stopped: Int): String? {
    val parts = buildList {
        if (running > 0) add("$running Running")
        if (paused > 0) add("$paused Paused")
        if (stopped > 0) add("$stopped Stopped")
    }
    return parts.joinToString("   ").ifEmpty { null }
}

private fun formatStorage(usedKb: Long?, totalKb: Long?): String {
    if (usedKb == null || totalKb == null) return "—"
    return "${usedKb.kilobytesToHumanReadable()} / ${totalKb.kilobytesToHumanReadable()}"
}

@Composable
private fun MetricCard(modifier: Modifier, label: String, percent: Double?, icon: ImageVector, color: Color) {
    val spacing = MaterialTheme.spacing
    val roundedPercent = percent?.let { Math.round(it).toInt() }
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(spacing.small))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(roundedPercent?.toString() ?: "—", style = MaterialTheme.typography.headlineLarge)
                if (roundedPercent != null) {
                    Text("%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp, start = spacing.extraSmall))
                }
            }
            Spacer(Modifier.height(spacing.itemSpacing))
            LinearProgressIndicator(
                progress = { (roundedPercent ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth().height(spacing.itemSpacing),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}

private val previewArrayStatus = ArrayStatus(
    statusLabel = "Array Started",
    healthy = true,
    usedKb = 130_589_261_824L / 1024,
    totalKb = 268_435_456_000L / 1024,
    paritySizeKb = 268_435_456_000L / 1024
)

private val previewParityCheckIdle = ParityCheckInfo(
    statusLabel = "Completed", running = false, paused = false,
    progressPercent = 100, speed = null, errors = 0, correcting = false
)

private val previewSystemMetrics = SystemMetrics(
    bootTimeIso = "2024-01-01T00:00:00Z",
    cpuLoadPercent = 24.0,
    memoryLoadPercent = 58.0
)

private val previewContainers = listOf(
    DockerContainerSummary("Plex Media Server", state = DockerContainerState.RUNNING, iconUrl = null),
    DockerContainerSummary("Nextcloud", state = DockerContainerState.RUNNING, iconUrl = null),
    DockerContainerSummary("Home Assistant", state = DockerContainerState.EXITED, iconUrl = null),
    DockerContainerSummary("qBittorrent", state = DockerContainerState.PAUSED, iconUrl = null),
    DockerContainerSummary("Pi-hole", state = DockerContainerState.RUNNING, iconUrl = null)
)

private val previewVms = listOf(
    VmDomain(id = "1", name = "Windows 11", state = VmRunState.RUNNING, statusLabel = "Running"),
    VmDomain(id = "2", name = "Ubuntu Server", state = VmRunState.STOPPED, statusLabel = "Shut Off")
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
                parityCheck = WidgetResult.Success(previewParityCheckIdle),
                systemMetrics = WidgetResult.Success(previewSystemMetrics),
                containers = WidgetResult.Success(previewContainers),
                vms = WidgetResult.Success(previewVms),
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

@Preview(name = "Parity Check Running", showBackground = true)
@Composable
fun DashboardParityCheckRunningPreview() {
    AppTheme {
        DashboardScreenContent(
            uiState = DashboardScreenState(
                serverName = "TOWER",
                arrayStatus = WidgetResult.Success(previewArrayStatus),
                parityCheck = WidgetResult.Success(
                    ParityCheckInfo(
                        statusLabel = "Running", running = true, paused = false,
                        progressPercent = 42, speed = "150", errors = 0, correcting = false
                    )
                ),
                systemMetrics = WidgetResult.Success(previewSystemMetrics),
                containers = WidgetResult.Success(previewContainers),
                vms = WidgetResult.Success(previewVms),
                unreadNotificationCount = WidgetResult.Success(3)
            )
        )
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
                parityCheck = WidgetResult.Success(previewParityCheckIdle),
                systemMetrics = WidgetResult.Failure(permissionDenied = true, message = null),
                containers = WidgetResult.Failure(permissionDenied = false, message = "Network error"),
                vms = WidgetResult.Failure(permissionDenied = false, message = "Network error")
            )
        )
    }
}
