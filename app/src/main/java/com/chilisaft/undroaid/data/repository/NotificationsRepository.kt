package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.api.runWidgetMutation
import com.chilisaft.undroaid.data.api.runWidgetQuery
import com.chilisaft.undroaid.data.models.Notification
import com.chilisaft.undroaid.data.models.NotificationLevel
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.graphql.ArchiveAllNotificationsMutation
import com.chilisaft.undroaid.graphql.ArchiveNotificationMutation
import com.chilisaft.undroaid.graphql.NotificationsListQuery
import com.chilisaft.undroaid.graphql.NotificationsOverviewQuery
import com.chilisaft.undroaid.graphql.type.NotificationImportance
import javax.inject.Inject

class NotificationsRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    /** Total unread count, for the bell icon's badge - independent of [getNotifications]'s page size. */
    suspend fun getUnreadCount(): WidgetResult<Int> =
        apolloClient.runWidgetQuery(NotificationsOverviewQuery()) { it.notifications.overview.unread.total }

    suspend fun getNotifications(): WidgetResult<List<Notification>> =
        apolloClient.runWidgetQuery(NotificationsListQuery()) { data -> data.notifications.list.map { it.toNotification() } }

    /** Archives a single notification. Callers should re-fetch [getNotifications] on success. */
    suspend fun archiveNotification(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(ArchiveNotificationMutation(id)) { }

    /** Archives every unread notification. Callers should re-fetch [getNotifications] on success. */
    suspend fun archiveAllNotifications(): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(ArchiveAllNotificationsMutation()) { }

    private fun NotificationsListQuery.List.toNotification() = Notification(
        id = id,
        title = title,
        description = description,
        level = importance.toNotificationLevel(),
        timestamp = formattedTimestamp
    )

    private fun NotificationImportance.toNotificationLevel(): NotificationLevel = when (this) {
        NotificationImportance.ALERT -> NotificationLevel.ALERT
        NotificationImportance.WARNING -> NotificationLevel.WARNING
        NotificationImportance.INFO -> NotificationLevel.INFO
        NotificationImportance.UNKNOWN__ -> NotificationLevel.INFO
    }
}
