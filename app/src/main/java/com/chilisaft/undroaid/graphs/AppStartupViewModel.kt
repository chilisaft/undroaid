package com.chilisaft.undroaid.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.data.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides, once per process start, which route [RootNavGraph] should start on - so the app never
 * composes the login screen just to immediately navigate away from it a moment later. [Loading]
 * is what [UndroaidActivity][com.chilisaft.undroaid.UndroaidActivity] holds the splash screen on;
 * it resolves instantly to [LoggedOut] when there's no saved session (nothing to check), or after
 * one background validation call when there is - same request [LoginViewModel] would otherwise
 * have fired itself on first composition, just moved earlier so the routing decision can be made
 * before any UI is shown. If that validation fails, we still land on [LoggedOut] and `LoginScreen`
 * mounts as normal - its own existing auto-login-on-init logic (unchanged) will retry once and
 * surface the real error, so a stale/revoked token degrades to exactly today's behavior rather
 * than a silent dead end.
 */
sealed interface AppStartupState {
    data object Loading : AppStartupState
    data object LoggedIn : AppStartupState
    data object LoggedOut : AppStartupState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val state: StateFlow<AppStartupState> = _state.asStateFlow()

    init {
        val saved = loginRepository.getSavedLogin()
        if (saved.serverUrl.isNullOrBlank() || saved.apiToken.isNullOrBlank()) {
            _state.value = AppStartupState.LoggedOut
        } else {
            viewModelScope.launch {
                val result = loginRepository.login(Login(saved.serverUrl, saved.apiToken))
                _state.value = if (result.isSuccess) AppStartupState.LoggedIn else AppStartupState.LoggedOut
            }
        }
    }
}
