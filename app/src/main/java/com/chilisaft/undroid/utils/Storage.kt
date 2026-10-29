package com.chilisaft.undroid.utils

import android.content.SharedPreferences
import com.chilisaft.undroid.data.models.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class Storage @Inject constructor(
    @DefaultPreferences defaultSharedPreferences: SharedPreferences,
    private val secureStore: SecureStore
) {

    private val preferences: SharedPreferences = defaultSharedPreferences

    var apiToken: String?
        get() = secureStore.getString(KEY_API_TOKEN)
        set(value) = secureStore.putString(KEY_API_TOKEN, value)

    var serverUrl: String?
        get() = secureStore.getString(KEY_SERVER_URL)
        set(value) = secureStore.putString(KEY_SERVER_URL, value)

    /**
     * The Server screen's last primary-network result, encoded by `ServerNetworkRepository`. Kept in
     * the [SecureStore] because it holds the server's LAN IP - as sensitive as [serverUrl].
     */
    var serverNetworkCache: String?
        get() = secureStore.getString(KEY_SERVER_NETWORK_CACHE)
        set(value) = secureStore.putString(KEY_SERVER_NETWORK_CACHE, value)

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

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_SERVER_URL = "serverUrl"
        private const val KEY_SERVER_NETWORK_CACHE = "server_network_cache"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHOW_CORE_LIST = "show_core_list"
        private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
    }
}