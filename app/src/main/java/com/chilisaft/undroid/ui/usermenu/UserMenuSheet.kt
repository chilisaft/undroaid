package com.chilisaft.undroid.ui.usermenu

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chilisaft.undroid.data.models.ApiKeyInfo
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.ConfirmActionDialog
import com.chilisaft.undroid.ui.components.InfoRow
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMenuSheet(
    uiState: UserMenuState,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutConfirmed: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    var showLogoutDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // surfaceContainerHigh, not the default Low - a bottom sheet is itself surfaceContainerLow,
            // so a default AppCard would vanish into it (same as ResourceHeaderCard on action sheets).
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    InfoRow(label = "API Key", value = uiState.apiKeyInfo.toDisplayString { it.name })
                    InfoRow(label = "Roles", value = uiState.apiKeyInfo.toDisplayString { it.roles.joinToString().ifEmpty { "None" } })
                    HorizontalDivider()
                    InfoRow(label = "Server URL", value = uiState.serverUrl ?: "Unknown")
                    InfoRow(label = "Server Version", value = uiState.serverVersion.toDisplayString())
                    InfoRow(label = "App Version", value = uiState.appVersion)
                }
            }

            Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(spacing.small))
                Text("Settings")
            }

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(spacing.small))
                Text("Log Out")
            }
        }
    }

    if (showLogoutDialog) {
        ConfirmActionDialog(
            title = "Log out?",
            text = "You'll need your server URL and API key to log back in.",
            confirmLabel = "Log Out",
            onConfirm = onLogoutConfirmed,
            onDismiss = { showLogoutDialog = false },
            destructive = true
        )
    }
}

private fun WidgetResult<String?>.toDisplayString(): String = when (this) {
    is WidgetResult.Success -> data ?: "Unknown"
    is WidgetResult.Failure -> if (permissionDenied) "No permission" else "Unavailable"
    WidgetResult.Loading -> "Loading…"
}

private fun WidgetResult<ApiKeyInfo>.toDisplayString(field: (ApiKeyInfo) -> String): String = when (this) {
    is WidgetResult.Success -> field(data)
    is WidgetResult.Failure -> if (permissionDenied) "No permission" else "Unavailable"
    WidgetResult.Loading -> "Loading…"
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun UserMenuSheetPreview() {
    AppTheme {
        UserMenuSheet(
            uiState = UserMenuState(
                serverUrl = "https://tower.local:443",
                appVersion = "1.0",
                serverVersion = WidgetResult.Success("6.12.10"),
                apiKeyInfo = WidgetResult.Success(ApiKeyInfo(name = "undroid-app", roles = listOf("Admin")))
            ),
            onDismiss = {},
            onSettingsClick = {},
            onLogoutConfirmed = {}
        )
    }
}
