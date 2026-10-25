package com.chilisaft.undroid.graphs

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The login/logout transitions from [RootNavGraph], against a stand-in graph with the same
 * routes (the real screens need Hilt and a server). Both previously left a stale screen on the
 * back stack - see [navigateToMainAfterLogin] / [navigateToLoginAfterLogout].
 */
@RunWith(AndroidJUnit4::class)
class RootNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    private fun launch(startDestination: String) {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(navController = navController, route = "root", startDestination = startDestination) {
                composable(UndroidGraph.AUTH_ROUTE) { Text("login") }
                composable(UndroidGraph.UNDROID_ROUTE) { Text("main") }
            }
        }
    }

    /** Screen routes on the back stack, bottom to top (the graph's own entry filtered out). */
    private fun backStackRoutes(): List<String> =
        navController.currentBackStack.value.mapNotNull { it.destination.route }.filter { it != "root" }

    @Test
    fun loginReplacesTheLoginScreenSoBackLeavesTheApp() {
        launch(UndroidGraph.AUTH_ROUTE)

        composeRule.runOnIdle { navController.navigateToMainAfterLogin() }

        composeRule.runOnIdle {
            assertThat(backStackRoutes()).containsExactly(UndroidGraph.UNDROID_ROUTE)
        }
    }

    @Test
    fun logoutAfterStartingLoggedInLeavesOnlyTheLoginScreen() {
        // Cold start with a saved session: the login route was never on the stack.
        launch(UndroidGraph.UNDROID_ROUTE)

        composeRule.runOnIdle { navController.navigateToLoginAfterLogout() }

        composeRule.runOnIdle {
            assertThat(backStackRoutes()).containsExactly(UndroidGraph.AUTH_ROUTE)
        }
    }

    @Test
    fun loginThenLogoutThenLoginNeverStacksScreens() {
        launch(UndroidGraph.AUTH_ROUTE)

        composeRule.runOnIdle { navController.navigateToMainAfterLogin() }
        composeRule.runOnIdle { navController.navigateToLoginAfterLogout() }
        composeRule.runOnIdle { navController.navigateToMainAfterLogin() }

        composeRule.runOnIdle {
            assertThat(backStackRoutes()).containsExactly(UndroidGraph.UNDROID_ROUTE)
        }
    }
}
