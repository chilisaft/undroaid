package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.ApiKeyInfo
import com.chilisaft.undroid.data.models.ArrayStatus
import com.chilisaft.undroid.data.models.ConnectInfo
import com.chilisaft.undroid.data.models.ConnectStatus
import com.chilisaft.undroid.data.models.DockerContainerSummary
import com.chilisaft.undroid.data.models.MetricsSample
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.data.models.ServerIdentity
import com.chilisaft.undroid.data.models.ServerSystem
import com.chilisaft.undroid.data.models.SystemMetrics
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.ApiKeyInfoQuery
import com.chilisaft.undroid.graphql.ArrayStatusQuery
import com.chilisaft.undroid.graphql.DockerStatusQuery
import com.chilisaft.undroid.graphql.MetricsPollQuery
import com.chilisaft.undroid.graphql.ParityCheckStatusQuery
import com.chilisaft.undroid.graphql.ServerConnectQuery
import com.chilisaft.undroid.graphql.ServerIdentityQuery
import com.chilisaft.undroid.graphql.ServerNameQuery
import com.chilisaft.undroid.graphql.ServerSystemQuery
import com.chilisaft.undroid.graphql.ServerVersionQuery
import com.chilisaft.undroid.graphql.SystemMetricsQuery
import com.chilisaft.undroid.graphql.type.RegistrationType
import com.chilisaft.undroid.graphql.type.Role
import com.chilisaft.undroid.graphql.type.ServerStatus
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

    suspend fun getServerIdentity(): WidgetResult<ServerIdentity> =
        apolloClient.runWidgetQuery(ServerIdentityQuery()) { data ->
            ServerIdentity(
                name = data.vars.name?.nonBlank(),
                comment = data.vars.comment?.nonBlank(),
                timeZone = data.vars.timeZone?.nonBlank(),
                licence = data.vars.regTy?.toLabel()
            )
        }

    suspend fun getServerSystem(): WidgetResult<ServerSystem> =
        apolloClient.runWidgetQuery(ServerSystemQuery()) { data -> data.info.toServerSystem() }

    /**
     * `server` is null when the server isn't signed in to Unraid Connect - reported as
     * [ConnectStatus.NEVER_CONNECTED] rather than as a failure.
     */
    suspend fun getConnectInfo(): WidgetResult<ConnectInfo> =
        apolloClient.runWidgetQuery(ServerConnectQuery()) { data ->
            val server = data.server
            ConnectInfo(
                status = when (server?.status) {
                    ServerStatus.ONLINE -> ConnectStatus.ONLINE
                    ServerStatus.OFFLINE -> ConnectStatus.OFFLINE
                    else -> ConnectStatus.NEVER_CONNECTED
                },
                owner = server?.owner?.username?.nonBlank(),
                lanIp = server?.lanip?.nonBlank(),
                wanIp = server?.wanip?.nonBlank(),
                localUrl = server?.localurl?.nonBlank(),
                remoteUrl = server?.remoteurl?.nonBlank()
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

    private fun ServerSystemQuery.Info.toServerSystem(): ServerSystem {
        val modules = memory.layout
        val memoryTypes = modules.mapNotNull { it.type?.nonBlank() }.distinct()
        return ServerSystem(
            unraidVersion = versions.core.unraid?.nonBlank(),
            apiVersion = versions.core.api?.nonBlank(),
            kernel = versions.core.kernel?.nonBlank(),
            bootTimeIso = os.uptime,
            cpu = cpuName(cpu.manufacturer, cpu.brand),
            cpuCores = cpu.cores,
            cpuThreads = cpu.threads,
            memoryTotalBytes = modules.mapNotNull { it.size.toLongOrNull()?.moduleSizeInBytes() }.takeIf { it.isNotEmpty() }?.sum(),
            memoryType = memoryTypes.singleOrNull(),
            motherboard = listOfNotNull(baseboard.manufacturer?.nonBlank(), baseboard.model?.nonBlank()).joinToString(" ").nonBlank()
        )
    }

    /**
     * The schema documents `MemoryLayout.size` as bytes, but a real server (Unraid 7.3.3) returned
     * kilobytes (2 x 16 GB modules -> 16777216 each). No module this app will meet is under 1 GiB,
     * so a value below 2^30 can only be KiB - accept both rather than trust either.
     */
    private fun Long.moduleSizeInBytes(): Long = if (this >= 1L shl 30) this else this * 1024

    /**
     * `brand` already includes the vendor and trademark symbols on real servers
     * ("Gen Intel® Core™ i3-12100" with manufacturer "Intel") - tidy it to "Intel Core i3-12100",
     * and only prefix the manufacturer when the brand doesn't name it.
     */
    private fun cpuName(manufacturer: String?, brand: String?): String? {
        val cleanBrand = brand
            ?.replace("®", "")?.replace("™", "")?.replace("(R)", "")?.replace("(TM)", "")
            ?.trim()?.removePrefix("Gen ")?.replace(Regex("\\s+"), " ")?.trim()
            ?.nonBlank()
        val vendor = manufacturer?.nonBlank()
        return when {
            cleanBrand == null -> vendor
            vendor == null || cleanBrand.contains(vendor, ignoreCase = true) -> cleanBrand
            else -> "$vendor $cleanBrand"
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

    private fun RegistrationType.toLabel(): String = when (this) {
        RegistrationType.BASIC -> "Basic"
        RegistrationType.PLUS -> "Plus"
        RegistrationType.PRO -> "Pro"
        RegistrationType.STARTER -> "Starter"
        RegistrationType.UNLEASHED -> "Unleashed"
        RegistrationType.LIFETIME -> "Lifetime"
        RegistrationType.INVALID -> "Invalid"
        RegistrationType.TRIAL -> "Trial"
        RegistrationType.UNKNOWN__ -> "Unknown"
    }

    private fun String.nonBlank(): String? = takeIf { it.isNotBlank() }

    private fun Role.toRoleLabel(): String = when (this) {
        Role.ADMIN -> "Admin"
        Role.CONNECT -> "Connect"
        Role.GUEST -> "Guest"
        Role.VIEWER -> "Viewer"
        Role.UNKNOWN__ -> "Unknown"
    }

}
