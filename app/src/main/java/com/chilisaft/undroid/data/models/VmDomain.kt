package com.chilisaft.undroid.data.models

data class VmDomain(
    val id: String,
    val name: String,
    val state: VmRunState,
    val statusLabel: String,
    /** Collapses into [VmRunState.STOPPED] for action purposes, but is a fault rather than a normal stopped state, so status indicators need to tell it apart. */
    val crashed: Boolean = false
)

/** The schema's `VmState` has 8 values; everything but RUNNING/PAUSED collapses to STOPPED - same coarse 3-bucket action-availability split as [DockerContainerState]. */
enum class VmRunState {
    RUNNING, PAUSED, STOPPED
}
