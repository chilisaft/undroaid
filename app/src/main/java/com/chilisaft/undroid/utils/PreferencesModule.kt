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
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedPreferences

/** Must match the `path` excluded from backups in res/xml/backup_rules.xml and data_extraction_rules.xml. */
private const val ENCRYPTED_PREFS_FILE = "encrypted_prefs"

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
    @EncryptedPreferences
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // If the file can't be decrypted - its Keystore key is gone, e.g. it came from a backup of
        // another device made before backup_rules.xml excluded it - creating it throws on every
        // launch. Start over with an empty file instead: the user only has to log in again.
        return try {
            createEncryptedSharedPreferences(context)
        } catch (e: Exception) {
            context.deleteSharedPreferences(ENCRYPTED_PREFS_FILE)
            createEncryptedSharedPreferences(context)
        }
    }

    // Note: this String-alias overload of create() is deprecated, as is security-crypto as a whole
    // - migrating off it is tracked separately.
    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            ENCRYPTED_PREFS_FILE,
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}
