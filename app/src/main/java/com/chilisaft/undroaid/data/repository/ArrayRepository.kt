package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.chilisaft.undroaid.data.api.runWidgetMutation
import com.chilisaft.undroaid.data.api.runWidgetQuery
import com.chilisaft.undroaid.data.models.ArrayDeviceInfo
import com.chilisaft.undroaid.data.models.ArrayDeviceRole
import com.chilisaft.undroaid.data.models.ArrayOverview
import com.chilisaft.undroaid.data.models.ArrayRunState
import com.chilisaft.undroaid.data.models.DiskIdentification
import com.chilisaft.undroaid.data.models.FlashInfo
import com.chilisaft.undroaid.data.models.ParityCheckInfo
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.graphql.fragment.ArrayDiskFields
import com.chilisaft.undroaid.graphql.ArraySetStateMutation
import com.chilisaft.undroaid.graphql.FlashInfoQuery
import com.chilisaft.undroaid.graphql.MainArrayQuery
import com.chilisaft.undroaid.graphql.ParityCheckCancelMutation
import com.chilisaft.undroaid.graphql.ParityCheckPauseMutation
import com.chilisaft.undroaid.graphql.ParityCheckResumeMutation
import com.chilisaft.undroaid.graphql.ParityCheckStartMutation
import com.chilisaft.undroaid.graphql.ParityCheckStatusQuery
import com.chilisaft.undroaid.graphql.SystemDisksQuery
import com.chilisaft.undroaid.graphql.type.ArrayDiskStatus
import com.chilisaft.undroaid.graphql.type.ArrayDiskType
import com.chilisaft.undroaid.graphql.type.ArrayState
import com.chilisaft.undroaid.graphql.type.ArrayStateInput
import com.chilisaft.undroaid.graphql.type.ArrayStateInputState
import com.chilisaft.undroaid.graphql.type.DiskInterfaceType
import com.chilisaft.undroaid.graphql.type.DiskSmartStatus
import com.chilisaft.undroaid.graphql.type.ParityCheckStatus
import javax.inject.Inject

