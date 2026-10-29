package com.chilisaft.undroid.ui.devices

import com.chilisaft.undroid.data.models.PciDevice
import com.chilisaft.undroid.data.models.PciDeviceKind
import com.chilisaft.undroid.data.models.UsbDevice
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.repository.DevicesRepository
import com.chilisaft.undroid.data.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DevicesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var devicesRepository: DevicesRepository
    private lateinit var settingsRepository: SettingsRepository
    private val showCoreList = MutableStateFlow(false)

    private val pci = listOf(PciDevice("00:02.0", PciDeviceKind.GRAPHICS, "UHD Graphics 730", "Intel"))
    private val usb = listOf(UsbDevice("SanDisk Corp. Ultra Fit", isCore = false))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        devicesRepository = mockk {
            coEvery { getPciDevices() } returns WidgetResult.Success(pci)
            coEvery { getUsbDevices() } returns WidgetResult.Success(usb)
        }
        settingsRepository = mockk { every { showCoreList } returns this@DevicesViewModelTest.showCoreList }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads both lists and follows the Show core devices setting`() {
        val viewModel = DevicesViewModel(devicesRepository, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.pci).isEqualTo(WidgetResult.Success(pci))
        assertThat(viewModel.uiState.value.usb).isEqualTo(WidgetResult.Success(usb))
        assertThat(viewModel.uiState.value.showCoreDevices).isFalse()

        showCoreList.value = true
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.showCoreDevices).isTrue()
    }

    @Test
    fun `a failed PCI list doesn't affect USB, and retrying reloads only PCI`() {
        coEvery { devicesRepository.getPciDevices() } returns WidgetResult.Failure(permissionDenied = false, message = "Cannot return null")

        val viewModel = DevicesViewModel(devicesRepository, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.usb).isEqualTo(WidgetResult.Success(usb))

        viewModel.refreshPci()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 2) { devicesRepository.getPciDevices() }
        coVerify(exactly = 1) { devicesRepository.getUsbDevices() }
    }
}
