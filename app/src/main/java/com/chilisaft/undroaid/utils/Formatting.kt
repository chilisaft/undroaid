package com.chilisaft.undroaid.utils

import java.time.Duration
import java.time.Instant

/**
 * Formats a size given in kilobytes (as returned by the Unraid API) into a human-readable
 * string using the largest unit that keeps the value >= 1, e.g. 130589261824L -> "124.5 GB".
 */
fun Long.kilobytesToHumanReadable(): String {
    val units = listOf("KB", "MB", "GB", "TB", "PB")
    var value = this.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}

/**
 * Formats an ISO-8601 boot timestamp (e.g. Unraid's `info.os.uptime`) as an elapsed-time
 * label such as "42d 12h 04m". Returns null if the timestamp can't be parsed or is in the future.
 */
fun String.toUptimeLabel(now: Instant = Instant.now()): String? {
    val bootInstant = runCatching { Instant.parse(this) }.getOrNull() ?: return null
    val duration = Duration.between(bootInstant, now)
    if (duration.isNegative) return null

    val totalSeconds = duration.seconds
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    return "%dd %02dh %02dm".format(days, hours, minutes)
}
