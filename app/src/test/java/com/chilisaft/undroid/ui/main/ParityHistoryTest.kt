package com.chilisaft.undroid.ui.main

import com.chilisaft.undroid.data.models.ParityHistoryEntry
import com.chilisaft.undroid.data.models.ParityHistoryStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParityHistoryTest {

    @Test
    fun `detail line shows duration, speed and errors`() {
        val entry = ParityHistoryEntry(null, ParityHistoryStatus.COMPLETED, 37686, 159241497.0, 0)
        assertThat(entry.detailLine()).isEqualTo("10h 28m · 159.2 MB/s · 0 errors")
    }

    @Test
    fun `a cancelled check's zero speed is left out`() {
        val entry = ParityHistoryEntry(null, ParityHistoryStatus.CANCELLED, 79, 0.0, 1)
        assertThat(entry.detailLine()).isEqualTo("1m · 1 error")
    }
}
