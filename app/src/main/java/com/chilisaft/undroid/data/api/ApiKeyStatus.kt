package com.chilisaft.undroid.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the server has rejected the saved API key since the last login. Set by
 * [KeyRejectedInterceptor] from any request; the logged-in app shows a "Log in again" dialog while
 * it's true. Cleared by a successful login.
 */
@Singleton
class ApiKeyStatus @Inject constructor() {
    private val _rejected = MutableStateFlow(false)
    val rejected: StateFlow<Boolean> = _rejected.asStateFlow()

    fun reportRejected() {
        _rejected.value = true
    }

    fun clear() {
        _rejected.value = false
    }
}
