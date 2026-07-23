package com.chilisaft.undroaid.ui.dashboard

import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.MetricsSample
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.NotificationsRepository
import com.chilisaft.undroaid.data.repository.ServerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
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
    private lateinit var notificationsRepository: NotificationsRepository

    private val testDispatcher = StandardTestDispatcher()

    private val testArrayStatus = ArrayStatus(statusLabel = "Array Started", healthy = true, usedKb = 1024L, totalKb = 4096L)
    private val testSystemMetrics = SystemMetrics(bootTimeIso = "2024-01-01T00:00:00Z", cpuLoadPercent = 24.5, memoryLoadPercent = 58.2)

    // Buffered so `emit` from the test body can complete without needing an actively-suspended
    // collector to rendezvous with - the ViewModel's collector runs on StandardTestDispatcher
    // and only actually processes the value once advanceUntilIdle() is called afterwards.
    private val metricsPollFlow = MutableSharedFlow<MetricsSample>(extraBufferCapacity = 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getServerName() } returns WidgetResult.Success("TOWER")
            coEvery { getArrayStatus() } returns WidgetResult.Success(testArrayStatus)
            coEvery { getSystemMetrics() } returns WidgetResult.Success(testSystemMetrics)
            coEvery { getDockerContainers() } returns WidgetResult.Success(emptyList())
            every { observeSystemMetricsPoll() } returns metricsPollFlow
        }
        notificationsRepository = mockk {
            coEvery { getUnreadCount() } returns WidgetResult.Success(0)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DashboardViewModel(repository, notificationsRepository)

    @Test
    fun `initial state is loading for every widget`() {
        viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertThat(state.serverName).isNull()
        assertThat(state.arrayStatus).isEqualTo(WidgetResult.Loading)
        assertThat(state.systemMetrics).isEqualTo(WidgetResult.Loading)
        assertThat(state.containers).isEqualTo(WidgetResult.Loading)
        assertThat(state.unreadNotificationCount).isEqualTo(WidgetResult.Loading)
    }

    @Test
    fun `each widget populates independently once loaded`() {
        coEvery { notificationsRepository.getUnreadCount() } returns WidgetResult.Success(3)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.serverName).isEqualTo("TOWER")
        assertThat(state.arrayStatus).isEqualTo(WidgetResult.Success(testArrayStatus))
        assertThat(state.systemMetrics).isEqualTo(WidgetResult.Success(testSystemMetrics))
        assertThat(state.containers).isEqualTo(WidgetResult.Success(emptyList<Nothing>()))
        assertThat(state.unreadNotificationCount).isEqualTo(WidgetResult.Success(3))
    }

    @Test
    fun `a failure on one widget does not affect the others`() {
        coEvery { repository.getDockerContainers() } returns WidgetResult.Failure(permissionDenied = true, message = null)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.containers).isEqualTo(WidgetResult.Failure(permissionDenied = true, message = null))
        assertThat(state.arrayStatus).isEqualTo(WidgetResult.Success(testArrayStatus))
        assertThat(state.systemMetrics).isEqualTo(WidgetResult.Success(testSystemMetrics))
    }

    @Test
    fun `retrying a single widget only reloads that widget`() {
        coEvery { repository.getDockerContainers() } returns WidgetResult.Failure(permissionDenied = false, message = "boom")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.containers).isInstanceOf(WidgetResult.Failure::class.java)

        coEvery { repository.getDockerContainers() } returns WidgetResult.Success(emptyList())
        val arrayStatusBeforeRetry = viewModel.uiState.value.arrayStatus

        viewModel.refreshContainers()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.containers).isEqualTo(WidgetResult.Success(emptyList<Nothing>()))
        assertThat(state.arrayStatus).isEqualTo(arrayStatusBeforeRetry)
    }

    @Test
    fun `refreshUnreadCount reloads just the notification badge`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.unreadNotificationCount).isEqualTo(WidgetResult.Success(0))

        coEvery { notificationsRepository.getUnreadCount() } returns WidgetResult.Success(5)
        viewModel.refreshUnreadCount()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.unreadNotificationCount).isEqualTo(WidgetResult.Success(5))
    }

    @Test
    fun `live metric polls merge into an already-loaded systemMetrics without touching uptime`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.systemMetrics).isEqualTo(WidgetResult.Success(testSystemMetrics))

        metricsPollFlow.emit(MetricsSample(cpuLoadPercent = 77.0, memoryLoadPercent = 91.0))
        testDispatcher.scheduler.advanceUntilIdle()

        val metrics = (viewModel.uiState.value.systemMetrics as WidgetResult.Success).data
        assertThat(metrics.cpuLoadPercent).isEqualTo(77.0)
        assertThat(metrics.memoryLoadPercent).isEqualTo(91.0)
        assertThat(metrics.bootTimeIso).isEqualTo(testSystemMetrics.bootTimeIso)
    }

    @Test
    fun `live metric polls are dropped while systemMetrics has not successfully loaded`() = runBlocking {
        coEvery { repository.getSystemMetrics() } returns WidgetResult.Failure(permissionDenied = false, message = "boom")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.systemMetrics).isInstanceOf(WidgetResult.Failure::class.java)

        metricsPollFlow.emit(MetricsSample(cpuLoadPercent = 77.0, memoryLoadPercent = 91.0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.systemMetrics).isInstanceOf(WidgetResult.Failure::class.java)
    }
}
