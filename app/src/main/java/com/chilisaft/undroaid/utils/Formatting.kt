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

/**
 * Estimated time remaining for a parity check: remaining bytes (the size being scanned, minus
 * what [progressPercent] says is already done) divided by the current [speed] (MB/s, as returned
 * by the API) - the same approach Unraid's own webGUI uses. Deliberately not based on elapsed
 * time: the API's `duration` field appears to reflect the *previous completed* run rather than
 * live elapsed time of an active check (per how the schema's `parityCheckStatus` resolver derives
 * it from `sbSynced`/`sbSynced2`), which would make an elapsed-time-based estimate unreliable.
 */
fun estimatedSecondsRemaining(progressPercent: Int?, speed: String?, totalSizeKb: Long?): Long? {
    val progress = progressPercent ?: return null
    val speedMBps = speed?.toDoubleOrNull() ?: return null
    val totalKb = totalSizeKb ?: return null
    if (progress <= 0 || progress >= 100 || speedMBps <= 0 || totalKb <= 0) return null
    val remainingMb = (totalKb * (100 - progress) / 100.0) / 1024.0
    return (remainingMb / speedMBps).toLong()
}

/** Formats a duration in seconds as a short label like "2h 15m", "45m", or "30s". */
fun Long.toRemainingTimeLabel(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return when {
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes > 0 -> "%dm".format(minutes)
        else -> "%ds".format(seconds)
    }
}
