package com.chilisaft.undroid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.Modifier
import com.chilisaft.undroid.data.models.ThemeMode
import com.chilisaft.undroid.data.repository.SettingsRepository
import com.chilisaft.undroid.graphs.AppStartupState
import com.chilisaft.undroid.graphs.AppStartupViewModel
import com.chilisaft.undroid.graphs.RootNavGraph
import com.chilisaft.undroid.graphs.UndroidGraph
import com.chilisaft.undroid.ui.UserMessages
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.LocalTemperatureUnit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UndroidActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userMessages: UserMessages

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
            val temperatureUnit by settingsRepository.temperatureUnit.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // The plain enableEdgeToEdge() call above picks status/nav bar icon colors from the
            // *system* dark mode - with the app forced to Light on a dark-mode phone (or vice
            // versa) that left white clock/battery icons on a light background. Re-apply it
            // whenever the app's own resolved theme changes.
            DisposableEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }
            AppTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
                CompositionLocalProvider(LocalTemperatureUnit provides temperatureUnit) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val startupState by appStartupViewModel.state.collectAsState()
                        // Nothing is drawn while Loading - the splash screen is still covering the
                        // window at that point, so there's no frame for a placeholder to matter.
                        if (startupState != AppStartupState.Loading) {
                            RootNavGraph(
                                userMessages = userMessages.messages,
                                // Read once - changing it in Settings applies from the next app start.
                                startTabRoute = remember { settingsRepository.startTab.value.route },
                                startDestination = if (startupState == AppStartupState.LoggedIn) {
                                    UndroidGraph.UNDROID_ROUTE
                                } else {
                                    UndroidGraph.AUTH_ROUTE
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
