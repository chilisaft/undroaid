package com.chilisaft.undroid.utils

import android.content.SharedPreferences
import com.chilisaft.undroid.data.models.ThemeMode
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class StorageTest {

    private lateinit var defaultPrefs: SharedPreferences
    private lateinit var encryptedPrefs: SharedPreferences
    private lateinit var defaultEditor: SharedPreferences.Editor
    private lateinit var encryptedEditor: SharedPreferences.Editor
    private lateinit var storage: Storage

    @Before
    fun setUp() {
        defaultPrefs = mockk()
        encryptedPrefs = mockk()
        defaultEditor = mockk(relaxed = true)
        encryptedEditor = mockk(relaxed = true)

        every { defaultPrefs.edit() } returns defaultEditor
        every { encryptedPrefs.edit() } returns encryptedEditor

        storage = Storage(defaultPrefs, encryptedPrefs)
    }

    @Test
    fun `apiToken is stored in encrypted preferences`() {
        val token = "test_token"
        storage.apiToken = token
        verify { encryptedEditor.putString("api_token", token) }

        every { encryptedPrefs.getString("api_token", null) } returns token
        assertThat(storage.apiToken).isEqualTo(token)
    }

    @Test
    fun `serverUrl is stored in encrypted preferences`() {
        val url = "http://test.com"
        storage.serverUrl = url
        verify { encryptedEditor.putString("serverUrl", url) }

        every { encryptedPrefs.getString("serverUrl", null) } returns url
        assertThat(storage.serverUrl).isEqualTo(url)
    }

    @Test
    fun `themeMode is stored in default preferences`() {
        storage.themeMode = ThemeMode.DARK
        verify { defaultEditor.putString("theme_mode", "DARK") }

        every { defaultPrefs.getString("theme_mode", null) } returns "DARK"
        assertThat(storage.themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `themeMode defaults to SYSTEM when nothing is stored`() {
        every { defaultPrefs.getString("theme_mode", null) } returns null
        assertThat(storage.themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `themeMode defaults to SYSTEM for an unrecognized stored value`() {
        every { defaultPrefs.getString("theme_mode", null) } returns "not-a-real-mode"
        assertThat(storage.themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `showCoreList is stored in default preferences`() {
        storage.showCoreList = true
        verify { defaultEditor.putBoolean("show_core_list", true) }

        every { defaultPrefs.getBoolean("show_core_list", false) } returns true
        assertThat(storage.showCoreList).isTrue()
    }

    @Test
    fun `useDynamicColor is stored in default preferences and defaults to false`() {
        storage.useDynamicColor = true
        verify { defaultEditor.putBoolean("use_dynamic_color", true) }

        every { defaultPrefs.getBoolean("use_dynamic_color", false) } returns true
        assertThat(storage.useDynamicColor).isTrue()
    }
}
