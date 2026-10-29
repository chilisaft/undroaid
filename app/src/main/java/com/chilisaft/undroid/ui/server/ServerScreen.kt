package com.chilisaft.undroid.ui.server

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroid.data.models.ConnectInfo
import com.chilisaft.undroid.data.models.ConnectStatus
import com.chilisaft.undroid.data.models.LicenceDates
import com.chilisaft.undroid.data.models.LicenceProblem
import com.chilisaft.undroid.data.models.NetworkSummary
import com.chilisaft.undroid.data.models.ServerIdentity
import com.chilisaft.undroid.data.models.ServerSystem
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.chilisaft.undroid.ui.components.InfoRow
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.SectionHeader
import com.chilisaft.undroid.ui.components.StatusLine
import com.chilisaft.undroid.ui.components.TagChip
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.components.tryOpenUri
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.kilobytesToHumanReadable
import com.chilisaft.undroid.utils.toDateLabel
import com.chilisaft.undroid.utils.toUptimeLabel
import java.time.Instant

@Composable
fun ServerScreen(onBack: () -> Unit = {}, viewModel: ServerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    ServerScreenContent(
        uiState = uiState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetryIdentity = viewModel::refreshIdentity,
        onRetrySystem = viewModel::refreshSystem,
        onRetryNetwork = viewModel::refreshNetwork,
        onRetryConnect = viewModel::refreshConnect
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreenContent(
    uiState: ServerScreenState,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryIdentity: () -> Unit = {},
    onRetrySystem: () -> Unit = {},
    onRetryNetwork: () -> Unit = {},
    onRetryConnect: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = { DetailTopBar(title = "Server", onBack = onBack) }
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
                    WidgetSection(result = uiState.identity, onRetry = onRetryIdentity, minHeight = 96.dp) {
                        IdentityCard(it, (uiState.licenceDates as? WidgetResult.Success)?.data)
                    }
                }
                item { SectionHeader("System") }
                item {
                    WidgetSection(result = uiState.system, onRetry = onRetrySystem, minHeight = 160.dp) { SystemCard(it) }
                }
                item { SectionHeader("Network") }
                item {
                    WidgetSection(result = uiState.network, onRetry = onRetryNetwork, minHeight = 72.dp) { NetworkCard(it) }
                }
                item { SectionHeader("Unraid Connect") }
                item {
                    WidgetSection(result = uiState.connect, onRetry = onRetryConnect, minHeight = 96.dp) { ConnectCard(it) }
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(identity: ServerIdentity, licenceDates: LicenceDates?) {
    val spacing = MaterialTheme.spacing
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(identity.name ?: "Unraid server", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false))
                identity.licence?.let {
                    Spacer(Modifier.width(spacing.small))
                    TagChip(it)
                }
            }
            identity.comment?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            identity.timeZone?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            identity.licenceProblem?.let { problem ->
                StatusLine(
                    tone = if (problem == LicenceProblem.NOT_VALIDATED) StatusTone.Warning else StatusTone.Error,
                    text = problem.label(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "See Tools → Registration in the webUI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            licenceDates?.let { dates -> licenceDatesLabel(dates, Instant.now()) }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun LicenceProblem.label(): String = when (this) {
    LicenceProblem.TRIAL_EXPIRED -> "Trial has expired"
    LicenceProblem.TRIAL_INVALID -> "Trial licence problem"
    LicenceProblem.WRONG_FLASH_DRIVE -> "Licence key is for a different flash drive"
    LicenceProblem.BLACKLISTED -> "Licence key is blacklisted"
    LicenceProblem.NO_KEY_FILE -> "No licence key file found"
    LicenceProblem.NO_FLASH_DRIVE -> "Boot flash drive not recognised"
    LicenceProblem.NOT_VALIDATED -> "Licence couldn't be validated"
}

/** "Licence expires 12 Nov 2026 · Updates until 3 Mar 2027" - whichever dates exist, past tense once passed. */
private fun licenceDatesLabel(dates: LicenceDates, now: Instant): String? = listOfNotNull(
    dates.expiresAt?.let { if (it.isAfter(now)) "Licence expires ${it.toDateLabel()}" else "Licence expired ${it.toDateLabel()}" },
    dates.updatesUntil?.let { if (it.isAfter(now)) "Updates until ${it.toDateLabel()}" else "Updates ended ${it.toDateLabel()}" }
).joinToString(" · ").takeIf { it.isNotEmpty() }

@Composable
private fun SystemCard(system: ServerSystem) {
    val spacing = MaterialTheme.spacing
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            system.unraidVersion?.let { InfoRow("Unraid", it) }
            system.apiVersion?.let { InfoRow("API", it) }
            system.kernel?.let { InfoRow("Kernel", it) }
            system.dockerVersion?.let { InfoRow("Docker", it) }
            system.bootTimeIso?.toUptimeLabel()?.let { InfoRow("Uptime", it) }
            HorizontalDivider()
            system.cpu?.let { InfoRow("CPU", it) }
            coresLabel(system.cpuCores, system.cpuThreads)?.let { InfoRow("Cores", it) }
            system.memoryTotalBytes?.let { bytes ->
                InfoRow("Memory", listOfNotNull((bytes / 1024).kilobytesToHumanReadable(), system.memoryType).joinToString(" "))
            }
            system.motherboard?.let { InfoRow("Motherboard", it) }
        }
    }
}

@Composable
private fun NetworkCard(network: NetworkSummary?) {
    val spacing = MaterialTheme.spacing
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (network == null) {
                Text(
                    "The server reports no primary network interface.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow("Interface", networkLabel(network))
                network.ipAddress?.let { InfoRow("IP address", it, copyText = it) }
            }
        }
    }
}

/**
 * The WAN IP is hidden until tapped - it's a privacy-sensitive value on a screen people may
 * screenshot or show to others.
 */
@Composable
private fun ConnectCard(connect: ConnectInfo) {
    val spacing = MaterialTheme.spacing
    val uriHandler = LocalUriHandler.current
    var showWanIp by remember { mutableStateOf(false) }
    val (tone, statusText) = when (connect.status) {
        ConnectStatus.ONLINE -> StatusTone.Ok to "Online"
        ConnectStatus.OFFLINE -> StatusTone.Warning to "Offline"
        ConnectStatus.NEVER_CONNECTED -> StatusTone.Neutral to "Not set up"
    }
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            StatusLine(tone = tone, text = statusText, style = MaterialTheme.typography.bodyMedium)
            if (connect.status == ConnectStatus.NEVER_CONNECTED) {
                Text(
                    "Sign in to Unraid Connect in the webUI to enable remote access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            connect.owner?.let { InfoRow("Account", it) }
            connect.lanIp?.let { InfoRow("LAN IP", it, copyText = it) }
            connect.wanIp?.let { wanIp ->
                InfoRow("WAN IP", if (showWanIp) wanIp else "Tap to show", onClick = { showWanIp = !showWanIp }, copyText = wanIp)
            }
            connect.localUrl?.let { url -> InfoRow("Local URL", url, onClick = { uriHandler.tryOpenUri(url) }, copyText = url) }
            connect.remoteUrl?.let { url -> InfoRow("Remote URL", url, onClick = { uriHandler.tryOpenUri(url) }, copyText = url) }
        }
    }
}

private fun coresLabel(cores: Int?, threads: Int?): String? = when {
    cores != null && threads != null && threads != cores -> "$cores cores · $threads threads"
    cores != null -> "$cores cores"
    threads != null -> "$threads threads"
    else -> null
}

/** "eth0 · 1 Gbps" / "eth0 · 2.5 Gbps" / "eth0 · 100 Mbps". */
private fun networkLabel(network: NetworkSummary): String {
    val speed = network.speedMbps?.let { mbps ->
        if (mbps >= 1000) "${"%.1f".format(mbps / 1000.0).removeSuffix(".0")} Gbps" else "$mbps Mbps"
    }
    return listOfNotNull(network.interfaceName, speed).joinToString(" · ")
}

private val previewState = ServerScreenState(
    identity = WidgetResult.Success(ServerIdentity(name = "Tower", comment = "Media server", timeZone = "Europe/Zurich", licence = "Pro")),
    system = WidgetResult.Success(
        ServerSystem(
            unraidVersion = "7.3.3", apiVersion = "4.20.0", kernel = "6.12.24-Unraid", dockerVersion = "27.5.1", bootTimeIso = "2026-10-10T08:00:00Z",
            cpu = "Intel Core i5-12400", cpuCores = 6, cpuThreads = 12, memoryTotalBytes = 34_359_738_368L, memoryType = "DDR4",
            motherboard = "ASUSTeK PRIME B660M-A"
        )
    ),
    network = WidgetResult.Success(NetworkSummary("eth0", "192.168.1.10", 1000)),
    connect = WidgetResult.Success(
        ConnectInfo(
            status = ConnectStatus.ONLINE, owner = "tower-owner", lanIp = "192.168.1.10", wanIp = "203.0.113.7",
            localUrl = "https://192-168-1-10.abc.myunraid.net", remoteUrl = null
        )
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ServerScreenPreview() {
    AppTheme {
        ServerScreenContent(uiState = previewState)
    }
}

@Preview(name = "Connect not set up, no SERVERS permission", showBackground = true)
@Composable
fun ServerScreenPartialPreview() {
    AppTheme {
        ServerScreenContent(
            uiState = previewState.copy(
                connect = WidgetResult.Failure(permissionDenied = true, message = null)
            )
        )
    }
}

@Preview(name = "Expired trial with licence dates", showBackground = true)
@Composable
fun ServerScreenLicenceProblemPreview() {
    AppTheme {
        ServerScreenContent(
            uiState = previewState.copy(
                identity = WidgetResult.Success(
                    ServerIdentity(name = "Tower", comment = null, timeZone = "Europe/Zurich", licence = "Trial", licenceProblem = LicenceProblem.TRIAL_EXPIRED)
                ),
                licenceDates = WidgetResult.Success(LicenceDates(expiresAt = Instant.parse("2026-10-01T00:00:00Z"), updatesUntil = null))
            )
        )
    }
}
