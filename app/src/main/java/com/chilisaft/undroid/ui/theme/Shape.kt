package com.chilisaft.undroid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The app's corner scale. Cards (and anything sitting at card level - widget placeholders, list
 * rows, the logs dialog) use [Shapes.large]; icon badges use [Shapes.medium]; the inner corners of
 * grouped list rows use [Shapes.extraSmall]. Screens should reach for these rather than building a
 * `RoundedCornerShape` from a spacing token.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
