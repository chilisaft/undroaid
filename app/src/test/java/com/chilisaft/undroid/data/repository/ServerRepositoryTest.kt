package com.chilisaft.undroid.data.repository

import com.chilisaft.undroid.data.models.TemperatureLevel
import com.chilisaft.undroid.data.models.SensorKind
import com.chilisaft.undroid.data.models.NetworkRate
import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.ConnectStatus
import com.chilisaft.undroid.data.models.LicenceDates
import com.chilisaft.undroid.data.models.LicenceProblem
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.ServerConnectQuery
import com.google.common.truth.Truth.assertThat
import java.time.Instant
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
    fun `getServerVersion returns the unraid core version`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"info": {"versions": {"core": {"unraid": "6.12.10"}}}}}"""))

        val result = repository.getServerVersion()

        assertThat(result).isEqualTo(WidgetResult.Success("6.12.10"))
    }

    @Test
    fun `getApiKeyInfo maps the key's name and roles`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"data": {"me": {"name": "undroid-app", "description": "", "roles": ["ADMIN", "CONNECT"]}}}"""
            )
        )

        val result = repository.getApiKeyInfo()

        val apiKeyInfo = (result as WidgetResult.Success).data
        assertThat(apiKeyInfo.name).isEqualTo("undroid-app")
        assertThat(apiKeyInfo.roles).containsExactly("Admin", "Connect").inOrder()
    }

    @Test
    fun `getApiKeyInfo reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getApiKeyInfo() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
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
              "parities": [{ "status": "DISK_OK", "size": "4294967296" }],
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
        assertThat(status.paritySizeKb).isEqualTo(4294967296L)
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
              "parities": [{ "status": "DISK_OK", "size": "4294967296" }],
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
    fun `getArrayStatus treats an unassigned slot (DISK_NP) as healthy, not a problem`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "state": "STARTED",
              "capacity": { "kilobytes": { "used": "1024", "total": "4096" } },
              "disks": [{ "status": "DISK_OK" }],
              "parities": [{ "status": "DISK_OK", "size": "4294967296" }, { "status": "DISK_NP", "size": "0" }],
              "caches": []
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getArrayStatus()

        assertThat((result as WidgetResult.Success).data.healthy).isTrue()
    }

    @Test
    fun `getParityCheckStatus maps status, progress, speed, and errors`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "parityCheckStatus": {
                "status": "RUNNING", "progress": 42, "speed": "150", "errors": 0,
                "correcting": false, "paused": false, "running": true
              }
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getParityCheckStatus()

        val parityCheck = (result as WidgetResult.Success).data
        assertThat(parityCheck.running).isTrue()
        assertThat(parityCheck.progressPercent).isEqualTo(42)
        assertThat(parityCheck.speed).isEqualTo("150")
        assertThat(parityCheck.statusLabel).isEqualTo("Running")
    }

    @Test
    fun `getParityCheckStatus treats parity check as running when status says RUNNING even if the running flag itself is false`() = runBlocking {
        // Confirmed against a real server: the API's `running`/`paused` booleans and its `status`
        // enum can disagree - `status` correctly read RUNNING while `running` was false/null in
        // the same response. Trusting the booleans alone silently hides an active check, which is
        // exactly the bug the user hit (Main tab showed "Running" text but no Pause/Stop buttons,
        // and the Dashboard's parity check banner never appeared at all).
        val json = """
        {
          "data": {
            "array": {
              "parityCheckStatus": {
                "status": "RUNNING", "progress": 42, "speed": "150", "errors": 0,
                "correcting": false, "paused": false, "running": false
              }
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getParityCheckStatus()

        val parityCheck = (result as WidgetResult.Success).data
        assertThat(parityCheck.running).isTrue()
        assertThat(parityCheck.paused).isFalse()
    }

    @Test
    fun `getParityCheckStatus reports never-run status with null fields`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "parityCheckStatus": {
                "status": "NEVER_RUN", "progress": null, "speed": null, "errors": null,
                "correcting": null, "paused": null, "running": null
              }
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getParityCheckStatus()

        val parityCheck = (result as WidgetResult.Success).data
        assertThat(parityCheck.statusLabel).isEqualTo("Never Run")
        assertThat(parityCheck.running).isFalse()
        assertThat(parityCheck.paused).isFalse()
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
    fun `getDockerContainers strips the leading slash from container names and maps status and icon url`() = runBlocking {
        val json = """
        {
          "data": {
            "docker": {
              "containers": [
                { "names": ["/Plex-Media-Server"], "state": "RUNNING", "status": "Up 3 hours (healthy)", "iconUrl": "https://example.com/plex.png" },
                { "names": ["/Home-Assistant"], "state": "EXITED", "status": "Created", "iconUrl": null },
                { "names": ["/qBittorrent"], "state": "PAUSED", "status": "Up 5 hours (Paused)", "iconUrl": null }
              ]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getDockerContainers()

        val containers = (result as WidgetResult.Success).data
        assertThat(containers).hasSize(3)
        assertThat(containers[0].name).isEqualTo("Plex-Media-Server")
        assertThat(containers[0].state).isEqualTo(DockerContainerState.RUNNING)
        assertThat(containers[0].iconUrl).isEqualTo("https://example.com/plex.png")
        assertThat(containers[0].statusText).isEqualTo("Up 3 hours (healthy)")
        assertThat(containers[1].statusText).isEqualTo("Created")
        assertThat(containers[1].name).isEqualTo("Home-Assistant")
        assertThat(containers[1].state).isEqualTo(DockerContainerState.EXITED)
        assertThat(containers[1].iconUrl).isNull()
        assertThat(containers[2].name).isEqualTo("qBittorrent")
        assertThat(containers[2].state).isEqualTo(DockerContainerState.PAUSED)
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

    @Test
    fun `getServerIdentity maps name, comment, time zone and licence, blanks as null`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vars": {"name": "TOWER", "comment": " ", "timeZone": "Europe/Zurich", "regTy": "PRO", "regState": "PRO"}}}"""))

        val identity = (repository.getServerIdentity() as WidgetResult.Success).data

        assertThat(identity.name).isEqualTo("TOWER")
        assertThat(identity.comment).isNull()
        assertThat(identity.timeZone).isEqualTo("Europe/Zurich")
        assertThat(identity.licence).isEqualTo("Pro")
        assertThat(identity.licenceProblem).isNull()
    }

    @Test
    fun `getServerIdentity reports Unraid's licence error states, grouped`() = runBlocking {
        val expected = mapOf(
            "EEXPIRED" to LicenceProblem.TRIAL_EXPIRED,
            "EGUID1" to LicenceProblem.WRONG_FLASH_DRIVE,
            "EBLACKLISTED2" to LicenceProblem.BLACKLISTED,
            "ENOFLASH4" to LicenceProblem.NO_FLASH_DRIVE,
            "ENOCONN" to LicenceProblem.NOT_VALIDATED,
            "BASIC" to null
        )
        expected.forEach { (state, problem) ->
            mockWebServer.enqueue(
                MockResponse().setBody("""{"data": {"vars": {"name": "T", "comment": null, "timeZone": null, "regTy": "TRIAL", "regState": "$state"}}}""")
            )
            val identity = (repository.getServerIdentity() as WidgetResult.Success).data
            assertThat(identity.licenceProblem).isEqualTo(problem)
        }
    }

    @Test
    fun `getLicenceDates treats a real Basic key's "0" and null as no dates`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody("""{"data": {"registration": {"expiration": "0", "updateExpiration": null}}}""")
        )

        val dates = (repository.getLicenceDates() as WidgetResult.Success).data

        assertThat(dates).isEqualTo(LicenceDates(expiresAt = null, updatesUntil = null))
    }

    @Test
    fun `getLicenceDates reads Unix seconds, milliseconds and ISO dates`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody("""{"data": {"registration": {"expiration": "1793232000", "updateExpiration": "1793232000000"}}}""")
        )
        val dates = (repository.getLicenceDates() as WidgetResult.Success).data
        assertThat(dates.expiresAt).isEqualTo(Instant.ofEpochSecond(1_793_232_000))
        assertThat(dates.updatesUntil).isEqualTo(Instant.ofEpochSecond(1_793_232_000))

        mockWebServer.enqueue(
            MockResponse().setBody("""{"data": {"registration": {"expiration": "2026-11-12T00:00:00Z", "updateExpiration": "soon"}}}""")
        )
        val isoDates = (repository.getLicenceDates() as WidgetResult.Success).data
        assertThat(isoDates.expiresAt).isEqualTo(Instant.parse("2026-11-12T00:00:00Z"))
        assertThat(isoDates.updatesUntil).isNull()
    }

    @Test
    fun `getLicenceDates copes with no registration at all`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"registration": null}}"""))

        val dates = (repository.getLicenceDates() as WidgetResult.Success).data

        assertThat(dates).isEqualTo(LicenceDates(expiresAt = null, updatesUntil = null))
    }

    @Test
    fun `getServerSystem combines cpu and board names and sums memory modules`() = runBlocking {
        val json = """
        {"data": {"info": {
          "os": {"uptime": "2026-10-10T08:00:00Z"},
          "versions": {"core": {"unraid": "7.3.3", "api": "4.20.0", "kernel": "6.12.24-Unraid"}, "packages": {"docker": "29.8.2"}},
          "cpu": {"manufacturer": "Intel", "brand": "Core i5-12400", "cores": 6, "threads": 12},
          "memory": {"layout": [{"size": "17179869184", "type": "DDR4"}, {"size": "17179869184", "type": "DDR4"}]},
          "baseboard": {"manufacturer": "ASUSTeK", "model": "PRIME B660M-A"}
        }}}
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val system = (repository.getServerSystem() as WidgetResult.Success).data

        assertThat(system.unraidVersion).isEqualTo("7.3.3")
        assertThat(system.apiVersion).isEqualTo("4.20.0")
        assertThat(system.dockerVersion).isEqualTo("29.8.2")
        assertThat(system.cpu).isEqualTo("Intel Core i5-12400")
        assertThat(system.cpuCores).isEqualTo(6)
        assertThat(system.cpuThreads).isEqualTo(12)
        assertThat(system.memoryTotalBytes).isEqualTo(34_359_738_368L)
        assertThat(system.memoryType).isEqualTo("DDR4")
        assertThat(system.motherboard).isEqualTo("ASUSTeK PRIME B660M-A")
    }

    @Test
    fun `getServerSystem copes with what a real server sends - KiB module sizes and a trademark-laden CPU brand`() = runBlocking {
        val json = """
        {"data": {"info": {
          "os": {"uptime": null},
          "versions": {"core": {"unraid": "7.3.3", "api": null, "kernel": null}, "packages": {"docker": " "}},
          "cpu": {"manufacturer": "Intel", "brand": "Gen Intel® Core™ i3-12100", "cores": 4, "threads": 8},
          "memory": {"layout": [{"size": "16777216", "type": "DDR4"}, {"size": "16777216", "type": "DDR4"}]},
          "baseboard": {"manufacturer": "ASRock", "model": "H610M-ITX/ac"}
        }}}
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val system = (repository.getServerSystem() as WidgetResult.Success).data

        assertThat(system.memoryTotalBytes).isEqualTo(34_359_738_368L) // 32 GiB
        assertThat(system.cpu).isEqualTo("Intel Core i3-12100")
        assertThat(system.dockerVersion).isNull()
    }

    @Test
    fun `getServerSystem leaves memory type empty when modules disagree, and tolerates missing parts`() = runBlocking {
        val json = """
        {"data": {"info": {
          "os": {"uptime": null},
          "versions": {"core": {"unraid": null, "api": null, "kernel": null}, "packages": null},
          "cpu": {"manufacturer": null, "brand": null, "cores": null, "threads": null},
          "memory": {"layout": [{"size": "8589934592", "type": "DDR4"}, {"size": "8589934592", "type": "DDR5"}]},
          "baseboard": {"manufacturer": null, "model": null}
        }}}
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val system = (repository.getServerSystem() as WidgetResult.Success).data

        assertThat(system.memoryTotalBytes).isEqualTo(17_179_869_184L)
        assertThat(system.memoryType).isNull()
        assertThat(system.cpu).isNull()
        assertThat(system.motherboard).isNull()
    }

    @Test
    fun `getConnectInfo maps an online server, blanks as null`() = runBlocking {
        val json = """
        {"data": {"server": {"status": "ONLINE", "lanip": "192.168.1.10", "wanip": "203.0.113.7",
          "localurl": "https://192-168-1-10.abc.myunraid.net", "remoteurl": "", "owner": {"username": "owner"}}}}
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val connect = (repository.getConnectInfo() as WidgetResult.Success).data

        assertThat(connect.status).isEqualTo(ConnectStatus.ONLINE)
        assertThat(connect.owner).isEqualTo("owner")
        assertThat(connect.wanIp).isEqualTo("203.0.113.7")
        assertThat(connect.remoteUrl).isNull()
    }

    @Test
    fun `getConnectInfo treats a missing server (never signed in to Connect) as not set up, not as an error`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"server": null}}"""))

        val connect = (repository.getConnectInfo() as WidgetResult.Success).data

        assertThat(connect.status).isEqualTo(ConnectStatus.NEVER_CONNECTED)
        assertThat(connect.lanIp).isNull()
    }

    @Test
    fun `the Connect query never asks for the server's API key`() {
        val query = ServerConnectQuery().document()

        assertThat(query).doesNotContain("apikey")
    }

    @Test
    fun `getTemperatures maps sensors and the server's counts`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"data": {"metrics": {"temperature": {
                    "summary": {"warningCount": 1, "criticalCount": 0},
                    "sensors": [
                        {"id": "nvme:Composite", "name": "nvme-pci-0200 Composite", "type": "NVME",
                         "current": {"value": 72.5, "unit": "CELSIUS", "status": "WARNING"}, "warning": 70, "critical": 85},
                        {"id": "coretemp:Package", "name": "coretemp-isa-0000 Package id 0", "type": "CPU_PACKAGE",
                         "current": {"value": 42, "unit": "CELSIUS", "status": "NORMAL"}, "warning": 70, "critical": 85}
                    ]}}}}"""
            )
        )

        val overview = (repository.getTemperatures() as WidgetResult.Success).data

        assertThat(overview.warningCount).isEqualTo(1)
        assertThat(overview.sensors[0].level).isEqualTo(TemperatureLevel.WARNING)
        assertThat(overview.headline?.kind).isEqualTo(SensorKind.CPU_PACKAGE)
        assertThat(overview.headline?.unitSymbol).isEqualTo("°C")
    }

    @Test
    fun `getTemperatures treats a missing temperature block as no sensors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"metrics": {"temperature": null}}}"""))

        val overview = (repository.getTemperatures() as WidgetResult.Success).data

        assertThat(overview.sensors).isEmpty()
        assertThat(overview.headline).isNull()
    }

    private val networkBody = """{"data": {"metrics": {"network": [
        {"name": "lo", "operstate": "unknown", "rxSec": 0, "txSec": 0},
        {"name": "veth2b565d5", "operstate": "up", "rxSec": 1079.8, "txSec": 4371.9},
        {"name": "br0", "operstate": "up", "rxSec": 1053.2, "txSec": 3178.7},
        {"name": "eth0", "operstate": "up", "rxSec": 900.0, "txSec": 3000.0}
    ]}}}"""

    @Test
    fun `getNetworkRate uses the preferred interface when listed`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(networkBody))

        val rate = (repository.getNetworkRate("eth0") as WidgetResult.Success).data

        assertThat(rate).isEqualTo(NetworkRate("eth0", 900.0, 3000.0))
    }

    @Test
    fun `getNetworkRate falls back to br0 without a known primary interface`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(networkBody))

        val rate = (repository.getNetworkRate(null) as WidgetResult.Success).data

        assertThat(rate?.interfaceName).isEqualTo("br0")
    }

    @Test
    fun `pickMainInterface prefers br0, then a bond, then ethernet, never virtual interfaces`() {
        assertThat(pickMainInterface(listOf("veth1", "eth0", "bond0", "br0"))).isEqualTo("br0")
        assertThat(pickMainInterface(listOf("veth1", "eth0", "bond0"))).isEqualTo("bond0")
        assertThat(pickMainInterface(listOf("br-18191748cb07", "eth1"))).isEqualTo("eth1")
        assertThat(pickMainInterface(listOf("lo", "veth1", "tailscale1"))).isNull()
    }
}
