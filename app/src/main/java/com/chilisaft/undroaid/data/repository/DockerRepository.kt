package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.api.runWidgetMutation
import com.chilisaft.undroaid.data.api.runWidgetQuery
import com.chilisaft.undroaid.data.models.DockerContainer
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.graphql.DockerContainersQuery
import com.chilisaft.undroaid.graphql.DockerPauseContainerMutation
import com.chilisaft.undroaid.graphql.DockerStartContainerMutation
import com.chilisaft.undroaid.graphql.DockerStopContainerMutation
import com.chilisaft.undroaid.graphql.DockerUnpauseContainerMutation
import com.chilisaft.undroaid.graphql.type.ContainerState
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
