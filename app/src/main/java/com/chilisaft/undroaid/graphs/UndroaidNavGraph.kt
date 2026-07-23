package com.chilisaft.undroaid.graphs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroaid.ui.main.MainScreen
import com.chilisaft.undroaid.ui.dashboard.DashboardScreen
import com.chilisaft.undroaid.ui.dashboard.DashboardViewModel
import com.chilisaft.undroaid.ui.notifications.NotificationsScreen
import com.chilisaft.undroaid.ui.server.ServerScreen
import com.chilisaft.undroaid.ui.settings.SettingsScreen
import com.chilisaft.undroaid.ui.shares.SharesScreen
import com.chilisaft.undroaid.ui.theme.spacing
import com.chilisaft.undroaid.ui.virtualization.VirtualizationScreen
import com.chilisaft.undroaid.ui.vms.VmsScreen

private const val SETTINGS_ROUTE = "settings"
private const val NOTIFICATIONS_ROUTE = "notifications"
private const val SHARES_ROUTE = "shares"
private const val SERVER_ROUTE = "server"

/**
 * Destinations shown as tabs in the bottom bar. Shares, Server, and Settings aren't here -
 * they're lower frequency screens, tucked one tap further behind the "More" sheet instead of
 * taking a permanent tab slot.
 */
sealed class BottomNavDestination(val title: String, val route: String, val icon: ImageVector, val icon_selected: ImageVector) {
    data object Dashboard : BottomNavDestination(
        title = "Dashboard",
        route = "dashboard",
        icon = Icons.Outlined.Dashboard,
        icon_selected = Icons.Filled.Dashboard
    )
    data object Main : BottomNavDestination(
        title = "Main",
        route = "main",
        icon = Icons.Outlined.Storage,
        icon_selected = Icons.Filled.Storage
    )
    data object Virtualization : BottomNavDestination(
        title = "Apps",
        route = "virtualization",
        icon = Icons.Outlined.SmartToy,
        icon_selected = Icons.Filled.SmartToy
    )
    data object Vms : BottomNavDestination(
        title = "VMs",
        route = "vms",
        icon = Icons.Outlined.Computer,
        icon_selected = Icons.Filled.Computer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UndroaidNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = BottomNavDestination.Dashboard.route,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val bottomNavItem = listOf(
        BottomNavDestination.Dashboard,
        BottomNavDestination.Main,
        BottomNavDestination.Virtualization,
        BottomNavDestination.Vms
    )
    // Derived from the back stack (rather than separate local state) so the highlighted tab
    // stays correct when navigation happens outside the bottom bar, e.g. Dashboard's
    // "Show all" docker button jumping straight to the Virtualization tab.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedIndex = bottomNavItem.indexOfFirst { it.route == currentRoute }

    var showMoreSheet by remember { mutableStateOf(false) }
    val moreSheetState = rememberModalBottomSheetState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Only shown on the actual tab destinations - Settings/Notifications/Shares/Server
            // are drill-in screens reached via an icon or the More sheet, with their own back
            // button, not tabs. Showing the bar there had nothing correct to highlight (it
            // used to fall back to Dashboard) since none of those routes are in bottomNavItem.
            if (selectedIndex >= 0) {
                NavigationBar {
                    bottomNavItem.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = {
                                if (index == selectedIndex) {
                                    Icon(imageVector = screen.icon_selected , contentDescription = screen.title)
                                } else {
                                    Icon(imageVector = screen.icon , contentDescription = screen.title)
                                }
                            },
                            selected = index == selectedIndex,
                            label = { Text(screen.title) },
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }

                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Filled.MoreHoriz, contentDescription = "More") },
                        selected = false,
                        label = { Text("More") },
                        onClick = { showMoreSheet = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                //.background(color = RickPrimary)
                .padding(innerPadding)
        ) {
            composable(BottomNavDestination.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNotificationsClick = { navController.navigate(NOTIFICATIONS_ROUTE) },
                    onShowAllContainers = {
                        navController.navigate(BottomNavDestination.Virtualization.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavDestination.Main.route) {
                MainScreen()
            }
            composable(BottomNavDestination.Virtualization.route) {
                VirtualizationScreen()
            }
            composable(BottomNavDestination.Vms.route) {
                VmsScreen()
            }
            composable(SHARES_ROUTE) {
                SharesScreen(onBack = { navController.popBackStack() })
            }
            composable(SERVER_ROUTE) {
                ServerScreen(onBack = { navController.popBackStack() })
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLoggedOut = onLoggedOut
                )
            }
            composable(NOTIFICATIONS_ROUTE) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onNotificationsChanged = dashboardViewModel::refreshUnreadCount
                )
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = moreSheetState
        ) {
            MoreMenuContent(
                onSharesClick = {
                    showMoreSheet = false
                    navController.navigate(SHARES_ROUTE)
                },
                onServerClick = {
                    showMoreSheet = false
                    navController.navigate(SERVER_ROUTE)
                },
                onSettingsClick = {
                    showMoreSheet = false
                    navController.navigate(SETTINGS_ROUTE)
                }
            )
        }
    }
}

@Composable
private fun MoreMenuContent(onSharesClick: () -> Unit, onServerClick: () -> Unit, onSettingsClick: () -> Unit) {
    MoreMenuItem(icon = Icons.Filled.FolderShared, label = "Shares", onClick = onSharesClick)
    MoreMenuItem(icon = Icons.Filled.Memory, label = "Server", onClick = onServerClick)
    MoreMenuItem(icon = Icons.Filled.Settings, label = "Settings", onClick = onSettingsClick)
}

@Composable
private fun MoreMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(spacing.medium))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
