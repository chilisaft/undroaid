package com.chilisaft.undroaid.ui.docker

import com.chilisaft.undroaid.data.models.DockerContainer
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.DockerRepository
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
class DockerViewModelTest {

    private lateinit var viewModel: DockerViewModel
    private lateinit var repository: DockerRepository

    private val testDispatcher = StandardTestDispatcher()

    private val testContainer = DockerContainer(
        id = "c1",
        name = "plex",
        image = "plexinc/pms-docker:latest",
        state = DockerContainerState.RUNNING,
        statusText = "Up 3 hours",
        iconUrl = null,
        webUiUrl = "http://tower:32400/web"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getContainers() } returns WidgetResult.Success(listOf(testContainer))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        viewModel = DockerViewModel(repository)
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Loading)
    }

    @Test
    fun `loads the container list`() {
        viewModel = DockerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(testContainer)))
    }

    @Test
    fun `stop calls the repository, tracks it while in flight, and reloads on success`() {
        coEvery { repository.stopContainer("c1") } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val stopped = testContainer.copy(state = DockerContainerState.EXITED, statusText = "Exited (0) 0 seconds ago")
        coEvery { repository.getContainers() } returns WidgetResult.Success(listOf(stopped))
        viewModel.stop("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.stopContainer("c1") }
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(stopped)))
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `start leaves the list untouched on failure`() {
        coEvery { repository.startContainer("c1") } returns WidgetResult.Failure(permissionDenied = true, message = null)
        viewModel = DockerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.start("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getContainers() }
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(testContainer)))
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `pause and unpause call their respective repository methods and reload on success`() {
        coEvery { repository.pauseContainer("c1") } returns WidgetResult.Success(Unit)
        coEvery { repository.unpauseContainer("c1") } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.pause("c1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.pauseContainer("c1") }

        viewModel.unpause("c1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.unpauseContainer("c1") }

        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `restart calls the repository and reloads on success`() {
        coEvery { repository.restartContainer("c1") } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restart("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.restartContainer("c1") }
        coVerify(exactly = 2) { repository.getContainers() }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `restart does not reload on failure`() {
        coEvery { repository.restartContainer("c1") } returns WidgetResult.Failure(permissionDenied = false, message = "boom")
        viewModel = DockerViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restart("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.restartContainer("c1") }
        coVerify(exactly = 1) { repository.getContainers() }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }
}
