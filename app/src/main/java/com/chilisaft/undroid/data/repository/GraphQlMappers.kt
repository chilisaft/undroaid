package com.chilisaft.undroid.data.repository

import com.chilisaft.undroid.data.models.DeviceHealth
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.graphql.ParityCheckStatusQuery
import com.chilisaft.undroid.graphql.type.ArrayDiskStatus
import com.chilisaft.undroid.graphql.type.ArrayState
import com.chilisaft.undroid.graphql.type.ContainerState
import com.chilisaft.undroid.graphql.type.ParityCheckStatus

/*
 * Schema-type -> app-model mappings shared by more than one repository (Dashboard and Main query
 * overlapping parts of the array; Dashboard and the Docker tab both list containers). One copy
 * each, so the two screens can't drift apart in how they label or judge the same server state.
 */

internal fun ArrayState.toLabel(): String = when (this) {
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

internal fun ParityCheckStatus.toLabel(): String = when (this) {
    ParityCheckStatus.NEVER_RUN -> "Never Run"
    ParityCheckStatus.RUNNING -> "Running"
    ParityCheckStatus.PAUSED -> "Paused"
    ParityCheckStatus.COMPLETED -> "Completed"
    ParityCheckStatus.CANCELLED -> "Cancelled"
    ParityCheckStatus.FAILED -> "Failed"
    ParityCheckStatus.UNKNOWN__ -> "Unknown"
}

/**
 * The server's `running`/`paused` booleans and its `status` enum are apparently derived
 * somewhat independently (confirmed against a real server: `status` correctly read RUNNING
 * while `running` was false/null in the same response) - ORing each boolean with the
 * matching enum value means either signal being right is enough, rather than trusting the
 * booleans alone and silently hiding an active check.
 */
internal fun ParityCheckStatusQuery.ParityCheckStatus.toParityCheckInfo() = ParityCheckInfo(
    statusLabel = status.toLabel(),
    running = (running ?: false) || status == ParityCheckStatus.RUNNING,
    paused = (paused ?: false) || status == ParityCheckStatus.PAUSED,
    progressPercent = progress,
    speed = speed,
    errors = errors,
    correcting = correcting
)

internal fun ContainerState.toDockerContainerState(): DockerContainerState = when (this) {
    ContainerState.RUNNING -> DockerContainerState.RUNNING
    ContainerState.PAUSED -> DockerContainerState.PAUSED
    ContainerState.EXITED -> DockerContainerState.EXITED
    ContainerState.UNKNOWN__ -> DockerContainerState.EXITED
}

internal fun ArrayDiskStatus.toLabel(): String = when (this) {
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

/**
 * `DISK_NP` is an *unassigned* slot (e.g. no second parity drive) - nothing is wrong with it, so
 * it must not turn the array's health red. Every other non-OK status (missing, disabled, wrong,
 * invalid, unknown...) is a real problem.
 */
internal fun ArrayDiskStatus?.toDeviceHealth(): DeviceHealth = when (this) {
    null, ArrayDiskStatus.DISK_OK -> DeviceHealth.OK
    ArrayDiskStatus.DISK_NP -> DeviceHealth.NOT_PRESENT
    else -> DeviceHealth.PROBLEM
}

/** Shared by the Dashboard's HEALTH badge and Main's array status line. */
internal fun Iterable<ArrayDiskStatus?>.allHealthy(): Boolean = none { it.toDeviceHealth() == DeviceHealth.PROBLEM }
