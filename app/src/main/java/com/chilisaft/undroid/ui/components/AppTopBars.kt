package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** Top bar for a bottom-nav tab: icon + title, and [actions] (normally [TabBarActions]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabTopBar(title: String, icon: ImageVector, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { ScreenTitle(title, icon) }, actions = actions)
}

/** Top bar for a drill-in screen (Settings, Notifications, ...): back arrow + title in the same style as [TabTopBar]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { ScreenTitle(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions
    )
}

/**
 * The notification bell (with unread badge) and account icon shown on every tab. Built once in
 * the nav graph and handed to each tab as its top bar actions, so every tab gets the same pair.
 */
@Composable
fun TabBarActions(unreadNotificationCount: Int, onNotificationsClick: () -> Unit, onUserClick: () -> Unit) {
    IconButton(onClick = onNotificationsClick) {
        BadgedBox(badge = {
            if (unreadNotificationCount > 0) {
                Badge { Text(if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString()) }
            }
        }) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
        }
    }
    IconButton(onClick = onUserClick) {
        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
    }
}
