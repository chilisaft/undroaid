package com.chilisaft.undroid.ui

import com.chilisaft.undroid.data.models.WidgetResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * One app-wide channel for short, transient messages to the user - shown as a Snackbar by
 * `UndroidNavGraph`, which has the app's single `SnackbarHost`. ViewModels call [show] when an
 * action (stop a container, start the array, dismiss a notification...) fails, instead of just
 * dropping the row's spinner and leaving the user to guess whether anything happened.
 *
 * A singleton rather than a per-ViewModel flow, so a message still shows if the user has already
 * navigated away from the screen that triggered it (e.g. a slow Stop Array failing after switching
 * tabs).
 */
@Singleton
class UserMessages @Inject constructor() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun show(message: String) {
        _messages.tryEmit(message)
    }

    /** "Couldn't [action]: [reason]", e.g. "Couldn't stop plex: your API key doesn't have permission". */
    fun showFailure(action: String, failure: WidgetResult.Failure) {
        show("Couldn't $action: ${failure.reason()}")
    }
}

private fun WidgetResult.Failure.reason(): String = when {
    permissionDenied -> "your API key doesn't have permission"
    !message.isNullOrBlank() -> message
    else -> "something went wrong"
}
