package com.chilisaft.undroid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.DashboardCards
import com.chilisaft.undroid.data.models.LiveUpdateSpeed
import com.chilisaft.undroid.data.models.StartTab
import com.chilisaft.undroid.data.models.TemperatureUnit
import com.chilisaft.undroid.data.models.ThemeMode
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.repository.ServerRepository
import com.chilisaft.undroid.data.repository.SettingsRepository
import com.chilisaft.undroid.utils.DebugInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsScreenState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showCoreList: Boolean = false,
    val useDynamicColor: Boolean = true,
    val startTab: StartTab = StartTab.HOME,
    val liveUpdateSpeed: LiveUpdateSpeed = LiveUpdateSpeed.FAST,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val dashboardCards: DashboardCards = DashboardCards(),
    /** Filled in once the server versions have been asked for; usable (as "unknown") before that. */
    val debugInfo: DebugInfo = DebugInfo.current(null, null)
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsScreenState())
    val uiState: StateFlow<SettingsScreenState> = _uiState.asStateFlow()

    init {
        with(settingsRepository) {
            collectInto(themeMode) { copy(themeMode = it) }
            collectInto(showCoreList) { copy(showCoreList = it) }
            collectInto(useDynamicColor) { copy(useDynamicColor = it) }
            collectInto(startTab) { copy(startTab = it) }
            collectInto(liveUpdateSpeed) { copy(liveUpdateSpeed = it) }
            collectInto(temperatureUnit) { copy(temperatureUnit = it) }
            collectInto(dashboardCards) { copy(dashboardCards = it) }
        }
        viewModelScope.launch {
            val versions = (serverRepository.getServerVersions() as? WidgetResult.Success)?.data
            _uiState.update { it.copy(debugInfo = DebugInfo.current(versions?.unraid, versions?.api)) }
        }
    }

    private fun <T> collectInto(flow: StateFlow<T>, apply: SettingsScreenState.(T) -> SettingsScreenState) {
        viewModelScope.launch { flow.collect { value -> _uiState.update { it.apply(value) } } }
    }

    fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    fun setShowCoreList(enabled: Boolean) = settingsRepository.setShowCoreList(enabled)

    fun setUseDynamicColor(enabled: Boolean) = settingsRepository.setUseDynamicColor(enabled)

    fun setStartTab(tab: StartTab) = settingsRepository.setStartTab(tab)

    fun setLiveUpdateSpeed(speed: LiveUpdateSpeed) = settingsRepository.setLiveUpdateSpeed(speed)

    fun setTemperatureUnit(unit: TemperatureUnit) = settingsRepository.setTemperatureUnit(unit)

    fun setDashboardCards(cards: DashboardCards) = settingsRepository.setDashboardCards(cards)
}
