package com.chilisaft.undroaid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.data.repository.SettingsRepository
import com.chilisaft.undroaid.graphs.RootNavGraph
import com.chilisaft.undroaid.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UndroaidActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    RootNavGraph()
                }
            }
        }
    }
}
