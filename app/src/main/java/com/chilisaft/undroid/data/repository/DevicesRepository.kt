package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.PciDevice
import com.chilisaft.undroid.data.models.PciDeviceKind
import com.chilisaft.undroid.data.models.UsbDevice
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.DevicesPciQuery
import com.chilisaft.undroid.graphql.DevicesUsbQuery
import javax.inject.Inject

/**
 * Backs the Devices screen - a read-only hardware inventory. PCI and USB are separate queries:
 * on a real server the API failed whole lists over a single null field (see DevicesPciQuery).
 */
class DevicesRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getPciDevices(): WidgetResult<List<PciDevice>> =
        apolloClient.runWidgetQuery(DevicesPciQuery()) { data ->
            data.info.devices.pci.orEmpty().map { it.toPciDevice() }.sortedBy { it.address }
        }

    suspend fun getUsbDevices(): WidgetResult<List<UsbDevice>> =
        apolloClient.runWidgetQuery(DevicesUsbQuery()) { data ->
            data.info.devices.usb.orEmpty().map { UsbDevice(it.name, isCore = it.name.contains("root hub", ignoreCase = true)) }
        }

    private fun DevicesPciQuery.Pci.toPciDevice(): PciDevice {
        val classCode = typeid.lowercase()
        // A real host bridge was named just "Device" - fall back to what its class code says.
        val productName = productname?.trim()?.takeIf { it.isNotEmpty() && it != "Device" }
        return PciDevice(
            address = id.removePrefix("pci/"),
            kind = classCode.toPciDeviceKind(),
            name = productName ?: classCode.toClassName(),
            vendor = vendorname?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * PCI class codes (class + subclass, e.g. "0604" = PCI bridge). Core = what only exists to
     * make the board work: memory controllers (05), bridges (06), communication controllers like
     * the management engine (07), system peripherals (08), SMBus (0c05) and other serial buses
     * such as I2C/SPI (0c80). USB controllers (0c03) stay visible - they're passed through to VMs.
     */
    private fun String.toPciDeviceKind(): PciDeviceKind = when {
        this == "0c03" -> PciDeviceKind.USB_CONTROLLER
        this == "0c05" || this == "0c80" -> PciDeviceKind.CORE
        startsWith("01") -> PciDeviceKind.STORAGE
        startsWith("02") -> PciDeviceKind.NETWORK
        startsWith("03") -> PciDeviceKind.GRAPHICS
        startsWith("05") || startsWith("06") || startsWith("07") || startsWith("08") -> PciDeviceKind.CORE
        else -> PciDeviceKind.OTHER
    }

    private fun String.toClassName(): String = when (this) {
        "0600" -> "Host bridge"
        "0601" -> "ISA bridge"
        "0604" -> "PCI bridge"
        "0500" -> "Memory controller"
        "0c05" -> "SMBus controller"
        else -> "PCI device"
    }
}
