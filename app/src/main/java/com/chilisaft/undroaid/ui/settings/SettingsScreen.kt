package com.chilisaft.undroaid.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoggedOut) {
        LaunchedEffect(Unit) { onLoggedOut() }
    }

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onThemeModeChange = viewModel::setThemeMode,
        onShowCoreListChange = viewModel::setShowCoreList,
        onUseDynamicColorChange = viewModel::setUseDynamicColor,
        onLogoutConfirmed = viewModel::logout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsScreenState,
    onBack: () -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onShowCoreListChange: (Boolean) -> Unit = {},
    onUseDynamicColorChange: (Boolean) -> Unit = {},
    onLogoutConfirmed: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            Spacer(Modifier.height(spacing.small))

            SettingsSection(title = "Appearance") {
                ThemeModeSelector(selected = uiState.themeMode, onSelected = onThemeModeChange)
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dynamic colors", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Match colors to your wallpaper (Android 12+). Turn off to use the Unraid orange theme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = uiState.useDynamicColor, onCheckedChange = onUseDynamicColorChange)
                }
            }

            SettingsSection(title = "Display") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show core devices", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Include core system devices in device lists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = uiState.showCoreList, onCheckedChange = onShowCoreListChange)
                }
            }

            SettingsSection(title = "Account") {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(spacing.small))
                    Text("Logout")
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout?") },
            text = { Text("You'll need your server URL and API token to log back in.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogoutConfirmed()
                }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelected: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to "System",
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                selected = selected == mode,
                onClick = { onSelected(mode) }
            ) {
                Text(label)
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreenContent(uiState = SettingsScreenState(themeMode = ThemeMode.SYSTEM, showCoreList = true))
    }
}
