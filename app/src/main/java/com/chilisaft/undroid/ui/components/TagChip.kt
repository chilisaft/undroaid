package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chilisaft.undroid.ui.theme.spacing

/** A small uppercase tag next to a title - a device's role on Main ("PARITY"), a share's "CACHE". */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = 2.dp)
        )
    }
}
