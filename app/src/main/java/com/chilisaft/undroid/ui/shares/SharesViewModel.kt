package com.chilisaft.undroid.ui.shares

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroid.data.models.ShareInfo
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.data.models.reloading
import com.chilisaft.undroid.data.repository.SharesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharesScreenState(
    val shares: WidgetResult<List<ShareInfo>> = WidgetResult.Loading,
    /** A refresh is running - drives the pull-to-refresh indicator, while the list keeps showing its current data. */
    val isRefreshing: Boolean = false
)

/** Backs the Shares screen. Read-only - share creation/editing is desktop-webUI territory (see TODO.md). */
@HiltViewModel
class SharesViewModel @Inject constructor(
    private val sharesRepository: SharesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharesScreenState())
    val uiState: StateFlow<SharesScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(shares = it.shares.reloading(), isRefreshing = true) }
            val result = sharesRepository.getShares()
            _uiState.update { it.copy(shares = result, isRefreshing = false) }
        }
    }
}
