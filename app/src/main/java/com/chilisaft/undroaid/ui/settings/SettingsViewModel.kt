package com.chilisaft.undroaid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.data.repository.LoginRepository
import com.chilisaft.undroaid.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsScreenState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showCoreList: Boolean = false,
    val useDynamicColor: Boolean = true,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsScreenState())
    val uiState: StateFlow<SettingsScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.themeMode,
                settingsRepository.showCoreList,
                settingsRepository.useDynamicColor
            ) { theme, showCoreList, useDynamicColor -> Triple(theme, showCoreList, useDynamicColor) }
                .collect { (theme, showCoreList, useDynamicColor) ->
                    _uiState.update {
                        it.copy(themeMode = theme, showCoreList = showCoreList, useDynamicColor = useDynamicColor)
                    }
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setShowCoreList(enabled: Boolean) {
        settingsRepository.setShowCoreList(enabled)
    }

    fun setUseDynamicColor(enabled: Boolean) {
        settingsRepository.setUseDynamicColor(enabled)
    }

    fun logout() {
        loginRepository.logout()
        _uiState.update { it.copy(isLoggedOut = true) }
    }
}
