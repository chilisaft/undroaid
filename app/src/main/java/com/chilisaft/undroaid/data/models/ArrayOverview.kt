package com.chilisaft.undroaid.data.models

enum class ArrayRunState {
    STARTED, STOPPED, OTHER
}

enum class ArrayDeviceRole {
    DATA, PARITY, BOOT, FLASH, CACHE
}

data class ArrayDeviceInfo(
    val id: String,
    val name: String?,
    val device: String?,
    val role: ArrayDeviceRole,
    val sizeKb: Long?,
    val usedKb: Long?,
    val freeKb: Long?,
    val statusLabel: String?,
    val tempC: Int?,
    val isSpinning: Boolean?,
    val fsType: String?,
    val transport: String?
)

data class DiskIdentification(
    val device: String,
    val model: String,
    val vendor: String,
    val serialNumber: String,
    val interfaceType: String,
    val smartStatus: String
)

/**
 * The boot device is always named "flash" in [ArrayDeviceInfo] (Unraid's fixed internal slot
 * name for it, idx 54 - see `ArrayDisk.idx`'s doc comment) regardless of the actual USB drive
 * plugged in. This carries the real vendor/product from the schema's dedicated `flash` query,
 * since `ArrayDisk`/the root `disks` query don't cover the boot device at all.
 */
data class FlashInfo(
    val vendor: String,
    val product: String
)

data class ParityCheckInfo(
    val statusLabel: String,
    val running: Boolean,
    val paused: Boolean,
    val progressPercent: Int?,
    val speed: String?,
    val errors: Int?,
    val correcting: Boolean?
)

data class ArrayOverview(
    val state: ArrayRunState,
    val statusLabel: String,
    val usedKb: Long?,
    val totalKb: Long?,
    val boot: ArrayDeviceInfo?,
    val caches: List<ArrayDeviceInfo>,
    val parities: List<ArrayDeviceInfo>,
    val disks: List<ArrayDeviceInfo>
)
