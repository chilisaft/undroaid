package com.chilisaft.undroid.data.models

/** The bottom-bar tab the app opens on. [route] matches `BottomNavDestination`'s routes. */
enum class StartTab(val label: String, val route: String) {
    HOME("Home", "dashboard"), MAIN("Main", "main"), DOCKER("Docker", "docker"), VMS("VMs", "vms")
}

/**
 * How often the Dashboard refreshes its live readings while it's on screen. [metricsMillis] is
 * CPU/memory, [sensorsMillis] temperature/network (slower - the network query takes ~1 s). Null
 * for [OFF]: no polling, pull to refresh only.
 */
enum class LiveUpdateSpeed(val label: String, val metricsMillis: Long?, val sensorsMillis: Long?) {
    FAST("1 s", 1_000, 5_000),
    NORMAL("3 s", 3_000, 10_000),
    SLOW("10 s", 10_000, 30_000),
    OFF("Off", null, null)
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"), FAHRENHEIT("°F");

    /** A Celsius value in this unit. */
    fun fromCelsius(celsius: Double): Double = if (this == FAHRENHEIT) celsius * 9 / 5 + 32 else celsius
}

/** Which optional Dashboard cards are shown - hidden ones aren't fetched or polled either. */
data class DashboardCards(val sensors: Boolean = true, val docker: Boolean = true, val vms: Boolean = true)

data class ServerVersions(val unraid: String?, val api: String?)
