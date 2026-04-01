package com.chilisaft.undroaid.ui.dashboard

import com.chilisaft.undroaid.data.models.Owner
import com.chilisaft.undroaid.data.models.Server
import com.chilisaft.undroaid.data.repository.ServerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var repository: ServerRepository

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
    fun `initial state is loading`() {
        coEvery { repository.getServerInformation() } returns Result.success(createTestServer())
        viewModel = DashboardViewModel(repository)
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isTrue()
        assertThat(state.server).isNull()
        assertThat(state.error).isNull()
    }

    @Test
    fun `successful server information fetch`() {
        val server = createTestServer()
        coEvery { repository.getServerInformation() } returns Result.success(server)
        viewModel = DashboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.server).isEqualTo(server)
        assertThat(state.error).isNull()
    }

    @Test
    fun `failed server information fetch`() {
        val errorMessage = "Failed to fetch server information"
        coEvery { repository.getServerInformation() } returns Result.failure(Exception(errorMessage))
        viewModel = DashboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.server).isNull()
        assertThat(state.error).isEqualTo(errorMessage)
    }

    private fun createTestServer(): Server {
        return Server(
            owner = Owner(
                username = "testuser",
                url = "http://test.com",
                avatar = "avatar.png"
            ),
            guid = "1234",
            apiKey = "apikey",
            name = "Test Server",
            wanIp = "1.1.1.1",
            lanIp = "192.168.1.1",
            localUrl = "http://local.test",
            remoteUrl = "http://remote.test"
        )
    }
}