package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.DockerContainerState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class DockerRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var repository: DockerRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        repository = DockerRepository(apolloClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getContainers maps names, image, state, status, icon, and web UI url`() = runBlocking {
        val json = """
        {
          "data": {
            "docker": {
              "containers": [
                { "id": "c1", "names": ["/plex"], "image": "plexinc/pms-docker:latest", "state": "RUNNING", "status": "Up 3 hours", "iconUrl": "https://example.com/plex.png", "webUiUrl": "http://tower:32400/web" },
                { "id": "c2", "names": ["/sonarr"], "image": "linuxserver/sonarr:latest", "state": "EXITED", "status": "Exited (0) 2 days ago", "iconUrl": null, "webUiUrl": null }
              ]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getContainers()

        val containers = (result as WidgetResult.Success).data
        assertThat(containers).hasSize(2)
        assertThat(containers[0].id).isEqualTo("c1")
        assertThat(containers[0].name).isEqualTo("plex")
        assertThat(containers[0].state).isEqualTo(DockerContainerState.RUNNING)
        assertThat(containers[0].iconUrl).isEqualTo("https://example.com/plex.png")
        assertThat(containers[0].webUiUrl).isEqualTo("http://tower:32400/web")
        assertThat(containers[1].name).isEqualTo("sonarr")
        assertThat(containers[1].state).isEqualTo(DockerContainerState.EXITED)
        assertThat(containers[1].iconUrl).isNull()
        assertThat(containers[1].webUiUrl).isNull()
    }

    @Test
    fun `getContainers reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getContainers() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `startContainer sends the given id and succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"docker": {"start": {"id": "c1"}}}}"""))

        val result = repository.startContainer("c1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
        assertThat(mockWebServer.takeRequest().body.readUtf8()).contains("\"c1\"")
    }

    @Test
    fun `stopContainer succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"docker": {"stop": {"id": "c1"}}}}"""))

        val result = repository.stopContainer("c1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `pauseContainer succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"docker": {"pause": {"id": "c1"}}}}"""))

        val result = repository.pauseContainer("c1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `unpauseContainer succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"docker": {"unpause": {"id": "c1"}}}}"""))

        val result = repository.unpauseContainer("c1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `restartContainer succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"docker": {"restart": {"id": "c1"}}}}"""))

        val result = repository.restartContainer("c1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `startContainer reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.startContainer("c1") as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }
}
