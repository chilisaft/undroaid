package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chilisaft.undroid.ui.theme.spacing

/**
 * The scrolling body of every screen: `spacing.itemSpacing` between items, a little room at the
 * top, and extra at the bottom so the last card doesn't sit flush against the nav bar. Horizontal
 * padding is the caller's (screens apply `spacing.medium` on the surrounding PullToRefreshBox so
 * loading/error placeholders line up with the list).
 */
@Composable
fun ScreenList(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    val spacing = MaterialTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing),
        contentPadding = PaddingValues(top = spacing.medium, bottom = spacing.extraLarge),
        content = content
    )
}
