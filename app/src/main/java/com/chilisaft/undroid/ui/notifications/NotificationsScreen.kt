package com.chilisaft.undroid.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroid.data.models.Notification
import com.chilisaft.undroid.data.models.NotificationLevel
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.ConfirmActionDialog
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.color
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.spacing

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationsChanged: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // The dashboard's unread badge doesn't observe this screen, so nudge it to refresh whenever
    // this screen leaves composition - via DisposableEffect rather than piggybacking on the
    // back-arrow's onClick, since Compose Navigation's system back gesture/button pops the
    // backstack directly and never calls that lambda, silently skipping the refresh.
    DisposableEffect(Unit) {
        onDispose { onNotificationsChanged() }
    }
    NotificationsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onDismiss = viewModel::dismiss,
        onDismissAll = viewModel::dismissAll,
        onSelectTab = viewModel::selectTab,
        onRestore = viewModel::restore,
        onDelete = viewModel::delete,
        onDeleteAll = viewModel::deleteAllArchived
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreenContent(
    uiState: NotificationsScreenState,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismiss: (String) -> Unit = {},
    onDismissAll: () -> Unit = {},
    onSelectTab: (NotificationsTab) -> Unit = {},
    onRestore: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onDeleteAll: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var showDismissAllDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Notification?>(null) }
    val isArchiveTab = uiState.tab == NotificationsTab.ARCHIVED
    val result = if (isArchiveTab) uiState.archived else uiState.notifications
    val notificationCount = (result as? WidgetResult.Success)?.data?.size ?: 0
    val isBulkRunning = if (isArchiveTab) uiState.isDeletingAll else uiState.isDismissingAll
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "Notifications",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { if (isArchiveTab) showDeleteAllDialog = true else showDismissAllDialog = true },
                        enabled = notificationCount > 0 && !isBulkRunning,
                        colors = if (isArchiveTab) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors()
                    ) {
                        if (isBulkRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isArchiveTab) "Delete All" else "Dismiss All")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = uiState.tab.ordinal) {
                Tab(selected = !isArchiveTab, onClick = { onSelectTab(NotificationsTab.NEW) }, text = { Text("New") })
                Tab(selected = isArchiveTab, onClick = { onSelectTab(NotificationsTab.ARCHIVED) }, text = { Text("Archived") })
            }
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.medium)
            ) {
                WidgetSection(result = result, onRetry = onRetry, minHeight = 200.dp) { notifications ->
                    if (notifications.isEmpty()) {
                        EmptyState(if (isArchiveTab) "No archived notifications" else "No new notifications")
                    } else {
                        ScreenList {
                            items(notifications, key = { it.id }) { notification ->
                                NotificationRow(notification = notification, isBusy = notification.id in uiState.dismissingIds) {
                                    if (isArchiveTab) {
                                        IconButton(onClick = { onRestore(notification.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.Unarchive, contentDescription = "Restore", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { deleteTarget = notification }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    } else {
                                        IconButton(onClick = { onDismiss(notification.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDismissAllDialog) {
        ConfirmActionDialog(
            title = "Dismiss all notifications?",
            text = if (notificationCount == 1) "The 1 unread notification will be archived." else "All $notificationCount unread notifications will be archived.",
            confirmLabel = "Dismiss All",
            onConfirm = onDismissAll,
            onDismiss = { showDismissAllDialog = false }
        )
    }

    if (showDeleteAllDialog) {
        ConfirmActionDialog(
            title = "Delete all archived notifications?",
            text = "Every archived notification is deleted from the server. This can't be undone.",
            confirmLabel = "Delete All",
            onConfirm = onDeleteAll,
            onDismiss = { showDeleteAllDialog = false },
            destructive = true
        )
    }

    deleteTarget?.let { target ->
        ConfirmActionDialog(
            title = "Delete this notification?",
            text = "\"${target.subject.ifBlank { target.title }}\" is deleted from the server. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(target.id) },
            onDismiss = { deleteTarget = null },
            destructive = true
        )
    }
}

@Composable
private fun NotificationRow(notification: Notification, isBusy: Boolean, actions: @Composable RowScope.() -> Unit) {
    val spacing = MaterialTheme.spacing
    val (icon, color) = when (notification.level) {
        NotificationLevel.ALERT -> Icons.Filled.Warning to StatusTone.Error.color
        NotificationLevel.WARNING -> Icons.Filled.Warning to StatusTone.Warning.color
        NotificationLevel.INFO -> Icons.Filled.TaskAlt to MaterialTheme.colorScheme.primary
    }
    AppCard {
        Row(modifier = Modifier.padding(spacing.medium), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                // subject and description sometimes duplicate the title or each other depending on
                // how the source plugin populated them - only show what actually adds information.
                if (notification.subject.isNotBlank() && notification.subject != notification.title) {
                    Text(notification.subject, style = MaterialTheme.typography.bodyMedium)
                }
                if (notification.description.isNotBlank() && notification.description != notification.subject) {
                    Text(notification.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                }
                if (notification.timestamp != null) {
                    Spacer(Modifier.height(spacing.small))
                    Text(notification.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.width(spacing.small))
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), content = actions)
            }
        }
    }
}

private val previewNotifications = listOf(
    Notification(
        id = "1",
        title = "Parity Check Complete",
        subject = "Parity Check Complete",
        description = "System finished scheduled parity check. Duration: 14h 22m. Errors found: 0.",
        level = NotificationLevel.INFO,
        timestamp = "2 hours ago"
    ),
    Notification(
        id = "2",
        title = "Disk 4 Temperature Alert",
        subject = "WDC_WD120EDAZ has reached 46°C",
        description = "Cooling threshold exceeded.",
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

@Preview(name = "Archived", showBackground = true)
@Composable
fun NotificationsScreenArchivedPreview() {
    AppTheme {
        NotificationsScreenContent(
            uiState = NotificationsScreenState(archived = WidgetResult.Success(previewNotifications), tab = NotificationsTab.ARCHIVED)
        )
    }
}
