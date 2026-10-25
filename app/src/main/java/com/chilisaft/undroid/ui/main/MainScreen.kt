package com.chilisaft.undroid.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.chilisaft.undroid.data.models.ArrayDeviceInfo
import com.chilisaft.undroid.data.models.ArrayDeviceRole
import com.chilisaft.undroid.data.models.ArrayOverview
import com.chilisaft.undroid.data.models.ArrayRunState
import com.chilisaft.undroid.data.models.DeviceHealth
import com.chilisaft.undroid.data.models.DiskIdentification
import com.chilisaft.undroid.data.models.FlashInfo
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.ConfirmActionDialog
import com.chilisaft.undroid.ui.components.ParityCheckProgress
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.SectionHeader
import com.chilisaft.undroid.ui.components.StatusDot
import com.chilisaft.undroid.ui.components.TabTopBar
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.kilobytesToHumanReadable

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel(), topBarActions: @Composable RowScope.() -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }
    MainScreenContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        topBarActions = topBarActions,
        onStartArray = viewModel::startArray,
        onStopArray = viewModel::stopArray,
        onRetryParityCheck = viewModel::refreshParityCheck,
        onStartParityCheck = viewModel::startParityCheck,
        onPauseParityCheck = viewModel::pauseParityCheck,
        onResumeParityCheck = viewModel::resumeParityCheck,
        onCancelParityCheck = viewModel::cancelParityCheck
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainScreenState,
    onRetry: () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    onStartArray: (String?) -> Unit = {},
    onStopArray: () -> Unit = {},
    onRetryParityCheck: () -> Unit = {},
    onStartParityCheck: () -> Unit = {},
    onPauseParityCheck: () -> Unit = {},
    onResumeParityCheck: () -> Unit = {},
    onCancelParityCheck: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = { TabTopBar(title = "Main", icon = Icons.Filled.Storage, actions = topBarActions) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.overview, onRetry = onRetry, minHeight = 200.dp) { overview ->
                ScreenList {
                    item { ArrayStatusLine(overview) }

                    if (overview.parities.isNotEmpty() || overview.disks.isNotEmpty()) {
                        item { SectionHeader("Array Devices") }
                        item {
                            DeviceGroupCard(overview.parities + overview.disks, uiState.diskIdentifications)
                        }
                    }

                    if (overview.caches.isNotEmpty()) {
                        item { SectionHeader("Pool Devices") }
                        val pools = overview.caches.groupBy { it.poolGroupKey() }
                        pools.forEach { (poolName, devices) ->
                            item(key = "pool-$poolName") {
                                PoolCard(poolName, devices, uiState.diskIdentifications)
                            }
                        }
                    }

                    overview.boot?.let { boot ->
                        item { SectionHeader("Boot Device") }
                        item { ArrayDeviceRow(boot, flashIdentificationLine(uiState.flashInfo)) }
                    }

                    item { SectionHeader("Array Operation") }
                    item {
                        ArrayOperationSection(
                            overview = overview,
                            parityCheckResult = uiState.parityCheck,
                            actioningArray = uiState.actioningArray,
                            actioningParityCheck = uiState.actioningParityCheck,
                            onStartArray = onStartArray,
                            onStopArray = onStopArray,
                            onRetryParityCheck = onRetryParityCheck,
                            onStartParityCheck = onStartParityCheck,
                            onPauseParityCheck = onPauseParityCheck,
                            onResumeParityCheck = onResumeParityCheck,
                            onCancelParityCheck = onCancelParityCheck
                        )
                    }

                }
            }
        }
    }
}

/** Best-effort match by device path - identification is its own widget, so it may still be loading/failed/absent. */
private fun identificationLine(device: ArrayDeviceInfo, result: WidgetResult<List<DiskIdentification>>): String? {
    val identifications = (result as? WidgetResult.Success)?.data ?: return null
    val identification = identifications.find { it.device == device.device } ?: return null
    return "${identification.vendor} ${identification.model} · S/N ${identification.serialNumber}"
}

private fun flashIdentificationLine(result: WidgetResult<FlashInfo>): String? {
    val flash = (result as? WidgetResult.Success)?.data ?: return null
    return "${flash.vendor} ${flash.product}"
}

private val ArrayDeviceInfo.statusTone: StatusTone
    get() = when (health) {
        DeviceHealth.OK -> StatusTone.Ok
        DeviceHealth.NOT_PRESENT -> StatusTone.Neutral
        DeviceHealth.PROBLEM -> StatusTone.Error
    }

