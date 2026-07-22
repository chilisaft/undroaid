package com.chilisaft.undroaid.utils

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SharedPreferencesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun tearDown() {
        SharedPreferences.provideEncryptedSharedPreferences(context).edit().clear().commit()
        SharedPreferences.provideDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun encryptedPreferences_roundTripsAValueThroughTheRealKeystore() {
        val secret = "test-api-token-12345"
        SharedPreferences.provideEncryptedSharedPreferences(context)
            .edit().putString("api_token", secret).commit()

        // A fresh instance forces a real decrypt via the Android Keystore, not just an in-memory read.
        val reread = SharedPreferences.provideEncryptedSharedPreferences(context)
            .getString("api_token", null)

        assertThat(reread).isEqualTo(secret)
    }

    @Test
    fun encryptedPreferences_storesCiphertextOnDisk_notThePlaintextValue() {
        val secret = "super-secret-value-should-not-appear-on-disk"
        SharedPreferences.provideEncryptedSharedPreferences(context)
            .edit().putString("api_token", secret).commit()

        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/encrypted_prefs.xml")
        assertThat(prefsFile.exists()).isTrue()
        assertThat(prefsFile.readText()).doesNotContain(secret)
    }

    @Test
    fun defaultPreferences_roundTripsAValue() {
        SharedPreferences.provideDefaultSharedPreferences(context)
            .edit().putString("theme_test_key", "dark").commit()

        val reread = SharedPreferences.provideDefaultSharedPreferences(context)
            .getString("theme_test_key", null)

        assertThat(reread).isEqualTo("dark")
    }
}
