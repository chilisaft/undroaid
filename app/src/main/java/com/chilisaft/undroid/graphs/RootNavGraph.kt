package com.chilisaft.undroid.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chilisaft.undroid.ui.login.LoginScreen
import kotlinx.coroutines.flow.Flow

object UndroidGraph {
    const val AUTH_ROUTE = "auth"
    const val UNDROID_ROUTE = "undroid"
}

@Composable
fun RootNavGraph(
    userMessages: Flow<String>,
    navController: NavHostController = rememberNavController(),
    startDestination: String = UndroidGraph.AUTH_ROUTE
) {
    NavHost(
        navController = navController,
        route = "root",
        startDestination = startDestination
    ) {
        composable(UndroidGraph.AUTH_ROUTE) {
            LoginScreen(onLoginSuccessful = navController::navigateToMainAfterLogin)
        }
        composable(route = UndroidGraph.UNDROID_ROUTE) {
            UndroidNavGraph(
                userMessages = userMessages,
                onLoggedOut = navController::navigateToLoginAfterLogout
            )
        }
    }
}

/**
 * Replaces the login screen with the app. Login must not stay underneath: Back would return to
 * it, and since its state still says "logged in" it would immediately navigate forward again -
 * Back could never leave the app.
 */
fun NavController.navigateToMainAfterLogin() {
    navigate(UndroidGraph.UNDROID_ROUTE) {
        popUpTo(UndroidGraph.AUTH_ROUTE) { inclusive = true }
    }
}

/**
 * Clears the entire back stack and shows the login screen. Popping up to the *root graph* rather
 * than to the login route matters: when the app started already logged in, the login route was
 * never on the stack, so `popUpTo(AUTH_ROUTE)` was silently ignored and the logged-out app (still
 * polling the server) stayed underneath, one Back press away.
 */
fun NavController.navigateToLoginAfterLogout() {
    navigate(UndroidGraph.AUTH_ROUTE) {
        popUpTo(graph.id) { inclusive = true }
    }
}