/**
 * Same rule as the Dashboard's "HEALTH" pill (any device not OK is a problem), plus the array's
 * run state: a stopped array is a normal, neutral state, not an error.
 */
private val ArrayOverview.statusTone: StatusTone
    get() = when {
        (parities + disks + caches).any { it.health == DeviceHealth.PROBLEM } -> StatusTone.Error
        state == ArrayRunState.STARTED -> StatusTone.Ok
        state == ArrayRunState.STOPPED -> StatusTone.Neutral
        else -> StatusTone.Warning
    }

@Composable
private fun ArrayStatusLine(overview: ArrayOverview) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(overview.statusTone)
            Spacer(Modifier.width(spacing.small))
            Text(overview.statusLabel, style = MaterialTheme.typography.titleMedium)
        }
        if (overview.usedKb != null && overview.totalKb != null) {
            Text(
                "${overview.usedKb.kilobytesToHumanReadable()} / ${overview.totalKb.kilobytesToHumanReadable()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArrayDeviceRow(device: ArrayDeviceInfo, identificationLine: String?) {
    AppCard {
        ArrayDeviceRowContent(device, identificationLine, modifier = Modifier.padding(MaterialTheme.spacing.medium))
    }
}

@Composable
private fun ArrayDeviceRowContent(device: ArrayDeviceInfo, identificationLine: String?, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.name ?: device.device ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(spacing.small))
                RoleBadge(device.role)
            }
            Spacer(Modifier.height(spacing.extraSmall))
            Text(
                text = deviceSummaryLine(device),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (identificationLine != null) {
                Text(
                    text = identificationLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(spacing.small))
        StatusDot(device.statusTone)
    }
}

/** Gap between grouped items - small enough that they still read as one cluster, not separate cards. */
private val GROUP_ITEM_GAP = 2.dp

/**
 * The corner shape for item [index] of [count] in a grouped cluster: outer corners (top of the
 * first item, bottom of the last) keep the card shape (`shapes.large`) - every corner that
 * touches a neighboring item flattens to `shapes.extraSmall`, matching Android's own Settings
 * grouped-list style. A single-item group (`count == 1`) is just a normal card shape.
 */
@Composable
private fun groupItemShape(index: Int, count: Int): CornerBasedShape {
    val outer = MaterialTheme.shapes.large
    val inner = MaterialTheme.shapes.extraSmall.topStart
    val top = if (index == 0) outer.topStart else inner
    val bottom = if (index == count - 1) outer.bottomStart else inner
    return outer.copy(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Renders every device in [devices] as its own small-gap, stepped-corner surface - the "grouped
 * list" look Android's own Settings app uses for related items (as opposed to one bounded `Card`
 * with hairline dividers): each row keeps its own background, but adjacent corners flatten to
 * `shapes.extraSmall` and the gap between rows shrinks to [GROUP_ITEM_GAP], so the cluster still
 * reads as one logical group rather than separate floating cards.
 */
@Composable
private fun DeviceGroupCard(devices: List<ArrayDeviceInfo>, identifications: WidgetResult<List<DiskIdentification>>) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(GROUP_ITEM_GAP)) {
        devices.forEachIndexed { index, device ->
            GroupedItemSurface(index = index, count = devices.size) {
                ArrayDeviceRowContent(device, identificationLine(device, identifications), modifier = Modifier.padding(spacing.medium))
            }
        }
    }
}

@Composable
private fun GroupedItemSurface(index: Int, count: Int, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = groupItemShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        content()
    }
}

/**
 * The schema has no pool-ID field anywhere - `array.caches` is just a flat list of `ArrayDisk`
 * filtered by type (confirmed against the `unraid-api` source: `get-array-data.ts` builds it with
 * a bare `disk.type === CACHE` filter, nothing pool-aware). `name` isn't computed by the API
 * either - it's passed straight through from the OS's `disks.ini` state file. That file's own
 * naming convention is what this keys off of: a pool's first/primary member is named exactly as
 * the pool (e.g. "cache"), and additional members get the same base name suffixed with their slot
 * order ("cache2", "cache3", ...) - so stripping a trailing digit run recovers the shared pool
 * name. This is a heuristic, not a guaranteed invariant: nothing stops a user from naming an
 * unrelated single-disk pool "cache2", which would wrongly merge into a real "cache" pool's group.
 * Good enough for the common case (a single multi-disk pool, e.g. a mirror) given the API exposes
 * nothing better.
 */
