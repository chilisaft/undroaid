package com.chilisaft.undroid.graphs

import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.chilisaft.undroid.data.models.Login
import com.chilisaft.undroid.data.repository.LoginException
import com.chilisaft.undroid.data.repository.LoginRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    private fun startWithSavedSessionFailing(error: Throwable): AppStartupViewModel {
        every { repository.getSavedLogin() } returns Login("http://test.com", "test_token")
        coEvery { repository.login(any()) } returns Result.failure(error)
        return AppStartupViewModel(repository)
    }

    @Test
    fun `stays LoggedIn when the server can't be reached - that's not a rejected key`() {
        val viewModel = startWithSavedSessionFailing(ApolloNetworkException("Failed to connect"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedIn)
    }

    @Test
    fun `stays LoggedIn when the network failure arrives wrapped, as LoginRepository returns it`() {
        val viewModel = startWithSavedSessionFailing(LoginException("Can't reach the server", ApolloNetworkException("Failed to connect")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedIn)
    }

    @Test
    fun `stays LoggedIn on a server-side 5xx error`() {
        val viewModel = startWithSavedSessionFailing(ApolloHttpException(503, emptyList(), null, "Service Unavailable"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedIn)
    }

    @Test
    fun `resolves to LoggedOut when the server rejects the key with 401`() {
        val viewModel = startWithSavedSessionFailing(ApolloHttpException(401, emptyList(), null, "Unauthorized"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedOut)
    }

    @Test
    fun `stays LoggedIn when the check times out instead of holding the splash screen`() {
        every { repository.getSavedLogin() } returns Login("http://test.com", "test_token")
        coEvery { repository.login(any()) } coAnswers {
            delay(60_000)
            Result.success(true)
        }

        val viewModel = AppStartupViewModel(repository)
        testDispatcher.scheduler.advanceTimeBy(4_001)
        testDispatcher.scheduler.runCurrent()

        assertThat(viewModel.state.value).isEqualTo(AppStartupState.LoggedIn)
    }
}
