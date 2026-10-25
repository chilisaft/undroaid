package com.chilisaft.undroid.ui.shares

import com.chilisaft.undroid.data.models.ShareInfo
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.repository.SharesRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
class SharesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SharesRepository

    private val appdata = ShareInfo("s1", "appdata", null, freeKb = 900L, sizeKb = 120L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk { coEvery { getShares() } returns WidgetResult.Success(listOf(appdata)) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the shares`() {
        val viewModel = SharesViewModel(repository)
        assertThat(viewModel.uiState.value.shares).isEqualTo(WidgetResult.Loading)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.shares).isEqualTo(WidgetResult.Success(listOf(appdata)))
    }

    @Test
    fun `a refresh keeps the list on screen and reports isRefreshing meanwhile`() {
        val viewModel = SharesViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { repository.getShares() } coAnswers {
            delay(1_000)
            WidgetResult.Success(emptyList())
        }

        viewModel.refresh()
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.isRefreshing).isTrue()
        assertThat(viewModel.uiState.value.shares).isEqualTo(WidgetResult.Success(listOf(appdata)))

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
        assertThat(viewModel.uiState.value.shares).isEqualTo(WidgetResult.Success(emptyList<ShareInfo>()))
    }
}
