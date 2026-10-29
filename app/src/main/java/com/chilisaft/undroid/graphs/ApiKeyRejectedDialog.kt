package com.chilisaft.undroid.graphs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.chilisaft.undroid.data.api.ApiKeyStatus
import com.chilisaft.undroid.data.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ApiKeyRejectedViewModel @Inject constructor(
    private val apiKeyStatus: ApiKeyStatus,
    private val loginRepository: LoginRepository
) : ViewModel() {
    val keyRejected: StateFlow<Boolean> = apiKeyStatus.rejected

    /** Forgets the rejected key (keeping the server URL for the login form) before going to login. */
    fun logInAgain() {
        loginRepository.forgetApiKey()
        apiKeyStatus.clear()
    }
}

/**
 * Shown over the logged-in app once the server rejects the saved API key (deleted or changed in
 * the webUI). Without it every section would just show an error. There's no way to dismiss it -
 * with the key rejected, nothing in the app can load.
 */
@Composable
fun ApiKeyRejectedDialog(onLogInAgain: () -> Unit, viewModel: ApiKeyRejectedViewModel = hiltViewModel()) {
    val rejected by viewModel.keyRejected.collectAsState()
    if (!rejected) return
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("API key not accepted") },
        text = {
            Text("The server no longer accepts the saved API key - it may have been deleted or changed. Log in again with a valid key.")
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.logInAgain()
                onLogInAgain()
            }) { Text("Log in again") }
        }
    )
}
