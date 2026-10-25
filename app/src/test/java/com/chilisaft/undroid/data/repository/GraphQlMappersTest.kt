package com.chilisaft.undroid.data.repository

import com.chilisaft.undroid.data.models.DeviceHealth
import com.chilisaft.undroid.graphql.type.ArrayDiskStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GraphQlMappersTest {

    @Test
    fun `device health comes from the status enum - OK, unassigned slot, or a real problem`() {
        assertThat(ArrayDiskStatus.DISK_OK.toDeviceHealth()).isEqualTo(DeviceHealth.OK)
        assertThat((null as ArrayDiskStatus?).toDeviceHealth()).isEqualTo(DeviceHealth.OK)
        assertThat(ArrayDiskStatus.DISK_NP.toDeviceHealth()).isEqualTo(DeviceHealth.NOT_PRESENT)
        listOf(
            ArrayDiskStatus.DISK_NP_MISSING, ArrayDiskStatus.DISK_INVALID, ArrayDiskStatus.DISK_WRONG,
            ArrayDiskStatus.DISK_DSBL, ArrayDiskStatus.DISK_NP_DSBL, ArrayDiskStatus.DISK_DSBL_NEW,
            ArrayDiskStatus.DISK_NEW, ArrayDiskStatus.UNKNOWN__
        ).forEach { assertThat(it.toDeviceHealth()).isEqualTo(DeviceHealth.PROBLEM) }
    }

    @Test
    fun `allHealthy ignores unassigned slots but not real problems`() {
        assertThat(listOf(ArrayDiskStatus.DISK_OK, ArrayDiskStatus.DISK_NP).allHealthy()).isTrue()
        assertThat(listOf(ArrayDiskStatus.DISK_OK, ArrayDiskStatus.DISK_DSBL).allHealthy()).isFalse()
    }
}
