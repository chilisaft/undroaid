package com.chilisaft.undroaid.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.DockerContainerSummary
import com.chilisaft.undroaid.data.models.ParityCheckInfo
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.components.ContainerIconBadge
import com.chilisaft.undroaid.ui.components.ScreenTitle
import com.chilisaft.undroaid.ui.components.WidgetSection
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing
import com.chilisaft.undroaid.utils.estimatedSecondsRemaining
import com.chilisaft.undroaid.utils.kilobytesToHumanReadable
import com.chilisaft.undroaid.utils.toRemainingTimeLabel
import com.chilisaft.undroaid.utils.toUptimeLabel
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNotificationsClick: () -> Unit = {},
    onUserClick: () -> Unit = {},
    onShowAllContainers: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardScreenContent(
        uiState = uiState,
        onNotificationsClick = onNotificationsClick,
        onUserClick = onUserClick,
        onShowAllContainers = onShowAllContainers,
        onRefresh = viewModel::refresh,
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
    onUserClick: () -> Unit = {},
    onShowAllContainers: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryArrayStatus: () -> Unit = {},
    onRetrySystemMetrics: () -> Unit = {},
    onRetryContainers: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(Icons.Filled.Dns, uiState.serverName ?: "Tower Command") },
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
                    IconButton(onClick = onUserClick) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
        }
    ) { padding ->
        val isRefreshing = uiState.arrayStatus is WidgetResult.Loading ||
            uiState.systemMetrics is WidgetResult.Loading ||
            uiState.containers is WidgetResult.Loading
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
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
                        DockerCard(containers = containers, onShowAll = onShowAllContainers)
                    }
                }

                item { Spacer(Modifier.height(spacing.extraLarge)) }
            }
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

/**
 * Only shown while a parity check is actually running/paused (see the caller) - the Main tab
 * covers the full array/parity-check management story, this is just a glanceable "one's active
 * right now" surface, so it disappears entirely the rest of the time rather than always taking
 * up space. Polled every 5s while running - see `DashboardViewModel.startParityCheckPollingIfNeeded`.
 */
@Composable
private fun ParityCheckBanner(parityCheck: ParityCheckInfo, paritySizeKb: Long?) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(spacing.mediumLarge)
    ) {
        val accentColor = if (parityCheck.paused) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Parity Check", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${parityCheck.progressPercent ?: 0}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            LinearProgressIndicator(
                progress = { (parityCheck.progressPercent ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = accentColor
            )
            Text(
                text = buildString {
                    append(if (parityCheck.paused) "Paused" else "Running")
                    parityCheck.speed?.let { append(" · $it MB/s") }
                    estimatedSecondsRemaining(parityCheck.progressPercent, parityCheck.speed, paritySizeKb)?.let {
                        append(" · ~${it.toRemainingTimeLabel()} remaining")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            color = MaterialTheme.colorScheme.tertiary
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
        Column(modifier = Modifier.padding(vertical = spacing.small)) {
            DockerCardHeader(
                containers = containers,
                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.extraSmall)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.medium))
            if (containers.isEmpty()) {
                Text(
                    text = "No containers configured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(spacing.medium)
                )
            } else {
                containers.take(MAX_VISIBLE_CONTAINERS).forEach { container ->
                    DockerRow(container.name, container.state, container.iconUrl)
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

/**
 * "Docker" title and the running/paused/stopped counts share one row (right-aligned counts)
 * instead of the title living outside the card and the counts on their own line below it - cuts
 * the dead space that left on a title-only row and a counts-only row, each using a fraction of
 * the card's width.
 */
@Composable
private fun DockerCardHeader(containers: List<DockerContainerSummary>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Docker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        dockerCountsLabel(containers)?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Omits any state with a zero count, so an all-running (the common case) list just reads "N Running". Null when there are no containers at all. */
private fun dockerCountsLabel(containers: List<DockerContainerSummary>): String? {
    val running = containers.count { it.state == DockerContainerState.RUNNING }
    val paused = containers.count { it.state == DockerContainerState.PAUSED }
    val stopped = containers.count { it.state == DockerContainerState.EXITED }
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
fun DockerRow(name: String, state: DockerContainerState, iconUrl: String? = null) {
    val spacing = MaterialTheme.spacing
    val isRunning = state == DockerContainerState.RUNNING
    val (dotColor, label) = when (state) {
        DockerContainerState.RUNNING -> Color(0xFF4CAF50) to "RUNNING"
        DockerContainerState.PAUSED -> MaterialTheme.colorScheme.tertiary to "PAUSED"
        DockerContainerState.EXITED -> MaterialTheme.colorScheme.error to "STOPPED"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContainerIconBadge(iconUrl = iconUrl, isRunning = isRunning)
            Spacer(Modifier.width(spacing.medium))
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(color = dotColor, shape = RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(spacing.extraSmall))
                    Text(
                        text = label,
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
                containers = WidgetResult.Failure(permissionDenied = false, message = "Network error")
            )
        )
    }
}
