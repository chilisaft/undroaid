package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.chilisaft.undroid.data.api.runWidgetMutation
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.DockerContainer
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.DockerLogLine
import com.chilisaft.undroid.data.models.DockerLogsPage
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.DockerContainerLogsQuery
import com.chilisaft.undroid.graphql.DockerContainersQuery
import com.chilisaft.undroid.graphql.DockerPauseContainerMutation
import com.chilisaft.undroid.graphql.DockerRestartContainerMutation
import com.chilisaft.undroid.graphql.DockerStartContainerMutation
import com.chilisaft.undroid.graphql.DockerStopContainerMutation
import com.chilisaft.undroid.graphql.DockerUnpauseContainerMutation
import com.chilisaft.undroid.graphql.type.ContainerState
import javax.inject.Inject

/** Backs the Docker tab's full container list + actions - see [ServerRepository] for the trimmed-down summary used on the Dashboard. */
class DockerRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getContainers(): WidgetResult<List<DockerContainer>> =
        apolloClient.runWidgetQuery(DockerContainersQuery()) { data -> data.docker.containers.map { it.toDockerContainer() } }

    suspend fun startContainer(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerStartContainerMutation(id)) { }

    suspend fun stopContainer(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerStopContainerMutation(id)) { }

    suspend fun pauseContainer(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerPauseContainerMutation(id)) { }

    suspend fun unpauseContainer(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerUnpauseContainerMutation(id)) { }

    suspend fun restartContainer(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerRestartContainerMutation(id)) { }

    /**
     * [tail] limits to the last N lines and is meant for the initial fetch; pass null on
     * follow-up polls once [since] (the previous response's cursor) narrows the query to just
     * new lines, otherwise every poll would re-fetch the same tail window.
     */
    suspend fun getLogs(id: String, tail: Int?, since: String?): WidgetResult<DockerLogsPage> =
        apolloClient.runWidgetQuery(
            DockerContainerLogsQuery(
                id = id,
                tail = tail?.let { Optional.Present(it) } ?: Optional.Absent,
                since = since?.let { Optional.Present(it) } ?: Optional.Absent
            )
        ) { data -> data.docker.logs.toDockerLogsPage() }

    private fun DockerContainerLogsQuery.Logs.toDockerLogsPage() = DockerLogsPage(
        lines = lines.map { DockerLogLine(timestamp = it.timestamp, message = it.message) },
        cursor = cursor
    )

    private fun DockerContainersQuery.Container.toDockerContainer() = DockerContainer(
        id = id,
        name = names.firstOrNull()?.removePrefix("/") ?: "Unknown",
        image = image,
        state = state.toDockerContainerState(),
        statusText = status,
        iconUrl = iconUrl,
        webUiUrl = webUiUrl
    )

    private fun ContainerState.toDockerContainerState(): DockerContainerState = when (this) {
        ContainerState.RUNNING -> DockerContainerState.RUNNING
        ContainerState.PAUSED -> DockerContainerState.PAUSED
        ContainerState.EXITED -> DockerContainerState.EXITED
        ContainerState.UNKNOWN__ -> DockerContainerState.EXITED
    }
}
