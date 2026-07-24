package com.chilisaft.undroaid.ui.usermenu

import com.chilisaft.undroaid.BuildConfig
import com.chilisaft.undroaid.data.models.ApiKeyInfo
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.LoginRepository
import com.chilisaft.undroaid.data.repository.ServerRepository
import com.chilisaft.undroaid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class UserMenuViewModelTest {

    private lateinit var viewModel: UserMenuViewModel
    private lateinit var serverRepository: ServerRepository
    private lateinit var loginRepository: LoginRepository
    private lateinit var storage: Storage

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        serverRepository = mockk {
            coEvery { getServerVersion() } returns WidgetResult.Success("6.12.10")
            coEvery { getApiKeyInfo() } returns WidgetResult.Success(ApiKeyInfo(name = "undroaid-app", roles = listOf("Admin")))
        }
        loginRepository = mockk(relaxed = true)
        storage = mockk {
            every { serverUrl } returns "https://tower.local:443"
        }
        viewModel = UserMenuViewModel(serverRepository, loginRepository, storage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state exposes the server url, app version, fetched server version, and api key info`() {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.serverUrl).isEqualTo("https://tower.local:443")
        assertThat(state.appVersion).isEqualTo(BuildConfig.VERSION_NAME)
        assertThat(state.serverVersion).isEqualTo(WidgetResult.Success("6.12.10"))
        assertThat(state.apiKeyInfo).isEqualTo(WidgetResult.Success(ApiKeyInfo(name = "undroaid-app", roles = listOf("Admin"))))
    }

    @Test
    fun `refreshServerVersion re-fetches the server version`() {
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { serverRepository.getServerVersion() } returns WidgetResult.Success("6.13.0")

        viewModel.refreshServerVersion()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.serverVersion).isEqualTo(WidgetResult.Success("6.13.0"))
    }

    @Test
    fun `refreshApiKeyInfo re-fetches the api key info`() {
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { serverRepository.getApiKeyInfo() } returns WidgetResult.Success(ApiKeyInfo(name = "viewer-key", roles = listOf("Viewer")))

        viewModel.refreshApiKeyInfo()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.apiKeyInfo).isEqualTo(WidgetResult.Success(ApiKeyInfo(name = "viewer-key", roles = listOf("Viewer"))))
    }

    @Test
    fun `logout clears credentials and flags the state as logged out`() {
        viewModel.logout()

        verify { loginRepository.logout() }
        assertThat(viewModel.uiState.value.isLoggedOut).isTrue()
    }
}
