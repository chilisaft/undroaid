package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.NetworkSummary
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.ServerNetworkQuery
import com.chilisaft.undroid.utils.Storage
import javax.inject.Inject

/** A previously fetched primary-network result; [network] is null when the server reported no primary interface. */
data class CachedNetwork(val network: NetworkSummary?, val fetchedAtMillis: Long)

/**
 * The Server screen's network section. `info.primaryNetwork` is slow on a real server (~2 s on
 * Unraid 7.3.x, against well under one for the rest of `info`) - and the API answers nothing else
 * while it runs, so every request sent alongside it waits too. The last result is therefore
 * cached: the screen shows [cached] instantly and refetches only after its other sections have
 * loaded, and only when [isStale]. Deliberately not warmed at app start - it would stall the
 * Dashboard's first queries. The cache is tied to the server URL it was fetched from, so logging
 * in to a different server never shows the old server's interface.
 */
class ServerNetworkRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val storage: Storage
) {

    fun cached(): CachedNetwork? {
        val serverUrl = storage.serverUrl ?: return null
        return storage.serverNetworkCache?.let { decode(it, serverUrl) }
    }

    /** Fetches the primary interface and caches a success; a permission failure drops the cache. */
    suspend fun fetch(): WidgetResult<NetworkSummary?> {
        val result = apolloClient.runWidgetQuery(ServerNetworkQuery()) { data ->
            data.info.primaryNetwork?.let { NetworkSummary(it.name, it.ipAddress?.takeIf(String::isNotBlank), it.speed?.takeIf { speed -> speed > 0 }) }
        }
        when {
            result is WidgetResult.Success -> storage.serverUrl?.let { serverUrl ->
                storage.serverNetworkCache = encode(serverUrl, CachedNetwork(result.data, System.currentTimeMillis()))
            }
            result is WidgetResult.Failure && result.permissionDenied -> storage.serverNetworkCache = null
        }
        return result
    }

    /** True when there's no cache or it's older than [maxAgeMillis]. */
    fun isStale(maxAgeMillis: Long): Boolean {
        val cached = cached() ?: return true
        return System.currentTimeMillis() - cached.fetchedAtMillis > maxAgeMillis
    }

    // One field per line: server URL, fetch time, then interface, IP, speed (empty = absent; an
    // empty interface means no primary interface). None of these can contain a line break.
    private fun encode(serverUrl: String, cached: CachedNetwork): String = listOf(
        serverUrl,
        cached.fetchedAtMillis.toString(),
        cached.network?.interfaceName.orEmpty(),
        cached.network?.ipAddress.orEmpty(),
        cached.network?.speedMbps?.toString().orEmpty()
    ).joinToString("\n")

    private fun decode(encoded: String, currentServerUrl: String): CachedNetwork? {
        val fields = encoded.split("\n")
        if (fields.size != 5 || fields[0] != currentServerUrl) return null
        val fetchedAt = fields[1].toLongOrNull() ?: return null
        val network = fields[2].takeIf { it.isNotEmpty() }?.let { name ->
            NetworkSummary(name, fields[3].takeIf { it.isNotEmpty() }, fields[4].toIntOrNull())
        }
        return CachedNetwork(network, fetchedAt)
    }
}
