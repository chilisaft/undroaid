package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The three badge sizes in use - paired with [ResourceItemSize] so a row's badge and text always scale together. */
object IconBadgeSize {
    val Compact: Dp = 40.dp
    val Regular: Dp = 52.dp
    val Large: Dp = 56.dp
}

/** The rounded square every list-row icon sits in ([ContainerIconBadge], [VmIconBadge]). */
@Composable
fun IconBadge(size: Dp, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = Modifier.size(size),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

/** Glyph tint for a badge: the brand primary while the thing it represents is running, muted otherwise. */
@Composable
@ReadOnlyComposable
fun iconBadgeTint(active: Boolean): Color =
    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
