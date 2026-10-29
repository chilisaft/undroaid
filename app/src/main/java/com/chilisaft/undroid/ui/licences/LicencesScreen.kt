package com.chilisaft.undroid.ui.licences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chilisaft.undroid.R
import com.chilisaft.undroid.ui.components.DetailTopBar
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/** Every library the app ships, with its licence - generated at build time by the AboutLibraries plugin. */
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    Scaffold(topBar = { DetailTopBar(title = "Open-Source Licences", onBack = onBack) }) { padding ->
        LibrariesContainer(libraries = libraries, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
