package com.chilisaft.undroid.ui.components

import android.content.ActivityNotFoundException
import androidx.compose.ui.platform.UriHandler

/**
 * Opens a web link - and only a web link. Returns false instead of opening anything when [uri] isn't
 * http(s), and instead of crashing when nothing on the device can open it or it's malformed. Links
 * like a container's web UI URL come from Docker templates written by third parties, so a
 * `market:`, `tel:` or `intent:` link must not be handed to whatever app claims it.
 */
fun UriHandler.tryOpenUri(uri: String): Boolean = try {
    val scheme = uri.substringBefore(':', missingDelimiterValue = "").lowercase()
    if (scheme == "http" || scheme == "https") {
        openUri(uri)
        true
    } else {
        false
    }
} catch (e: ActivityNotFoundException) {
    false
} catch (e: IllegalArgumentException) {
    false
}
