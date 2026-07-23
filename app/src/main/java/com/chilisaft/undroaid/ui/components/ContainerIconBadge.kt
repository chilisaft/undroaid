package com.chilisaft.undroaid.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * A Docker container's icon, loaded from [iconUrl] via Coil - falls back to a generic layers
 * glyph when there's no icon, or it fails to load. Shared between the Dashboard's docker
 * preview (compact, default [size]) and the full Docker tab list (larger, for legibility).
 */
@Composable
fun ContainerIconBadge(iconUrl: String?, isRunning: Boolean, size: Dp = 40.dp) {
    val tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val imageSize = size * 0.7f
    val fallbackIconSize = size * 0.5f
    val loadingSize = size * 0.35f
    Surface(
        modifier = Modifier.size(size),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (iconUrl.isNullOrBlank()) {
                Icon(Icons.Filled.Layers, contentDescription = null, tint = tint, modifier = Modifier.size(fallbackIconSize))
            } else {
                SubcomposeAsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(imageSize),
                    loading = { CircularProgressIndicator(modifier = Modifier.size(loadingSize), strokeWidth = 2.dp) },
                    error = { Icon(Icons.Filled.Layers, contentDescription = null, tint = tint, modifier = Modifier.size(fallbackIconSize)) }
                )
            }
        }
    }
}
