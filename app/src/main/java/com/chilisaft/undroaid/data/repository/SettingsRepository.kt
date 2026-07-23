package com.chilisaft.undroaid.data.repository

import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.utils.Storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds app preferences in memory so changes made in Settings apply immediately - the theme
 * in particular needs to reach [AppTheme][com.chilisaft.undroaid.ui.theme.AppTheme] at the
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
}
