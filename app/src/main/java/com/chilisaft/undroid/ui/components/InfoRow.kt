package com.chilisaft.undroid.ui.components

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.chilisaft.undroid.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * A "Label ........ value" line inside a card (account sheet, Server screen). [onClick] makes the
 * row tappable (e.g. open a URL, reveal a hidden value); the value is then drawn in the primary
 * color to say so. [copyText] makes a long-press copy it to the clipboard - passed separately from
 * [value] because what's shown isn't always what's worth copying (a hidden WAN IP shows a hint).
 */
@Composable
fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null, copyText: String? = null) {
    val spacing = MaterialTheme.spacing
    val copy = copyText?.let { rememberCopyToClipboard(label, it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || copy != null) {
                    Modifier.combinedClickable(onClick = onClick ?: {}, onLongClick = copy)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(spacing.medium))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Copies [text] with a haptic tick. Android 13+ confirms clipboard writes itself, so the toast is
 * only for older versions (minSdk 28) - showing both would announce the copy twice.
 */
@Composable
fun rememberCopyToClipboard(label: String, text: String): () -> Unit {
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, text))) }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
        }
    }
}
