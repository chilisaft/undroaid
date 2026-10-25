package com.chilisaft.undroid.data.api

import com.chilisaft.undroid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class UnraidInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var storage: Storage
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        storage = mockk()
        client = OkHttpClient.Builder().addInterceptor(UnraidInterceptor(storage)).build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `rewrites url to stored server url`() {
        every { storage.serverUrl } returns server.url("/").toString()
        every { storage.apiToken } returns null

        server.enqueue(MockResponse())
        client.newCall(Request.Builder().url("http://placeholder/graphql").build()).execute()

        assertThat(server.takeRequest().path).isEqualTo("/graphql")
    }

    @Test
    fun `refuses to send the request when no server url is stored`() {
        every { storage.serverUrl } returns null
        every { storage.apiToken } returns "secret-key"

        val result = runCatching { client.newCall(Request.Builder().url(server.url("/graphql")).build()).execute() }

        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidServerUrlException::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `X-Server-Url header overrides stored server url and is stripped`() {
        every { storage.serverUrl } returns "http://should-not-be-used"
        every { storage.apiToken } returns null

        server.enqueue(MockResponse())
        val request = Request.Builder()
            .url("http://placeholder/graphql")
            .header("X-Server-Url", server.url("/").toString())
            .build()
        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/graphql")
        assertThat(recordedRequest.getHeader("X-Server-Url")).isNull()
    }

    @Test(expected = IOException::class)
    fun `throws for an invalid X-Server-Url override`() {
        every { storage.serverUrl } returns null
        every { storage.apiToken } returns null

        val request = Request.Builder()
            .url("http://placeholder/graphql")
            .header("X-Server-Url", "not a valid url")
            .build()
        client.newCall(request).execute()
    }

    @Test
    fun `refuses to send the request when the stored server url is invalid`() {
        // Previously a bad *stored* URL silently left the request pointed at ApolloClient's
        // placeholder URL - with the API key attached. It must fail instead.
        every { storage.serverUrl } returns "not a valid url"
        every { storage.apiToken } returns "secret-key"

        val result = runCatching { client.newCall(Request.Builder().url(server.url("/graphql")).build()).execute() }

        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidServerUrlException::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `adds api key header from storage`() {
        every { storage.serverUrl } returns server.url("/").toString()
        every { storage.apiToken } returns "test-api-token"

        server.enqueue(MockResponse())
        client.newCall(Request.Builder().url(server.url("/graphql")).build()).execute()

        assertThat(server.takeRequest().getHeader("X-API-KEY")).isEqualTo("test-api-token")
    }

    @Test
    fun `does not add api key header when storage token is null`() {
        every { storage.serverUrl } returns server.url("/").toString()
        every { storage.apiToken } returns null

        server.enqueue(MockResponse())
        client.newCall(Request.Builder().url(server.url("/graphql")).build()).execute()

        assertThat(server.takeRequest().getHeader("X-API-KEY")).isNull()
    }

    @Test
    fun `X-API-KEY-OVERRIDE header overrides stored token and is stripped`() {
        every { storage.serverUrl } returns server.url("/").toString()
        every { storage.apiToken } returns "stored-token"

        server.enqueue(MockResponse())
        val request = Request.Builder()
            .url(server.url("/graphql"))
            .header("X-API-KEY-OVERRIDE", " override-token ")
            .build()
        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.getHeader("X-API-KEY")).isEqualTo("override-token")
        assertThat(recordedRequest.getHeader("X-API-KEY-OVERRIDE")).isNull()
    }
}
