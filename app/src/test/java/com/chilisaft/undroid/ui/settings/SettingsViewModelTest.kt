package com.chilisaft.undroid.ui.settings

import io.mockk.coEvery
import com.chilisaft.undroid.data.repository.ServerRepository
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.ServerVersions
import com.chilisaft.undroid.data.models.DashboardCards
import com.chilisaft.undroid.data.models.TemperatureUnit
import com.chilisaft.undroid.data.models.LiveUpdateSpeed
import com.chilisaft.undroid.data.models.StartTab
import com.chilisaft.undroid.data.models.ThemeMode
import com.chilisaft.undroid.data.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var serverRepository: ServerRepository

    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val showCoreListFlow = MutableStateFlow(false)
    private val useDynamicColorFlow = MutableStateFlow(true)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true) {
            every { themeMode } returns themeModeFlow
            every { showCoreList } returns showCoreListFlow
            every { useDynamicColor } returns useDynamicColorFlow
            every { startTab } returns MutableStateFlow(StartTab.DOCKER)
            every { liveUpdateSpeed } returns MutableStateFlow(LiveUpdateSpeed.SLOW)
            every { temperatureUnit } returns MutableStateFlow(TemperatureUnit.FAHRENHEIT)
            every { dashboardCards } returns MutableStateFlow(DashboardCards(vms = false))
        }
        serverRepository = mockk {
            coEvery { getServerVersions() } returns WidgetResult.Success(ServerVersions("7.3.4", "4.37.9"))
        }
        viewModel = SettingsViewModel(settingsRepository, serverRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state reflects the repository's current preferences`() {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertThat(state.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(state.showCoreList).isFalse()
        assertThat(state.useDynamicColor).isTrue()
    }

    @Test
    fun `state updates live when the repository's flows change`() {
        testDispatcher.scheduler.advanceUntilIdle()
        themeModeFlow.value = ThemeMode.DARK
        showCoreListFlow.value = true
        useDynamicColorFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(state.showCoreList).isTrue()
        assertThat(state.useDynamicColor).isFalse()
    }

    @Test
    fun `setThemeMode delegates to the repository`() {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        verify { settingsRepository.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `setShowCoreList delegates to the repository`() {
        viewModel.setShowCoreList(true)
        verify { settingsRepository.setShowCoreList(true) }
    }

    @Test
    fun `setUseDynamicColor delegates to the repository`() {
        viewModel.setUseDynamicColor(false)
        verify { settingsRepository.setUseDynamicColor(false) }
    }


    @Test
    fun `state carries the new preferences and the server versions for the debug info`() {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertThat(state.startTab).isEqualTo(StartTab.DOCKER)
        assertThat(state.liveUpdateSpeed).isEqualTo(LiveUpdateSpeed.SLOW)
        assertThat(state.temperatureUnit).isEqualTo(TemperatureUnit.FAHRENHEIT)
        assertThat(state.dashboardCards).isEqualTo(DashboardCards(vms = false))
        assertThat(state.debugInfo.unraidVersion).isEqualTo("7.3.4")
        assertThat(state.debugInfo.apiVersion).isEqualTo("4.37.9")
    }
}
