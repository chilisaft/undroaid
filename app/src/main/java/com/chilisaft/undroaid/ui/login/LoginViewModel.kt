package com.advice.array.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.repository.ServerRepository
import com.chilisaft.undroaid.ui.login.LoginScreenState
import com.chilisaft.undroaid.utils.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val storage: Storage
): ViewModel() {

    val status: LiveData<LoginScreenState>
        get() = _status

    private var shouldShowHelpMessage: Boolean = false

    val helpMessage: LiveData<Boolean>
        get() = _helpMessage

    private val _status = MutableLiveData<LoginScreenState>()
    private val _helpMessage = MutableLiveData(false)
    private var errorCount = 0

    val uuid: LiveData<String>
        get() = _uuid

    private val _uuid = MutableLiveData<String>()

    val loginMessage: LiveData<String>
        get() = _loginMessage

    private val _loginMessage = MutableLiveData<String>()

    init {
        _uuid.value = storage.uuid

        _status.postValue(LoginScreenState.Init)

        val address = storage.address
        val apiToken = storage.apiToken
        if (!address.isNullOrBlank() && !apiToken.isNullOrBlank()) {
            testLogin(address, apiToken, track = false)
        }

        // fetchLoginConfig()
    }

    fun testLogin(address: String, apiToken: String, track: Boolean = true) {
        _status.postValue(LoginScreenState.Loading)
        viewModelScope.launch {
            storage.address = address
            storage.apiToken = apiToken
            val result = serverRepository.getServerInformation()
            when (result.isSuccess) {
                true -> {
                    errorCount = 0

                    _status.postValue(LoginScreenState.Success)
                }

                false -> {
                    if (++errorCount >= 3 && shouldShowHelpMessage) {
                        _helpMessage.postValue(true)
                    }
                    _status.postValue(LoginScreenState.Error(result.exceptionOrNull()?.message ?: "Unknown error"))
                }
            }
        }
    }

    fun reset() {
        _helpMessage.value = false
        errorCount = 0
    }

//    private fun fetchLoginConfig() {
//        val db = Firebase.firestore
//        db.collection("config")
//            .document("login")
//            .get()
//            .addOnSuccessListener {
//                try {
//                    val config = it.toObject(FirebaseLoginConfig::class.java)
//                    _loginMessage.value = config?.message
//                    shouldShowHelpMessage = config?.shouldShowHelpMessage ?: false
//                } catch (ex: Exception) {
//                    // do nothing
//                }
//            }
//    }
}
