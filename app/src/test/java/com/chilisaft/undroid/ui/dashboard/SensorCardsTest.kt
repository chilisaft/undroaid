package com.chilisaft.undroid.ui.dashboard

import com.chilisaft.undroid.data.models.TemperatureOverview
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensorCardsTest {

    @Test
    fun `summary line names the headline sensor and the server's counts`() {
        assertThat(previewTemperatures.summaryLine(previewTemperatures.headline)).isEqualTo("CPU · all normal")
        val warm = previewTemperatures.copy(warningCount = 2)
        assertThat(warm.summaryLine(warm.headline)).isEqualTo("CPU · 2 warm")
        val hot = previewTemperatures.copy(warningCount = 2, criticalCount = 1)
        assertThat(hot.summaryLine(hot.headline)).isEqualTo("CPU · 1 hot")
        val none = TemperatureOverview(emptyList(), 0, 0)
        assertThat(none.summaryLine(none.headline)).isEqualTo("no sensors")
    }
}
