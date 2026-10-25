package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.ArrayStatus
import com.chilisaft.undroid.data.models.DockerContainerSummary
import com.chilisaft.undroid.data.models.MetricsSample
import com.chilisaft.undroid.data.models.ApiKeyInfo
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.SystemMetrics
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.ApiKeyInfoQuery
import com.chilisaft.undroid.graphql.ArrayStatusQuery
import com.chilisaft.undroid.graphql.DockerStatusQuery
import com.chilisaft.undroid.graphql.MetricsPollQuery
import com.chilisaft.undroid.graphql.ParityCheckStatusQuery
import com.chilisaft.undroid.graphql.ServerNameQuery
import com.chilisaft.undroid.graphql.ServerVersionQuery
import com.chilisaft.undroid.graphql.SystemMetricsQuery
import com.chilisaft.undroid.graphql.type.Role
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
            healthy = diskStatuses.isNotEmpty() && diskStatuses.allHealthy(),
            usedKb = capacity.kilobytes.used.toLongOrNull(),
            totalKb = capacity.kilobytes.total.toLongOrNull(),
            paritySizeKb = parities.firstOrNull()?.size?.toLongOrNull()
        )
    }

    private fun DockerStatusQuery.Container.toDockerContainerSummary() = DockerContainerSummary(
        name = names.firstOrNull()?.removePrefix("/") ?: "Unknown",
        state = state.toDockerContainerState(),
        iconUrl = iconUrl
    )

    private fun Role.toRoleLabel(): String = when (this) {
        Role.ADMIN -> "Admin"
        Role.CONNECT -> "Connect"
        Role.GUEST -> "Guest"
        Role.VIEWER -> "Viewer"
        Role.UNKNOWN__ -> "Unknown"
    }

}
