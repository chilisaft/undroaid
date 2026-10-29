package com.chilisaft.undroid.ui.theme

import com.chilisaft.undroid.data.models.TemperatureUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitsTest {

    @Test
    fun `formats Celsius readings in the chosen unit`() {
        assertThat(TemperatureUnit.CELSIUS.format(39.85)).isEqualTo("40°C")
        assertThat(TemperatureUnit.FAHRENHEIT.format(100.0)).isEqualTo("212°F")
        assertThat(TemperatureUnit.FAHRENHEIT.format(39.85, decimals = 1)).isEqualTo("103.7°F")
    }
}
