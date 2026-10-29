package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.models.NotificationLevel
import com.chilisaft.undroid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationsRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apolloClient: ApolloClient
    private lateinit var repository: NotificationsRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        apolloClient = ApolloClient.Builder()
            .serverUrl(mockWebServer.url("/").toString())
            .build()
        repository = NotificationsRepository(apolloClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getUnreadCount returns the overview total`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"notifications": {"overview": {"unread": {"total": 7}}}}}"""))

        val result = repository.getUnreadCount()

        assertThat(result).isEqualTo(WidgetResult.Success(7))
    }

    @Test
    fun `getNotifications maps id and importance to notification level`() = runBlocking {
        val json = """
        {
          "data": {
            "notifications": {
              "list": [
                { "id": "n1", "title": "Parity Check Complete", "subject": "Parity Check Complete", "description": "No errors found.", "importance": "INFO", "formattedTimestamp": "2 hours ago" },
                { "id": "n2", "title": "Disk 4 Temperature Alert", "subject": "WDC_WD120EDAZ has reached 46°C", "description": "Cooling threshold exceeded.", "importance": "ALERT", "formattedTimestamp": "5 hours ago" }
              ]
            }
          }
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        val result = repository.getNotifications()

        val notifications = (result as WidgetResult.Success).data
        assertThat(notifications).hasSize(2)
        assertThat(notifications[0].id).isEqualTo("n1")
        assertThat(notifications[0].subject).isEqualTo("Parity Check Complete")
        assertThat(notifications[0].level).isEqualTo(NotificationLevel.INFO)
        assertThat(notifications[1].id).isEqualTo("n2")
        assertThat(notifications[1].level).isEqualTo(NotificationLevel.ALERT)
    }

    @Test
    fun `getNotifications reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.getNotifications() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `archiveNotification sends the given id and succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"archiveNotification": {"id": "n1"}}}"""))

        val result = repository.archiveNotification("n1")

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
        assertThat(mockWebServer.takeRequest().body.readUtf8()).contains("\"n1\"")
    }

    @Test
    fun `archiveAllNotifications succeeds when the mutation returns no errors`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"archiveAll": {"unread": {"total": 0}}}}"""))

        val result = repository.archiveAllNotifications()

        assertThat(result).isInstanceOf(WidgetResult.Success::class.java)
    }

    @Test
    fun `archiveAllNotifications reports permission denied separately from other failures`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"errors": [{"message": "Forbidden", "extensions": {"code": "FORBIDDEN"}}]}"""))

        val result = repository.archiveAllNotifications() as WidgetResult.Failure

        assertThat(result.permissionDenied).isTrue()
    }

    @Test
    fun `getNotifications asks for archived notifications when told to`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"notifications": {"list": []}}}"""))

        repository.getNotifications(archived = true)

        assertThat(mockWebServer.takeRequest().body.readUtf8()).contains("\"type\":\"ARCHIVE\"")
    }

    @Test
    fun `restoreNotification unarchives the given id`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"unarchiveNotifications": {"unread": {"total": 1}}}}"""))

        val result = repository.restoreNotification("n1")

        assertThat(result).isEqualTo(WidgetResult.Success(Unit))
        assertThat(mockWebServer.takeRequest().body.readUtf8()).contains("\"id\":\"n1\"")
    }

    @Test
    fun `deleteArchivedNotification deletes the given id`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("""{"data": {"deleteNotification": {"archive": {"total": 0}}}}"""))

        assertThat(repository.deleteArchivedNotification("n1")).isEqualTo(WidgetResult.Success(Unit))
        assertThat(mockWebServer.takeRequest().body.readUtf8()).contains("\"id\":\"n1\"")
    }

    @Test
    fun `deleteAllArchivedNotifications reports a missing permission`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody("""{"errors": [{"message": "Forbidden resource", "extensions": {"code": "FORBIDDEN"}}], "data": null}""")
        )

        val result = repository.deleteAllArchivedNotifications()

        assertThat((result as WidgetResult.Failure).permissionDenied).isTrue()
    }
}
