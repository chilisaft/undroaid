package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.ShareProtection
import com.chilisaft.undroid.data.models.ShareSettings
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

    /** [settings] is raw JSON for the allocation fields - all null unless a test sets them. */
    private fun share(
        id: String,
        name: String?,
        free: String?,
        size: String?,
        comment: String? = null,
        settings: String = """"include": null, "exclude": null, "allocator": null, "splitLevel": null, "floor": null, "color": null"""
    ) =
        """{ "id": "$id", "name": ${name?.let { "\"$it\"" }}, "comment": ${comment?.let { "\"$it\"" }},
            "free": ${free?.let { "\"$it\"" }}, "size": ${size?.let { "\"$it\"" }}, $settings }"""

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
    fun `maps a real server's settings - protection colour, allocator, split level and floor`() = runBlocking {
        val realMedia = """"include": [], "exclude": [], "allocator": "mostfree", "splitLevel": "3", "floor": "100000000", "color": "green-on""""
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"shares": [${share("s1", "media", free = "1", size = "0", settings = realMedia)}]}}"""))

        val share = (repository.getShares() as WidgetResult.Success).data.single()

        assertThat(share.protection).isEqualTo(ShareProtection.PROTECTED)
        assertThat(share.settings).isEqualTo(
            ShareSettings(allocator = "mostfree", splitLevel = "3", minimumFreeKb = 100_000_000L, includedDisks = emptyList(), excludedDisks = emptyList())
        )
    }

    @Test
    fun `an empty split level is kept (it means split any directory), unknown colours map to no protection info`() = runBlocking<Unit> {
        val settings = """"include": ["disk1", "disk2"], "exclude": ["disk3"], "allocator": "highwater", "splitLevel": "", "floor": "0", "color": "grey-off""""
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"shares": [${share("s1", "isos", free = "1", size = "0", settings = settings)}]}}"""))

        val share = (repository.getShares() as WidgetResult.Success).data.single()

        assertThat(share.protection).isNull()
        assertThat(share.settings.splitLevel).isEqualTo("")
        assertThat(share.settings.minimumFreeKb).isEqualTo(0L)
        assertThat(share.settings.includedDisks).containsExactly("disk1", "disk2").inOrder()
        assertThat(share.settings.excludedDisks).containsExactly("disk3")
    }

    @Test
    fun `yellow and red colours mean some files are unprotected`() = runBlocking<Unit> {
        val yellow = """"include": null, "exclude": null, "allocator": null, "splitLevel": null, "floor": null, "color": "yellow-on""""
        val red = """"include": null, "exclude": null, "allocator": null, "splitLevel": null, "floor": null, "color": "red-on""""
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"shares": [
            ${share("s1", "a", free = null, size = null, settings = yellow)},
            ${share("s2", "b", free = null, size = null, settings = red)}
        ]}}"""))

        val shares = (repository.getShares() as WidgetResult.Success).data

        assertThat(shares.map { it.protection }).containsExactly(ShareProtection.UNPROTECTED, ShareProtection.UNPROTECTED)
    }

    @Test
    fun `reports a missing SHARE permission as permission denied`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getShares() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }
}
