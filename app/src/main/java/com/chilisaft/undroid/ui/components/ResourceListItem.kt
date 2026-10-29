package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.spacing

/**
 * - [Compact]: a preview row inside a Dashboard card
 * - [Regular]: a row in a tab's full list (Docker, VMs)
 * - [Header]: the item's summary at the top of its action sheet
 */
enum class ResourceItemSize { Compact, Regular, Header }

private val ResourceItemSize.badgeSize: Dp
    get() = when (this) {
        ResourceItemSize.Compact -> IconBadgeSize.Compact
        ResourceItemSize.Regular -> IconBadgeSize.Regular
        ResourceItemSize.Header -> IconBadgeSize.Large
    }

@Composable
private fun ResourceItemSize.titleStyle(): TextStyle = when (this) {
    ResourceItemSize.Compact -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    ResourceItemSize.Regular -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
    ResourceItemSize.Header -> MaterialTheme.typography.titleMedium
}

/**
 * A container/VM row's content: icon badge, name (with an optional [tag] chip, e.g. "Update"),
 * optional [subtitle], and a [StatusLine] - no
 * surface of its own, so it can sit in a list card ([ResourceRowCard]), a sheet header
 * ([ResourceHeaderCard]), or directly inside a larger card (the Dashboard previews).
 * [leading] receives the badge size matching [size].
 */
@Composable
fun ResourceListItem(
    title: String,
    tone: StatusTone,
    statusText: String,
    leading: @Composable (badgeSize: Dp) -> Unit,
    modifier: Modifier = Modifier,
    size: ResourceItemSize = ResourceItemSize.Regular,
    subtitle: String? = null,
    tag: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        leading(size.badgeSize)
        Spacer(Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = size.titleStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (tag != null) {
                    Spacer(Modifier.width(spacing.small))
                    TagChip(tag)
                }
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(spacing.extraSmall))
            StatusLine(
                tone = tone,
                text = statusText,
                style = if (size == ResourceItemSize.Header) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(spacing.medium))
            trailing()
        }
    }
}

/** A tab list row: a clickable [AppCard] holding a [ResourceListItem], with an overflow hint (or a spinner while [isActioning]). */
@Composable
fun ResourceRowCard(
    title: String,
    tone: StatusTone,
    statusText: String,
    isActioning: Boolean,
    onClick: () -> Unit,
    leading: @Composable (badgeSize: Dp) -> Unit,
    tag: String? = null
) {
    AppCard(onClick = onClick, enabled = !isActioning) {
        ResourceListItem(
            title = title,
            tone = tone,
            statusText = statusText,
            leading = leading,
            tag = tag,
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            trailing = {
                if (isActioning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
    }
}

/** The summary card at the top of an [ActionSheet] - same content as the row that opened it, one size up. */
@Composable
fun ResourceHeaderCard(
    title: String,
    tone: StatusTone,
    statusText: String,
    leading: @Composable (badgeSize: Dp) -> Unit,
    subtitle: String? = null
) {
    AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        ResourceListItem(
            title = title,
            tone = tone,
            statusText = statusText,
            leading = leading,
            size = ResourceItemSize.Header,
            subtitle = subtitle,
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}
