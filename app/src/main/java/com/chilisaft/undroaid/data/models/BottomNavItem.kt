package com.chilisaft.undroaid.data.models

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val icon_selected: ImageVector,
    val route:String,
)