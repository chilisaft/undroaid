package com.chilisaft.undroid.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chilisaft.undroid.data.models.NetworkRate
import com.chilisaft.undroid.data.models.SensorKind
import com.chilisaft.undroid.data.models.TemperatureLevel
import com.chilisaft.undroid.data.models.TemperatureOverview
import com.chilisaft.undroid.data.models.TemperatureSensorReading
import com.chilisaft.undroid.data.models.TemperatureUnit
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.StatusLine
import com.chilisaft.undroid.ui.theme.LocalTemperatureUnit
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.format
import com.chilisaft.undroid.ui.theme.color
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.bytesPerSecondToLabel

/** Same frame as the CPU/memory cards: small label + icon on top, then the content. */
@Composable
private fun SensorCard(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val spacing = MaterialTheme.spacing
    AppCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(spacing.small))
            content()
        }
    }
}

/** The CPU temperature (or the hottest sensor) with the server's own warm/hot count; tap for every sensor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureCard(temperatures: TemperatureOverview, modifier: Modifier = Modifier) {
    var showSensors by remember { mutableStateOf(false) }
    val headline = temperatures.headline
    val tone = headline?.level?.tone ?: StatusTone.Neutral
    SensorCard(
        modifier = modifier,
        label = "TEMPERATURE",
        icon = Icons.Filled.Thermostat,
        iconTint = if (tone == StatusTone.Ok) MaterialTheme.colorScheme.primary else tone.color,
        onClick = if (temperatures.sensors.isNotEmpty()) ({ showSensors = true }) else null
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            val unit = LocalTemperatureUnit.current
            Text(headline?.celsius?.let { "%.0f".format(unit.fromCelsius(it)) } ?: "—", style = MaterialTheme.typography.headlineLarge)
            if (headline != null) {
                Text(
                    unit.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp, start = MaterialTheme.spacing.extraSmall)
                )
            }
        }
        Text(
            temperatures.summaryLine(headline),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (showSensors) {
        ModalBottomSheet(onDismissRequest = { showSensors = false }) {
            SensorList(temperatures.sensors)
        }
    }
}

/** "CPU · all normal", "CPU · 1 warm", "NVMe · 2 hot" - the headline's kind, then the server's counts. */
internal fun TemperatureOverview.summaryLine(headline: TemperatureSensorReading?): String {
    val counts = when {
        criticalCount > 0 -> "$criticalCount hot"
        warningCount > 0 -> "$warningCount warm"
        sensors.isEmpty() -> "no sensors"
        else -> "all normal"
    }
    return listOfNotNull(headline?.kind?.label, counts).joinToString(" · ")
}

@Composable
private fun SensorList(sensors: List<TemperatureSensorReading>) {
    val spacing = MaterialTheme.spacing
    val unit = LocalTemperatureUnit.current
    Text("Temperatures", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = spacing.medium))
    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        itemsIndexed(sensors, key = { _, sensor -> sensor.id }) { index, sensor ->
            if (index > 0) HorizontalDivider()
            Row(modifier = Modifier.padding(vertical = spacing.extraSmall), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sensor.kind.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(sensor.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    sensor.limitsLine(unit)?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusLine(tone = sensor.level.tone, text = unit.format(sensor.celsius, decimals = 1), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Spacer(Modifier.height(spacing.large))
}

private fun TemperatureSensorReading.limitsLine(unit: TemperatureUnit): String? = listOfNotNull(
    warningCelsius?.let { "warm at ${unit.format(it)}" },
    criticalCelsius?.let { "hot at ${unit.format(it)}" }
).joinToString(" · ").ifEmpty { null }

private val TemperatureLevel.tone: StatusTone
    get() = when (this) {
        TemperatureLevel.NORMAL -> StatusTone.Ok
        TemperatureLevel.WARNING -> StatusTone.Warning
        TemperatureLevel.CRITICAL -> StatusTone.Error
        TemperatureLevel.UNKNOWN -> StatusTone.Neutral
    }

/** Download and upload of the main interface, refreshed with the temperature. */
@Composable
fun NetworkCard(rate: NetworkRate?, modifier: Modifier = Modifier, emptyText: String = "No interface") {
    SensorCard(
        modifier = modifier,
        label = rate?.interfaceName?.let { "NETWORK · $it" } ?: "NETWORK",
        icon = Icons.Filled.SettingsEthernet,
        iconTint = MaterialTheme.colorScheme.tertiary
    ) {
        if (rate == null) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            RateLine(Icons.Filled.ArrowDownward, rate.rxBytesPerSecond.bytesPerSecondToLabel(), "Download")
            RateLine(Icons.Filled.ArrowUpward, rate.txBytesPerSecond.bytesPerSecondToLabel(), "Upload")
        }
    }
}

@Composable
private fun RateLine(icon: ImageVector, text: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

internal val previewTemperatures = TemperatureOverview(
    sensors = listOf(
        TemperatureSensorReading("1", "coretemp-isa-0000 Package id 0", SensorKind.CPU_PACKAGE, 42.0, TemperatureLevel.NORMAL, 70.0, 85.0),
        TemperatureSensorReading("2", "nvme-pci-0200 Composite", SensorKind.NVME, 39.85, TemperatureLevel.NORMAL, null, null)
    ),
    warningCount = 0,
    criticalCount = 0
)
