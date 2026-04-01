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
import java.io.IOException

class UrlRewriteInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `interceptor rewrites url when server url is present`() {
        val storage = mockk<Storage>()
        // The interceptor should rewrite the URL to use this new base.
        every { storage.serverUrl } returns server.url("/").toString()

        val interceptor = UrlRewriteInterceptor(storage)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        server.enqueue(MockResponse())

        // The original request uses a placeholder URL to prove it gets rewritten.
        val request = Request.Builder().url("http://placeholder/graphql").build()
        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        // Assert that the path of the request received by the server is the one from the original URL.
        assertThat(recordedRequest.path).isEqualTo("/graphql")
    }

    @Test
    fun `interceptor does not rewrite url when server url is not present`() {
        val storage = mockk<Storage>()
        every { storage.serverUrl } returns ""

        val interceptor = UrlRewriteInterceptor(storage)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        server.enqueue(MockResponse())

        // The original request should point to the MockWebServer.
        val requestUrl = server.url("/graphql")
        val request = Request.Builder().url(requestUrl).build()
        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        // Assert that the URL received by the server is the one we sent, proving it was not rewritten.
        assertThat(recordedRequest.requestUrl).isEqualTo(requestUrl)
    }

    @Test(expected = IOException::class)
    fun `interceptor throws exception for invalid server url`() {
        val storage = mockk<Storage>()
        every { storage.serverUrl } returns "invalid-url"

        val interceptor = UrlRewriteInterceptor(storage)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val request = Request.Builder().url("http://placeholder/graphql").build()
        client.newCall(request).execute()
    }
}
