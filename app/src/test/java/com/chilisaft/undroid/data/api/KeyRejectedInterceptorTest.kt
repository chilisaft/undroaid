package com.chilisaft.undroid.data.api

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.graphql.TestLoginQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class KeyRejectedInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiKeyStatus: ApiKeyStatus
    private lateinit var apolloClient: ApolloClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apiKeyStatus = ApiKeyStatus()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .addInterceptor(KeyRejectedInterceptor(apiKeyStatus))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // What Unraid 7.3.4 sends for an invalid key: HTTP 200 with an UNAUTHENTICATED error.
    private val invalidKeyBody = """
        {"errors": [{"message": "Invalid API key format", "path": ["me"],
          "extensions": {"code": "UNAUTHENTICATED", "originalError": {"message": "Invalid API key format", "error": "Unauthorized", "statusCode": 401}}}],
         "data": null}
    """.trimIndent()

    @Test
    fun `an UNAUTHENTICATED error reports the key as rejected`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(invalidKeyBody))

        apolloClient.query(TestLoginQuery()).execute()

        assertThat(apiKeyStatus.rejected.value).isTrue()
    }

    @Test
    fun `an HTTP 401 reports the key as rejected`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        runCatching { apolloClient.query(TestLoginQuery()).execute() }

        assertThat(apiKeyStatus.rejected.value).isTrue()
    }

    @Test
    fun `a missing permission does not report the key as rejected`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"errors": [{"message": "Forbidden resource", "extensions": {"code": "FORBIDDEN"}}], "data": null}"""
            )
        )

        apolloClient.query(TestLoginQuery()).execute()

        assertThat(apiKeyStatus.rejected.value).isFalse()
    }

    @Test
    fun `a login check with a key that isn't saved yet is ignored`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(invalidKeyBody))

        apolloClient.query(TestLoginQuery())
            .addHttpHeader(UnraidInterceptor.API_KEY_OVERRIDE_HEADER, "typo")
            .execute()

        assertThat(apiKeyStatus.rejected.value).isFalse()
    }
}
