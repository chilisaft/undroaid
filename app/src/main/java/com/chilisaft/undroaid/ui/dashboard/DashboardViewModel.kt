package com.chilisaft.undroaid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.Server
import com.chilisaft.undroaid.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class DashboardScreenState(
    val server: Server? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardScreenState())
    val uiState: StateFlow<DashboardScreenState> = _uiState.asStateFlow()

    init {
        loadServerInformation()
    }

    private fun loadServerInformation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val serverInformationResult = serverRepository.getServerInformation()
            serverInformationResult.onSuccess { server ->
                _uiState.update {
                    it.copy(
                        server = server,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message
                    )
                }
            }
        }
    }
}