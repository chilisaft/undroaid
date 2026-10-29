package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.chilisaft.undroid.data.api.runWidgetMutation
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.ArrayDeviceInfo
import com.chilisaft.undroid.data.models.ArrayDeviceRole
import com.chilisaft.undroid.data.models.ArrayOverview
import com.chilisaft.undroid.data.models.ArrayRunState
import com.chilisaft.undroid.data.models.DiskIdentification
import com.chilisaft.undroid.data.models.FlashInfo
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.ParityHistoryEntry
import com.chilisaft.undroid.data.models.ParityHistoryStatus
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.fragment.ArrayDiskFields
import com.chilisaft.undroid.graphql.ArraySetStateMutation
import com.chilisaft.undroid.graphql.FlashInfoQuery
import com.chilisaft.undroid.graphql.MainArrayQuery
import com.chilisaft.undroid.graphql.ParityCheckCancelMutation
import com.chilisaft.undroid.graphql.ParityCheckPauseMutation
import com.chilisaft.undroid.graphql.ParityCheckResumeMutation
import com.chilisaft.undroid.graphql.ParityCheckStartMutation
import com.chilisaft.undroid.graphql.ParityCheckStatusQuery
import com.chilisaft.undroid.graphql.ParityHistoryQuery
import com.chilisaft.undroid.graphql.SystemDisksQuery
import com.chilisaft.undroid.graphql.type.ArrayDiskType
import com.chilisaft.undroid.graphql.type.ArrayState
import com.chilisaft.undroid.graphql.type.ArrayStateInput
import com.chilisaft.undroid.graphql.type.ArrayStateInputState
import com.chilisaft.undroid.graphql.type.DiskInterfaceType
import com.chilisaft.undroid.graphql.type.DiskSmartStatus
import com.chilisaft.undroid.graphql.type.ParityCheckStatus
import java.time.Instant
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

    /** Past parity checks, newest first. */
    suspend fun getParityHistory(): WidgetResult<List<ParityHistoryEntry>> =
        apolloClient.runWidgetQuery(ParityHistoryQuery()) { data ->
            data.parityHistory.map { it.toParityHistoryEntry() }.sortedByDescending { it.date }
        }

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

    private fun ParityHistoryQuery.ParityHistory.toParityHistoryEntry() = ParityHistoryEntry(
        date = date?.let { runCatching { Instant.parse(it) }.getOrNull() },
        status = when (status) {
            ParityCheckStatus.COMPLETED -> ParityHistoryStatus.COMPLETED
            ParityCheckStatus.CANCELLED -> ParityHistoryStatus.CANCELLED
            ParityCheckStatus.FAILED -> ParityHistoryStatus.FAILED
            else -> ParityHistoryStatus.OTHER
        },
        durationSeconds = duration?.toLong(),
        bytesPerSecond = speed?.toDoubleOrNull(),
        errors = errors
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
        health = status.toDeviceHealth(),
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
    private fun ArrayState.toRunState(): ArrayRunState = when (this) {
        ArrayState.STARTED -> ArrayRunState.STARTED
        ArrayState.STOPPED -> ArrayRunState.STOPPED
        else -> ArrayRunState.OTHER
    }

    private fun ArrayDiskType.toRole(): ArrayDeviceRole = when (this) {
        ArrayDiskType.DATA -> ArrayDeviceRole.DATA
        ArrayDiskType.PARITY -> ArrayDeviceRole.PARITY
        ArrayDiskType.BOOT -> ArrayDeviceRole.BOOT
        ArrayDiskType.FLASH -> ArrayDeviceRole.FLASH
        ArrayDiskType.CACHE -> ArrayDeviceRole.CACHE
        ArrayDiskType.UNKNOWN__ -> ArrayDeviceRole.DATA
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
