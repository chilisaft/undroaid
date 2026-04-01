package com.chilisaft.undroaid.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.ui.theme.AppTheme
import com.chilisaft.undroaid.ui.theme.spacing

@Composable
fun LoginScreen(onLoginSuccessful: () -> Unit) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LoginContent(
        uiState = uiState,
        onServerUrlChange = { viewModel.onServerUrlChange(it) },
        onApiTokenChange = { viewModel.onApiTokenChange(it) },
        onLoginClick = { viewModel.login() },
        isLoginEnabled = viewModel.isLoginEnabled(),
        onLoginSuccessful = onLoginSuccessful
    )
}

@Composable
fun LoginContent(
    uiState: LoginScreenState,
    onServerUrlChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    isLoginEnabled: Boolean,
    onLoginSuccessful: () -> Unit
) {
    val spacing = MaterialTheme.spacing

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.large)
            ) {
                Spacer(modifier = Modifier.height(150.dp))

                // Using displayMedium for Chakra Petch font branding
                Text(
                    text = "Unraid Login",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(spacing.large))

                ServerUrlField(
                    value = uiState.serverUrl,
                    onChange = onServerUrlChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.small))

                ApiTokenField(
                    value = uiState.apiToken,
                    onChange = onApiTokenChange,
                    submit = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                // Switched to Button for Primary color usage
                Button(
                    onClick = onLoginClick,
                    enabled = isLoginEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(spacing.medium)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary // Contrast against primary button
                        )
                    } else {
                        Text("Login")
                    }
                }

                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Proper side-effect handling for navigation
                if (uiState.isLoggedIn) {
                    LaunchedEffect(Unit) {
                        onLoginSuccessful()
                    }
                }
            }
        }
    }
}

@Composable
fun ServerUrlField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Server URL",
    placeholder: String = "Enter your Unraid server URL"
) {
    val focusManager = LocalFocusManager.current
    val leadingIcon = @Composable {
        Icon(
            Icons.Default.Computer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        placeholder = { Text(placeholder) },
        label = { Text(label) },
        shape = RoundedCornerShape(30),
        singleLine = true
    )
}

@Composable
fun ApiTokenField(
    value: String,
    onChange: (String) -> Unit,
    submit: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "API Token",
    placeholder: String = "Enter your API Token"
) {
    var isTokenVisible by remember { mutableStateOf(false) }

    val leadingIcon = @Composable {
        Icon(
            Icons.Default.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
    val trailingIcon = @Composable {
        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
            Icon(
                if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isTokenVisible) "Hide token" else "Show token",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password
        ),
        keyboardActions = KeyboardActions(
            onDone = { submit() }
        ),
        placeholder = { Text(placeholder) },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(30),
        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation()
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginScreenPreview() {
    AppTheme {
        LoginContent(
            uiState = LoginScreenState(
                serverUrl = "http://192.168.1.100",
                apiToken = "dummy-token"
            ),
            onServerUrlChange = {},
            onApiTokenChange = {},
            onLoginClick = {},
            isLoginEnabled = true,
            onLoginSuccessful = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLoadingPreview() {
    AppTheme {
        LoginContent(
            uiState = LoginScreenState(isLoading = true),
            onServerUrlChange = {},
            onApiTokenChange = {},
            onLoginClick = {},
            isLoginEnabled = false,
            onLoginSuccessful = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenErrorPreview() {
    AppTheme {
        LoginContent(
            uiState = LoginScreenState(error = "Connection failed"),
            onServerUrlChange = {},
            onApiTokenChange = {},
            onLoginClick = {},
            isLoginEnabled = true,
            onLoginSuccessful = {}
        )
    }
}