package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.chilisaft.undroid.ui.theme.spacing

/** A bottom sheet of [ActionSheetItem]s, optionally topped by a [header] (usually a [ResourceHeaderCard]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    onDismissRequest: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = MaterialTheme.spacing
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = spacing.large)) {
            if (header != null) {
                Box(modifier = Modifier.padding(horizontal = spacing.medium)) { header() }
                Spacer(Modifier.height(spacing.small))
            }
            content()
        }
    }
}

/**
 * One tappable action row. [destructive] colors it with the error role - used for actions that
 * interrupt something running (Stop, Force Stop, Logout), matching Main's red Stop Array button.
 */
@Composable
fun ActionSheetItem(icon: ImageVector, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    val spacing = MaterialTheme.spacing
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconTint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(Modifier.width(spacing.medium))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
