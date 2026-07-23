package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.api.Error

/**
 * Best-effort detection of "this API key doesn't have permission for this resource" errors.
 *
 * The Unraid API documents per-field required permissions (`@usePermissions` in the schema)
 * but doesn't document the exact error shape returned when a check fails. This checks the
 * standard GraphQL `extensions.code` first, then falls back to matching common phrasing in
 * the error message. If neither matches, the error is treated as a generic failure rather
 * than a permission one - worth revisiting against a real Unraid instance to confirm the
 * actual shape and tighten this.
 */
private val PERMISSION_ERROR_CODES = setOf("FORBIDDEN", "UNAUTHORIZED", "UNAUTHENTICATED", "PERMISSION_DENIED")
private val PERMISSION_KEYWORDS = listOf("permission", "forbidden", "unauthorized", "not authorized", "access denied")

fun List<Error>.anyPermissionDenied(): Boolean = any { it.isPermissionDenied() }

private fun Error.isPermissionDenied(): Boolean {
    val code = (extensions?.get("code") as? String)?.uppercase()
    if (code != null && code in PERMISSION_ERROR_CODES) return true

    val text = message.lowercase()
    return PERMISSION_KEYWORDS.any { it in text }
}
