package com.chilisaft.undroaid.data.repository

import com.chilisaft.undroaid.data.models.ThemeMode
import com.chilisaft.undroaid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var storage: Storage
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        storage = mockk(relaxed = true) {
            every { themeMode } returns ThemeMode.SYSTEM
            every { showCoreList } returns false
            every { useDynamicColor } returns true
        }
        repository = SettingsRepository(storage)
    }

    @Test
    fun `themeMode starts from the persisted value`() {
        assertThat(repository.themeMode.value).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `setThemeMode persists and updates the in-memory value immediately`() {
        repository.setThemeMode(ThemeMode.DARK)

        verify { storage.themeMode = ThemeMode.DARK }
        assertThat(repository.themeMode.value).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `showCoreList starts from the persisted value`() {
        assertThat(repository.showCoreList.value).isFalse()
    }

    @Test
    fun `setShowCoreList persists and updates the in-memory value immediately`() {
        repository.setShowCoreList(true)

        verify { storage.showCoreList = true }
        assertThat(repository.showCoreList.value).isTrue()
    }

    @Test
    fun `useDynamicColor starts from the persisted value`() {
        assertThat(repository.useDynamicColor.value).isTrue()
    }

    @Test
    fun `setUseDynamicColor persists and updates the in-memory value immediately`() {
        repository.setUseDynamicColor(false)

        verify { storage.useDynamicColor = false }
        assertThat(repository.useDynamicColor.value).isFalse()
    }
}
