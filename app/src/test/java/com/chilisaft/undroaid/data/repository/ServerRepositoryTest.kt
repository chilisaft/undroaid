package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.Owner
import com.chilisaft.undroaid.data.models.Server
import com.chilisaft.undroaid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ServerRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var storage: Storage
    private lateinit var repository: ServerRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        storage = mockk(relaxed = true)
        repository = ServerRepository(apolloClient, storage)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getServerInformation returns success on valid response`() = runBlocking {
        val jsonResponse = """ 
        {
          "data": {
            "server": {
              "owner": {
                "userId": "1",
                "username": "testuser",
                "url": "http://test.com",
                "avatar": "avatar.png"
              },
              "guid": "1234",
              "apikey": "apikey",
              "name": "Test Server",
              "wanip": "1.1.1.1",
              "lanip": "192.168.1.1",
              "localurl": "http://local.test",
              "remoteurl": "http://remote.test"
            }
          }
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse))

        val result = repository.getServerInformation()

        assertThat(result.isSuccess).isTrue()
        val server = result.getOrNull()
        assertThat(server).isNotNull()
        assertThat(server?.name).isEqualTo("Test Server")
        assertThat(server?.owner?.username).isEqualTo("testuser")
    }

    @Test
    fun `getServerInformation returns failure on graphql error`() = runBlocking {
        val errorResponse = """
        {
          "errors": [{"message": "Test GraphQL Error"}]
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(errorResponse))

        val result = repository.getServerInformation()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Test GraphQL Error")
    }
}
