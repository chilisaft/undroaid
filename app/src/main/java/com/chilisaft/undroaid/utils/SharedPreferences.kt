package com.chilisaft.undroaid.utils

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
import jakarta.inject.Qualifier
import jakarta.inject.Singleton

@Qualifier
@Singleton
@Retention(AnnotationRetention.BINARY)
annotation class DefaultPreferences

@Qualifier
@Singleton
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedPreferences

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferences {

    @Provides
    @DefaultPreferences
    fun provideDefaultSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @EncryptedPreferences
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // This is the correct, non-deprecated method for creating EncryptedSharedPreferences.
        // It takes the master key alias as a String and manages the key lifecycle internally.
        return EncryptedSharedPreferences.create(
            "encrypted_prefs", // 1. The file name
            MasterKey.DEFAULT_MASTER_KEY_ALIAS, // 2. The master key alias
            context, // 3. The context
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
