package com.chilisaft.undroid.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroid.data.models.DashboardCards
import com.chilisaft.undroid.data.models.LiveUpdateSpeed
import com.chilisaft.undroid.data.models.StartTab
import com.chilisaft.undroid.data.models.TemperatureUnit
import com.chilisaft.undroid.data.models.ThemeMode
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.chilisaft.undroid.ui.components.SectionHeader
import com.chilisaft.undroid.ui.components.rememberCopyToClipboard
import com.chilisaft.undroid.ui.components.tryOpenUri
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.AppLinks

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLicences: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onThemeModeChange = viewModel::setThemeMode,
        onUseDynamicColorChange = viewModel::setUseDynamicColor,
        onShowCoreListChange = viewModel::setShowCoreList,
        onStartTabChange = viewModel::setStartTab,
        onLiveUpdateSpeedChange = viewModel::setLiveUpdateSpeed,
        onTemperatureUnitChange = viewModel::setTemperatureUnit,
        onDashboardCardsChange = viewModel::setDashboardCards,
        onOpenLicences = onOpenLicences
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsScreenState,
    onBack: () -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onUseDynamicColorChange: (Boolean) -> Unit = {},
    onShowCoreListChange: (Boolean) -> Unit = {},
    onStartTabChange: (StartTab) -> Unit = {},
    onLiveUpdateSpeedChange: (LiveUpdateSpeed) -> Unit = {},
    onTemperatureUnitChange: (TemperatureUnit) -> Unit = {},
    onDashboardCardsChange: (DashboardCards) -> Unit = {},
    onOpenLicences: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    val uriHandler = LocalUriHandler.current
    val copyDebugInfo = rememberCopyToClipboard("Undroid debug info", uiState.debugInfo.toText())
    val cards = uiState.dashboardCards

    Scaffold(
        topBar = { DetailTopBar(title = "Settings", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
        ) {
            Spacer(Modifier.height(spacing.extraSmall))

            SettingsSection(title = "General") {
                SettingLabel("Start tab", "The tab the app opens on. Applies from the next start.")
                ChoiceRow(StartTab.entries.map { it to it.label }, uiState.startTab, onStartTabChange)
            }

            SettingsSection(title = "Appearance") {
                ChoiceRow(
                    listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
                    uiState.themeMode,
                    onThemeModeChange
                )
                HorizontalDivider()
                SwitchRow(
                    title = "Dynamic colors",
                    description = "Match colors to your wallpaper (Android 12+). Off by default, using the app's own forest theme.",
                    checked = uiState.useDynamicColor,
                    onCheckedChange = onUseDynamicColorChange
                )
                HorizontalDivider()
                SettingLabel("Temperature unit", "For the Dashboard's sensors and the disk temperatures on Main.")
                ChoiceRow(TemperatureUnit.entries.map { it to it.symbol }, uiState.temperatureUnit, onTemperatureUnitChange)
            }

            SettingsSection(title = "Dashboard") {
                SettingLabel(
                    "Live updates",
                    "How often CPU and memory refresh while the Dashboard is open (temperature and network less often). " +
                        "Off: pull to refresh only."
                )
                ChoiceRow(LiveUpdateSpeed.entries.map { it to it.label }, uiState.liveUpdateSpeed, onLiveUpdateSpeedChange)
                HorizontalDivider()
                SwitchRow(
                    title = "Temperature & network",
                    checked = cards.sensors,
                    onCheckedChange = { onDashboardCardsChange(cards.copy(sensors = it)) }
                )
                SwitchRow(title = "Docker", checked = cards.docker, onCheckedChange = { onDashboardCardsChange(cards.copy(docker = it)) })
                SwitchRow(title = "Virtual machines", checked = cards.vms, onCheckedChange = { onDashboardCardsChange(cards.copy(vms = it)) })
            }

            SettingsSection(title = "Devices") {
                SwitchRow(
                    title = "Show core devices",
                    description = "Also list chipset devices - bridges, memory and bus controllers, USB root hubs - on the Devices screen.",
                    checked = uiState.showCoreList,
                    onCheckedChange = onShowCoreListChange
                )
            }

            SettingsSection(title = "About & Help", contentPadding = PaddingValues(vertical = spacing.small)) {
                LinkRow(Icons.Filled.Info, "Undroid ${uiState.debugInfo.appVersion}", "Free software under the GNU GPL v3")
                LinkRow(Icons.AutoMirrored.Filled.OpenInNew, "Website", "github.com/chilisaft/undroaid") {
                    uriHandler.tryOpenUri(AppLinks.WEBSITE)
                }
                LinkRow(Icons.Filled.BugReport, "Report a Problem", "Opens a GitHub issue with the debug info filled in") {
                    uriHandler.tryOpenUri(uiState.debugInfo.issueUrl())
                }
                LinkRow(Icons.Filled.ContentCopy, "Copy Debug Info", "App, Android, device and Unraid versions - never your server address or key", copyDebugInfo)
                LinkRow(Icons.Filled.Description, "Open-Source Licences", "The libraries Undroid is built with", onOpenLicences)
            }

            Spacer(Modifier.height(spacing.medium))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.spacing.medium),
    content: @Composable ColumnScope.() -> Unit
) {
    SectionHeader(title)
    AppCard {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingLabel(title: String, description: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        description?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, description: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) { SettingLabel(title, description) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** One choice out of a few, as a full-width segmented row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label, maxLines = 1) }
            )
        }
    }
}

/** An icon, title and subtitle; tappable when [onClick] is set. */
@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(spacing.medium))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, heightDp = 1600)
@Preview(name = "Dark Mode", showBackground = true, heightDp = 1600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreenContent(uiState = SettingsScreenState(themeMode = ThemeMode.SYSTEM))
    }
}
