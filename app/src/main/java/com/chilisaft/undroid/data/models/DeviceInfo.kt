package com.chilisaft.undroid.data.models

/** How the Devices screen groups PCI devices - derived from the standard PCI class code. */
enum class PciDeviceKind { GRAPHICS, STORAGE, NETWORK, USB_CONTROLLER, OTHER, CORE }

/**
 * One PCI device (the API's `info.devices.pci`, INFO permission). [CORE] devices - bridges,
 * memory/SMBus/I2C/SPI controllers, the management engine - are hidden unless the user turns
 * on "Show core devices": the schema has no such flag, so it's decided by class code.
 */
data class PciDevice(
    /** Bus address, e.g. "00:02.0". */
    val address: String,
    val kind: PciDeviceKind,
    val name: String,
    val vendor: String?
)

/** One USB device - the API only reliably returns its name. Root hubs count as core devices. */
data class UsbDevice(val name: String, val isCore: Boolean)
