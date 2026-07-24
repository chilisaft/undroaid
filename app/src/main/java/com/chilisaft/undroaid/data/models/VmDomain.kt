package com.chilisaft.undroaid.data.models

data class VmDomain(
    val id: String,
    val name: String,
    val state: VmRunState,
    val statusLabel: String
)

/** The schema's `VmState` has 8 values; everything but RUNNING/PAUSED collapses to STOPPED - same coarse 3-bucket action-availability split as [DockerContainerState]. */
enum class VmRunState {
    RUNNING, PAUSED, STOPPED
}
