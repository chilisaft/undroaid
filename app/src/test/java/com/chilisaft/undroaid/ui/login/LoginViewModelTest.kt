package com.chilisaft.undroaid.ui.login

import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.data.repository.LoginRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel
    private lateinit var repository: LoginRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        every { repository.getSavedLogin() } returns Login(null, null)
        viewModel = LoginViewModel(repository)
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.serverUrl).isEmpty()
        assertThat(state.apiToken).isEmpty()
        assertThat(state.error).isNull()
        assertThat(state.isLoggedIn).isFalse()
    }

    @Test
    fun `login successful`() {
        val serverUrl = "http://test.com"
        val apiToken = "test_token"
        every { repository.getSavedLogin() } returns Login(null, null)
        coEvery { repository.login(Login(serverUrl, apiToken)) } returns Result.success(true)
        viewModel = LoginViewModel(repository)

        viewModel.onServerUrlChange(serverUrl)
        viewModel.onApiTokenChange(apiToken)
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `login failed`() {
        val serverUrl = "http://test.com"
        val apiToken = "test_token"
        val errorMessage = "Invalid credentials"
        every { repository.getSavedLogin() } returns Login(null, null)
        coEvery { repository.login(Login(serverUrl, apiToken)) } returns Result.failure(Exception(errorMessage))
        viewModel = LoginViewModel(repository)

        viewModel.onServerUrlChange(serverUrl)
        viewModel.onApiTokenChange(apiToken)
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo(errorMessage)
    }

    @Test
    fun `auto login with saved credentials`() {
        val serverUrl = "http://test.com"
        val apiToken = "test_token"
        every { repository.getSavedLogin() } returns Login(serverUrl, apiToken)
        coEvery { repository.login(Login(serverUrl, apiToken)) } returns Result.success(true)

        viewModel = LoginViewModel(repository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isTrue()
    }
}