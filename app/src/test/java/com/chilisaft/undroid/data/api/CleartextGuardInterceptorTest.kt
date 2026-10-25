package com.chilisaft.undroid.data.api

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import java.net.InetAddress

class CleartextGuardInterceptorTest {

    private fun ip(literal: String) = InetAddress.getByName(literal) // literals only - no DNS lookup

    @Test
    fun `private and local addresses allow plain http`() {
        listOf(
            "127.0.0.1", "10.0.0.5", "172.16.0.1", "172.31.255.254", "192.168.69.7",
            "169.254.10.10", "100.64.0.1", "100.127.255.254", "::1", "fd12:3456::1", "fe80::1"
        ).forEach { assertWithMessage(it).that(ip(it).isPrivateNetworkAddress()).isTrue() }
    }

    @Test
    fun `public addresses do not`() {
        listOf(
            "8.8.8.8", "172.32.0.1", "192.169.0.1", "100.63.255.255", "100.128.0.1", "2001:4860:4860::8888"
        ).forEach { assertWithMessage(it).that(ip(it).isPrivateNetworkAddress()).isFalse() }
    }

    @Test
    fun `plain http to a local server goes through`() {
        val server = MockWebServer().apply { start() } // binds to localhost
        server.enqueue(MockResponse().setBody("ok"))
        val client = OkHttpClient.Builder().addNetworkInterceptor(CleartextGuardInterceptor()).build()

        val body = client.newCall(Request.Builder().url(server.url("/graphql")).build()).execute().use { it.body?.string() }

        assertThat(body).isEqualTo("ok")
        server.shutdown()
    }
}
