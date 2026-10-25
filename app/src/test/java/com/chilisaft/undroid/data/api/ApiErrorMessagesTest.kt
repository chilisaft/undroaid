package com.chilisaft.undroid.data.api

import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.JsonDataException
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiErrorMessagesTest {

    @Test
    fun `network failures read as an unreachable server`() {
        assertThat(ApolloNetworkException("Failed to execute GraphQL http network request").toUserMessage())
            .isEqualTo("Can't reach the server - check the server URL and your connection")
    }

    @Test
    fun `an unparseable server URL is called out specifically`() {
        val error = ApolloNetworkException("Failed to execute GraphQL http network request", InvalidServerUrlException("http://bad url/graphql"))

        assertThat(error.toUserMessage()).isEqualTo("That doesn't look like a valid server URL")
    }

    @Test
    fun `http status codes map to what they mean for the user`() {
        fun http(code: Int) = ApolloHttpException(code, emptyList(), null, "Http request failed with status code `$code`").toUserMessage()

        assertThat(http(401)).isEqualTo("The server rejected the API key")
        assertThat(http(403)).isEqualTo("Your API key doesn't have permission for this")
        assertThat(http(404)).isEqualTo("No Unraid API found at this address - check the server URL")
        assertThat(http(502)).isEqualTo("The server ran into an error (HTTP 502)")
        assertThat(http(418)).isEqualTo("Unexpected response from the server (HTTP 418)")
    }

    @Test
    fun `anything else is an unexpected response`() {
        assertThat(JsonDataException("bad json").toUserMessage()).isEqualTo("Unexpected response from the server")
    }
}
