package com.chilisaft.undroaid.ui.serverinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.Server
import com.chilisaft.undroaid.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerInfoViewModel @Inject constructor(private val serverRepository: ServerRepository) : ViewModel() {

    private val _serverInfo = MutableLiveData<ServerInfoState>()
    val serverInfo: LiveData<ServerInfoState> = _serverInfo

    sealed class ServerInfoState {
        data object Loading : ServerInfoState()
        data class Success(val server: Server) : ServerInfoState()
        data class Error(val message: String) : ServerInfoState()
    }

    fun loadServerInformation() {
        _serverInfo.value = ServerInfoState.Loading
        viewModelScope.launch {
            val result = serverRepository.getServerInformation()
            _serverInfo.value = result.fold(
                onSuccess = { ServerInfoState.Success(it) },
                onFailure = { ServerInfoState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}