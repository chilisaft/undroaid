package com.chilisaft.undroid.utils

import android.os.Build
import com.chilisaft.undroid.BuildConfig
import java.net.URLEncoder

/** Where the app lives - the GitHub repository doubles as its website. */
object AppLinks {
    const val WEBSITE = "https://github.com/chilisaft/undroaid"
    const val NEW_ISSUE = "$WEBSITE/issues/new"
}

/**
 * What a bug report needs to know about the setup. Deliberately never the server address or the
 * API key - this text ends up in a public GitHub issue.
 */
data class DebugInfo(
    val appVersion: String,
    val androidVersion: String,
    val device: String,
    val unraidVersion: String?,
    val apiVersion: String?
) {
    fun toText(): String = listOf(
        "App: Undroid $appVersion",
        "Android: $androidVersion",
        "Device: $device",
        "Unraid: ${unraidVersion ?: "unknown"} · API ${apiVersion ?: "unknown"}"
    ).joinToString("\n")

    /** A new GitHub issue with a short template and this info filled in. */
    fun issueUrl(): String {
        val body = "**What happened?**\n\n\n**What did you expect?**\n\n\n**Steps to reproduce**\n\n\n---\n${toText()}"
        return "${AppLinks.NEW_ISSUE}?body=${URLEncoder.encode(body, "UTF-8").replace("+", "%20")}"
    }

    companion object {
        fun current(unraidVersion: String?, apiVersion: String?) = DebugInfo(
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})",
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            unraidVersion = unraidVersion,
            apiVersion = apiVersion
        )
    }
}
