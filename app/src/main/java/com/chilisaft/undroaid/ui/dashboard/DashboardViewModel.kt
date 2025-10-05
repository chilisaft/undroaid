package com.chilisaft.undroaid.ui.dashboard

import androidx.lifecycle.ViewModel
import com.chilisaft.undroaid.data.models.Server
import com.chilisaft.undroaid.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
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
        var serverInformation: Result<Server>
        runBlocking {
            serverInformation = serverRepository.getServerInformation()
        }

        if (serverInformation.isFailure) {
            // TODO: show error
        } else {
            _uiState.value = _uiState.value.copy(
                server = serverInformation.getOrThrow(),
                isLoading = false
            )
        }
    }
}