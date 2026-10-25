package com.chilisaft.undroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * What a status indicator means, independent of what it's attached to - a container, a VM, a
 * disk, the array. Every status dot/line in the app maps its own state onto one of these, so the
 * same meaning always gets the same color:
 * - [Ok]: running / healthy
 * - [Warning]: paused, or a transitional/unusual state worth a glance
 * - [Neutral]: stopped or idle - a normal state, not a problem
 * - [Error]: an actual fault (a failed disk, a crashed VM)
 */
enum class StatusTone { Ok, Warning, Neutral, Error }

/**
 * Semantic status colors that Material3's [androidx.compose.material3.ColorScheme] has no role
 * for. [ok] and [warning] stay fixed regardless of dynamic color - "green means running, amber
 * means paused" should read the same whatever the wallpaper is - while [neutral] and [error] come
 * from the active scheme.
 */
@Immutable
data class StatusColors(
    val ok: Color,
    val warning: Color,
    val neutral: Color,
    val error: Color
)

private val okLight = Color(0xFF2E8B4E)
private val okDark = Color(0xFF7FD39A)

// The brand gold (see ForestGold) - also the light theme's tertiary, so paused indicators match
// the rest of the app's gold accents when dynamic color is off.
private val warningLight = ForestGold
private val warningDark = Color(0xFFF0C374)

fun statusColors(darkTheme: Boolean, neutral: Color, error: Color) = StatusColors(
    ok = if (darkTheme) okDark else okLight,
    warning = if (darkTheme) warningDark else warningLight,
    neutral = neutral,
    error = error
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current

val StatusTone.color: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        StatusTone.Ok -> MaterialTheme.statusColors.ok
        StatusTone.Warning -> MaterialTheme.statusColors.warning
        StatusTone.Neutral -> MaterialTheme.statusColors.neutral
        StatusTone.Error -> MaterialTheme.statusColors.error
    }
