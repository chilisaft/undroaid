package com.chilisaft.undroaid.ui.notifications

import com.chilisaft.undroaid.data.models.Notification
import com.chilisaft.undroaid.data.models.NotificationLevel
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.NotificationsRepository
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
class NotificationsViewModelTest {

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var repository: NotificationsRepository

    private val testDispatcher = StandardTestDispatcher()

    private val testNotification = Notification(
        id = "n1",
        title = "Parity Check Complete",
        description = "No errors found.",
        level = NotificationLevel.INFO,
        timestamp = "2 hours ago"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getNotifications() } returns WidgetResult.Success(listOf(testNotification))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        viewModel = NotificationsViewModel(repository)
        assertThat(viewModel.uiState.value.notifications).isEqualTo(WidgetResult.Loading)
    }

    @Test
    fun `loads the notification list`() {
        viewModel = NotificationsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.notifications).isEqualTo(WidgetResult.Success(listOf(testNotification)))
    }

    @Test
    fun `dismiss archives the given id, tracks it while in flight, and reloads on success`() {
        coEvery { repository.archiveNotification("n1") } returns WidgetResult.Success(Unit)
        viewModel = NotificationsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotifications() } returns WidgetResult.Success(emptyList())
        viewModel.dismiss("n1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.archiveNotification("n1") }
        assertThat(viewModel.uiState.value.notifications).isEqualTo(WidgetResult.Success(emptyList<Notification>()))
        assertThat(viewModel.uiState.value.dismissingIds).isEmpty()
    }

    @Test
    fun `dismiss leaves the list untouched on failure`() {
        coEvery { repository.archiveNotification("n1") } returns WidgetResult.Failure(permissionDenied = true, message = null)
        viewModel = NotificationsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismiss("n1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getNotifications() }
        assertThat(viewModel.uiState.value.notifications).isEqualTo(WidgetResult.Success(listOf(testNotification)))
        assertThat(viewModel.uiState.value.dismissingIds).isEmpty()
    }

    @Test
    fun `dismissAll archives everything and reloads on success`() {
        coEvery { repository.archiveAllNotifications() } returns WidgetResult.Success(Unit)
        viewModel = NotificationsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotifications() } returns WidgetResult.Success(emptyList())
        viewModel.dismissAll()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.archiveAllNotifications() }
        assertThat(viewModel.uiState.value.notifications).isEqualTo(WidgetResult.Success(emptyList<Notification>()))
        assertThat(viewModel.uiState.value.isDismissingAll).isFalse()
    }

    @Test
    fun `dismissAll leaves the list untouched on failure`() {
        coEvery { repository.archiveAllNotifications() } returns WidgetResult.Failure(permissionDenied = false, message = "boom")
        viewModel = NotificationsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissAll()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getNotifications() }
        assertThat(viewModel.uiState.value.notifications).isEqualTo(WidgetResult.Success(listOf(testNotification)))
        assertThat(viewModel.uiState.value.isDismissingAll).isFalse()
    }
}
