package com.chilisaft.undroaid.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.Notification
import com.chilisaft.undroaid.data.models.NotificationLevel
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.components.WidgetSection
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationsChanged: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    NotificationsScreenContent(
        uiState = uiState,
        // The dashboard's unread badge doesn't observe this screen, so nudge it to refresh
        // whenever the user leaves - cheap enough to do unconditionally rather than tracking
        // whether anything actually changed.
        onBack = { onNotificationsChanged(); onBack() },
        onRetry = viewModel::refresh,
        onDismiss = viewModel::dismiss,
        onDismissAll = viewModel::dismissAll
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreenContent(
    uiState: NotificationsScreenState,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismiss: (String) -> Unit = {},
    onDismissAll: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val hasNotifications = (uiState.notifications as? WidgetResult.Success)?.data?.isNotEmpty() == true
                    TextButton(
                        onClick = onDismissAll,
                        enabled = hasNotifications && !uiState.isDismissingAll
                    ) {
                        if (uiState.isDismissingAll) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("DISMISS ALL")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.notifications, onRetry = onRetry, minHeight = 200.dp) { notifications ->
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No new notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                        contentPadding = PaddingValues(vertical = spacing.medium)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                isDismissing = notification.id in uiState.dismissingIds,
                                onDismiss = { onDismiss(notification.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: Notification, isDismissing: Boolean, onDismiss: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val (icon, color) = when (notification.level) {
        NotificationLevel.ALERT -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
        NotificationLevel.WARNING -> Icons.Filled.Warning to MaterialTheme.colorScheme.tertiary
        NotificationLevel.INFO -> Icons.Filled.TaskAlt to MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(spacing.medium), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(notification.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                if (notification.timestamp != null) {
                    Spacer(Modifier.height(spacing.small))
                    Text(notification.timestamp, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.width(spacing.small))
            if (isDismissing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private val previewNotifications = listOf(
    Notification(
        id = "1",
        title = "Parity Check Complete",
        description = "System finished scheduled parity check. Duration: 14h 22m. Errors found: 0.",
        level = NotificationLevel.INFO,
        timestamp = "2 hours ago"
    ),
    Notification(
        id = "2",
        title = "Disk 4 Temperature Alert",
        description = "WDC_WD120EDAZ has reached 46°C. Cooling threshold exceeded.",
        level = NotificationLevel.ALERT,
        timestamp = "5 hours ago"
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NotificationsScreenPreview() {
    AppTheme {
        NotificationsScreenContent(uiState = NotificationsScreenState(notifications = WidgetResult.Success(previewNotifications)))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
fun NotificationsScreenEmptyPreview() {
    AppTheme {
        NotificationsScreenContent(uiState = NotificationsScreenState(notifications = WidgetResult.Success(emptyList())))
    }
}
