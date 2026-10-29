package com.chilisaft.undroid.data.repository

import com.chilisaft.undroid.data.models.DashboardCards
import com.chilisaft.undroid.data.models.LiveUpdateSpeed
import com.chilisaft.undroid.data.models.StartTab
import com.chilisaft.undroid.data.models.TemperatureUnit
import com.chilisaft.undroid.data.models.ThemeMode
import com.chilisaft.undroid.utils.Storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds app preferences in memory so changes made in Settings apply immediately - the theme
 * in particular needs to reach [AppTheme][com.chilisaft.undroid.ui.theme.AppTheme] at the
 * app root without a restart - while [Storage] keeps them persisted across launches.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val storage: Storage
) {
    private val _themeMode = MutableStateFlow(storage.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showCoreList = MutableStateFlow(storage.showCoreList)
    val showCoreList: StateFlow<Boolean> = _showCoreList.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(storage.useDynamicColor)
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _startTab = MutableStateFlow(storage.startTab)
    val startTab: StateFlow<StartTab> = _startTab.asStateFlow()

    private val _liveUpdateSpeed = MutableStateFlow(storage.liveUpdateSpeed)
    val liveUpdateSpeed: StateFlow<LiveUpdateSpeed> = _liveUpdateSpeed.asStateFlow()

    private val _temperatureUnit = MutableStateFlow(storage.temperatureUnit)
    val temperatureUnit: StateFlow<TemperatureUnit> = _temperatureUnit.asStateFlow()

    private val _dashboardCards = MutableStateFlow(storage.dashboardCards)
    val dashboardCards: StateFlow<DashboardCards> = _dashboardCards.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        storage.themeMode = mode
        _themeMode.value = mode
    }

    fun setShowCoreList(enabled: Boolean) {
        storage.showCoreList = enabled
        _showCoreList.value = enabled
    }

    fun setUseDynamicColor(enabled: Boolean) {
        storage.useDynamicColor = enabled
        _useDynamicColor.value = enabled
    }

    fun setStartTab(tab: StartTab) {
        storage.startTab = tab
        _startTab.value = tab
    }

    fun setLiveUpdateSpeed(speed: LiveUpdateSpeed) {
        storage.liveUpdateSpeed = speed
        _liveUpdateSpeed.value = speed
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        storage.temperatureUnit = unit
        _temperatureUnit.value = unit
    }

    fun setDashboardCards(cards: DashboardCards) {
        storage.dashboardCards = cards
        _dashboardCards.value = cards
    }
}
