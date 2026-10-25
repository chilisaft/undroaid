package com.chilisaft.undroid.ui.shares

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.chilisaft.undroid.ui.components.EmptyState
import com.chilisaft.undroid.ui.theme.spacing

/** Placeholder until the real screen is built - see TODO.md. */
@Composable
fun SharesScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = { DetailTopBar(title = "Shares", onBack = onBack) }
    ) { padding ->
        EmptyState(
            text = "Shares are coming soon.\nThis will list your user shares with their size and free space.",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MaterialTheme.spacing.large)
        )
    }
}
