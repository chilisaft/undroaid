package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ServerRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var repository: ServerRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        repository = ServerRepository(apolloClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getServerName returns the hostname`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vars": {"name": "TOWER"}}}"""))

        val result = repository.getServerName()

        assertThat(result).isEqualTo(WidgetResult.Success("TOWER"))
    }

    @Test
    fun `getArrayStatus maps a healthy array`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "state": "STARTED",
              "capacity": { "kilobytes": { "used": "1024", "total": "4096" } },
              "disks": [{ "status": "DISK_OK" }],
              "parities": [{ "status": "DISK_OK" }],
              "caches": [{ "status": "DISK_OK" }]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getArrayStatus()

        val status = (result as WidgetResult.Success).data
        assertThat(status.statusLabel).isEqualTo("Array Started")
        assertThat(status.healthy).isTrue()
        assertThat(status.usedKb).isEqualTo(1024L)
        assertThat(status.totalKb).isEqualTo(4096L)
    }

    @Test
    fun `getArrayStatus flags array as unhealthy when a disk is not OK`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "state": "STARTED",
              "capacity": { "kilobytes": { "used": "1024", "total": "4096" } },
              "disks": [{ "status": "DISK_DSBL" }],
              "parities": [{ "status": "DISK_OK" }],
              "caches": []
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getArrayStatus()

        assertThat((result as WidgetResult.Success).data.healthy).isFalse()
    }

    @Test
    fun `getArrayStatus reports permission denied separately from other failures`() = runBlocking {
        val json = """
        {
          "errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getArrayStatus() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `getArrayStatus reports non-permission failures without the permission flag`() = runBlocking {
        val json = """
        {
          "errors": [{"message": "Internal server error"}]
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getArrayStatus() as WidgetResult.Failure

        assertThat(result.permissionDenied).isFalse()
        assertThat(result.message).contains("Internal server error")
    }

    @Test
    fun `getSystemMetrics tolerates null cpu and memory`() = runBlocking {
        val json = """
        {
          "data": {
            "info": { "os": { "uptime": null } },
            "metrics": { "cpu": null, "memory": null }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getSystemMetrics()

        val metrics = (result as WidgetResult.Success).data
        assertThat(metrics.bootTimeIso).isNull()
        assertThat(metrics.cpuLoadPercent).isNull()
        assertThat(metrics.memoryLoadPercent).isNull()
    }

    @Test
    fun `getSystemMetrics maps cpu and memory percentages`() = runBlocking {
        val json = """
        {
          "data": {
            "info": { "os": { "uptime": "2024-01-01T00:00:00Z" } },
            "metrics": { "cpu": { "percentTotal": 24.5 }, "memory": { "percentTotal": 58.2 } }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getSystemMetrics()

        val metrics = (result as WidgetResult.Success).data
        assertThat(metrics.bootTimeIso).isEqualTo("2024-01-01T00:00:00Z")
        assertThat(metrics.cpuLoadPercent).isEqualTo(24.5)
        assertThat(metrics.memoryLoadPercent).isEqualTo(58.2)
    }

    @Test
    fun `getDockerContainers strips the leading slash from container names and maps the icon url`() = runBlocking {
        val json = """
        {
          "data": {
            "docker": {
              "containers": [
                { "names": ["/Plex-Media-Server"], "state": "RUNNING", "iconUrl": "https://example.com/plex.png" },
                { "names": ["/Home-Assistant"], "state": "EXITED", "iconUrl": null }
              ]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getDockerContainers()

        val containers = (result as WidgetResult.Success).data
        assertThat(containers).hasSize(2)
        assertThat(containers[0].name).isEqualTo("Plex-Media-Server")
        assertThat(containers[0].isRunning).isTrue()
        assertThat(containers[0].iconUrl).isEqualTo("https://example.com/plex.png")
        assertThat(containers[1].name).isEqualTo("Home-Assistant")
        assertThat(containers[1].isRunning).isFalse()
        assertThat(containers[1].iconUrl).isNull()
    }

    @Test
    fun `getDockerContainers reports permission denied`() = runBlocking {
        val json = """
        {
          "errors": [{"message": "You do not have permission to view Docker containers"}]
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getDockerContainers() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `a network failure on one widget is reported as a non-permission failure`() = runBlocking {
        mockWebServer.shutdown()

        val result = repository.getDockerContainers() as WidgetResult.Failure

        assertThat(result.permissionDenied).isFalse()
    }

    @Test
    fun `observeSystemMetricsPoll emits a new reading on every tick`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"metrics": {"cpu": {"percentTotal": 10.0}, "memory": {"percentTotal": 20.0}}}}"""))
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"metrics": {"cpu": {"percentTotal": 30.0}, "memory": {"percentTotal": 40.0}}}}"""))

        val samples = withTimeout(5_000) {
            repository.observeSystemMetricsPoll(intervalMillis = 10).take(2).toList()
        }

        assertThat(samples).hasSize(2)
        assertThat(samples[0].cpuLoadPercent).isEqualTo(10.0)
        assertThat(samples[0].memoryLoadPercent).isEqualTo(20.0)
        assertThat(samples[1].cpuLoadPercent).isEqualTo(30.0)
        assertThat(samples[1].memoryLoadPercent).isEqualTo(40.0)
    }

    @Test
    fun `observeSystemMetricsPoll skips a failed tick and keeps polling`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "boom"}]}"""))
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"metrics": {"cpu": {"percentTotal": 50.0}, "memory": {"percentTotal": 60.0}}}}"""))

        val sample = withTimeout(5_000) {
            repository.observeSystemMetricsPoll(intervalMillis = 10).first()
        }

        assertThat(sample.cpuLoadPercent).isEqualTo(50.0)
        assertThat(sample.memoryLoadPercent).isEqualTo(60.0)
    }
}
