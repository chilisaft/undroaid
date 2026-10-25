package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The app's one card surface: full width, [MaterialTheme.shapes]`.large` corners, and
 * `surfaceContainerLow` by default (pass `surfaceContainerHigh` for a card that sits on top of
 * another surface, e.g. a header inside a bottom sheet). Clickable when [onClick] is set; a
 * disabled card keeps its normal colors rather than Material's greyed-out disabled ones, since
 * it's only ever disabled while a row action is in flight (which already shows its own spinner).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    val contentColor = contentColorFor(containerColor)
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor,
        disabledContentColor = contentColor
    )
    if (onClick != null) {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            content = content
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            content = content
        )
    }
}
