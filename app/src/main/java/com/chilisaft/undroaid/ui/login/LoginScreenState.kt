package com.chilisaft.undroaid.ui.login

sealed class LoginScreenState {
    data object Init : LoginScreenState()
    data object Loading : LoginScreenState()
    data object Success : LoginScreenState()
    data class Error(val message: String) : LoginScreenState()
}