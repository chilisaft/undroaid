package com.chilisaft.undroid.data.api

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

/**
 * Watches every response for the server rejecting the saved API key and reports it to
 * [ApiKeyStatus], so the app can ask the user to log in again instead of showing each section's
 * error. Unraid answers an invalid key with a GraphQL error coded `UNAUTHENTICATED` (HTTP 200);
 * a plain HTTP 401 counts too. Login checks (which try a key that isn't saved yet) are skipped -
 * the login form shows their error itself.
 */
class KeyRejectedInterceptor(private val apiKeyStatus: ApiKeyStatus) : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
        val responses = chain.proceed(request)
        if (request.httpHeaders.orEmpty().any { it.name == UnraidInterceptor.API_KEY_OVERRIDE_HEADER }) return responses
        return responses
            .onEach { if (it.errors.orEmpty().anyKeyRejected() || it.exception.isUnauthorized()) apiKeyStatus.reportRejected() }
            .catch { e ->
                if (e.isUnauthorized()) apiKeyStatus.reportRejected()
                throw e
            }
    }
}

private fun Throwable?.isUnauthorized() = this is ApolloHttpException && statusCode == 401
