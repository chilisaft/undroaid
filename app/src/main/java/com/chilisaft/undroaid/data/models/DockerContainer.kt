package com.chilisaft.undroaid.data.models

data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val state: DockerContainerState,
    val statusText: String,
    val iconUrl: String?,
    val webUiUrl: String?
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
