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

data class NotificationsScreenState(
    val notifications: WidgetResult<List<Notification>> = WidgetResult.Loading,
    val isDismissingAll: Boolean = false,
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

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(notifications = it.notifications.reloading(), isRefreshing = true) }
            val result = notificationsRepository.getNotifications()
            _uiState.update { it.copy(notifications = result, isRefreshing = false) }
        }
    }

    fun dismiss(id: String) {
        if (id in _uiState.value.dismissingIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(dismissingIds = it.dismissingIds + id) }
            val result = notificationsRepository.archiveNotification(id)
            _uiState.update { it.copy(dismissingIds = it.dismissingIds - id) }
            when (result) {
                is WidgetResult.Success -> refresh()
                is WidgetResult.Failure -> userMessages.showFailure("dismiss the notification", result)
                WidgetResult.Loading -> Unit
            }
        }
    }

    fun dismissAll() {
        if (_uiState.value.isDismissingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDismissingAll = true) }
            val result = notificationsRepository.archiveAllNotifications()
            _uiState.update { it.copy(isDismissingAll = false) }
            when (result) {
                is WidgetResult.Success -> refresh()
                is WidgetResult.Failure -> userMessages.showFailure("dismiss all notifications", result)
                WidgetResult.Loading -> Unit
            }
        }
    }
}
