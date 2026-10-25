package com.chilisaft.undroid.data.api

import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import java.io.IOException

/** Thrown by [UnraidInterceptor] for a server URL that can't be parsed - so the UI can say so instead of a generic network error. */
class InvalidServerUrlException(url: String) : IOException("Invalid Server URL: $url")

/**
 * A sentence a user can act on, instead of Apollo's internals ("Failed to execute GraphQL http
 * network request", "Http request failed with status code `401`"). Used for widget errors, the
 * failure Snackbar and the login form. GraphQL-level errors (the server answered, but with an
 * error) aren't covered here - their server-written message is passed through as-is.
 */
fun ApolloException.toUserMessage(): String = when (this) {
    is ApolloNetworkException ->
        if (causes().any { it is InvalidServerUrlException }) {
            "That doesn't look like a valid server URL"
        } else {
            "Can't reach the server - check the server URL and your connection"
        }
    is ApolloHttpException -> when (statusCode) {
        401 -> "The server rejected the API key"
        403 -> "Your API key doesn't have permission for this"
        404 -> "No Unraid API found at this address - check the server URL"
        in 500..599 -> "The server ran into an error (HTTP $statusCode)"
        else -> "Unexpected response from the server (HTTP $statusCode)"
    }
    else -> "Unexpected response from the server"
}

private fun Throwable.causes(): Sequence<Throwable> = generateSequence(cause) { it.cause }
