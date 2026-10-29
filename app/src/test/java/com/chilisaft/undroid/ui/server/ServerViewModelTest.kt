package com.chilisaft.undroid.ui.server

import com.chilisaft.undroid.data.models.ConnectInfo
import com.chilisaft.undroid.data.models.ConnectStatus
import com.chilisaft.undroid.data.models.LicenceDates
import com.chilisaft.undroid.data.models.NetworkSummary
import com.chilisaft.undroid.data.models.ServerIdentity
import com.chilisaft.undroid.data.models.ServerSystem
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.repository.CachedNetwork
import com.chilisaft.undroid.data.repository.ServerNetworkRepository
import com.chilisaft.undroid.data.repository.ServerRepository
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
class ServerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ServerRepository
    private lateinit var networkRepository: ServerNetworkRepository

    private val identity = ServerIdentity(name = "TOWER", comment = null, timeZone = null, licence = "Pro")
    private val system = ServerSystem(
        unraidVersion = "7.3.3", apiVersion = null, kernel = null, dockerVersion = null, bootTimeIso = null, cpu = null, cpuCores = null,
        cpuThreads = null, memoryTotalBytes = null, memoryType = null, motherboard = null
    )
    private val network = NetworkSummary("br0", "192.168.69.7", 1000)
    private val connect = ConnectInfo(ConnectStatus.NEVER_CONNECTED, null, null, null, null, null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            coEvery { getServerIdentity() } returns WidgetResult.Success(identity)
            coEvery { getLicenceDates() } returns WidgetResult.Success(LicenceDates(expiresAt = null, updatesUntil = null))
            coEvery { getServerSystem() } returns WidgetResult.Success(system)
            coEvery { getConnectInfo() } returns WidgetResult.Success(connect)
        }
        networkRepository = mockk {
            every { cached() } returns null
            every { isStale(any()) } returns true
            coEvery { fetch() } returns WidgetResult.Success(network)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a missing SERVERS permission only affects the Connect section`() {
        coEvery { repository.getConnectInfo() } returns WidgetResult.Failure(permissionDenied = true, message = null)

        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.identity).isEqualTo(WidgetResult.Success(identity))
        assertThat(state.system).isEqualTo(WidgetResult.Success(system))
        assertThat(state.connect).isEqualTo(WidgetResult.Failure(permissionDenied = true, message = null))
    }

    @Test
    fun `retrying one section only reloads that section`() {
        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshConnect()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { repository.getConnectInfo() }
        coVerify(exactly = 1) { repository.getServerIdentity() }
        coVerify(exactly = 1) { repository.getServerSystem() }
    }

    @Test
    fun `a full refresh keeps sections on screen and reports isRefreshing meanwhile`() {
        val viewModel = ServerViewModel(repository, networkRepository)
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

    @Test
    fun `the network section starts from the cache and is replaced once the fresh result arrives`() {
        every { networkRepository.cached() } returns CachedNetwork(network.copy(ipAddress = "10.0.0.2"), fetchedAtMillis = 0)
        coEvery { networkRepository.fetch() } coAnswers {
            delay(2_000)
            WidgetResult.Success(network)
        }

        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.network).isEqualTo(WidgetResult.Success(network.copy(ipAddress = "10.0.0.2")))

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.network).isEqualTo(WidgetResult.Success(network))
    }

    @Test
    fun `a transient network failure keeps the cached interface, a permission failure replaces it`() {
        every { networkRepository.cached() } returns CachedNetwork(network, fetchedAtMillis = 0)
        coEvery { networkRepository.fetch() } returns WidgetResult.Failure(permissionDenied = false, message = "timeout")

        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.network).isEqualTo(WidgetResult.Success(network))

        coEvery { networkRepository.fetch() } returns WidgetResult.Failure(permissionDenied = true, message = null)
        viewModel.refreshNetwork()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.network).isEqualTo(WidgetResult.Failure(permissionDenied = true, message = null))
    }

    @Test
    fun `without a cache, the network section loads like any other`() {
        val viewModel = ServerViewModel(repository, networkRepository)
        assertThat(viewModel.uiState.value.network).isEqualTo(WidgetResult.Loading)

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.network).isEqualTo(WidgetResult.Success(network))
    }

    @Test
    fun `a fresh cache isn't refetched on open, but pull-to-refresh refetches it`() {
        every { networkRepository.cached() } returns CachedNetwork(network, fetchedAtMillis = 0)
        every { networkRepository.isStale(any()) } returns false

        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { networkRepository.fetch() }

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { networkRepository.fetch() }
    }

    @Test
    fun `the network is fetched only after the other sections, and outside the pull indicator`() {
        coEvery { repository.getServerSystem() } coAnswers {
            delay(500)
            WidgetResult.Success(system)
        }

        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()
        coVerify(exactly = 0) { networkRepository.fetch() }

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { networkRepository.fetch() }
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
    }

    @Test
    fun `missing licence dates don't affect the identity section`() {
        coEvery { repository.getLicenceDates() } returns WidgetResult.Failure(permissionDenied = true, message = null)

        val viewModel = ServerViewModel(repository, networkRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.identity).isEqualTo(WidgetResult.Success(identity))
        assertThat(viewModel.uiState.value.licenceDates).isEqualTo(WidgetResult.Failure(permissionDenied = true, message = null))
    }
}
