package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chilisaft.undroid.data.models.ParityCheckInfo
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.color
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.estimatedSecondsRemaining
import com.chilisaft.undroid.utils.toRemainingTimeLabel

/**
 * "Parity Check" title, then either progress (bar + percent + speed/errors/ETA) while a check is
 * running or paused, or just the last result's label otherwise. Shared by the Dashboard's banner
 * and Main's Array Operation card so both read identically.
 */
@Composable
fun ParityCheckProgress(parityCheck: ParityCheckInfo, paritySizeKb: Long?, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val isActive = parityCheck.running || parityCheck.paused
    val accentColor = (if (parityCheck.paused) StatusTone.Warning else StatusTone.Ok).color
    val percent = parityCheck.progressPercent ?: 0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Parity Check", style = MaterialTheme.typography.titleSmall)
            if (isActive) {
                Text("$percent%", style = MaterialTheme.typography.titleSmall, color = accentColor)
            }
        }
        if (isActive) {
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = accentColor
            )
        }
        Text(
            text = if (isActive) parityCheckDetails(parityCheck, paritySizeKb) else parityCheck.statusLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parityCheckDetails(parityCheck: ParityCheckInfo, paritySizeKb: Long?): String = buildString {
    append(if (parityCheck.paused) "Paused" else "Running")
    parityCheck.speed?.let { append(" · $it MB/s") }
    append(" · ${parityCheck.errors ?: 0} errors")
    estimatedSecondsRemaining(parityCheck.progressPercent, parityCheck.speed, paritySizeKb)?.let {
        append(" · ~${it.toRemainingTimeLabel()} remaining")
    }
}
