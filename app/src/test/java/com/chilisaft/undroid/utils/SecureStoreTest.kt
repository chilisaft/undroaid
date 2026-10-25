package com.chilisaft.undroid.utils

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SecureStoreTest {

    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var store: SecureStore

    /** Reversible stand-in for the Keystore cipher; "corrupt" simulates a value whose key is gone. */
    private val fakeCipher = object : StringCipher {
        override fun encrypt(plaintext: String) = "enc($plaintext)"
        override fun decrypt(ciphertext: String): String {
            check(ciphertext.startsWith("enc(")) { "can't decrypt" }
            return ciphertext.removePrefix("enc(").removeSuffix(")")
        }
    }

    @Before
    fun setUp() {
        preferences = mockk()
        editor = mockk(relaxed = true)
        every { preferences.edit() } returns editor
        store = SecureStore(preferences, fakeCipher)
    }

    @Test
    fun `values are written encrypted, never as plaintext`() {
        store.putString("api_token", "secret")

        verify { editor.putString("api_token", "enc(secret)") }
        verify(exactly = 0) { editor.putString(any(), "secret") }
    }

    @Test
    fun `values are decrypted on read`() {
        every { preferences.getString("api_token", null) } returns "enc(secret)"

        assertThat(store.getString("api_token")).isEqualTo("secret")
    }

    @Test
    fun `writing null removes the value`() {
        store.putString("api_token", null)

        verify { editor.remove("api_token") }
    }

    @Test
    fun `a value that can't be decrypted reads as absent instead of crashing`() {
        every { preferences.getString("api_token", null) } returns "corrupt"

        assertThat(store.getString("api_token")).isNull()
    }
}
