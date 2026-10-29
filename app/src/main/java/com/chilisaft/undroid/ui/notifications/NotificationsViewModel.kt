package com.chilisaft.undroid.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.Notification
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.NotificationsRepository
import com.chilisaft.undroid.ui.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationsTab { NEW, ARCHIVED }

data class NotificationsScreenState(
    val notifications: WidgetResult<List<Notification>> = WidgetResult.Loading,
    /** Loaded the first time the Archived tab is opened. */
    val archived: WidgetResult<List<Notification>> = WidgetResult.Loading,
    val tab: NotificationsTab = NotificationsTab.NEW,
    val isDismissingAll: Boolean = false,
    val isDeletingAll: Boolean = false,
    /** Notifications with an action in flight (dismiss, restore or delete) - their row shows a spinner. */
    val dismissingIds: Set<String> = emptySet(),
    /** A refresh is running - drives the pull-to-refresh indicator, while the list keeps showing its current data. */
    val isRefreshing: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val userMessages: UserMessages
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsScreenState())
    val uiState: StateFlow<NotificationsScreenState> = _uiState.asStateFlow()

    private var archivedLoaded = false

    init {
        refresh()
    }

    fun selectTab(tab: NotificationsTab) {
        _uiState.update { it.copy(tab = tab) }
        if (tab == NotificationsTab.ARCHIVED && !archivedLoaded) refreshArchived()
    }

    /** Refreshes the list on the current tab. */
    fun refresh() {
        if (_uiState.value.tab == NotificationsTab.ARCHIVED) refreshArchived() else refreshNew()
    }

    private fun refreshNew() {
        viewModelScope.launch {
            _uiState.update { it.copy(notifications = it.notifications.reloading(), isRefreshing = true) }
            val result = notificationsRepository.getNotifications()
            _uiState.update { it.copy(notifications = result, isRefreshing = false) }
        }
    }

    private fun refreshArchived() {
        archivedLoaded = true
        viewModelScope.launch {
            _uiState.update { it.copy(archived = it.archived.reloading(), isRefreshing = true) }
            val result = notificationsRepository.getNotifications(archived = true)
            _uiState.update { it.copy(archived = result, isRefreshing = false) }
        }
    }

    /** Archives one unread notification; the archived list reloads next time it's shown. */
    fun dismiss(id: String) = runAction(id, "dismiss the notification") { notificationsRepository.archiveNotification(id) }

    /** Moves an archived notification back to New. */
    fun restore(id: String) = runAction(id, "restore the notification") { notificationsRepository.restoreNotification(id) }

    /** Permanently deletes one archived notification. */
    fun delete(id: String) = runAction(id, "delete the notification") { notificationsRepository.deleteArchivedNotification(id) }

    fun dismissAll() {
        if (_uiState.value.isDismissingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDismissingAll = true) }
            val result = notificationsRepository.archiveAllNotifications()
            _uiState.update { it.copy(isDismissingAll = false) }
            handle(result, "dismiss all notifications")
        }
    }

    fun deleteAllArchived() {
        if (_uiState.value.isDeletingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAll = true) }
            val result = notificationsRepository.deleteAllArchivedNotifications()
            _uiState.update { it.copy(isDeletingAll = false) }
            handle(result, "delete the archived notifications")
        }
    }

    private fun runAction(id: String, description: String, action: suspend () -> WidgetResult<Unit>) {
        if (id in _uiState.value.dismissingIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(dismissingIds = it.dismissingIds + id) }
            val result = action()
            _uiState.update { it.copy(dismissingIds = it.dismissingIds - id) }
            handle(result, description)
        }
    }

    /** Every action moves notifications between the two lists, so both reload on success. */
    private fun handle(result: WidgetResult<Unit>, description: String) {
        when (result) {
            is WidgetResult.Success -> {
                refreshNew()
                if (archivedLoaded) refreshArchived()
            }
            is WidgetResult.Failure -> userMessages.showFailure(description, result)
            WidgetResult.Loading -> Unit
        }
    }
}
