package com.chilisaft.undroaid.ui.vms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.VmDomain
import com.chilisaft.undroaid.data.models.VmRunState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.components.ScreenTitle
import com.chilisaft.undroaid.ui.components.WidgetSection
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun VmsScreen(viewModel: VmsViewModel = hiltViewModel(), onUserClick: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    VmsScreenContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onUserClick = onUserClick,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onForceStop = viewModel::forceStop,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onReboot = viewModel::reboot
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmsScreenContent(
    uiState: VmsScreenState,
    onRetry: () -> Unit = {},
    onUserClick: () -> Unit = {},
    onStart: (String) -> Unit = {},
    onStop: (String) -> Unit = {},
    onForceStop: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onResume: (String) -> Unit = {},
    onReboot: (String) -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var selectedVm by remember { mutableStateOf<VmDomain?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(Icons.Filled.Computer, "Virtual Machines") },
                actions = {
                    IconButton(onClick = onUserClick) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.vms is WidgetResult.Loading,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.vms, onRetry = onRetry, minHeight = 200.dp) { vms ->
                if (vms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No virtual machines found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing),
                        contentPadding = PaddingValues(vertical = spacing.medium)
                    ) {
                        items(vms, key = { it.id }) { vm ->
                            VmRow(
                                vm = vm,
                                isActioning = vm.id in uiState.actioningIds,
                                onClick = { selectedVm = vm }
                            )
                        }
                    }
                }
            }
        }
    }

    val vm = selectedVm
    if (vm != null) {
        VmActionSheet(
            vm = vm,
            onDismiss = { selectedVm = null },
            onStart = { onStart(vm.id); selectedVm = null },
            onStop = { onStop(vm.id); selectedVm = null },
            onForceStop = { onForceStop(vm.id); selectedVm = null },
            onPause = { onPause(vm.id); selectedVm = null },
            onResume = { onResume(vm.id); selectedVm = null },
            onReboot = { onReboot(vm.id); selectedVm = null }
        )
    }
}

@Composable
private fun VmRunState.dotColor(): Color = when (this) {
    VmRunState.RUNNING -> Color(0xFF4CAF50)
    VmRunState.PAUSED -> MaterialTheme.colorScheme.tertiary
    VmRunState.STOPPED -> Color(0xFF9E9E9E)
}

@Composable
private fun VmIconBadge(state: VmRunState, size: Dp = 40.dp) {
    val tint = if (state == VmRunState.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Surface(
        modifier = Modifier.size(size),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Computer, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
private fun VmRow(vm: VmDomain, isActioning: Boolean, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
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
            VmIconBadge(state = vm.state, size = 52.dp)
            Spacer(Modifier.width(spacing.mediumLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vm.name,
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
                            .background(color = vm.state.dotColor(), shape = RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(spacing.extraSmall))
                    Text(
                        text = vm.statusLabel,
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
private fun VmActionSheet(
    vm: VmDomain,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onForceStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReboot: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = spacing.large)) {
            VmInfoCard(vm = vm, modifier = Modifier.padding(horizontal = spacing.medium))
            Spacer(Modifier.height(spacing.small))
            when (vm.state) {
                VmRunState.RUNNING -> {
                    ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onReboot)
                    ActionSheetItem(Icons.Filled.Pause, "Pause", onPause)
                    ActionSheetItem(Icons.Filled.Stop, "Stop", onStop)
                    ActionSheetItem(Icons.Filled.PowerSettingsNew, "Force Stop", onForceStop)
                }
                VmRunState.PAUSED -> {
                    ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onReboot)
                    ActionSheetItem(Icons.Filled.PlayArrow, "Resume", onResume)
                    ActionSheetItem(Icons.Filled.Stop, "Stop", onStop)
                    ActionSheetItem(Icons.Filled.PowerSettingsNew, "Force Stop", onForceStop)
                }
                VmRunState.STOPPED -> {
                    ActionSheetItem(Icons.Filled.PlayArrow, "Start", onStart)
                }
            }
        }
    }
}

@Composable
private fun VmInfoCard(vm: VmDomain, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VmIconBadge(state = vm.state, size = 56.dp)
            Spacer(Modifier.width(spacing.mediumLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vm.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(spacing.extraSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = vm.state.dotColor(), shape = RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(spacing.extraSmall))
                    Text(
                        text = vm.statusLabel,
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

private val previewVms = listOf(
    VmDomain(id = "1", name = "Windows 11", state = VmRunState.RUNNING, statusLabel = "Running"),
    VmDomain(id = "2", name = "Ubuntu Server", state = VmRunState.STOPPED, statusLabel = "Shut Off"),
    VmDomain(id = "3", name = "macOS Sonoma", state = VmRunState.PAUSED, statusLabel = "Paused")
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VmsScreenPreview() {
    AppTheme {
        VmsScreenContent(uiState = VmsScreenState(vms = WidgetResult.Success(previewVms)))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
fun VmsScreenEmptyPreview() {
    AppTheme {
        VmsScreenContent(uiState = VmsScreenState(vms = WidgetResult.Success(emptyList())))
    }
}
