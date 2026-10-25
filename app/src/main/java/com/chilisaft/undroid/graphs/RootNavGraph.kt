package com.chilisaft.undroid.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroid.ui.login.LoginScreen

object UndroidGraph {
    const val AUTH_ROUTE = "auth"
    const val UNDROID_ROUTE = "undroid"
}

@Composable
fun RootNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = UndroidGraph.AUTH_ROUTE
) {
    NavHost(
        navController = navController,
        route = "root",
        startDestination = startDestination
    ) {
        composable(UndroidGraph.AUTH_ROUTE) {
            LoginScreen(
                onLoginSuccessful = { navController.navigate(UndroidGraph.UNDROID_ROUTE) }
            )
        }
        composable(route = UndroidGraph.UNDROID_ROUTE) {
            UndroidNavGraph(
                onLoggedOut = {
                    navController.navigate(UndroidGraph.AUTH_ROUTE) {
                        popUpTo(UndroidGraph.AUTH_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}