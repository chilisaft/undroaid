package com.chilisaft.undroaid

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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.chilisaft.undroaid.data.models.BottomNavItem

object UndroaidRoutes {
    const val ARRAY_ROUTE = "array"
    const val DASHBOARD_ROUTE = "dashboard"
    const val SHARES_ROUTE = "shares"
    const val VIRTUALIZATION_ROUTE = "virtualization"
    const val SERVER_ROUTE = "server"
}


val UndroaidNavItems = listOf(
    BottomNavItem(
        label = "Array",
        icon = Icons.Outlined.Storage,
        icon_selected = Icons.Filled.Storage,
        route = UndroaidRoutes.ARRAY_ROUTE
    ),
    BottomNavItem(
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard,
        icon_selected = Icons.Filled.Dashboard,
        route = UndroaidRoutes.DASHBOARD_ROUTE
    ),
    BottomNavItem(
        label = "Shares",
        icon = Icons.Outlined.FolderShared,
        icon_selected = Icons.Filled.FolderShared,
        route = UndroaidRoutes.SHARES_ROUTE
    ),
    BottomNavItem(
        label = "Docker",
        icon = Icons.Outlined.SmartToy,
        icon_selected = Icons.Filled.SmartToy,
        route = UndroaidRoutes.VIRTUALIZATION_ROUTE
    ),
    BottomNavItem(
        label = "Server",
        icon = Icons.Outlined.Memory,
        icon_selected = Icons.Filled.Memory,
        route = UndroaidRoutes.SERVER_ROUTE
    )
)

/**
 * Models the navigation actions in the app.
 */
class UndroaidNavigationActions(private val navController: NavHostController) {

    fun navigateToDashboard() {
        navController.navigate(UndroaidRoutes.DASHBOARD_ROUTE) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    fun navigateToArray() {
        navController.navigate(UndroaidRoutes.ARRAY_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToServer() {
        navController.navigate(UndroaidRoutes.SERVER_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToShares() {
        navController.navigate(UndroaidRoutes.SHARES_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }


    fun navigateToVirtualization() {
        navController.navigate(UndroaidRoutes.VIRTUALIZATION_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}