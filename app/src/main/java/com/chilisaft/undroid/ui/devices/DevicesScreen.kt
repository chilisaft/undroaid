package com.chilisaft.undroid.ui.devices

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroid.data.models.PciDevice
import com.chilisaft.undroid.data.models.PciDeviceKind
import com.chilisaft.undroid.data.models.UsbDevice
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.SectionHeader
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing

@Composable
fun DevicesScreen(onBack: () -> Unit = {}, viewModel: DevicesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    DevicesScreenContent(
        uiState = uiState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetryPci = viewModel::refreshPci,
        onRetryUsb = viewModel::refreshUsb
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreenContent(
    uiState: DevicesScreenState,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryPci: () -> Unit = {},
    onRetryUsb: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = { DetailTopBar(title = "Devices", onBack = onBack) }
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
                item { SectionHeader("PCI") }
                item {
                    WidgetSection(result = uiState.pci, onRetry = onRetryPci, minHeight = 160.dp) { devices ->
                        PciGroups(devices, uiState.showCoreDevices)
                    }
                }
                item { SectionHeader("USB") }
                item {
                    WidgetSection(result = uiState.usb, onRetry = onRetryUsb, minHeight = 96.dp) { devices ->
                        UsbList(devices, uiState.showCoreDevices)
                    }
                }
            }
        }
    }
}

private val pciGroupOrder = listOf(
    PciDeviceKind.GRAPHICS to "Graphics",
    PciDeviceKind.STORAGE to "Storage",
    PciDeviceKind.NETWORK to "Network",
    PciDeviceKind.USB_CONTROLLER to "USB controllers",
    PciDeviceKind.OTHER to "Other",
    PciDeviceKind.CORE to "Core devices"
)

@Composable
private fun PciGroups(devices: List<PciDevice>, showCore: Boolean) {
    val spacing = MaterialTheme.spacing
    val visible = if (showCore) devices else devices.filter { it.kind != PciDeviceKind.CORE }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
        if (visible.isEmpty()) {
            EmptyState("No PCI devices reported")
        }
        pciGroupOrder.forEach { (kind, title) ->
            val group = visible.filter { it.kind == kind }
            if (group.isNotEmpty()) {
                DeviceCard(title, group.map { it.name to listOfNotNull(it.vendor, it.address).joinToString(" · ") })
            }
        }
        HiddenCoreHint(count = if (showCore) 0 else devices.size - visible.size)
    }
}

@Composable
private fun UsbList(devices: List<UsbDevice>, showCore: Boolean) {
    val spacing = MaterialTheme.spacing
    val visible = if (showCore) devices else devices.filterNot { it.isCore }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
        if (visible.isEmpty()) {
            EmptyState("No USB devices reported")
        } else {
            DeviceCard(title = null, rows = visible.map { it.name to null })
        }
        HiddenCoreHint(count = if (showCore) 0 else devices.size - visible.size)
    }
}

/** A card of device rows - name, plus an optional detail line (vendor and bus address). */
@Composable
private fun DeviceCard(title: String?, rows: List<Pair<String, String?>>) {
    val spacing = MaterialTheme.spacing
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            rows.forEachIndexed { index, (name, detail) ->
                if (index > 0) HorizontalDivider()
                Column {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    detail?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenCoreHint(count: Int) {
    if (count <= 0) return
    Text(
        "$count core ${if (count == 1) "device" else "devices"} hidden - turn on \"Show core devices\" in Settings to list them.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
    )
}

private val previewState = DevicesScreenState(
    pci = WidgetResult.Success(
        listOf(
            PciDevice("00:00.0", PciDeviceKind.CORE, "Host bridge", "Intel"),
            PciDevice("00:02.0", PciDeviceKind.GRAPHICS, "UHD Graphics 730", "Intel"),
            PciDevice("00:14.0", PciDeviceKind.USB_CONTROLLER, "600 Series Chipset Family USB 3.2 Gen 2x2 (20Gbs) XHCI Host Controller", "Intel"),
            PciDevice("00:17.0", PciDeviceKind.STORAGE, "600 Series Chipset Family SATA Controller (AHCI)", "Intel"),
            PciDevice("01:00.0", PciDeviceKind.STORAGE, "NVMe SSD Controller MAP1202 (DRAM-less)", "MAXIO Technology (Hangzhou) Ltd."),
            PciDevice("00:1f.6", PciDeviceKind.NETWORK, "Ethernet Connection (17) I219-V", "Intel")
        )
    ),
    usb = WidgetResult.Success(
        listOf(UsbDevice("Linux Foundation 2.0 root hub", isCore = true), UsbDevice("SanDisk Corp. Ultra Fit", isCore = false))
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DevicesScreenPreview() {
    AppTheme {
        DevicesScreenContent(uiState = previewState)
    }
}

@Preview(name = "Core devices shown", showBackground = true)
@Composable
fun DevicesScreenCorePreview() {
    AppTheme {
        DevicesScreenContent(uiState = previewState.copy(showCoreDevices = true))
    }
}
