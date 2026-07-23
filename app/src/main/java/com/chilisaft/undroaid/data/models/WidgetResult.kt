package com.chilisaft.undroaid.data.models

/**
 * Outcome of fetching a single, independently-loadable piece of dashboard data.
 * Kept distinct from [Result] so the UI can tell a permission problem (retrying
 * won't help until the API key's role changes) apart from a transient failure.
 */
sealed class WidgetResult<out T> {
    data object Loading : WidgetResult<Nothing>()
    data class Success<T>(val data: T) : WidgetResult<T>()
    data class Failure(val permissionDenied: Boolean, val message: String?) : WidgetResult<Nothing>()
}
