package com.chilisaft.undroid.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.chilisaft.undroid.data.models.WidgetResult

/**
 * Runs [query] and reports the outcome as a [WidgetResult], distinguishing a permission
 * failure from any other error. Shared by every repository that backs an independently
 * loadable/retryable UI widget - see [ServerRepository][com.chilisaft.undroid.data.repository.ServerRepository]
 * for why widgets are split into their own operations in the first place.
 */
suspend fun <D : Query.Data, T> ApolloClient.runWidgetQuery(query: Query<D>, map: (D) -> T): WidgetResult<T> = try {
    this.query(query).execute().toWidgetResult(map)
} catch (e: ApolloException) {
    WidgetResult.Failure(permissionDenied = e is ApolloHttpException && e.statusCode == 403, message = e.toUserMessage())
}

/**
 * Mutation counterpart to [runWidgetQuery]. [readTimeoutSeconds] raises the read timeout for
 * this one call (see [UnraidInterceptor.READ_TIMEOUT_HEADER]) - for mutations the server takes
 * longer than the default 10 s to answer.
 */
suspend fun <D : Mutation.Data, T> ApolloClient.runWidgetMutation(
    mutation: Mutation<D>,
    readTimeoutSeconds: Int? = null,
    map: (D) -> T
): WidgetResult<T> = try {
    this.mutation(mutation)
        .apply { if (readTimeoutSeconds != null) addHttpHeader(UnraidInterceptor.READ_TIMEOUT_HEADER, readTimeoutSeconds.toString()) }
        .execute()
        .toWidgetResult(map)
} catch (e: ApolloException) {
    WidgetResult.Failure(permissionDenied = e is ApolloHttpException && e.statusCode == 403, message = e.toUserMessage())
}

private fun <D : Operation.Data, T> ApolloResponse<D>.toWidgetResult(map: (D) -> T): WidgetResult<T> {
    val currentData = data
    return if (currentData != null && !hasErrors()) {
        // A mapper that trips over an unexpected response shape (a `!!`, a parse, an index) must
        // fail just this widget like any other error, not crash the app - only ApolloException
        // is caught around the network call itself.
        try {
            WidgetResult.Success(map(currentData))
        } catch (e: RuntimeException) {
            WidgetResult.Failure(permissionDenied = false, message = "Unexpected response from the server")
        }
    } else {
        WidgetResult.Failure(
            permissionDenied = errors.orEmpty().anyPermissionDenied(),
            message = errors?.joinToString { it.message } ?: "Unknown error"
        )
    }
}
