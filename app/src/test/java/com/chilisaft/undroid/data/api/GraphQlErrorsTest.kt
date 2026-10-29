package com.chilisaft.undroid.data.api

import com.apollographql.apollo.api.Error
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GraphQlErrorsTest {

    @Test
    fun `detects permission denied via extensions code`() {
        val error = Error.Builder("Nope").extensions(mapOf("code" to "FORBIDDEN")).build()
        assertThat(listOf(error).anyPermissionDenied()).isTrue()
    }

    @Test
    fun `extensions code match is case-insensitive`() {
        val error = Error.Builder("Nope").extensions(mapOf("code" to "forbidden")).build()
        assertThat(listOf(error).anyPermissionDenied()).isTrue()
    }

    @Test
    fun `detects permission denied via message keywords when no code is present`() {
        val error = Error.Builder("You do not have permission to access this resource").build()
        assertThat(listOf(error).anyPermissionDenied()).isTrue()
    }

    @Test
    fun `does not flag unrelated errors as permission denied`() {
        val error = Error.Builder("Internal server error").build()
        assertThat(listOf(error).anyPermissionDenied()).isFalse()
    }

    @Test
    fun `empty error list is not permission denied`() {
        assertThat(emptyList<Error>().anyPermissionDenied()).isFalse()
    }

    @Test
    fun `one permission error among several is enough to flag the whole response`() {
        val errors = listOf(
            Error.Builder("Internal server error").build(),
            Error.Builder("Forbidden").extensions(mapOf("code" to "FORBIDDEN")).build()
        )
        assertThat(errors.anyPermissionDenied()).isTrue()
    }

    // The real shape Unraid 7.3.4 sends for an invalid API key.
    private val invalidKeyError = Error.Builder("Invalid API key format")
        .extensions(mapOf("code" to "UNAUTHENTICATED", "originalError" to mapOf("statusCode" to 401)))
        .build()

    @Test
    fun `a rejected key is not a missing permission`() {
        assertThat(listOf(invalidKeyError).anyPermissionDenied()).isFalse()
    }

    @Test
    fun `detects a rejected key via the UNAUTHENTICATED code`() {
        assertThat(listOf(invalidKeyError).anyKeyRejected()).isTrue()
    }

    @Test
    fun `a forbidden field is not a rejected key`() {
        val error = Error.Builder("Forbidden resource").extensions(mapOf("code" to "FORBIDDEN")).build()
        assertThat(listOf(error).anyKeyRejected()).isFalse()
    }
}