private fun ArrayDeviceInfo.poolGroupKey(): String {
    val label = name ?: return id
    return label.trimEnd { it.isDigit() }.ifEmpty { label }
}

/**
 * A pool with more than one member disk (e.g. a mirror) has no dedicated "pool" concept in this
 * schema, hence [ArrayDeviceInfo.poolGroupKey]'s naming heuristic above; the actual redundancy
 * profile (mirror/raid/single) isn't exposed either. The pool name is just a small plain title
 * above the cluster, not a row inside it - each device row already carries its own CACHE
 * [RoleBadge], so repeating that (plus a device count) in a whole extra capsule read as
 * redundant.
 */
@Composable
private fun PoolCard(poolName: String, devices: List<ArrayDeviceInfo>, identifications: WidgetResult<List<DiskIdentification>>) {
    if (devices.size == 1) {
        ArrayDeviceRow(devices[0], identificationLine(devices[0], identifications))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
        Text(
            text = poolName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = MaterialTheme.spacing.small)
        )
        DeviceGroupCard(devices, identifications)
    }
}

private fun deviceSummaryLine(device: ArrayDeviceInfo): String {
    val parts = mutableListOf<String>()
    device.sizeKb?.let { parts += it.kilobytesToHumanReadable() }
    if (device.usedKb != null && device.freeKb != null) {
        parts += "${device.usedKb.kilobytesToHumanReadable()} used"
    }
    device.tempC?.let { parts += "${it}°C" }
    device.isSpinning?.let { parts += if (it) "Spinning" else "Standby" }
    if (device.health != DeviceHealth.OK) device.statusLabel?.let { parts += it }
    return if (parts.isEmpty()) "No data" else parts.joinToString(" · ")
}

@Composable
private fun RoleBadge(role: ArrayDeviceRole) {
    val label = when (role) {
        ArrayDeviceRole.DATA -> "DATA"
        ArrayDeviceRole.PARITY -> "PARITY"
        ArrayDeviceRole.BOOT -> "BOOT"
        ArrayDeviceRole.FLASH -> "FLASH"
        ArrayDeviceRole.CACHE -> "CACHE"
    }
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrayOperationSection(
    overview: ArrayOverview,
    parityCheckResult: WidgetResult<ParityCheckInfo>,
    actioningArray: Boolean,
    actioningParityCheck: Boolean,
    onStartArray: (String?) -> Unit,
    onStopArray: () -> Unit,
    onRetryParityCheck: () -> Unit,
    onStartParityCheck: () -> Unit,
    onPauseParityCheck: () -> Unit,
    onResumeParityCheck: () -> Unit,
    onCancelParityCheck: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    var showStartDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showCancelParityDialog by remember { mutableStateOf(false) }

    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            // Array start/stop
            when (overview.state) {
                ArrayRunState.STOPPED -> Button(
                    onClick = { showStartDialog = true },
                    enabled = !actioningArray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (actioningArray) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Start Array")
                }
                ArrayRunState.STARTED -> Button(
                    onClick = { showStopDialog = true },
                    enabled = !actioningArray,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (actioningArray) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Stop Array")
                }
                ArrayRunState.OTHER -> Text(
                    "Array: ${overview.statusLabel} - no start/stop action available in this state",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Parity check - its own independently loadable/retryable widget (see
            // ArrayRepository.getParityCheckStatus) so a parity-specific hiccup no longer blanks
            // the array/cache/boot device tables above.
            WidgetSection(result = parityCheckResult, onRetry = onRetryParityCheck, minHeight = 80.dp) { parityCheck ->
                ParityCheckControls(
                    parityCheck = parityCheck,
                    paritySizeKb = overview.parities.firstOrNull()?.sizeKb,
                    actioning = actioningParityCheck,
                    onStart = onStartParityCheck,
                    onPause = onPauseParityCheck,
                    onResume = onResumeParityCheck,
                    onCancel = { showCancelParityDialog = true }
                )
            }
        }
    }

    if (showStopDialog) {
        ConfirmActionDialog(
            title = "Stop the array?",
            text = "Shares go offline and every Docker container and VM stops until the array is started again.",
            confirmLabel = "Stop Array",
            onConfirm = onStopArray,
            onDismiss = { showStopDialog = false },
            destructive = true
        )
    }

    if (showCancelParityDialog) {
        ConfirmActionDialog(
            title = "Stop the parity check?",
            text = "Progress so far is discarded - the next check starts again from the beginning.",
            confirmLabel = "Stop Check",
            onConfirm = onCancelParityCheck,
            onDismiss = { showCancelParityDialog = false },
            destructive = true
        )
    }

    if (showStartDialog) {
        StartArrayDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { password ->
                showStartDialog = false
                onStartArray(password)
            }
        )
    }
}

@Composable
private fun ParityCheckControls(
    parityCheck: ParityCheckInfo,
    paritySizeKb: Long?,
    actioning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val isActive = parityCheck.running || parityCheck.paused
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        ParityCheckProgress(parityCheck, paritySizeKb)
        // Always shown, individually enabled/disabled by state, rather than swapping between
        // different button sets - Start doubles as Resume while paused (one action either way).
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            OutlinedButton(
                onClick = if (parityCheck.paused) onResume else onStart,
                enabled = !actioning && !parityCheck.running,
                modifier = Modifier.weight(1f)
            ) {
                if (actioning) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(if (parityCheck.paused) "Resume" else "Start")
            }
            OutlinedButton(
                onClick = onPause,
                enabled = !actioning && parityCheck.running,
                modifier = Modifier.weight(1f)
            ) { Text("Pause") }
            OutlinedButton(
                onClick = onCancel,
                enabled = !actioning && isActive,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) { Text("Stop") }
        }
    }
}

