package com.chilisaft.undroaid.ui.settings

import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.data.repository.LoginRepository
import com.chilisaft.undroaid.data.repository.SettingsRepository
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
    private lateinit var loginRepository: LoginRepository

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
        }
        loginRepository = mockk(relaxed = true)
        viewModel = SettingsViewModel(settingsRepository, loginRepository)
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
    fun `logout clears credentials and flags the state as logged out`() {
        viewModel.logout()
        verify { loginRepository.logout() }
        assertThat(viewModel.uiState.value.isLoggedOut).isTrue()
    }
}
