package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chilisaft.undroid.R

/**
 * The app icon, drawn in-app: the launcher icon's own background color and foreground vector in
 * a rounded square. Uses the exact same resources as the adaptive icon (not a copy), so it can't
 * drift from what's on the home screen. Fixed brand colors - deliberately not theme-tinted.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.extraLarge,
        color = colorResource(R.color.ic_launcher_background)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
