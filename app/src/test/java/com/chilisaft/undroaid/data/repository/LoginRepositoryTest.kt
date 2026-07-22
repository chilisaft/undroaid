package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoginRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var storage: Storage
    private lateinit var repository: LoginRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        storage = mockk {
            every { serverUrl = any() } returns Unit
            every { apiToken = any() } returns Unit
        }
        repository = LoginRepository(apolloClient, storage)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `login success`() = runBlocking {
        val login = Login("http://test.com", "token")
        val successResponse = """
        {
          "data": {
            "server": {
              "guid": "1234",
              "name": "Test Server"
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(successResponse))

        val result = repository.login(login)

        assertThat(result.isSuccess).isTrue()
        verify { storage.serverUrl = login.serverUrl }
        verify { storage.apiToken = login.apiToken }
    }

    @Test
    fun `login failure due to graphql error`() = runBlocking {
        val login = Login("http://test.com", "token")
        val errorResponse = """
        {
          "errors": [{"message": "Invalid credentials"}]
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(errorResponse))

        val result = repository.login(login)

        assertThat(result.isFailure).isTrue()
        // Credentials are only persisted on success; a failed attempt must leave storage untouched.
        verify(exactly = 0) { storage.serverUrl = any() }
        verify(exactly = 0) { storage.apiToken = any() }
    }
}