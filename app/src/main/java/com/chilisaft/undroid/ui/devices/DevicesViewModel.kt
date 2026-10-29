package com.chilisaft.undroid.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.PciDevice
import com.chilisaft.undroid.data.models.UsbDevice
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.DevicesRepository
import com.chilisaft.undroid.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesScreenState(
    val pci: WidgetResult<List<PciDevice>> = WidgetResult.Loading,
    val usb: WidgetResult<List<UsbDevice>> = WidgetResult.Loading,
    /** The "Show core devices" setting - core devices are hidden from both lists unless it's on. */
    val showCoreDevices: Boolean = false,
    /** A refresh is running - drives the pull-to-refresh indicator, while the lists keep showing their current data. */
    val isRefreshing: Boolean = false
)

/** Backs the Devices screen. PCI and USB load independently - see [DevicesRepository]. */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val devicesRepository: DevicesRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesScreenState())
    val uiState: StateFlow<DevicesScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.showCoreList.collect { show -> _uiState.update { it.copy(showCoreDevices = show) } }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            listOf(
                launch { loadPci() },
                launch { loadUsb() }
            ).joinAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshPci() {
        viewModelScope.launch { loadPci() }
    }

    fun refreshUsb() {
        viewModelScope.launch { loadUsb() }
    }

    private suspend fun loadPci() {
        _uiState.update { it.copy(pci = it.pci.reloading()) }
        _uiState.update { it.copy(pci = devicesRepository.getPciDevices()) }
    }

    private suspend fun loadUsb() {
        _uiState.update { it.copy(usb = it.usb.reloading()) }
        _uiState.update { it.copy(usb = devicesRepository.getUsbDevices()) }
    }
}
