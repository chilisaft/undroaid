package com.chilisaft.undroaid.graphs

import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.data.repository.LoginRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
class AppStartupViewModelTest {

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
    fun `resolves to LoggedOut immediately when there's no saved session, without calling the network`() {
        every { repository.getSavedLogin() } returns Login(null, null)

        val viewModel = AppStartupViewModel(repository)

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedOut)
        coVerify(exactly = 0) { repository.login(any()) }
    }

    @Test
    fun `starts Loading and resolves to LoggedIn once the saved session validates successfully`() {
        val serverUrl = "http://test.com"
        val apiToken = "test_token"
        every { repository.getSavedLogin() } returns Login(serverUrl, apiToken)
        coEvery { repository.login(Login(serverUrl, apiToken)) } returns Result.success(true)

        val viewModel = AppStartupViewModel(repository)
        assertThat(viewModel.state.value).isEqualTo(AppStartupState.Loading)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedIn)
    }

    @Test
    fun `resolves to LoggedOut when the saved session fails to validate`() {
        val serverUrl = "http://test.com"
        val apiToken = "test_token"
        every { repository.getSavedLogin() } returns Login(serverUrl, apiToken)
        coEvery { repository.login(Login(serverUrl, apiToken)) } returns Result.failure(Exception("boom"))

        val viewModel = AppStartupViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedOut)
    }
}
