package com.chilisaft.undroid.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.chilisaft.undroid.data.models.TemperatureUnit

/** The temperature unit chosen in Settings, provided at the app root (`UndroidActivity`). */
val LocalTemperatureUnit = staticCompositionLocalOf { TemperatureUnit.CELSIUS }

/** A Celsius reading in [this] unit with its symbol, e.g. 39.85 -> "40°C" or "103.7°F" with one decimal. */
fun TemperatureUnit.format(celsius: Double, decimals: Int = 0): String =
    "%.${decimals}f%s".format(fromCelsius(celsius), symbol)
