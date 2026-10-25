package com.chilisaft.undroid.ui

import com.chilisaft.undroid.data.models.WidgetResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserMessagesTest {

    /** Subscribes before running [block], since [UserMessages.messages] doesn't replay to late collectors. */
    private suspend fun UserMessages.nextMessageAfter(block: UserMessages.() -> Unit): String = coroutineScope {
        val message = async(start = CoroutineStart.UNDISPATCHED) { messages.first() }
        block()
        message.await()
    }

    @Test
    fun `showFailure explains a permission failure in plain words`() = runTest(UnconfinedTestDispatcher()) {
        val message = UserMessages().nextMessageAfter {
            showFailure("stop plex", WidgetResult.Failure(permissionDenied = true, message = "FORBIDDEN"))
        }
        assertThat(message).isEqualTo("Couldn't stop plex: your API key doesn't have permission")
    }

    @Test
    fun `showFailure passes the server's message through`() = runTest(UnconfinedTestDispatcher()) {
        val message = UserMessages().nextMessageAfter {
            showFailure("start the array", WidgetResult.Failure(permissionDenied = false, message = "Array is busy"))
        }
        assertThat(message).isEqualTo("Couldn't start the array: Array is busy")
    }

    @Test
    fun `showFailure falls back to a generic reason when there is no message`() = runTest(UnconfinedTestDispatcher()) {
        val message = UserMessages().nextMessageAfter {
            showFailure("dismiss the notification", WidgetResult.Failure(permissionDenied = false, message = null))
        }
        assertThat(message).isEqualTo("Couldn't dismiss the notification: something went wrong")
    }
}
