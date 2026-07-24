package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.ArrayDeviceRole
import com.chilisaft.undroaid.data.models.ArrayRunState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ArrayRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var repository: ArrayRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        repository = ArrayRepository(apolloClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun diskJson(idx: Int, name: String, device: String, type: String, status: String = "DISK_OK") = """
        {
          "__typename": "ArrayDisk",
          "id": "$idx", "idx": $idx, "name": "$name", "device": "$device", "size": "1953514584",
          "status": "$status", "rotational": true, "temp": 34, "numReads": "100", "numWrites": "200",
          "numErrors": "0", "fsSize": "1953000000", "fsFree": "900000000", "fsUsed": "1053000000",
          "type": "$type", "warning": 80, "critical": 95, "fsType": "xfs", "transport": "sata",
          "isSpinning": true
        }
    """.trimIndent()

    @Test
    fun `getArrayOverview maps state, capacity, and devices`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "state": "STARTED",
              "capacity": { "kilobytes": { "used": "1024", "total": "4096" } },
              "boot": ${diskJson(54, "flash", "/dev/sda", "BOOT")},
              "parities": [${diskJson(0, "parity", "/dev/sdb", "PARITY")}],
              "disks": [${diskJson(1, "disk1", "/dev/sdc", "DATA")}],
              "caches": [${diskJson(30, "cache", "/dev/sdd", "CACHE")}]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getArrayOverview()

        val overview = (result as WidgetResult.Success).data
        assertThat(overview.state).isEqualTo(ArrayRunState.STARTED)
        assertThat(overview.statusLabel).isEqualTo("Array Started")
        assertThat(overview.usedKb).isEqualTo(1024L)
        assertThat(overview.totalKb).isEqualTo(4096L)
        assertThat(overview.boot?.device).isEqualTo("/dev/sda")
        assertThat(overview.boot?.role).isEqualTo(ArrayDeviceRole.BOOT)
        assertThat(overview.parities).hasSize(1)
        assertThat(overview.parities[0].role).isEqualTo(ArrayDeviceRole.PARITY)
        assertThat(overview.disks).hasSize(1)
        assertThat(overview.disks[0].sizeKb).isEqualTo(1953514584L)
        assertThat(overview.disks[0].usedKb).isEqualTo(1053000000L)
        assertThat(overview.disks[0].statusLabel).isEqualTo("OK")
        assertThat(overview.caches).hasSize(1)
        assertThat(overview.caches[0].role).isEqualTo(ArrayDeviceRole.CACHE)
    }

    @Test
    fun `getArrayOverview reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getArrayOverview() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `getDiskIdentifications maps model, vendor, and serial`() = runBlocking {
        val json = """
        {
          "data": {
            "disks": [
              {
                "device": "/dev/sdc", "name": "WDC WD140EDGZ", "vendor": "Western Digital",
                "serialNum": "ABC123", "firmwareRevision": "1.0", "interfaceType": "SATA",
                "smartStatus": "OK"
              }
            ]
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getDiskIdentifications()

        val identifications = (result as WidgetResult.Success).data
        assertThat(identifications).hasSize(1)
        assertThat(identifications[0].device).isEqualTo("/dev/sdc")
        assertThat(identifications[0].model).isEqualTo("WDC WD140EDGZ")
        assertThat(identifications[0].vendor).isEqualTo("Western Digital")
        assertThat(identifications[0].serialNumber).isEqualTo("ABC123")
        assertThat(identifications[0].interfaceType).isEqualTo("SATA")
        assertThat(identifications[0].smartStatus).isEqualTo("OK")
    }

    @Test
    fun `getDiskIdentifications reports permission denied separately from other failures - a different resource than ARRAY`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getDiskIdentifications() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `getFlashInfo maps vendor and product`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"flash": {"vendor": "SanDisk", "product": "Ultra Fit"}}}"""))

        val result = repository.getFlashInfo()

        val flashInfo = (result as WidgetResult.Success).data
        assertThat(flashInfo.vendor).isEqualTo("SanDisk")
        assertThat(flashInfo.product).isEqualTo("Ultra Fit")
    }

    @Test
    fun `getFlashInfo reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getFlashInfo() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
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
    fun `getParityCheckStatus treats parity check as running when status says RUNNING even if the running flag itself is false or null`() = runBlocking {
        // Confirmed against a real server: the API's `running`/`paused` booleans and its `status`
        // enum can disagree - `status` correctly read RUNNING while `running` was false/null in
        // the same response. Trusting the booleans alone silently hides an active check.
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
    fun `getParityCheckStatus treats parity check as paused when status says PAUSED even if the paused flag itself is false`() = runBlocking {
        val json = """
        {
          "data": {
            "array": {
              "parityCheckStatus": {
                "status": "PAUSED", "progress": 42, "speed": null, "errors": 0,
                "correcting": false, "paused": false, "running": false
              }
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getParityCheckStatus()

        val parityCheck = (result as WidgetResult.Success).data
        assertThat(parityCheck.paused).isTrue()
        assertThat(parityCheck.running).isFalse()
    }

    @Test
    fun `getParityCheckStatus reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getParityCheckStatus() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `setArrayState sends START with the given password and succeeds`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"array": {"setState": {"state": "STARTED"}}}}"""))

        val result = repository.setArrayState(start = true, password = "hunter2")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
        val body = mockWebServer.takeRequest().body.readUtf8()
        assertThat(body).contains("START")
        assertThat(body).contains("hunter2")
    }

    @Test
    fun `setArrayState omits the password when blank`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"array": {"setState": {"state": "STOPPED"}}}}"""))

        repository.setArrayState(start = false, password = "")

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertThat(body).contains("STOP")
        assertThat(body).doesNotContain("decryptionPassword")
    }

    @Test
    fun `startParityCheck sends the correct flag and succeeds`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"parityCheck": {"start": {}}}}"""))

        val result = repository.startParityCheck(correct = true)

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
        val body = mockWebServer.takeRequest().body.readUtf8()
        assertThat(body).contains("\"correct\":true")
    }

    @Test
    fun `pauseParityCheck succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"parityCheck": {"pause": {}}}}"""))

        val result = repository.pauseParityCheck()

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `resumeParityCheck succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"parityCheck": {"resume": {}}}}"""))

        val result = repository.resumeParityCheck()

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `cancelParityCheck succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"parityCheck": {"cancel": {}}}}"""))

        val result = repository.cancelParityCheck()

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }
}
