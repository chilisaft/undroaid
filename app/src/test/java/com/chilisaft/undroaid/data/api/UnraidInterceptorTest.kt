package com.chilisaft.undroaid.data.api

import com.chilisaft.undroaid.utils.Storage
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

class UnraidInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var storage: Storage
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        storage = mockk(relaxed = true)
        val interceptor = UnraidInterceptor(storage)
        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `interceptor rewrites url from storage`() {
        every { storage.serverUrl } returns "192.168.1.50"
        mockWebServer.enqueue(MockResponse())

        val request = Request.Builder()
            .url("http://placeholder.local/graphql")
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.requestUrl.toString()).isEqualTo("http://192.168.1.50/graphql")
    }

    @Test
    fun `interceptor handles override headers`() {
        every { storage.serverUrl } returns "old-url.com"
        every { storage.apiToken } returns "old-token"
        mockWebServer.enqueue(MockResponse())

        val request = Request.Builder()
            .url("http://placeholder.local/graphql")
            .addHeader("X-Server-Url", "new-unraid.local")
            .addHeader("X-API-KEY-OVERRIDE", "new-token")
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.requestUrl.toString()).isEqualTo("http://new-unraid.local/graphql")
        assertThat(recordedRequest.getHeader("X-API-KEY")).isEqualTo("new-token")
        // Verify override headers are removed
        assertThat(recordedRequest.getHeader("X-Server-Url")).isNull()
        assertThat(recordedRequest.getHeader("X-API-KEY-OVERRIDE")).isNull()
    }

    @Test
    fun `interceptor adds api key from storage`() {
        every { storage.serverUrl } returns "http://unraid.local/graphql"
        every { storage.apiToken } returns "secret-token"
        mockWebServer.enqueue(MockResponse())

        val request = Request.Builder()
            .url("http://placeholder.local/graphql")
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("X-API-KEY")).isEqualTo("secret-token")
    }
}
