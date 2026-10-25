package com.chilisaft.undroid.ui.components

import androidx.compose.ui.platform.UriHandler
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UriHandlersTest {

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun `web links are opened`() {
        val handler = RecordingUriHandler()

        assertThat(handler.tryOpenUri("http://192.168.1.10:8080/")).isTrue()
        assertThat(handler.tryOpenUri("HTTPS://tower.example.net")).isTrue()
        assertThat(handler.opened).hasSize(2)
    }

    @Test
    fun `anything that isn't a web link is refused, not handed to another app`() {
        val handler = RecordingUriHandler()

        listOf("market://details?id=x", "tel:123", "intent://scan/#Intent;end", "javascript:alert(1)", "file:///sdcard/x", "tower:8080")
            .forEach { assertThat(handler.tryOpenUri(it)).isFalse() }
        assertThat(handler.opened).isEmpty()
    }
}
