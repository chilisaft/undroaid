package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.api.runWidgetQuery
import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.DockerContainerSummary
import com.chilisaft.undroaid.data.models.MetricsSample
import com.chilisaft.undroaid.data.models.ApiKeyInfo
import com.chilisaft.undroaid.data.models.ParityCheckInfo
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.graphql.ApiKeyInfoQuery
import com.chilisaft.undroaid.graphql.ArrayStatusQuery
import com.chilisaft.undroaid.graphql.DockerStatusQuery
import com.chilisaft.undroaid.graphql.MetricsPollQuery
import com.chilisaft.undroaid.graphql.ParityCheckStatusQuery
import com.chilisaft.undroaid.graphql.ServerNameQuery
import com.chilisaft.undroaid.graphql.ServerVersionQuery
import com.chilisaft.undroaid.graphql.SystemMetricsQuery
import com.chilisaft.undroaid.graphql.type.ArrayDiskStatus
import com.chilisaft.undroaid.graphql.type.ArrayState
import com.chilisaft.undroaid.graphql.type.ContainerState
import com.chilisaft.undroaid.graphql.type.ParityCheckStatus
import com.chilisaft.undroaid.graphql.type.Role
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Each dashboard widget is fetched with its own GraphQL operation so that a permission
 * failure on one Unraid resource (e.g. DOCKER) doesn't null out data the API key *does*
 * have access to. The query boundaries mirror the API's own `@usePermissions` resource
 * grouping (ARRAY / INFO / DOCKER / VARS), not just visual layout - `info` and `metrics`
 * are combined because the schema requires the same INFO permission for both. Notifications
 * live in [NotificationsRepository] - they're their own screen now, not a dashboard widget.
 */
class ServerRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getServerName(): WidgetResult<String?> =
        apolloClient.runWidgetQuery(ServerNameQuery()) { it.vars.name }

    suspend fun getServerVersion(): WidgetResult<String?> =
        apolloClient.runWidgetQuery(ServerVersionQuery()) { it.info.versions.core.unraid }

    /**
     * The schema's `me` field resolves to whatever identity is authenticating the request -
     * for an API key (this app's only auth method), that's the key itself, so `name` here is
     * the API key's name, not a human user account.
     */
    suspend fun getApiKeyInfo(): WidgetResult<ApiKeyInfo> =
        apolloClient.runWidgetQuery(ApiKeyInfoQuery()) { data ->
            ApiKeyInfo(name = data.me.name, roles = data.me.roles.map { it.toRoleLabel() })
        }

    suspend fun getArrayStatus(): WidgetResult<ArrayStatus> =
        apolloClient.runWidgetQuery(ArrayStatusQuery()) { it.array.toArrayStatus() }

    /** Independent from [getArrayStatus] - see the identical doc comment on `ArrayRepository.getParityCheckStatus()`. */
    suspend fun getParityCheckStatus(): WidgetResult<ParityCheckInfo> =
        apolloClient.runWidgetQuery(ParityCheckStatusQuery()) { data -> data.array.parityCheckStatus.toParityCheckInfo() }

    suspend fun getSystemMetrics(): WidgetResult<SystemMetrics> =
        apolloClient.runWidgetQuery(SystemMetricsQuery()) { data ->
            SystemMetrics(
                bootTimeIso = data.info.os.uptime,
                cpuLoadPercent = data.metrics.cpu?.percentTotal,
                memoryLoadPercent = data.metrics.memory?.percentTotal
            )
        }

    suspend fun getDockerContainers(): WidgetResult<List<DockerContainerSummary>> =
        apolloClient.runWidgetQuery(DockerStatusQuery()) { data -> data.docker.containers.map { it.toDockerContainerSummary() } }

    /**
     * Live CPU/memory load, polled at [intervalMillis] rather than pushed over a GraphQL
     * subscription. A subscription's update cadence is decided by the server, not the client,
     * so it can't guarantee a specific refresh rate - a plain timed poll of the same query
     * mechanism the rest of the dashboard already uses can. A failed tick is skipped (keeping
     * whatever was last shown) rather than stopping the loop.
     */
    fun observeSystemMetricsPoll(intervalMillis: Long = 1_000): Flow<MetricsSample> = flow {
        while (true) {
            val result = apolloClient.runWidgetQuery(MetricsPollQuery()) { data ->
                MetricsSample(
                    cpuLoadPercent = data.metrics.cpu?.percentTotal,
                    memoryLoadPercent = data.metrics.memory?.percentTotal
                )
            }
            if (result is WidgetResult.Success) {
                emit(result.data)
            }
            delay(intervalMillis)
        }
    }

    private fun ArrayStatusQuery.Array.toArrayStatus(): ArrayStatus {
        val diskStatuses = disks.map { it.status } + parities.map { it.status } + caches.map { it.status }
        return ArrayStatus(
            statusLabel = state.toLabel(),
            healthy = diskStatuses.isNotEmpty() && diskStatuses.all { it == ArrayDiskStatus.DISK_OK },
            usedKb = capacity.kilobytes.used.toLongOrNull(),
            totalKb = capacity.kilobytes.total.toLongOrNull(),
            paritySizeKb = parities.firstOrNull()?.size?.toLongOrNull()
        )
    }

    /** See the identical comment on `ArrayRepository.toParityCheckInfo()` - same fix, mirrored here for Dashboard's query. */
    private fun ParityCheckStatusQuery.ParityCheckStatus.toParityCheckInfo() = ParityCheckInfo(
        statusLabel = status.toLabel(),
        running = (running ?: false) || status == ParityCheckStatus.RUNNING,
        paused = (paused ?: false) || status == ParityCheckStatus.PAUSED,
        progressPercent = progress,
        speed = speed,
        errors = errors,
        correcting = correcting
    )

    private fun DockerStatusQuery.Container.toDockerContainerSummary() = DockerContainerSummary(
        name = names.firstOrNull()?.removePrefix("/") ?: "Unknown",
        state = state.toDockerContainerState(),
        iconUrl = iconUrl
    )

    private fun ContainerState.toDockerContainerState(): DockerContainerState = when (this) {
        ContainerState.RUNNING -> DockerContainerState.RUNNING
        ContainerState.PAUSED -> DockerContainerState.PAUSED
        ContainerState.EXITED -> DockerContainerState.EXITED
        ContainerState.UNKNOWN__ -> DockerContainerState.EXITED
    }

    private fun Role.toRoleLabel(): String = when (this) {
        Role.ADMIN -> "Admin"
        Role.CONNECT -> "Connect"
        Role.GUEST -> "Guest"
        Role.VIEWER -> "Viewer"
        Role.UNKNOWN__ -> "Unknown"
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

    private fun ParityCheckStatus.toLabel(): String = when (this) {
        ParityCheckStatus.NEVER_RUN -> "Never Run"
        ParityCheckStatus.RUNNING -> "Running"
        ParityCheckStatus.PAUSED -> "Paused"
        ParityCheckStatus.COMPLETED -> "Completed"
        ParityCheckStatus.CANCELLED -> "Cancelled"
        ParityCheckStatus.FAILED -> "Failed"
        ParityCheckStatus.UNKNOWN__ -> "Unknown"
    }
}
