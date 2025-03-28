package com.chilisaft.undroaid.utils

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.chilisaft.undroaid.data.repository.StorageRepository
import java.util.UUID
import javax.inject.Inject
import androidx.core.content.edit
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class Storage @Inject constructor(
    @DefaultPreferences defaultSharedPreferences: SharedPreferences,
    @EncryptedPreferences encryptedSharedPreferences: SharedPreferences
) : StorageRepository {

    private val preferences: SharedPreferences = defaultSharedPreferences
    private val securePreferences: SharedPreferences = encryptedSharedPreferences

    override var apiToken: String?
        get() = securePreferences.getString(KEY_API_TOKEN, null)
        set(value) {
            securePreferences.edit() { putString(KEY_API_TOKEN, value) }
        }

    override var serverUrl: String?
        get() = securePreferences.getString(KEY_SERVER_URL, null)
        set(value) {
            securePreferences.edit() { putString(KEY_SERVER_URL, value) }
        }

    override var lastNotification: String?
        get() = preferences.getString(KEY_LAST_NOTIFICATION, null)
        set(value) = preferences.edit() { putString(KEY_LAST_NOTIFICATION, value) }

    override var theme: Int
        get() = preferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = preferences.edit() { putInt(KEY_THEME, value) }

    override var showCoreList: Boolean
        get() = preferences.getBoolean(KEY_SHOW_CORE_LIST, false)
        set(value) = preferences.edit() { putBoolean(KEY_SHOW_CORE_LIST, value) }

    override val uuid: String
        get() {
            val string = preferences.getString(KEY_UUID, null)
            if (string != null) {
                return string
            }

            val uuid = UUID.randomUUID().toString()
            preferences.edit() { putString(KEY_UUID, uuid) }
            return uuid
        }

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_SERVER_URL = "serverUrl"
        private const val KEY_LAST_NOTIFICATION = "last_notification"
        private const val KEY_THEME = "theme"
        private const val KEY_SHOW_CORE_LIST = "show_core_list"
        private const val KEY_UUID = "uuid"
    }
}