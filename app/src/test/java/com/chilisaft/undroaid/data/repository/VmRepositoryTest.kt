package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.VmRunState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class VmRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var repository: VmRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        repository = VmRepository(apolloClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getVms maps id, name, and state`() = runBlocking {
        val json = """
        {
          "data": {
            "vms": {
              "domains": [
                { "id": "v1", "name": "Windows 11", "state": "RUNNING" },
                { "id": "v2", "name": "Ubuntu Server", "state": "SHUTOFF" },
                { "id": "v3", "name": "macOS", "state": "PAUSED" }
              ]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getVms()

        val vms = (result as WidgetResult.Success).data
        assertThat(vms).hasSize(3)
        assertThat(vms[0].id).isEqualTo("v1")
        assertThat(vms[0].name).isEqualTo("Windows 11")
        assertThat(vms[0].state).isEqualTo(VmRunState.RUNNING)
        assertThat(vms[0].statusLabel).isEqualTo("Running")
        assertThat(vms[1].state).isEqualTo(VmRunState.STOPPED)
        assertThat(vms[1].statusLabel).isEqualTo("Shut Off")
        assertThat(vms[2].state).isEqualTo(VmRunState.PAUSED)
        assertThat(vms[2].statusLabel).isEqualTo("Paused")
    }

    @Test
    fun `getVms treats every non-running, non-paused state as STOPPED`() = runBlocking {
        val json = """
        {
          "data": {
            "vms": {
              "domains": [
                { "id": "v1", "name": "a", "state": "NOSTATE" },
                { "id": "v2", "name": "b", "state": "IDLE" },
                { "id": "v3", "name": "c", "state": "SHUTDOWN" },
                { "id": "v4", "name": "d", "state": "CRASHED" },
                { "id": "v5", "name": "e", "state": "PMSUSPENDED" }
              ]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getVms()

        val vms = (result as WidgetResult.Success).data
        assertThat(vms.map { it.state }).containsExactly(
            VmRunState.STOPPED, VmRunState.STOPPED, VmRunState.STOPPED, VmRunState.STOPPED, VmRunState.STOPPED
        ).inOrder()
    }

    @Test
    fun `getVms tolerates a null domain name`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vms": {"domains": [{"id": "v1", "name": null, "state": "RUNNING"}]}}}"""))

        val result = repository.getVms()

        assertThat((result as WidgetResult.Success).data[0].name).isEqualTo("Unnamed VM")
    }

    @Test
    fun `getVms reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getVms() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `startVm sends the given id and succeeds`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vm": {"start": true}}}"""))

        val result = repository.startVm("v1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
        assertThat(mockWebServer.takeRequest().body.readUtf8()).contains("\"v1\"")
    }

    @Test
    fun `stopVm succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vm": {"stop": true}}}"""))

        val result = repository.stopVm("v1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `forceStopVm succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vm": {"forceStop": true}}}"""))

        val result = repository.forceStopVm("v1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `pauseVm succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vm": {"pause": true}}}"""))

        val result = repository.pauseVm("v1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `resumeVm succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vm": {"resume": true}}}"""))

        val result = repository.resumeVm("v1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `rebootVm succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vm": {"reboot": true}}}"""))

        val result = repository.rebootVm("v1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `startVm reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.startVm("v1") as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }
}
