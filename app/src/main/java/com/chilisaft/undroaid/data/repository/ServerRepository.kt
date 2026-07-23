package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.api.runWidgetQuery
import com.chilisaft.undroaid.data.models.ArrayStatus
import com.chilisaft.undroaid.data.models.DockerContainerSummary
import com.chilisaft.undroaid.data.models.MetricsSample
import com.chilisaft.undroaid.data.models.SystemMetrics
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.graphql.ArrayStatusQuery
import com.chilisaft.undroaid.graphql.DockerStatusQuery
import com.chilisaft.undroaid.graphql.MetricsPollQuery
import com.chilisaft.undroaid.graphql.ServerNameQuery
import com.chilisaft.undroaid.graphql.SystemMetricsQuery
import com.chilisaft.undroaid.graphql.type.ArrayDiskStatus
import com.chilisaft.undroaid.graphql.type.ArrayState
import com.chilisaft.undroaid.graphql.type.ContainerState
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

    suspend fun getArrayStatus(): WidgetResult<ArrayStatus> =
        apolloClient.runWidgetQuery(ArrayStatusQuery()) { it.array.toArrayStatus() }

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
            totalKb = capacity.kilobytes.total.toLongOrNull()
        )
    }

    private fun DockerStatusQuery.Container.toDockerContainerSummary() = DockerContainerSummary(
        name = names.firstOrNull()?.removePrefix("/") ?: "Unknown",
        isRunning = state == ContainerState.RUNNING
    )

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
}
