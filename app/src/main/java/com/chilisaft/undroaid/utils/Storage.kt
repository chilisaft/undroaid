package com.chilisaft.undroaid.utils

import android.content.SharedPreferences
import com.chilisaft.undroaid.data.models.ThemeMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class Storage @Inject constructor(
    @DefaultPreferences defaultSharedPreferences: SharedPreferences,
    @EncryptedPreferences encryptedSharedPreferences: SharedPreferences
) {

    private val preferences: SharedPreferences = defaultSharedPreferences
    private val securePreferences: SharedPreferences = encryptedSharedPreferences

    var apiToken: String?
        get() = securePreferences.getString(KEY_API_TOKEN, null)
        set(value) {
            securePreferences.edit() { putString(KEY_API_TOKEN, value) }
        }

    var serverUrl: String?
        get() = securePreferences.getString(KEY_SERVER_URL, null)
        set(value) {
            securePreferences.edit() { putString(KEY_SERVER_URL, value) }
        }

    var lastNotification: String?
        get() = preferences.getString(KEY_LAST_NOTIFICATION, null)
        set(value) = preferences.edit() { putString(KEY_LAST_NOTIFICATION, value) }

    var themeMode: ThemeMode
        get() = preferences.getString(KEY_THEME_MODE, null)?.let { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrNull()
        } ?: ThemeMode.SYSTEM
        set(value) = preferences.edit() { putString(KEY_THEME_MODE, value.name) }

    var showCoreList: Boolean
        get() = preferences.getBoolean(KEY_SHOW_CORE_LIST, false)
        set(value) = preferences.edit() { putBoolean(KEY_SHOW_CORE_LIST, value) }

    // Defaults to false so the app's own forest theme (see ui/theme/Color.kt) is what new
    // installs actually see, rather than deferring to the system's wallpaper-derived Android 12+
    // dynamic color on every device that supports it - users can still opt into dynamic color
    // from Settings.
    var useDynamicColor: Boolean
        get() = preferences.getBoolean(KEY_USE_DYNAMIC_COLOR, false)
        set(value) = preferences.edit() { putBoolean(KEY_USE_DYNAMIC_COLOR, value) }

    val uuid: String
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
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHOW_CORE_LIST = "show_core_list"
        private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
        private const val KEY_UUID = "uuid"
    }
}