package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * A Docker container's icon, loaded from [iconUrl] via Coil - falls back to a generic layers
 * glyph (the same one the Docker tab uses) when there's no icon, or it fails to load.
 */
@Composable
fun ContainerIconBadge(iconUrl: String?, isRunning: Boolean, size: Dp = IconBadgeSize.Compact) {
    val tint = iconBadgeTint(isRunning)
    val fallbackIconSize = size * 0.5f
    IconBadge(size) {
        if (iconUrl.isNullOrBlank()) {
            Icon(Icons.Filled.Layers, contentDescription = null, tint = tint, modifier = Modifier.size(fallbackIconSize))
        } else {
            SubcomposeAsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier.size(size * 0.7f),
                loading = { CircularProgressIndicator(modifier = Modifier.size(size * 0.35f), strokeWidth = 2.dp) },
                error = { Icon(Icons.Filled.Layers, contentDescription = null, tint = tint, modifier = Modifier.size(fallbackIconSize)) }
            )
        }
    }
}
