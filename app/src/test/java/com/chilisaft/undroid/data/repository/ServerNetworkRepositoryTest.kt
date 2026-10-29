package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.NetworkSummary
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ServerNetworkRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ServerNetworkRepository

    // In-memory stand-ins for the two Storage values the repository uses.
    private var serverUrl: String? = "http://192.168.69.7"
    private var cache: String? = null

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        val apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        val storage = mockk<Storage> {
            every { serverUrl } answers { this@ServerNetworkRepositoryTest.serverUrl }
            every { serverNetworkCache } answers { cache }
            every { serverNetworkCache = any() } answers { cache = firstArg() }
        }
        repository = ServerNetworkRepository(apolloClient, storage)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun enqueueNetwork(networkJson: String) {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"info": {"primaryNetwork": $networkJson}}}"""))
    }

    @Test
    fun `fetch maps the primary interface and caches it`() = runBlocking {
        enqueueNetwork("""{"name": "br0", "ipAddress": "192.168.69.7", "speed": 1000}""")

        val result = repository.fetch()

        val expected = NetworkSummary("br0", "192.168.69.7", 1000)
        assertThat(result).isEqualTo(WidgetResult.Success(expected))
        assertThat(repository.cached()?.network).isEqualTo(expected)
    }

    @Test
    fun `a missing speed or IP survives the cache round trip as null`() = runBlocking {
        enqueueNetwork("""{"name": "br0", "ipAddress": "", "speed": null}""")

        repository.fetch()

        assertThat(repository.cached()?.network).isEqualTo(NetworkSummary("br0", null, null))
    }

    @Test
    fun `no primary interface is cached too, as a null network`() = runBlocking {
        enqueueNetwork("null")

        repository.fetch()

        val cached = repository.cached()
        assertThat(cached).isNotNull()
        assertThat(cached?.network).isNull()
    }

    @Test
    fun `the cache is ignored after logging in to a different server`() = runBlocking {
        enqueueNetwork("""{"name": "br0", "ipAddress": "192.168.69.7", "speed": 1000}""")
        repository.fetch()

        serverUrl = "http://10.0.0.5"

        assertThat(repository.cached()).isNull()
    }

    @Test
    fun `a transient failure keeps the cache, a permission failure drops it`() = runBlocking {
        enqueueNetwork("""{"name": "br0", "ipAddress": "192.168.69.7", "speed": 1000}""")
        repository.fetch()

        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        repository.fetch()
        assertThat(repository.cached()).isNotNull()

        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))
        repository.fetch()
        assertThat(repository.cached()).isNull()
    }

    @Test
    fun `isStale is true without a cache and once the cache outlives the given age`() = runBlocking {
        assertThat(repository.isStale(maxAgeMillis = 60_000)).isTrue()

        enqueueNetwork("""{"name": "br0", "ipAddress": "192.168.69.7", "speed": 1000}""")
        repository.fetch()

        assertThat(repository.isStale(maxAgeMillis = 60_000)).isFalse()
        assertThat(repository.isStale(maxAgeMillis = -1)).isTrue()
    }

    @Test
    fun `a corrupt cache entry reads as no cache`() {
        cache = "garbage"

        assertThat(repository.cached()).isNull()
    }
}
