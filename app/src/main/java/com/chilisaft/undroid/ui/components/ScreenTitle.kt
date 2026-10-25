package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chilisaft.undroid.ui.theme.spacing

/**
 * A top-bar title in the app's bold, tightly-tracked style - used by both [TabTopBar] (with a
 * leading [icon]) and [DetailTopBar] (without one, since the back arrow takes that slot).
 */
@Composable
fun ScreenTitle(text: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.5).sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
