package com.chilisaft.undroaid.utils

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class FormattingTest {

    @Test
    fun `kilobytesToHumanReadable formats small values in KB`() {
        assertThat(512L.kilobytesToHumanReadable()).isEqualTo("512.0 KB")
    }

    @Test
    fun `kilobytesToHumanReadable formats large values in TB`() {
        // 1 TB = 1024^3 KB
        assertThat((1024L * 1024 * 1024).kilobytesToHumanReadable()).isEqualTo("1.0 TB")
    }

    @Test
    fun `kilobytesToHumanReadable caps at PB for very large values`() {
        val hugeValue = 1024L * 1024 * 1024 * 1024 * 5 // 5 PB in KB
        assertThat(hugeValue.kilobytesToHumanReadable()).isEqualTo("5.0 PB")
    }

    @Test
    fun `toUptimeLabel computes elapsed time from boot timestamp`() {
        val now = Instant.parse("2024-01-03T04:05:00Z")
        val bootTime = "2024-01-01T00:00:00Z"
        assertThat(bootTime.toUptimeLabel(now)).isEqualTo("2d 04h 05m")
    }

    @Test
    fun `toUptimeLabel returns null for unparsable input`() {
        assertThat("not-a-timestamp".toUptimeLabel()).isNull()
    }

    @Test
    fun `toUptimeLabel returns null when boot time is in the future`() {
        val now = Instant.parse("2024-01-01T00:00:00Z")
        val bootTime = "2024-01-02T00:00:00Z"
        assertThat(bootTime.toUptimeLabel(now)).isNull()
    }

    @Test
    fun `estimatedSecondsRemaining computes remaining time from progress, speed, and total size`() {
        // 4 TB disk, 50% done, 100 MB/s -> 2 TB remaining / 100 MB/s
        val totalSizeKb = 4L * 1024 * 1024 * 1024
        val seconds = estimatedSecondsRemaining(progressPercent = 50, speed = "100", totalSizeKb = totalSizeKb)
        val expectedSeconds = (2L * 1024 * 1024) / 100
        assertThat(seconds).isEqualTo(expectedSeconds)
    }

    @Test
    fun `estimatedSecondsRemaining returns null when any input is missing`() {
        assertThat(estimatedSecondsRemaining(null, "100", 1000L)).isNull()
        assertThat(estimatedSecondsRemaining(50, null, 1000L)).isNull()
        assertThat(estimatedSecondsRemaining(50, "100", null)).isNull()
        assertThat(estimatedSecondsRemaining(50, "not-a-number", 1000L)).isNull()
    }

    @Test
    fun `estimatedSecondsRemaining returns null at 0 percent, 100 percent, or zero speed`() {
        assertThat(estimatedSecondsRemaining(0, "100", 1000L)).isNull()
        assertThat(estimatedSecondsRemaining(100, "100", 1000L)).isNull()
        assertThat(estimatedSecondsRemaining(50, "0", 1000L)).isNull()
    }

    @Test
    fun `toRemainingTimeLabel formats hours, minutes, and seconds`() {
        assertThat((2L * 3600 + 15 * 60).toRemainingTimeLabel()).isEqualTo("2h 15m")
        assertThat((45L * 60).toRemainingTimeLabel()).isEqualTo("45m")
        assertThat(30L.toRemainingTimeLabel()).isEqualTo("30s")
    }
}
