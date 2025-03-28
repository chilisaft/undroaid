package com.chilisaft.undroaid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroaid.ui.array.ArrayScreen
import com.chilisaft.undroaid.ui.dashboard.DashboardScreen
import com.chilisaft.undroaid.ui.server.ServerScreen
import com.chilisaft.undroaid.ui.shares.SharesScreen
import com.chilisaft.undroaid.ui.virtualization.VirtualizationScreen
import kotlinx.coroutines.CoroutineScope

@Composable
fun UndroaidNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    startDestination: String = UndroaidRoutes.DASHBOARD_ROUTE,
    navActions: UndroaidNavigationActions = remember (navController) {
        UndroaidNavigationActions(navController)
    }
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = currentRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(UndroaidRoutes.ARRAY_ROUTE) {
                navActions.navigateToArray()
            }
            composable(UndroaidRoutes.DASHBOARD_ROUTE) {
                navActions.navigateToDashboard()
            }
            composable(UndroaidRoutes.SHARES_ROUTE) {
                navActions.navigateToShares()
            }
            composable(UndroaidRoutes.SERVER_ROUTE) {
                navActions.navigateToServer()
            }
            composable(UndroaidRoutes.VIRTUALIZATION_ROUTE) {
                navActions.navigateToVirtualization()
            }
            composable(UndroaidRoutes.LOGIN_ROUTE) {
                navActions.navigateToLogin()
            }
        }

    }


}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar() {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        UndroaidNavItems.forEach { navItem ->
            NavigationBarItem(
                selected = currentRoute == navItem.route,

                // navigate on click
                onClick = {
                    navController.navigate(navItem.route)
                },

                // Icon of navItem
                icon = {
                    if (currentRoute == navItem.route) {
                        Icon(imageVector = navItem.icon_selected , contentDescription = navItem.label)
                    } else {
                        Icon(imageVector = navItem.icon , contentDescription = navItem.label)
                    }
                },

                // label
                label = {
                    Text(text = navItem.label)
                },
                alwaysShowLabel = true,

//                colors = NavigationBarItemDefaults.colors(
//                    selectedIconColor = Color.White, // Icon color when selected
//                    unselectedIconColor = Color.White, // Icon color when not selected
//                    selectedTextColor = Color.White, // Label color when selected
//                    indicatorColor = Color(0xFF195334) // Highlight color for selected item
//                )
            )
        }
    }
}
