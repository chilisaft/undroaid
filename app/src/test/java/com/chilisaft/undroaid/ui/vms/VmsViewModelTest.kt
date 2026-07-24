package com.chilisaft.undroaid.ui.vms

import com.chilisaft.undroaid.data.models.VmDomain
import com.chilisaft.undroaid.data.models.VmRunState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.VmRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
class VmsViewModelTest {

    private lateinit var viewModel: VmsViewModel
    private lateinit var repository: VmRepository

    private val testDispatcher = StandardTestDispatcher()

    private val testVm = VmDomain(id = "v1", name = "Windows 11", state = VmRunState.RUNNING, statusLabel = "Running")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getVms() } returns WidgetResult.Success(listOf(testVm))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        viewModel = VmsViewModel(repository)
        assertThat(viewModel.uiState.value.vms).isEqualTo(WidgetResult.Loading)
    }

    @Test
    fun `loads the vm list`() {
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.vms).isEqualTo(WidgetResult.Success(listOf(testVm)))
    }

    @Test
    fun `stop calls the repository, tracks it while in flight, and reloads on success`() {
        coEvery { repository.stopVm("v1") } returns WidgetResult.Success(Unit)
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val stopped = testVm.copy(state = VmRunState.STOPPED, statusLabel = "Shut Off")
        coEvery { repository.getVms() } returns WidgetResult.Success(listOf(stopped))
        viewModel.stop("v1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.stopVm("v1") }
        assertThat(viewModel.uiState.value.vms).isEqualTo(WidgetResult.Success(listOf(stopped)))
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `start leaves the list untouched on failure`() {
        coEvery { repository.startVm("v1") } returns WidgetResult.Failure(permissionDenied = true, message = null)
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.start("v1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getVms() }
        assertThat(viewModel.uiState.value.vms).isEqualTo(WidgetResult.Success(listOf(testVm)))
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `pause and resume call their respective repository methods and reload on success`() {
        coEvery { repository.pauseVm("v1") } returns WidgetResult.Success(Unit)
        coEvery { repository.resumeVm("v1") } returns WidgetResult.Success(Unit)
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.pause("v1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.pauseVm("v1") }

        viewModel.resume("v1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.resumeVm("v1") }

        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `forceStop calls the repository and reloads on success`() {
        coEvery { repository.forceStopVm("v1") } returns WidgetResult.Success(Unit)
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.forceStop("v1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.forceStopVm("v1") }
        coVerify(exactly = 2) { repository.getVms() }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `reboot calls the repository and reloads on success`() {
        coEvery { repository.rebootVm("v1") } returns WidgetResult.Success(Unit)
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reboot("v1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.rebootVm("v1") }
        coVerify(exactly = 2) { repository.getVms() }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `reboot does not reload on failure`() {
        coEvery { repository.rebootVm("v1") } returns WidgetResult.Failure(permissionDenied = false, message = "boom")
        viewModel = VmsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reboot("v1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.rebootVm("v1") }
        coVerify(exactly = 1) { repository.getVms() }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }
}
