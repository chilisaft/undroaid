package com.chilisaft.undroaid.graphs

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroaid.ui.main.MainScreen
import com.chilisaft.undroaid.ui.dashboard.DashboardScreen
import com.chilisaft.undroaid.ui.dashboard.DashboardViewModel
import com.chilisaft.undroaid.ui.server.ServerScreen
import com.chilisaft.undroaid.ui.shares.SharesScreen
import com.chilisaft.undroaid.ui.virtualization.VirtualizationScreen

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
    data object Shares : BottomNavDestination(
        title = "Shares",
        route = "shares",
        icon = Icons.Outlined.FolderShared,
        icon_selected = Icons.Filled.FolderShared
    )
    data object Virtualization : BottomNavDestination(
        title = "Virtualization",
        route = "virtualization",
        icon = Icons.Outlined.SmartToy,
        icon_selected = Icons.Filled.SmartToy
    )
    data object Server : BottomNavDestination(
        title = "Server",
        route = "server",
        icon = Icons.Outlined.Memory,
        icon_selected = Icons.Filled.Memory
    )
}

@Composable
fun UndroaidNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = BottomNavDestination.Dashboard.route,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val bottomNavItem = listOf(
        BottomNavDestination.Dashboard,
        BottomNavDestination.Main,
        BottomNavDestination.Virtualization,
        BottomNavDestination.Shares,
        BottomNavDestination.Server
    )
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                //containerColor = primary
            ) {
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
                        label = { androidx.compose.material3.Text(screen.title) },
                        onClick = {
                            selectedIndex = index
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
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(BottomNavDestination.Main.route) {
                MainScreen()
            }
            composable(BottomNavDestination.Shares.route) {
                SharesScreen()
            }
            composable(BottomNavDestination.Server.route) {
                ServerScreen()
            }
            composable(BottomNavDestination.Virtualization.route) {
                VirtualizationScreen()
            }
        }
    }
}