/** Backs the Main tab - array/parity/cache/boot device tables plus array/parity-check control. */
class ArrayRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getArrayOverview(): WidgetResult<ArrayOverview> =
        apolloClient.runWidgetQuery(MainArrayQuery()) { data -> data.array.toArrayOverview() }

    /**
     * Disk identification (model/vendor/serial) lives on the root `disks` query, not on
     * `ArrayDisk` - a separate DISK-resource permission from the array's own ARRAY permission,
     * so it's fetched and can fail independently of the array overview.
     */
    suspend fun getDiskIdentifications(): WidgetResult<List<DiskIdentification>> =
        apolloClient.runWidgetQuery(SystemDisksQuery()) { data -> data.disks.map { it.toDiskIdentification() } }

    /** Boot device identification - the root `disks`/`DISK` query doesn't cover the flash boot drive, only the dedicated `flash` field does. */
    suspend fun getFlashInfo(): WidgetResult<FlashInfo> =
        apolloClient.runWidgetQuery(FlashInfoQuery()) { data -> FlashInfo(vendor = data.flash.vendor, product = data.flash.product) }

    /**
     * Independent from [getArrayOverview] - a parity-check-specific hiccup only blanks the
     * parity card, not the whole device-list overview (they used to share one query/`WidgetResult`,
     * which meant a failure here blanked everything - see NOTES.md).
     */
    suspend fun getParityCheckStatus(): WidgetResult<ParityCheckInfo> =
        apolloClient.runWidgetQuery(ParityCheckStatusQuery()) { data -> data.array.parityCheckStatus.toParityCheckInfo() }

    suspend fun setArrayState(start: Boolean, password: String?): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(
            ArraySetStateMutation(
                ArrayStateInput(
                    desiredState = if (start) ArrayStateInputState.START else ArrayStateInputState.STOP,
                    decryptionPassword = if (password.isNullOrBlank()) Optional.Absent else Optional.Present(password)
                )
            )
        ) { }

    suspend fun startParityCheck(correct: Boolean = false): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(ParityCheckStartMutation(correct)) { }

    suspend fun pauseParityCheck(): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(ParityCheckPauseMutation()) { }

    suspend fun resumeParityCheck(): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(ParityCheckResumeMutation()) { }

    suspend fun cancelParityCheck(): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(ParityCheckCancelMutation()) { }

    private fun MainArrayQuery.Array.toArrayOverview() = ArrayOverview(
        state = state.toRunState(),
        statusLabel = state.toLabel(),
        usedKb = capacity.kilobytes.used.toLongOrNull(),
        totalKb = capacity.kilobytes.total.toLongOrNull(),
        boot = boot?.arrayDiskFields?.toArrayDeviceInfo(),
        caches = caches.map { it.arrayDiskFields.toArrayDeviceInfo() },
        parities = parities.map { it.arrayDiskFields.toArrayDeviceInfo() },
        disks = disks.map { it.arrayDiskFields.toArrayDeviceInfo() }
    )

    private fun ArrayDiskFields.toArrayDeviceInfo() = ArrayDeviceInfo(
        id = id,
        name = name,
        device = device,
        role = type.toRole(),
        sizeKb = size?.toLongOrNull(),
        usedKb = fsUsed?.toLongOrNull(),
        freeKb = fsFree?.toLongOrNull(),
        statusLabel = status?.toLabel(),
        tempC = temp,
        isSpinning = isSpinning,
        fsType = fsType,
        transport = transport
    )

    private fun SystemDisksQuery.Disk.toDiskIdentification() = DiskIdentification(
        device = device,
        model = name,
        vendor = vendor,
        serialNumber = serialNum,
        interfaceType = interfaceType.toLabel(),
        smartStatus = smartStatus.toLabel()
    )

    /**
     * The server's `running`/`paused` booleans and its `status` enum are apparently derived
     * somewhat independently (confirmed against a real server: `status` correctly read RUNNING
     * while `running` was false/null in the same response) - ORing each boolean with the
     * matching enum value means either signal being right is enough, rather than trusting the
     * booleans alone and silently hiding an active check.
     */
    private fun ParityCheckStatusQuery.ParityCheckStatus.toParityCheckInfo() = ParityCheckInfo(
        statusLabel = status.toLabel(),
        running = (running ?: false) || status == ParityCheckStatus.RUNNING,
        paused = (paused ?: false) || status == ParityCheckStatus.PAUSED,
        progressPercent = progress,
        speed = speed,
        errors = errors,
        correcting = correcting
    )

    private fun ArrayState.toRunState(): ArrayRunState = when (this) {
        ArrayState.STARTED -> ArrayRunState.STARTED
        ArrayState.STOPPED -> ArrayRunState.STOPPED
        else -> ArrayRunState.OTHER
    }

    private fun ArrayState.toLabel(): String = when (this) {
        ArrayState.STARTED -> "Array Started"
        ArrayState.STOPPED -> "Array Stopped"
        ArrayState.NEW_ARRAY -> "New Array"
        ArrayState.RECON_DISK -> "Reconstructing Disk"
        ArrayState.DISABLE_DISK -> "Disk Disabled"
        ArrayState.SWAP_DSBL -> "Disk Swap Pending"
        ArrayState.INVALID_EXPANSION -> "Invalid Expansion"
        ArrayState.PARITY_NOT_BIGGEST -> "Parity Too Small"
        ArrayState.TOO_MANY_MISSING_DISKS -> "Too Many Missing Disks"
        ArrayState.NEW_DISK_TOO_SMALL -> "New Disk Too Small"
        ArrayState.NO_DATA_DISKS -> "No Data Disks"
        ArrayState.UNKNOWN__ -> "Unknown"
    }

    private fun ArrayDiskType.toRole(): ArrayDeviceRole = when (this) {
        ArrayDiskType.DATA -> ArrayDeviceRole.DATA
        ArrayDiskType.PARITY -> ArrayDeviceRole.PARITY
        ArrayDiskType.BOOT -> ArrayDeviceRole.BOOT
        ArrayDiskType.FLASH -> ArrayDeviceRole.FLASH
        ArrayDiskType.CACHE -> ArrayDeviceRole.CACHE
        ArrayDiskType.UNKNOWN__ -> ArrayDeviceRole.DATA
    }

    private fun ArrayDiskStatus.toLabel(): String = when (this) {
        ArrayDiskStatus.DISK_NP -> "Not Present"
        ArrayDiskStatus.DISK_OK -> "OK"
        ArrayDiskStatus.DISK_NP_MISSING -> "Missing"
        ArrayDiskStatus.DISK_INVALID -> "Invalid"
        ArrayDiskStatus.DISK_WRONG -> "Wrong Disk"
        ArrayDiskStatus.DISK_DSBL -> "Disabled"
        ArrayDiskStatus.DISK_NP_DSBL -> "Disabled (Missing)"
        ArrayDiskStatus.DISK_DSBL_NEW -> "Disabled (New)"
        ArrayDiskStatus.DISK_NEW -> "New Disk"
        ArrayDiskStatus.UNKNOWN__ -> "Unknown"
    }

    private fun ParityCheckStatus.toLabel(): String = when (this) {
        ParityCheckStatus.NEVER_RUN -> "Never Run"
        ParityCheckStatus.RUNNING -> "Running"
        ParityCheckStatus.PAUSED -> "Paused"
        ParityCheckStatus.COMPLETED -> "Completed"
        ParityCheckStatus.CANCELLED -> "Cancelled"
        ParityCheckStatus.FAILED -> "Failed"
        ParityCheckStatus.UNKNOWN__ -> "Unknown"
    }

    private fun DiskInterfaceType.toLabel(): String = when (this) {
        DiskInterfaceType.SAS -> "SAS"
        DiskInterfaceType.SATA -> "SATA"
        DiskInterfaceType.USB -> "USB"
        DiskInterfaceType.PCIE -> "PCIe"
        DiskInterfaceType.UNKNOWN -> "Unknown"
        DiskInterfaceType.UNKNOWN__ -> "Unknown"
    }

    private fun DiskSmartStatus.toLabel(): String = when (this) {
        DiskSmartStatus.OK -> "OK"
        DiskSmartStatus.UNKNOWN -> "Unknown"
        DiskSmartStatus.UNKNOWN__ -> "Unknown"
    }
}
