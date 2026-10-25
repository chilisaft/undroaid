package com.chilisaft.undroid.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.chilisaft.undroid.data.models.Login
import com.chilisaft.undroid.data.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Decides, once per process start, which route [RootNavGraph] should start on - so the app never
 * composes the login screen just to immediately navigate away from it a moment later. [Loading]
 * is what [UndroidActivity][com.chilisaft.undroid.UndroidActivity] holds the splash screen on;
 * it resolves instantly to [LoggedOut] when there's no saved session (nothing to check), or after
 * one background validation call when there is - same request [LoginViewModel] would otherwise
 * have fired itself on first composition, just moved earlier so the routing decision can be made
 * before any UI is shown. Only a *rejected* key lands on [LoggedOut] (where `LoginScreen`'s own
 * auto-login retries once and surfaces the real error). A server that's merely unreachable, slow
 * or erroring still resolves to [LoggedIn] - the saved session is presumably fine, and each screen
 * shows its own error with a Retry - instead of dumping the user on the login form every time the
 * phone starts the app without network.
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
                // Bounded so an unreachable server can't hold the splash screen for OkHttp's full
                // connect/read timeouts - a timeout counts as "couldn't check", not "rejected".
                val result = withTimeoutOrNull(STARTUP_CHECK_TIMEOUT_MS) {
                    loginRepository.login(Login(saved.serverUrl, saved.apiToken))
                }
                _state.value = when {
                    result == null || result.isSuccess -> AppStartupState.LoggedIn
                    result.exceptionOrNull().isConnectivityProblem() -> AppStartupState.LoggedIn
                    else -> AppStartupState.LoggedOut
                }
            }
        }
    }
}

private const val STARTUP_CHECK_TIMEOUT_MS = 4_000L

/**
 * Couldn't reach a working server (no network, DNS, refused, 5xx from a proxy) - as opposed to the
 * server rejecting the key. Checks the whole cause chain, since LoginRepository wraps the Apollo
 * exception in a LoginException.
 */
private fun Throwable?.isConnectivityProblem(): Boolean =
    generateSequence(this) { it.cause }.any { it is ApolloNetworkException || (it is ApolloHttpException && it.statusCode >= 500) }
