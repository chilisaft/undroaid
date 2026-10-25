package com.chilisaft.undroid.data.api

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.VmsQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ApolloWidgetExtensionsTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `a mapper that throws on an unexpected response becomes a Failure instead of crashing`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vms": {"domains": []}}}"""))

        val result = apolloClient.runWidgetQuery(VmsQuery()) { data -> data.vms.domains!!.first() }

        assertThat(result).isInstanceOf(WidgetResult.Failure::class.java)
        assertThat((result as WidgetResult.Failure).permissionDenied).isFalse()
    }

    @Test
    fun `a mapper that succeeds still yields Success`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"vms": {"domains": []}}}"""))

        val result = apolloClient.runWidgetQuery(VmsQuery()) { data -> data.vms.domains.orEmpty().size }

        assertThat(result).isEqualTo(WidgetResult.Success(0))
    }
}
