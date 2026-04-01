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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardScreenContent(uiState = uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(uiState: DashboardScreenState) {
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
                            text = uiState.server?.name ?: "Tower Command",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                ArrayStatusCard(
                    status = "Array Started",
                    uptime = "42d 12h 04m",
                    storage = "124.5 TB"
                )
            }

            // Metrics Grid
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "CPU LOAD",
                        value = "24",
                        unit = "%",
                        progress = 0.24f,
                        icon = Icons.Filled.Memory,
                        color = MaterialTheme.colorScheme.primary
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "MEMORY",
                        value = "58",
                        unit = "%",
                        progress = 0.58f,
                        icon = Icons.Filled.Reorder,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                SectionHeader(title = "Docker")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(spacing.mediumLarge)
                ) {
                    Column(modifier = Modifier.padding(vertical = spacing.small)) {
                        DockerRow("Plex Media Server", isRunning = true, Icons.Filled.Layers)
                        DockerRow("Nextcloud", isRunning = true, Icons.Filled.Backup)
                        DockerRow("Home Assistant", isRunning = false, Icons.Filled.Lan)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Activity")
                    TextButton(onClick = { /* TODO */ }) {
                        Text("DISMISS ALL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                NotificationItem(
                    title = "Parity Check Complete",
                    desc = "System finished scheduled parity check. Duration: 14h 22m. Errors found: 0.",
                    time = "2 HOURS AGO",
                    icon = Icons.Filled.TaskAlt,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                NotificationItem(
                    title = "Disk 4 Temperature Alert",
                    desc = "WDC_WD120EDAZ has reached 46°C. Cooling threshold exceeded.",
                    time = "5 HOURS AGO",
                    icon = Icons.Filled.Warning,
                    color = MaterialTheme.colorScheme.error
                )
            }

            item { Spacer(Modifier.height(spacing.extraLarge)) }
        }
    }
}

@Composable
fun ArrayStatusCard(status: String, uptime: String, storage: String) {
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
                        Text(status, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = spacing.small, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(50)))
                            Spacer(Modifier.width(spacing.extraSmall))
                            Text("HEALTH: OK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(spacing.small))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoColumn("UPTIME", uptime)
                    Box(modifier = Modifier.width(1.dp).height(spacing.extraLarge).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)))
                    InfoColumn("TOTAL STORAGE", storage)
                }
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, label: String, value: String, unit: String, progress: Float, icon: ImageVector, color: Color) {
    val spacing = MaterialTheme.spacing
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
                Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
            }
            Spacer(Modifier.height(spacing.itemSpacing))
            LinearProgressIndicator(
                progress = { progress },
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
fun DockerRow(name: String, isRunning: Boolean, icon: ImageVector) {
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
                        icon,
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

@Composable
fun NotificationItem(title: String, desc: String, time: String, icon: ImageVector, color: Color) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(spacing.medium)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(spacing.medium))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                Spacer(Modifier.height(spacing.small))
                Text(time, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardPreview() {
    AppTheme {
        DashboardScreenContent(uiState = DashboardScreenState())
    }
}
