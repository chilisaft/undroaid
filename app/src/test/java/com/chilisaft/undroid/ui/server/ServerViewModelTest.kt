package com.chilisaft.undroid.ui.server

import com.chilisaft.undroid.data.models.ConnectInfo
import com.chilisaft.undroid.data.models.ConnectStatus
import com.chilisaft.undroid.data.models.ServerIdentity
import com.chilisaft.undroid.data.models.ServerSystem
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.repository.ServerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
class ServerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ServerRepository

    private val identity = ServerIdentity(name = "TOWER", comment = null, timeZone = null, licence = "Pro")
    private val system = ServerSystem(
        unraidVersion = "7.3.3", apiVersion = null, kernel = null, bootTimeIso = null, cpu = null, cpuCores = null,
        cpuThreads = null, memoryTotalBytes = null, memoryType = null, motherboard = null, network = null
    )
    private val connect = ConnectInfo(ConnectStatus.NEVER_CONNECTED, null, null, null, null, null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getServerIdentity() } returns WidgetResult.Success(identity)
            coEvery { getServerSystem() } returns WidgetResult.Success(system)
            coEvery { getConnectInfo() } returns WidgetResult.Success(connect)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a missing SERVERS permission only affects the Connect section`() {
        coEvery { repository.getConnectInfo() } returns WidgetResult.Failure(permissionDenied = true, message = null)

        val viewModel = ServerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.identity).isEqualTo(WidgetResult.Success(identity))
        assertThat(state.system).isEqualTo(WidgetResult.Success(system))
        assertThat(state.connect).isEqualTo(WidgetResult.Failure(permissionDenied = true, message = null))
    }

    @Test
    fun `retrying one section only reloads that section`() {
        val viewModel = ServerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshConnect()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { repository.getConnectInfo() }
        coVerify(exactly = 1) { repository.getServerIdentity() }
        coVerify(exactly = 1) { repository.getServerSystem() }
    }

    @Test
    fun `a full refresh keeps sections on screen and reports isRefreshing meanwhile`() {
        val viewModel = ServerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { repository.getServerIdentity() } coAnswers {
            delay(1_000)
            WidgetResult.Success(identity.copy(name = "TOWER2"))
        }

        viewModel.refresh()
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.isRefreshing).isTrue()
        assertThat(viewModel.uiState.value.identity).isEqualTo(WidgetResult.Success(identity))

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
        assertThat(viewModel.uiState.value.identity).isEqualTo(WidgetResult.Success(identity.copy(name = "TOWER2")))
    }
}
