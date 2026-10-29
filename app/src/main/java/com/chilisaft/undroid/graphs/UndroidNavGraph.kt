package com.chilisaft.undroid.graphs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.ActionSheet
import com.chilisaft.undroid.ui.components.ActionSheetItem
import com.chilisaft.undroid.ui.components.TabBarActions
import com.chilisaft.undroid.ui.main.MainScreen
import com.chilisaft.undroid.ui.dashboard.DashboardScreen
import com.chilisaft.undroid.ui.dashboard.DashboardViewModel
import com.chilisaft.undroid.ui.devices.DevicesScreen
import com.chilisaft.undroid.ui.docker.DockerScreen
import com.chilisaft.undroid.ui.notifications.NotificationsScreen
import com.chilisaft.undroid.ui.server.ServerScreen
import com.chilisaft.undroid.ui.settings.SettingsScreen
import com.chilisaft.undroid.ui.shares.SharesScreen
import com.chilisaft.undroid.ui.usermenu.UserMenuSheet
import com.chilisaft.undroid.ui.usermenu.UserMenuViewModel
import com.chilisaft.undroid.ui.vms.VmsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val SETTINGS_ROUTE = "settings"
private const val NOTIFICATIONS_ROUTE = "notifications"
private const val SHARES_ROUTE = "shares"
private const val SERVER_ROUTE = "server"
private const val DEVICES_ROUTE = "devices"

/**
 * Destinations shown as tabs in the bottom bar. Shares, Server and Devices are lower-frequency
 * screens, tucked one tap further behind the "More" sheet instead of taking a permanent tab slot.
 * Settings is reached from the account sheet.
 */
sealed class BottomNavDestination(val title: String, val route: String, val icon: ImageVector, val icon_selected: ImageVector) {
    data object Dashboard : BottomNavDestination(
        title = "Home",
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
    data object Docker : BottomNavDestination(
        title = "Docker",
        route = "docker",
        icon = Icons.Outlined.Layers,
        icon_selected = Icons.Filled.Layers
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
fun UndroidNavGraph(
    userMessages: Flow<String> = emptyFlow(),
    navController: NavHostController = rememberNavController(),
    startDestination: String = BottomNavDestination.Dashboard.route,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val bottomNavItem = listOf(
        BottomNavDestination.Dashboard,
        BottomNavDestination.Main,
        BottomNavDestination.Docker,
        BottomNavDestination.Vms
    )
    // Derived from the back stack (rather than separate local state) so the highlighted tab
    // stays correct when navigation happens outside the bottom bar, e.g. Dashboard's
    // "Show all" docker button jumping straight to the Docker tab.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedIndex = bottomNavItem.indexOfFirst { it.route == currentRoute }

    var showMoreSheet by remember { mutableStateOf(false) }
    var showUserSheet by remember { mutableStateOf(false) }

    // The app's one SnackbarHost - see UserMessages. Lives on this outer Scaffold (not each
    // screen's) so it sits above the bottom bar and survives navigating between screens.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(userMessages) {
        userMessages.collect { snackbarHostState.showSnackbar(it, withDismissAction = true) }
    }

    // The same bell + account pair on every tab. The unread count lives on DashboardViewModel
    // (the one instance hoisted to this graph), refreshed whenever Notifications is left.
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val unreadCount = (dashboardState.unreadNotificationCount as? WidgetResult.Success)?.data ?: 0
    val tabActions: @Composable RowScope.() -> Unit = {
        TabBarActions(
            unreadNotificationCount = unreadCount,
            onNotificationsClick = { navController.navigate(NOTIFICATIONS_ROUTE) },
            onUserClick = { showUserSheet = true }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            // With the bottom bar hidden (drill-in screens) nothing else keeps the snackbar clear
            // of the gesture/nav bar, since this Scaffold's own insets are zeroed.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = if (selectedIndex >= 0) Modifier else Modifier.navigationBarsPadding()
            )
        },
        bottomBar = {
            // Only shown on the actual tab destinations - Settings/Notifications/Shares/Server/Devices
            // are drill-in screens reached via an icon or a sheet, with their own back
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
                .padding(innerPadding)
        ) {
            composable(BottomNavDestination.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    topBarActions = tabActions,
                    onShowAllContainers = {
                        navController.navigate(BottomNavDestination.Docker.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onShowAllVms = {
                        navController.navigate(BottomNavDestination.Vms.route) {
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
                MainScreen(topBarActions = tabActions)
            }
            composable(BottomNavDestination.Docker.route) {
                DockerScreen(topBarActions = tabActions)
            }
            composable(BottomNavDestination.Vms.route) {
                VmsScreen(topBarActions = tabActions)
            }
            composable(SHARES_ROUTE) {
                SharesScreen(onBack = { navController.popBackStack() })
            }
            composable(SERVER_ROUTE) {
                ServerScreen(onBack = { navController.popBackStack() })
            }
            composable(DEVICES_ROUTE) {
                DevicesScreen(onBack = { navController.popBackStack() })
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(onBack = { navController.popBackStack() })
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
        ActionSheet(onDismissRequest = { showMoreSheet = false }) {
            ActionSheetItem(Icons.Filled.FolderShared, "Shares", {
                showMoreSheet = false
                navController.navigate(SHARES_ROUTE)
            })
            ActionSheetItem(Icons.Filled.Dns, "Server", {
                showMoreSheet = false
                navController.navigate(SERVER_ROUTE)
            })
            ActionSheetItem(Icons.Filled.DeveloperBoard, "Devices", {
                showMoreSheet = false
                navController.navigate(DEVICES_ROUTE)
            })
        }
    }

    if (showUserSheet) {
        val userMenuViewModel: UserMenuViewModel = hiltViewModel()
        val userMenuState by userMenuViewModel.uiState.collectAsState()

        LaunchedEffect(userMenuState.isLoggedOut) {
            if (userMenuState.isLoggedOut) {
                showUserSheet = false
                onLoggedOut()
            }
        }

        UserMenuSheet(
            uiState = userMenuState,
            onDismiss = { showUserSheet = false },
            onSettingsClick = {
                showUserSheet = false
                navController.navigate(SETTINGS_ROUTE)
            },
            onLogoutConfirmed = userMenuViewModel::logout
        )
    }
}
