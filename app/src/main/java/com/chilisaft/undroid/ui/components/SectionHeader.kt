package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chilisaft.undroid.ui.theme.spacing

/**
 * A small uppercase label above a group of cards (Main's device sections, Settings' sections).
 * Carries a little extra top padding so, inside a list spaced at `spacing.itemSpacing`, it sits
 * closer to the group it introduces than to the one above it.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = MaterialTheme.spacing.small)
    )
}
