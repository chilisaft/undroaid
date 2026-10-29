package com.chilisaft.undroid.data.models

data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val state: DockerContainerState,
    val statusText: String,
    val iconUrl: String?,
    val webUiUrl: String?,
    /**
     * Only true when Unraid knows a newer image exists. False also covers "unknown" - on a real
     * server `isUpdateAvailable` was null for containers Unraid doesn't manage (e.g. Nextcloud
     * AIO's helpers, compose sidecars), and those can't be offered an update either way.
     */
    val updateAvailable: Boolean = false
)

enum class DockerContainerState {
    RUNNING, PAUSED, EXITED
}

data class DockerLogLine(
    val timestamp: String,
    val message: String
)

data class DockerLogsPage(
    val lines: List<DockerLogLine>,
    val cursor: String?
)
