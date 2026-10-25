package com.chilisaft.undroid.ui.vms

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.chilisaft.undroid.data.models.VmDomain
import com.chilisaft.undroid.data.models.VmRunState
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.ActionSheet
import com.chilisaft.undroid.ui.components.ActionSheetItem
import com.chilisaft.undroid.ui.components.ConfirmActionDialog
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.ResourceHeaderCard
import com.chilisaft.undroid.ui.components.ResourceRowCard
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.TabTopBar
import com.chilisaft.undroid.ui.components.VmIconBadge
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.components.statusTone
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing

@Composable
fun VmsScreen(viewModel: VmsViewModel = hiltViewModel(), topBarActions: @Composable RowScope.() -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { }
    }
    VmsScreenContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        topBarActions = topBarActions,
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
    topBarActions: @Composable RowScope.() -> Unit = {},
    onStart: (String) -> Unit = {},
    onStop: (String) -> Unit = {},
    onForceStop: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onResume: (String) -> Unit = {},
    onReboot: (String) -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var selectedVm by remember { mutableStateOf<VmDomain?>(null) }
    var forceStopCandidate by remember { mutableStateOf<VmDomain?>(null) }

    Scaffold(
        topBar = { TabTopBar(title = "VMs", icon = Icons.Filled.Computer, actions = topBarActions) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.vms, onRetry = onRetry, minHeight = 200.dp) { vms ->
                if (vms.isEmpty()) {
                    EmptyState("No virtual machines found")
                } else {
                    ScreenList {
                        items(vms, key = { it.id }) { vm ->
                            ResourceRowCard(
                                title = vm.name,
                                tone = vm.statusTone,
                                statusText = vm.statusLabel,
                                isActioning = vm.id in uiState.actioningIds,
                                onClick = { selectedVm = vm },
                                leading = { size -> VmIconBadge(state = vm.state, size = size) }
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
            onForceStop = { forceStopCandidate = vm; selectedVm = null },
            onPause = { onPause(vm.id); selectedVm = null },
            onResume = { onResume(vm.id); selectedVm = null },
            onReboot = { onReboot(vm.id); selectedVm = null }
        )
    }

    val candidate = forceStopCandidate
    if (candidate != null) {
        ConfirmActionDialog(
            title = "Force stop ${candidate.name}?",
            text = "This cuts the VM's power immediately instead of asking it to shut down. Anything unsaved inside it is lost.",
            confirmLabel = "Force Stop",
            onConfirm = { onForceStop(candidate.id) },
            onDismiss = { forceStopCandidate = null },
            destructive = true
        )
    }
}

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
    ActionSheet(
        onDismissRequest = onDismiss,
        header = {
            ResourceHeaderCard(
                title = vm.name,
                tone = vm.statusTone,
                statusText = vm.statusLabel,
                leading = { size -> VmIconBadge(state = vm.state, size = size) }
            )
        }
    ) {
        when (vm.state) {
            VmRunState.RUNNING -> {
                ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onReboot)
                ActionSheetItem(Icons.Filled.Pause, "Pause", onPause)
                ActionSheetItem(Icons.Filled.Stop, "Stop", onStop, destructive = true)
                ActionSheetItem(Icons.Filled.PowerSettingsNew, "Force Stop", onForceStop, destructive = true)
            }
            VmRunState.PAUSED -> {
                ActionSheetItem(Icons.Filled.RestartAlt, "Restart", onReboot)
                ActionSheetItem(Icons.Filled.PlayArrow, "Resume", onResume)
                ActionSheetItem(Icons.Filled.Stop, "Stop", onStop, destructive = true)
                ActionSheetItem(Icons.Filled.PowerSettingsNew, "Force Stop", onForceStop, destructive = true)
            }
            VmRunState.STOPPED -> {
                ActionSheetItem(Icons.Filled.PlayArrow, "Start", onStart)
            }
        }
    }
}

private val previewVms = listOf(
    VmDomain(id = "1", name = "Windows 11", state = VmRunState.RUNNING, statusLabel = "Running"),
    VmDomain(id = "2", name = "Ubuntu Server", state = VmRunState.STOPPED, statusLabel = "Shut Off"),
    VmDomain(id = "3", name = "macOS Sonoma", state = VmRunState.PAUSED, statusLabel = "Paused"),
    VmDomain(id = "4", name = "Home Assistant OS", state = VmRunState.STOPPED, statusLabel = "Crashed", crashed = true)
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
