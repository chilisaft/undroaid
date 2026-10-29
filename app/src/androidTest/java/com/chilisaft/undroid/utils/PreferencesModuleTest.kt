package com.chilisaft.undroid.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

/**
 * Credential storage against the real Android Keystore. Uses its own key alias and file names
 * throughout, so it's safe to run on a device with a real login - it never touches the app's
 * actual credentials (unlike its predecessor, which cleared them in tearDown).
 */
@RunWith(AndroidJUnit4::class)
class PreferencesModuleTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun testStore() = SecureStore(
        context.getSharedPreferences(TEST_FILE, Context.MODE_PRIVATE),
        KeystoreStringCipher(TEST_ALIAS)
    )

    @After
    fun tearDown() {
        // commit() waits for any apply() still in flight, so the delete below can't be undone by a late write.
        context.getSharedPreferences(TEST_FILE, Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteSharedPreferences(TEST_FILE)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(TEST_ALIAS)
        PreferencesModule.provideDefaultSharedPreferences(context).edit().remove(TEST_DEFAULT_KEY).commit()
    }

    @Test
    fun secureStore_roundTripsAValueThroughTheRealKeystore() {
        testStore().putString("api_token", "test-api-token-12345")

        // A fresh instance forces a real decrypt via the Android Keystore, not an in-memory read.
        assertThat(testStore().getString("api_token")).isEqualTo("test-api-token-12345")
    }

    @Test
    fun secureStore_storesCiphertextOnDisk_notThePlaintextValue() {
        val secret = "super-secret-value-should-not-appear-on-disk"
        testStore().putString("api_token", secret)

        // SecureStore writes with apply() (asynchronous) - give the disk write a moment to land.
        val file = File(context.applicationInfo.dataDir, "shared_prefs/$TEST_FILE.xml")
        val deadline = System.currentTimeMillis() + 2_000
        while (!file.exists() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertThat(file.exists()).isTrue()
        assertThat(file.readText()).doesNotContain(secret)
    }

    @Test
    fun keystoreCipher_usesAFreshIvEveryTime() {
        val cipher = KeystoreStringCipher(TEST_ALIAS)

        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"))
    }

    @Test
    fun defaultPreferences_roundTripsAValue() {
        PreferencesModule.provideDefaultSharedPreferences(context).edit().putString(TEST_DEFAULT_KEY, "dark").commit()

        assertThat(PreferencesModule.provideDefaultSharedPreferences(context).getString(TEST_DEFAULT_KEY, null)).isEqualTo("dark")
    }

    private companion object {
        const val TEST_FILE = "secure_prefs_instrumented_test"
        const val TEST_ALIAS = "undroid_credentials_instrumented_test"
        const val TEST_DEFAULT_KEY = "instrumented_test_key"
    }
}
