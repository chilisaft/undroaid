package com.chilisaft.undroid.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
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

/** Holds the server URL + API key. Must match the `path` excluded from backups in res/xml/backup_rules.xml and data_extraction_rules.xml. */
internal const val SECURE_PREFS_FILE = "secure_prefs"
internal const val CREDENTIALS_KEY_ALIAS = "undroid_credentials"

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
    fun provideSecureStore(@ApplicationContext context: Context): SecureStore = SecureStore(
        context.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE),
        KeystoreStringCipher(CREDENTIALS_KEY_ALIAS)
    )
}
