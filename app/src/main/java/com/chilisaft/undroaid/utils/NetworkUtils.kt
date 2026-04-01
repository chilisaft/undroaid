package com.chilisaft.undroaid.utils

/**
 * Sanitizes the Unraid server URL to ensure it has a scheme and ends with /graphql
 */
fun String.sanitizeUnraidUrl(): String {
    val trimmed = this.trim()
    if (trimmed.isBlank()) return ""

    val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        "http://$trimmed"
    } else {
        trimmed
    }
    
    val noTrailingSlash = withScheme.removeSuffix("/")
    return if (noTrailingSlash.endsWith("/graphql")) {
        noTrailingSlash
    } else {
        "$noTrailingSlash/graphql"
    }
}
