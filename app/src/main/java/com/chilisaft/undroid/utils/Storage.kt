package com.chilisaft.undroid.utils

import android.content.SharedPreferences
import com.chilisaft.undroid.data.models.DashboardCards
import com.chilisaft.undroid.data.models.LiveUpdateSpeed
import com.chilisaft.undroid.data.models.StartTab
import com.chilisaft.undroid.data.models.TemperatureUnit
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

    var startTab: StartTab
        get() = enumPreference(KEY_START_TAB, StartTab.HOME)
        set(value) = preferences.edit() { putString(KEY_START_TAB, value.name) }

    var liveUpdateSpeed: LiveUpdateSpeed
        get() = enumPreference(KEY_LIVE_UPDATE_SPEED, LiveUpdateSpeed.FAST)
        set(value) = preferences.edit() { putString(KEY_LIVE_UPDATE_SPEED, value.name) }

    var temperatureUnit: TemperatureUnit
        get() = enumPreference(KEY_TEMPERATURE_UNIT, TemperatureUnit.CELSIUS)
        set(value) = preferences.edit() { putString(KEY_TEMPERATURE_UNIT, value.name) }

    var dashboardCards: DashboardCards
        get() = DashboardCards(
            sensors = preferences.getBoolean(KEY_CARD_SENSORS, true),
            docker = preferences.getBoolean(KEY_CARD_DOCKER, true),
            vms = preferences.getBoolean(KEY_CARD_VMS, true)
        )
        set(value) = preferences.edit() {
            putBoolean(KEY_CARD_SENSORS, value.sensors)
            putBoolean(KEY_CARD_DOCKER, value.docker)
            putBoolean(KEY_CARD_VMS, value.vms)
        }

    /** A stored enum by name, or [default] when unset or no longer a valid value. */
    private inline fun <reified T : Enum<T>> enumPreference(key: String, default: T): T =
        preferences.getString(key, null)?.let { stored -> runCatching { enumValueOf<T>(stored) }.getOrNull() } ?: default

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_SERVER_URL = "serverUrl"
        private const val KEY_SERVER_NETWORK_CACHE = "server_network_cache"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHOW_CORE_LIST = "show_core_list"
        private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
        private const val KEY_START_TAB = "start_tab"
        private const val KEY_LIVE_UPDATE_SPEED = "live_update_speed"
        private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        private const val KEY_CARD_SENSORS = "dashboard_card_sensors"
        private const val KEY_CARD_DOCKER = "dashboard_card_docker"
        private const val KEY_CARD_VMS = "dashboard_card_vms"
    }
}