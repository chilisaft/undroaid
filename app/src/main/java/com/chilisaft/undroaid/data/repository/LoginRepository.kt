package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.models.Login
import com.chilisaft.undroaid.graphql.TestLoginQuery
import com.chilisaft.undroaid.utils.Storage
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val storage: Storage
) {
    suspend fun login(login: Login): Result<Boolean> {
        storage.serverUrl = login.serverUrl
        storage.apiToken = login.apiToken
        return try {
            val response = apolloClient.query(TestLoginQuery()).execute()
            if (!response.hasErrors()) {
                response.data?.server?.let { serverData ->
                    Result.success(true)
                }
                    ?: Result.failure(Exception("GraphQL error: ${response.errors?.joinToString { it.message }}"))
            } else {
                Result.failure(Exception("GraphQL error: ${response.errors?.joinToString { it.message }}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSavedLogin(): Login {
        return Login(storage.serverUrl, storage.apiToken)
    }

}