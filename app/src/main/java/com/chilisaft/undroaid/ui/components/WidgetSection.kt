package com.chilisaft.undroaid.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.ui.theme.spacing

/**
 * Renders [result] as a loading spinner, a soft-fail placard (distinguishing "no permission"
 * from other errors), or [content] - so a failure here never disturbs sibling widgets.
 */
@Composable
fun <T> WidgetSection(
    result: WidgetResult<T>,
    onRetry: () -> Unit,
    minHeight: Dp,
    content: @Composable (T) -> Unit
) {
    when (result) {
        is WidgetResult.Loading -> WidgetPlaceholder(minHeight) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        is WidgetResult.Failure -> WidgetPlaceholder(minHeight) {
            WidgetErrorContent(failure = result, onRetry = onRetry)
        }
        is WidgetResult.Success -> content(result.data)
    }
}

@Composable
private fun WidgetPlaceholder(minHeight: Dp, content: @Composable () -> Unit) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
        shape = RoundedCornerShape(spacing.mediumLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun WidgetErrorContent(failure: WidgetResult.Failure, onRetry: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = if (failure.permissionDenied) Icons.Filled.Lock else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(spacing.extraSmall))
        Text(
            text = if (failure.permissionDenied) {
                "Your API key doesn't have permission to view this"
            } else {
                failure.message ?: "Couldn't load this data"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(spacing.small))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
