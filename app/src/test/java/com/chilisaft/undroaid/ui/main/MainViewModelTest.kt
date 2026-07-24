package com.chilisaft.undroaid.ui.main

import com.chilisaft.undroaid.data.models.ArrayOverview
import com.chilisaft.undroaid.data.models.ArrayRunState
import com.chilisaft.undroaid.data.models.DiskIdentification
import com.chilisaft.undroaid.data.models.FlashInfo
import com.chilisaft.undroaid.data.models.ParityCheckInfo
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.ArrayRepository
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
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: ArrayRepository

    private val testDispatcher = StandardTestDispatcher()

    private val testOverview = ArrayOverview(
        state = ArrayRunState.STARTED,
        statusLabel = "Array Started",
        usedKb = 1024L,
        totalKb = 4096L,
        boot = null,
        caches = emptyList(),
        parities = emptyList(),
        disks = emptyList()
    )

    private fun parityCheck(running: Boolean, progress: Int = 0) = ParityCheckInfo(
        statusLabel = if (running) "Running" else "Never Run",
        running = running,
        paused = false,
        progressPercent = progress,
        speed = null,
        errors = 0,
        correcting = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getArrayOverview() } returns WidgetResult.Success(testOverview)
            coEvery { getParityCheckStatus() } returns WidgetResult.Success(parityCheck(running = false))
            coEvery { getDiskIdentifications() } returns WidgetResult.Success(emptyList())
            coEvery { getFlashInfo() } returns WidgetResult.Success(FlashInfo(vendor = "SanDisk", product = "Ultra Fit"))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh loads overview, parity check, disk identifications, and flash info`() {
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.overview).isEqualTo(WidgetResult.Success(testOverview))
        assertThat(state.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = false)))
        assertThat(state.diskIdentifications).isEqualTo(WidgetResult.Success(emptyList<DiskIdentification>()))
        assertThat(state.flashInfo).isEqualTo(WidgetResult.Success(FlashInfo(vendor = "SanDisk", product = "Ultra Fit")))
    }

    @Test
    fun `startArray sends the password and refreshes on success`() {
        coEvery { repository.setArrayState(start = true, password = "hunter2") } returns WidgetResult.Success(Unit)
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startArray("hunter2")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.setArrayState(start = true, password = "hunter2") }
        assertThat(viewModel.uiState.value.actioningArray).isFalse()
    }

    @Test
    fun `stopArray does not refresh on failure`() {
        coEvery { repository.setArrayState(start = false, password = null) } returns WidgetResult.Failure(permissionDenied = false, message = "boom")
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.stopArray()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getArrayOverview() } // only the initial load - failure shouldn't trigger a refresh
        assertThat(viewModel.uiState.value.actioningArray).isFalse()
    }

    @Test
    fun `startParityCheck refreshes on success`() {
        coEvery { repository.startParityCheck() } returns WidgetResult.Success(Unit)
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startParityCheck()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.startParityCheck() }
        coVerify(exactly = 2) { repository.getParityCheckStatus() } // initial load + refresh after success
        assertThat(viewModel.uiState.value.actioningParityCheck).isFalse()
    }

    @Test
    fun `pauseParityCheck, resumeParityCheck, and cancelParityCheck all refresh on success`() {
        coEvery { repository.pauseParityCheck() } returns WidgetResult.Success(Unit)
        coEvery { repository.resumeParityCheck() } returns WidgetResult.Success(Unit)
        coEvery { repository.cancelParityCheck() } returns WidgetResult.Success(Unit)
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.pauseParityCheck()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.resumeParityCheck()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cancelParityCheck()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.pauseParityCheck() }
        coVerify { repository.resumeParityCheck() }
        coVerify { repository.cancelParityCheck() }
    }

    @Test
    fun `polls the parity check every 5s while running, and stops once it finishes`() {
        coEvery { repository.getParityCheckStatus() } returnsMany listOf(
            WidgetResult.Success(parityCheck(running = true, progress = 10)),
            WidgetResult.Success(parityCheck(running = true, progress = 20)),
            WidgetResult.Success(parityCheck(running = false, progress = 100))
        )
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.runCurrent() // only the initial load - advanceUntilIdle would fast-forward through the poll's delay too
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = true, progress = 10)))

        testDispatcher.scheduler.advanceTimeBy(5_000)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = true, progress = 20)))

        testDispatcher.scheduler.advanceTimeBy(5_000)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = false, progress = 100)))

        coVerify(exactly = 3) { repository.getParityCheckStatus() }
    }

    @Test
    fun `a failed poll tick is skipped, keeping the last good parity check, rather than stopping the poll`() {
        // Regression test: a Failure result used to be indistinguishable from "the check
        // finished" (both made isParityCheckRunning() return false), so a single transient
        // failure - e.g. a network hiccup right after the device unlocks - permanently killed
        // the poll loop. It should instead keep the last successful parity check on screen and
        // keep polling, recovering on the next successful tick.
        coEvery { repository.getParityCheckStatus() } returnsMany listOf(
            WidgetResult.Success(parityCheck(running = true, progress = 10)),
            WidgetResult.Failure(permissionDenied = false, message = "boom"),
            WidgetResult.Success(parityCheck(running = true, progress = 30)),
            WidgetResult.Success(parityCheck(running = false, progress = 100))
        )
        viewModel = MainViewModel(repository)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = true, progress = 10)))

        // Tick 2 fails - the last good parity check (progress 10) should still be showing.
        testDispatcher.scheduler.advanceTimeBy(5_000)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = true, progress = 10)))

        // Tick 3 succeeds again - polling kept going despite the failed tick.
        testDispatcher.scheduler.advanceTimeBy(5_000)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = true, progress = 30)))

        testDispatcher.scheduler.advanceTimeBy(5_000)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.parityCheck).isEqualTo(WidgetResult.Success(parityCheck(running = false, progress = 100)))

        coVerify(exactly = 4) { repository.getParityCheckStatus() }
    }

    @Test
    fun `does not poll when no parity check is running`() {
        viewModel = MainViewModel(repository) // default stub: running = false
        testDispatcher.scheduler.advanceUntilIdle()

        testDispatcher.scheduler.advanceTimeBy(10_000)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { repository.getParityCheckStatus() }
    }
}
