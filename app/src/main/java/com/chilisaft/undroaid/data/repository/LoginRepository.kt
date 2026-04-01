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
        val serverUrl = login.serverUrl?.trim() ?: ""
        if (serverUrl.isBlank()) return Result.failure(Exception("Server URL cannot be empty"))

        return try {
            val response = apolloClient.query(TestLoginQuery())
                .addHttpHeader("X-Server-Url", serverUrl)
                .addHttpHeader("X-API-KEY-OVERRIDE", login.apiToken?.trim() ?: "")
                .execute()

            if (response.data?.server != null && !response.hasErrors()) {
                // Login successful, save credentials
                storage.serverUrl = login.serverUrl
                storage.apiToken = login.apiToken
                Result.success(true)
            } else {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: ApolloException) {
            Result.failure(e)
        }
    }

    fun getSavedLogin(): Login {
        return Login(storage.serverUrl, storage.apiToken)
    }
}
