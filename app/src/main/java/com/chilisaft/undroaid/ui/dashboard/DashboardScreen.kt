package com.chilisaft.undroaid.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chilisaft.undroaid.data.models.Server

@Composable
fun DashboardScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val viewModel: DashboardViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        // Main layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.padding(10.dp))

            // Display server data or loading message
            if (uiState.server != null) {
                ServerInfo(server = uiState.server!!)
            } else {
                Text(text = "Loading server data...")
            }
        }
    }
}

@Composable
fun ServerInfo(server: Server) {
    // Server Info
    Column {
        Text(text = "Server Name: ${server.name}")
        Text(text = "IP Address: ${server.lanIp}")
        Text(text = "Local Url: ${server.localUrl}")
    }
}
