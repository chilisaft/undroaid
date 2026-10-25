package com.chilisaft.undroid.data.api

import com.chilisaft.undroid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Where the API key may and may not go, using the app's real OkHttpClient (as built by
 * [GraphQlServiceModule]). The key is a custom `X-API-KEY` header, which OkHttp's redirect
 * handling does *not* strip on a cross-host redirect (it only drops `Authorization`).
 */
class ApiKeyExposureTest {

    private lateinit var unraid: MockWebServer
    private lateinit var elsewhere: MockWebServer
    private lateinit var storage: Storage

    @Before
    fun setUp() {
        unraid = MockWebServer().apply { start() }
        elsewhere = MockWebServer().apply { start() }
        storage = mockk {
            every { serverUrl } returns unraid.url("/").toString()
            every { apiToken } returns "secret-key"
        }
    }

    @After
    fun tearDown() {
        unraid.shutdown()
        elsewhere.shutdown()
    }

    private fun client() = GraphQlServiceModule.provideOkHttpClient(UnraidInterceptor(storage))

    @Test
    fun `a redirect from the server never carries the API key to another host`() {
        unraid.enqueue(MockResponse().setResponseCode(302).setHeader("Location", elsewhere.url("/graphql")))
        elsewhere.enqueue(MockResponse().setBody("{}"))

        client().newCall(Request.Builder().url("https://placeholder.local/graphql").build()).execute().close()

        assertThat(unraid.takeRequest().getHeader("X-API-KEY")).isEqualTo("secret-key")
        val forwarded = elsewhere.takeRequest(1, TimeUnit.SECONDS)
        assertThat(forwarded?.getHeader("X-API-KEY")).isNull()
    }

    @Test
    fun `an unusable stored server URL fails the request instead of sending the key to the placeholder host`() {
        every { storage.serverUrl } returns "http://bad url with spaces"

        val result = runCatching {
            client().newCall(Request.Builder().url(elsewhere.url("/graphql")).build()).execute().close()
        }

        assertThat(result.isFailure).isTrue()
        assertThat(elsewhere.takeRequest(1, TimeUnit.SECONDS)).isNull()
    }
}
