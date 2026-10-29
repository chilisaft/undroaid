package com.chilisaft.undroid.utils

import com.google.common.truth.Truth.assertThat
import java.net.URLDecoder
import org.junit.Test

class DebugInfoTest {

    private val info = DebugInfo(
        appVersion = "1.0 (1, debug)",
        androidVersion = "16 (API 36)",
        device = "Google Pixel 10 Pro",
        unraidVersion = "7.3.4",
        apiVersion = null
    )

    @Test
    fun `text lists app, Android, device and server versions, unknown when missing`() {
        assertThat(info.toText()).isEqualTo(
            "App: Undroid 1.0 (1, debug)\nAndroid: 16 (API 36)\nDevice: Google Pixel 10 Pro\nUnraid: 7.3.4 · API unknown"
        )
    }

    @Test
    fun `issue url opens a new GitHub issue with the info in the body`() {
        val url = info.issueUrl()

        assertThat(url).startsWith("https://github.com/chilisaft/undroaid/issues/new?body=")
        assertThat(url).doesNotContain(" ")
        assertThat(URLDecoder.decode(url.substringAfter("body="), "UTF-8")).endsWith(info.toText())
    }
}
