package com.chilisaft.undroid.data.models

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

/**
 * What a widget should hold while it's being re-fetched: its current data if it has any (the
 * screen's pull-to-refresh indicator shows that a refresh is running), or [WidgetResult.Loading]
 * if there's nothing to show yet (first load, or retrying after a failure). Keeps a refresh from
 * replacing a populated list with a loading placeholder and losing the scroll position.
 */
fun <T> WidgetResult<T>.reloading(): WidgetResult<T> = if (this is WidgetResult.Success) this else WidgetResult.Loading
