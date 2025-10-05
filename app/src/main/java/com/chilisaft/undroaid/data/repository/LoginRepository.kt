package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.graphql.TestLoginQuery
import com.chilisaft.undroaid.utils.Storage
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val storage: Storage
) {
    suspend fun login(login: Login): Result<Boolean> {
        // Temporarily set credentials for the login attempt.
        // The AuthInterceptor will use these.
        storage.serverUrl = login.serverUrl
        storage.apiToken = login.apiToken

        return try {
            val response = apolloClient.query(TestLoginQuery()).execute()

            if (response.data?.server != null && !response.hasErrors()) {
                // Login successful, credentials are valid and now stored.
                Result.success(true)
            } else {
                // Clear credentials if login failed
                storage.serverUrl = ""
                storage.apiToken = ""
                val errorMessage = response.errors?.joinToString { it.message } ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: ApolloException) {
            // Clear credentials if login failed due to a network or parsing error
            storage.serverUrl = ""
            storage.apiToken = ""
            Result.failure(e)
        }
    }

    fun getSavedLogin(): Login {
        return Login(storage.serverUrl, storage.apiToken)
    }
}