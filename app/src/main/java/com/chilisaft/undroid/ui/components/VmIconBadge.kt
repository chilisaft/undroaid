package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.chilisaft.undroid.data.models.VmRunState

/**
 * A VM's icon badge - unlike Docker containers, the schema has no per-VM icon/image, so every
 * row uses the same computer glyph, tinted by running state.
 */
@Composable
fun VmIconBadge(state: VmRunState, size: Dp = IconBadgeSize.Compact) {
    IconBadge(size) {
        Icon(
            Icons.Filled.Computer,
            contentDescription = null,
            tint = iconBadgeTint(state == VmRunState.RUNNING),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
