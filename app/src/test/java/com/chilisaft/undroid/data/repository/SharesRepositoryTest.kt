package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class SharesRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: SharesRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        repository = SharesRepository(
            ApolloClient.Builder().serverUrl(mockWebServer.url("/").toString()).build()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun share(id: String, name: String?, free: String?, size: String?, comment: String? = null) =
        """{ "id": "$id", "name": ${name?.let { "\"$it\"" }}, "comment": ${comment?.let { "\"$it\"" }},
            "free": ${free?.let { "\"$it\"" }}, "size": ${size?.let { "\"$it\"" }} }"""

    @Test
    fun `maps shares sorted by name, with free space and comment`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"shares": [
            ${share("s2", "media", free = "4000", size = "0")},
            ${share("s1", "appdata", free = "900", size = "120", comment = "Container data")}
        ]}}"""))

        val shares = (repository.getShares() as WidgetResult.Success).data

        assertThat(shares.map { it.name }).containsExactly("appdata", "media").inOrder()
        assertThat(shares[0].comment).isEqualTo("Container data")
        assertThat(shares[0].freeKb).isEqualTo(900L)
        assertThat(shares[0].sizeKb).isEqualTo(120L)
    }

    @Test
    fun `a size of 0 means Unraid hasn't computed it - reported as unknown, not as empty`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"shares": [${share("s1", "media", free = "700", size = "0")}]}}"""))

        val share = (repository.getShares() as WidgetResult.Success).data.single()

        assertThat(share.sizeKb).isNull()
        assertThat(share.freeKb).isEqualTo(700L)
    }

    @Test
    fun `tolerates missing name, comment and usage`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"shares": [${share("s1", null, free = null, size = null, comment = "  ")}]}}"""))

        val share = (repository.getShares() as WidgetResult.Success).data.single()

        assertThat(share.name).isEqualTo("Unnamed share")
        assertThat(share.comment).isNull()
        assertThat(share.freeKb).isNull()
        assertThat(share.sizeKb).isNull()
    }

    @Test
    fun `reports a missing SHARE permission as permission denied`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getShares() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }
}
