package com.chilisaft.undroid.ui.components

import android.content.ActivityNotFoundException
import androidx.compose.ui.platform.UriHandler

/**
 * [UriHandler.openUri], but returns false instead of crashing the app when nothing on the device
 * can open [uri] or it's malformed - which happens with server-supplied links like a container's
 * web UI URL (no scheme, unresolved template, LAN-only address...).
 */
fun UriHandler.tryOpenUri(uri: String): Boolean = try {
    openUri(uri)
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: IllegalArgumentException) {
    false
}
