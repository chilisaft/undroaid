package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.chilisaft.undroid.data.api.runWidgetMutation
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.DockerContainer
import com.chilisaft.undroid.data.models.DockerLogLine
import com.chilisaft.undroid.data.models.DockerLogsPage
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.DockerContainerLogsQuery
import com.chilisaft.undroid.graphql.DockerContainersQuery
import com.chilisaft.undroid.graphql.DockerCheckForUpdatesMutation
import com.chilisaft.undroid.graphql.DockerPauseContainerMutation
import com.chilisaft.undroid.graphql.DockerRestartContainerMutation
import com.chilisaft.undroid.graphql.DockerStartContainerMutation
import com.chilisaft.undroid.graphql.DockerStopContainerMutation
import com.chilisaft.undroid.graphql.DockerUnpauseContainerMutation
import com.chilisaft.undroid.graphql.DockerUpdateContainerMutation
import com.chilisaft.undroid.graphql.DockerUpdateContainersMutation
import javax.inject.Inject

/** Backs the Docker tab's full container list + actions - see [ServerRepository] for the trimmed-down summary used on the Dashboard. */
class DockerRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getContainers(): WidgetResult<List<DockerContainer>> =
        apolloClient.runWidgetQuery(DockerContainersQuery()) { data -> data.docker!!.containers.map { it.toDockerContainer() } }

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

    /** Asks the registries for the latest image digests - afterwards [getContainers] reports fresh update flags. */
    suspend fun checkForUpdates(): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerCheckForUpdatesMutation(), readTimeoutSeconds = CHECK_TIMEOUT_SECONDS) { }

    /** Pulls the latest image and recreates the container - which gives it a new id. */
    suspend fun updateContainer(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerUpdateContainerMutation(id), readTimeoutSeconds = UPDATE_TIMEOUT_SECONDS) { }

    suspend fun updateContainers(ids: List<String>): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(DockerUpdateContainersMutation(ids), readTimeoutSeconds = UPDATE_TIMEOUT_SECONDS) { }

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
        ) { data -> data.docker!!.logs.toDockerLogsPage() }

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
        webUiUrl = webUiUrl,
        updateAvailable = isUpdateAvailable == true
    )

    private companion object {
        const val CHECK_TIMEOUT_SECONDS = 60
        // Pulling images can take minutes - long enough for any realistic update, while a server
        // that has truly stopped answering still fails eventually.
        const val UPDATE_TIMEOUT_SECONDS = 30 * 60
    }
}
