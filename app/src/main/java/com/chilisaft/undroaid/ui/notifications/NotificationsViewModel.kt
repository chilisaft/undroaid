package com.chilisaft.undroaid.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.Notification
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.data.repository.NotificationsRepository
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
    val dismissingIds: Set<String> = emptySet()
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsScreenState())
    val uiState: StateFlow<NotificationsScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(notifications = WidgetResult.Loading) }
            _uiState.update { it.copy(notifications = notificationsRepository.getNotifications()) }
        }
    }

    fun dismiss(id: String) {
        if (id in _uiState.value.dismissingIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(dismissingIds = it.dismissingIds + id) }
            val result = notificationsRepository.archiveNotification(id)
            _uiState.update { it.copy(dismissingIds = it.dismissingIds - id) }
            if (result is WidgetResult.Success) {
                refresh()
            }
        }
    }

    fun dismissAll() {
        if (_uiState.value.isDismissingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDismissingAll = true) }
            val result = notificationsRepository.archiveAllNotifications()
            _uiState.update { it.copy(isDismissingAll = false) }
            if (result is WidgetResult.Success) {
                refresh()
            }
        }
    }
}
