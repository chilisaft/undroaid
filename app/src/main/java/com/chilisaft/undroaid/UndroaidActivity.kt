package com.chilisaft.undroaid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.Modifier
import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.data.repository.SettingsRepository
import com.chilisaft.undroaid.graphs.AppStartupState
import com.chilisaft.undroaid.graphs.AppStartupViewModel
import com.chilisaft.undroaid.graphs.RootNavGraph
import com.chilisaft.undroaid.graphs.UndroaidGraph
import com.chilisaft.undroaid.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UndroaidActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appStartupViewModel: AppStartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Holds the splash on screen only for the routing decision itself (instant when there's
        // no saved session, one network round-trip when there is) - never for the login screen to
        // flash into view first. See AppStartupViewModel's doc comment for the full reasoning.
        splashScreen.setKeepOnScreenCondition { appStartupViewModel.state.value is AppStartupState.Loading }

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState()
            val useDynamicColor by settingsRepository.useDynamicColor.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            AppTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val startupState by appStartupViewModel.state.collectAsState()
                    // Nothing is drawn while Loading - the splash screen is still covering the
                    // window at that point, so there's no frame for a placeholder to matter.
                    if (startupState != AppStartupState.Loading) {
                        RootNavGraph(
                            startDestination = if (startupState == AppStartupState.LoggedIn) {
                                UndroaidGraph.UNDROAID_ROUTE
                            } else {
                                UndroaidGraph.AUTH_ROUTE
                            }
                        )
                    }
                }
            }
        }
    }
}
