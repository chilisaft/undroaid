package com.chilisaft.undroid.ui.shares

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroid.data.models.ShareInfo
import com.chilisaft.undroid.data.models.ShareProtection
import com.chilisaft.undroid.data.models.ShareSettings
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.ui.components.AppCard
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.components.IconBadge
import com.chilisaft.undroid.ui.components.IconBadgeSize
import com.chilisaft.undroid.ui.components.InfoRow
import com.chilisaft.undroid.ui.components.ScreenList
import com.chilisaft.undroid.ui.components.StatusLine
import com.chilisaft.undroid.ui.components.WidgetSection
import com.chilisaft.undroid.ui.theme.AppTheme
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.spacing
import com.chilisaft.undroid.utils.kilobytesToHumanReadable

@Composable
fun SharesScreen(onBack: () -> Unit = {}, viewModel: SharesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    SharesScreenContent(uiState = uiState, onBack = onBack, onRefresh = viewModel::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharesScreenContent(
    uiState: SharesScreenState,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    var selectedShare by remember { mutableStateOf<ShareInfo?>(null) }
    Scaffold(
        topBar = { DetailTopBar(title = "Shares", onBack = onBack) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            WidgetSection(result = uiState.shares, onRetry = onRefresh, minHeight = 200.dp) { shares ->
                if (shares.isEmpty()) {
                    EmptyState("No shares found")
                } else {
                    ScreenList {
                        items(shares, key = { it.id }) { share -> ShareRow(share, onClick = { selectedShare = share }) }
                    }
                }
            }
        }
    }
    selectedShare?.let { ShareDetailsSheet(it, onDismiss = { selectedShare = null }) }
}

@Composable
private fun ShareRow(share: ShareInfo, onClick: () -> Unit) {
    val spacing = MaterialTheme.spacing
    AppCard(onClick = onClick) {
        Row(modifier = Modifier.padding(spacing.medium), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(IconBadgeSize.Compact) {
                Icon(
                    Icons.Filled.FolderShared,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconBadgeSize.Compact * 0.5f)
                )
            }
            Spacer(Modifier.width(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    share.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (share.comment != null) {
                    Text(
                        share.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(spacing.extraSmall))
                Text(
                    usageLine(share),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Only the exception is called out in the list - "protected" on every row is noise.
                if (share.protection == ShareProtection.UNPROTECTED) {
                    Spacer(Modifier.height(spacing.extraSmall))
                    StatusLine(tone = StatusTone.Warning, text = "Some or all files unprotected")
                }
            }
        }
    }
}

/** Read-only details: the share's allocation settings, in the webUI's own terms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareDetailsSheet(share: ShareInfo, onDismiss: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val settings = share.settings
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(share.name, style = MaterialTheme.typography.titleLarge)
            share.comment?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // surfaceContainerHigh: a default AppCard would vanish into the sheet (see UserMenuSheet).
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    share.protection?.let { protection ->
                        StatusLine(
                            tone = if (protection == ShareProtection.PROTECTED) StatusTone.Ok else StatusTone.Warning,
                            text = if (protection == ShareProtection.PROTECTED) "All files protected" else "Some or all files unprotected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider()
                    }
                    share.sizeKb?.let { InfoRow("In share", it.kilobytesToHumanReadable()) }
                    share.freeKb?.let { InfoRow("Free", it.kilobytesToHumanReadable()) }
                    settings.allocator?.let { InfoRow("Allocation", allocatorLabel(it)) }
                    settings.splitLevel?.let { InfoRow("Split level", splitLevelLabel(it)) }
                    settings.minimumFreeKb?.let { InfoRow("Minimum free", if (it > 0) it.kilobytesToHumanReadable() else "None") }
                    InfoRow("Included disks", settings.includedDisks.joinToString().ifEmpty { "All" })
                    if (settings.excludedDisks.isNotEmpty()) {
                        InfoRow("Excluded disks", settings.excludedDisks.joinToString())
                    }
                }
            }
        }
    }
}

private fun allocatorLabel(allocator: String): String = when (allocator) {
    "highwater" -> "High-water"
    "mostfree" -> "Most-free"
    "fillup" -> "Fill-up"
    else -> allocator
}

/** Unraid's split levels: "" splits any directory, "0" never splits (manual), N = only the top N levels. */
private fun splitLevelLabel(splitLevel: String): String = when (val levels = splitLevel.toIntOrNull()) {
    null -> if (splitLevel.isEmpty()) "Any directory" else splitLevel
    0 -> "Manual"
    1 -> "Top level only"
    else -> "Top $levels levels"
}

/** "1.2 TB in share · 3.9 TB free" - the share's own size only when Unraid has computed it. */
private fun usageLine(share: ShareInfo): String = buildList {
    share.sizeKb?.let { add("${it.kilobytesToHumanReadable()} in share") }
    share.freeKb?.let { add("${it.kilobytesToHumanReadable()} free") }
}.joinToString(" · ").ifEmpty { "No usage data" }

private val previewShares = listOf(
    ShareInfo(
        "1", "appdata", "Container data", freeKb = 263_000_000L, sizeKb = 120_000_000L, protection = ShareProtection.UNPROTECTED,
        settings = ShareSettings(allocator = "highwater", splitLevel = "", minimumFreeKb = 46_885_052L)
    ),
    ShareInfo("2", "media", "Movies and TV", freeKb = 4_100_000_000L, sizeKb = null),
    ShareInfo("3", "isos", null, freeKb = null, sizeKb = null)
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SharesScreenPreview() {
    AppTheme {
        SharesScreenContent(uiState = SharesScreenState(shares = WidgetResult.Success(previewShares)))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
fun SharesScreenEmptyPreview() {
    AppTheme {
        SharesScreenContent(uiState = SharesScreenState(shares = WidgetResult.Success(emptyList())))
    }
}
