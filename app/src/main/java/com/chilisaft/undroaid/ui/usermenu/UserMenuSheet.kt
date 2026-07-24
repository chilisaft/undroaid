package com.chilisaft.undroaid.ui.usermenu

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chilisaft.undroaid.data.models.ApiKeyInfo
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.mediumLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
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
                Text("Logout")
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

@Composable
private fun InfoRow(label: String, value: String) {
    val spacing = MaterialTheme.spacing
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(spacing.medium))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
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
                apiKeyInfo = WidgetResult.Success(ApiKeyInfo(name = "undroaid-app", roles = listOf("Admin")))
            ),
            onDismiss = {},
            onSettingsClick = {},
            onLogoutConfirmed = {}
        )
    }
}
