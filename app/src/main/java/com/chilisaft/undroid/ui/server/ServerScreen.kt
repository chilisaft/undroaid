package com.chilisaft.undroid.ui.server

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
fun ServerScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = { DetailTopBar(title = "Server", onBack = onBack) }
    ) { padding ->
        EmptyState(
            text = "Server details are coming soon.\nThis will show your server's identity, addresses and Unraid Connect status.",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MaterialTheme.spacing.large)
        )
    }
}
