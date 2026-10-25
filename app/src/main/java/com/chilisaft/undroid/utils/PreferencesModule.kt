package com.chilisaft.undroid.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultPreferences

/** Holds the server URL + API key. Must match the `path` excluded from backups in res/xml/backup_rules.xml and data_extraction_rules.xml. */
internal const val SECURE_PREFS_FILE = "secure_prefs"
internal const val CREDENTIALS_KEY_ALIAS = "undroid_credentials"

/** The pre-2026-10 EncryptedSharedPreferences file - only read once, to migrate out of it. Also excluded from backups. */
internal const val LEGACY_ENCRYPTED_PREFS_FILE = "encrypted_prefs"

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    @DefaultPreferences
    fun provideDefaultSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSecureStore(@ApplicationContext context: Context): SecureStore {
        val store = SecureStore(
            context.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE),
            KeystoreStringCipher(CREDENTIALS_KEY_ALIAS)
        )
        migrateLegacyEncryptedPreferences(context, LEGACY_ENCRYPTED_PREFS_FILE, store, Storage.SECURE_KEYS)
        return store
    }
}

/**
 * One-time move out of androidx.security's deprecated EncryptedSharedPreferences: copies [keys]
 * from the legacy file into [store] (unless the store already has a value), then deletes the
 * legacy file. If the legacy file can't be opened at all (its key is gone), it's just deleted - the
 * user logs in again. A no-op once the legacy file no longer exists, so the deprecated library is
 * only ever touched on the first launch after updating; it can be dropped entirely later (see TODO.md).
 */
internal fun migrateLegacyEncryptedPreferences(context: Context, legacyFileName: String, store: SecureStore, keys: List<String>) {
    val legacyFile = File(context.applicationInfo.dataDir, "shared_prefs/$legacyFileName.xml")
    if (!legacyFile.exists()) return
    runCatching {
        val legacy = EncryptedSharedPreferences.create(
            legacyFileName,
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        keys.forEach { key ->
            val value = legacy.getString(key, null)
            if (value != null && store.getString(key) == null) store.putString(key, value)
        }
    }
    context.deleteSharedPreferences(legacyFileName)
}
