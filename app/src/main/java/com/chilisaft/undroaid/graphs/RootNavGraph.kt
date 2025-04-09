package com.chilisaft.undroaid.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroaid.ui.login.LoginScreen

object UndroaidGraph {
    const val AUTH_ROUTE = "auth"
    const val UNDROAID_ROUTE = "undroaid"
}

@Composable
fun RootNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = UndroaidGraph.AUTH_ROUTE
) {
    NavHost(
        navController = navController,
        route = "root",
        startDestination = startDestination
    ) {
        composable(UndroaidGraph.AUTH_ROUTE) {
            LoginScreen(
                onLoginSuccessful = { navController.navigate(UndroaidGraph.UNDROAID_ROUTE) }
            )
        }
        composable(route = UndroaidGraph.UNDROAID_ROUTE) {
            UndroaidNavGraph()
        }
    }
}