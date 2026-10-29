package com.chilisaft.undroid.data.api

import com.apollographql.apollo.api.Error

/**
 * Best-effort detection of "this API key doesn't have permission for this resource" errors.
 *
 * Unraid 7.3.4 (API 4.37.9) sends `extensions.code = "FORBIDDEN"` ("Forbidden resource") when the
 * key lacks a permission, which the code check catches; an invalid key gets "UNAUTHENTICATED".
 * The message-keyword fallback stays for other API versions. Anything else is treated as a
 * generic failure rather than a permission one.
 */
private val PERMISSION_ERROR_CODES = setOf("FORBIDDEN", "UNAUTHORIZED", "PERMISSION_DENIED")
private const val KEY_REJECTED_CODE = "UNAUTHENTICATED"
private val PERMISSION_KEYWORDS = listOf("permission", "forbidden", "unauthorized", "not authorized", "access denied")

fun List<Error>.anyPermissionDenied(): Boolean = any { it.isPermissionDenied() }

/** The server didn't accept the API key at all (deleted, changed, mistyped) - see [KeyRejectedInterceptor]. */
fun List<Error>.anyKeyRejected(): Boolean = any { it.code() == KEY_REJECTED_CODE }

private fun Error.code(): String? = (extensions?.get("code") as? String)?.uppercase()

private fun Error.isPermissionDenied(): Boolean {
    val code = code()
    // A rejected key isn't a missing permission - its message ("Invalid API key format") mustn't
    // reach the keyword fallback either.
    if (code == KEY_REJECTED_CODE) return false
    if (code != null && code in PERMISSION_ERROR_CODES) return true

    val text = message.lowercase()
    return PERMISSION_KEYWORDS.any { it in text }
}
