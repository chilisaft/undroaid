package com.chilisaft.undroaid.data.models

data class Notification(
    val id: String,
    val title: String,
    val description: String,
    val level: NotificationLevel,
    val timestamp: String?
)

enum class NotificationLevel {
    INFO, WARNING, ALERT
}
