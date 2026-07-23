package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloException
import com.chilisaft.undroaid.data.models.WidgetResult

/**
 * Runs [query] and reports the outcome as a [WidgetResult], distinguishing a permission
 * failure from any other error. Shared by every repository that backs an independently
 * loadable/retryable UI widget - see [ServerRepository][com.chilisaft.undroaid.data.repository.ServerRepository]
 * for why widgets are split into their own operations in the first place.
 */
suspend fun <D : Query.Data, T> ApolloClient.runWidgetQuery(query: Query<D>, map: (D) -> T): WidgetResult<T> = try {
    this.query(query).execute().toWidgetResult(map)
} catch (e: ApolloException) {
    WidgetResult.Failure(permissionDenied = false, message = e.message)
}

/** Mutation counterpart to [runWidgetQuery]. */
suspend fun <D : Mutation.Data, T> ApolloClient.runWidgetMutation(mutation: Mutation<D>, map: (D) -> T): WidgetResult<T> = try {
    this.mutation(mutation).execute().toWidgetResult(map)
} catch (e: ApolloException) {
    WidgetResult.Failure(permissionDenied = false, message = e.message)
}

private fun <D : Operation.Data, T> ApolloResponse<D>.toWidgetResult(map: (D) -> T): WidgetResult<T> {
    val currentData = data
    return if (currentData != null && !hasErrors()) {
        WidgetResult.Success(map(currentData))
    } else {
        WidgetResult.Failure(
            permissionDenied = errors.orEmpty().anyPermissionDenied(),
            message = errors?.joinToString { it.message } ?: "Unknown error"
        )
    }
}
