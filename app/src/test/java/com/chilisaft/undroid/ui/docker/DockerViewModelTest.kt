package com.chilisaft.undroid.ui.docker

import com.chilisaft.undroid.data.models.DockerContainer
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.DockerLogLine
import com.chilisaft.undroid.data.models.DockerLogsPage
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.repository.DockerRepository
import com.chilisaft.undroid.ui.UserMessages
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
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
class DockerViewModelTest {

    private lateinit var viewModel: DockerViewModel
    private val userMessages: UserMessages = mockk(relaxed = true)
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
        viewModel = DockerViewModel(repository, userMessages)
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Loading)
    }

    @Test
    fun `loads the container list`() {
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(testContainer)))
    }

    @Test
    fun `stop calls the repository, tracks it while in flight, and reloads on success`() {
        coEvery { repository.stopContainer("c1") } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository, userMessages)
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
    fun `stop keeps polling until the container actually reports exited, then stops`() {
        coEvery { repository.stopContainer("c1") } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()

        val stopped = testContainer.copy(state = DockerContainerState.EXITED, statusText = "Exited (0) 0 seconds ago")
        coEvery { repository.getContainers() } returnsMany listOf(
            WidgetResult.Success(listOf(testContainer)),
            WidgetResult.Success(listOf(testContainer)),
            WidgetResult.Success(listOf(stopped))
        )
        viewModel.stop("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 4) { repository.getContainers() }
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(stopped)))
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `start leaves the list untouched on failure and tells the user why`() {
        coEvery { repository.startContainer("c1") } returns WidgetResult.Failure(permissionDenied = true, message = null)
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.start("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        verify { userMessages.showFailure("start ${testContainer.name}", WidgetResult.Failure(permissionDenied = true, message = null)) }
        coVerify(exactly = 1) { repository.getContainers() }
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(testContainer)))
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `pause and unpause call their respective repository methods and reload on success`() {
        coEvery { repository.pauseContainer("c1") } returns WidgetResult.Success(Unit)
        coEvery { repository.unpauseContainer("c1") } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository, userMessages)
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
        viewModel = DockerViewModel(repository, userMessages)
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
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restart("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.restartContainer("c1") }
        coVerify(exactly = 1) { repository.getContainers() }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }

    @Test
    fun `the log tail polls while visible, pauses while hidden, and resumes from its cursor`() {
        coEvery { repository.getLogs("c1", any(), any()) } returns
            WidgetResult.Success(DockerLogsPage(lines = listOf(DockerLogLine("t", "hello")), cursor = "cursor-1"))
        viewModel = DockerViewModel(repository, userMessages)
        viewModel.onScreenVisible()
        testDispatcher.scheduler.runCurrent()

        viewModel.openLogs("c1", "plex")
        testDispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { repository.getLogs("c1", 300, null) } // initial tail

        viewModel.onScreenHidden()
        testDispatcher.scheduler.advanceTimeBy(30_000)
        testDispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { repository.getLogs(any(), any(), any()) } // nothing while hidden

        viewModel.onScreenVisible()
        testDispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { repository.getLogs("c1", null, "cursor-1") } // picks up where it left off
        assertThat(viewModel.uiState.value.logs?.result).isEqualTo(
            WidgetResult.Success(listOf(DockerLogLine("t", "hello"), DockerLogLine("t", "hello")))
        )
    }

    @Test
    fun `check for updates reloads the list and reports how many were found`() {
        val updatable = testContainer.copy(updateAvailable = true)
        coEvery { repository.checkForUpdates() } returns WidgetResult.Success(Unit)
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { repository.getContainers() } returns WidgetResult.Success(listOf(updatable))

        viewModel.checkForUpdates()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(updatable)))
        assertThat(viewModel.uiState.value.isCheckingForUpdates).isFalse()
        verify { userMessages.show("1 update available") }
    }

    @Test
    fun `a failed check is reported and leaves the list alone`() {
        val failure = WidgetResult.Failure(permissionDenied = true, message = null)
        coEvery { repository.checkForUpdates() } returns failure
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkForUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { userMessages.showFailure("check for updates", failure) }
        coVerify(exactly = 1) { repository.getContainers() }
    }

    @Test
    fun `update shows a spinner while it runs, then reloads the list - the container comes back with a new id`() {
        val updated = testContainer.copy(id = "c1-new", updateAvailable = false)
        coEvery { repository.updateContainer("c1") } coAnswers {
            delay(5_000)
            WidgetResult.Success(Unit)
        }
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { repository.getContainers() } returns WidgetResult.Success(listOf(updated))

        viewModel.update("c1")
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.actioningIds).containsExactly("c1")

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
        assertThat(viewModel.uiState.value.containers).isEqualTo(WidgetResult.Success(listOf(updated)))
        verify { userMessages.show("Updated plex") }
    }

    @Test
    fun `update all sends exactly the confirmed ids and reports a failure by count`() {
        val second = testContainer.copy(id = "c2", name = "sonarr")
        coEvery { repository.getContainers() } returns WidgetResult.Success(listOf(testContainer, second))
        val failure = WidgetResult.Failure(permissionDenied = false, message = "timeout")
        coEvery { repository.updateContainers(listOf("c1", "c2")) } returns failure
        viewModel = DockerViewModel(repository, userMessages)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAll(listOf("c1", "c2"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.updateContainers(listOf("c1", "c2")) }
        verify { userMessages.showFailure("update 2 containers", failure) }
        assertThat(viewModel.uiState.value.actioningIds).isEmpty()
    }
}