@Composable
private fun StartArrayDialog(onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Array") },
        text = {
            Column {
                Text(
                    "Leave blank unless your array disks are encrypted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Decryption password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.ifBlank { null }) }) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val previewOverview = ArrayOverview(
    state = ArrayRunState.STARTED,
    statusLabel = "Array Started",
    usedKb = 1_953_514_584L,
    totalKb = 7_814_058_336L,
    boot = ArrayDeviceInfo(
        id = "54", name = "flash", device = "/dev/sda", role = ArrayDeviceRole.FLASH,
        sizeKb = 30_500_000L, usedKb = null, freeKb = null, statusLabel = "OK", health = DeviceHealth.OK,
        tempC = null, isSpinning = null, fsType = "vfat", transport = "usb"
    ),
    caches = listOf(
        ArrayDeviceInfo(
            id = "30", name = "cache", device = "/dev/sdd", role = ArrayDeviceRole.CACHE,
            sizeKb = 1_953_514_584L, usedKb = 500_000_000L, freeKb = 1_453_514_584L, statusLabel = "OK", health = DeviceHealth.OK,
            tempC = 38, isSpinning = true, fsType = "btrfs", transport = "nvme"
        ),
        ArrayDeviceInfo(
            id = "31", name = "cache", device = "/dev/sde", role = ArrayDeviceRole.CACHE,
            sizeKb = 1_953_514_584L, usedKb = 500_000_000L, freeKb = 1_453_514_584L, statusLabel = "OK", health = DeviceHealth.OK,
            tempC = 37, isSpinning = true, fsType = "btrfs", transport = "nvme"
        )
    ),
    parities = listOf(
        ArrayDeviceInfo(
            id = "0", name = "parity", device = "/dev/sdb", role = ArrayDeviceRole.PARITY,
            sizeKb = 7_814_058_336L, usedKb = null, freeKb = null, statusLabel = "OK", health = DeviceHealth.OK,
            tempC = 32, isSpinning = true, fsType = null, transport = "sata"
        )
    ),
    disks = listOf(
        ArrayDeviceInfo(
            id = "1", name = "disk1", device = "/dev/sdc", role = ArrayDeviceRole.DATA,
            sizeKb = 7_814_058_336L, usedKb = 3_500_000_000L, freeKb = 4_314_058_336L, statusLabel = "OK", health = DeviceHealth.OK,
            tempC = 34, isSpinning = true, fsType = "xfs", transport = "sata"
        )
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreview() {
    AppTheme {
        MainScreenContent(
            uiState = MainScreenState(
                overview = WidgetResult.Success(previewOverview),
                parityCheck = WidgetResult.Success(
                    ParityCheckInfo(
                        statusLabel = "Running", running = true, paused = false,
                        progressPercent = 42, speed = "150", errors = 0, correcting = false
                    )
                ),
                diskIdentifications = WidgetResult.Success(
                    listOf(
                        DiskIdentification(
                            device = "/dev/sdc", model = "WDC WD140EDGZ", vendor = "Western Digital",
                            serialNumber = "ABC123", interfaceType = "SATA", smartStatus = "OK"
                        )
                    )
                ),
                flashInfo = WidgetResult.Success(FlashInfo(vendor = "SanDisk", product = "Ultra Fit"))
            )
        )
    }
}
