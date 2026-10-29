package com.chilisaft.undroid.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.chilisaft.undroid.data.models.ParityHistoryEntry
import com.chilisaft.undroid.data.models.ParityHistoryStatus
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.StatusLine
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.bytesPerSecondToLabel
import com.chilisaft.undroid.utils.toDateLabel
import com.chilisaft.undroid.utils.toRemainingTimeLabel

private const val RECENT_COUNT = 3

/** The newest few parity checks, with "Show All" opening the full history in a sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParityHistoryCard(entries: List<ParityHistoryEntry>) {
    val spacing = MaterialTheme.spacing
    var showAll by remember { mutableStateOf(false) }
    if (entries.isEmpty()) {
        EmptyState("No parity checks recorded")
        return
    }
    AppCard {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            entries.take(RECENT_COUNT).forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                ParityHistoryRow(entry)
            }
            if (entries.size > RECENT_COUNT) {
                TextButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show All (${entries.size})")
                }
            }
        }
    }
    if (showAll) {
        ModalBottomSheet(onDismissRequest = { showAll = false }) {
            Text(
                "Parity History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = spacing.medium)
            )
            LazyColumn(
                modifier = Modifier.padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                itemsIndexed(entries) { index, entry ->
                    if (index > 0) HorizontalDivider()
                    ParityHistoryRow(entry, modifier = Modifier.padding(vertical = spacing.extraSmall))
                }
            }
        }
    }
}

@Composable
private fun ParityHistoryRow(entry: ParityHistoryEntry, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.date?.toDateLabel() ?: "Unknown date",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            StatusLine(tone = entry.tone, text = entry.status.label)
        }
        Text(
            entry.detailLine(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Errors make a check stand out whatever its status; a cancelled one is neutral. */
private val ParityHistoryEntry.tone: StatusTone
    get() = when {
        status == ParityHistoryStatus.FAILED || (errors ?: 0) > 0 -> StatusTone.Error
        status == ParityHistoryStatus.COMPLETED -> StatusTone.Ok
        else -> StatusTone.Neutral
    }

/** "10h 28m · 159.2 MB/s · 0 errors" - a cancelled check reports speed 0, which is left out. */
internal fun ParityHistoryEntry.detailLine(): String = listOfNotNull(
    durationSeconds?.toRemainingTimeLabel(),
    bytesPerSecond?.takeIf { it > 0 }?.bytesPerSecondToLabel(),
    errors?.let { if (it == 1) "1 error" else "%,d errors".format(it) }
).joinToString(" · ")
