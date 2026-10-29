package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.chilisaft.undroid.data.api.ApiKeyStatus
import com.chilisaft.undroid.data.api.UnraidInterceptor
import com.chilisaft.undroid.data.api.toUserMessage
import com.chilisaft.undroid.data.models.Login
import com.chilisaft.undroid.graphql.TestLoginQuery
import com.chilisaft.undroid.utils.Storage
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val storage: Storage,
    private val apiKeyStatus: ApiKeyStatus
) {
    suspend fun login(login: Login): Result<Boolean> {
        val serverUrl = login.serverUrl?.trim() ?: ""
        if (serverUrl.isBlank()) return Result.failure(Exception("Server URL cannot be empty"))

        return try {
            val response = apolloClient.query(TestLoginQuery())
                .addHttpHeader("X-Server-Url", serverUrl)
                .addHttpHeader(UnraidInterceptor.API_KEY_OVERRIDE_HEADER, login.apiToken?.trim() ?: "")
                .execute()

            if (response.data?.me != null && !response.hasErrors()) {
                // Login successful, save credentials
                storage.serverUrl = login.serverUrl
                storage.apiToken = login.apiToken
                apiKeyStatus.clear()
                Result.success(true)
            } else {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: ApolloException) {
            // Keep the Apollo exception as the cause - AppStartupViewModel inspects it to tell
            // "server unreachable" apart from "key rejected".
            Result.failure(LoginException(e.toUserMessage(), e))
        }
    }

    fun getSavedLogin(): Login {
        return Login(storage.serverUrl, storage.apiToken)
    }

    /** After the server rejected the saved key: drop it but keep the server URL for the login form. */
    fun forgetApiKey() {
        storage.apiToken = null
    }

    fun logout() {
        storage.serverUrl = null
        storage.apiToken = null
        storage.serverNetworkCache = null
    }
}

/** A failed login whose [message] is ready to show on the login form; [cause] is the underlying Apollo error. */
class LoginException(message: String, cause: Throwable) : Exception(message, cause)
