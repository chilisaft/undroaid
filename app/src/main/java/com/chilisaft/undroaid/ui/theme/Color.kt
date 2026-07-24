package com.chilisaft.undroaid.ui.theme
import androidx.compose.ui.graphics.Color

// Brand Colors from Design
val UnraidOrange = Color(0xFFFF8C2F)
val DarkGrey = Color(0xFF1C1B1B)
val LightGrey = Color(0xFFF4F4F5)
val StitchBlue = Color(0xFF2196F3)
val StitchBrown = Color(0xFFA16A44)
val StitchCyan = Color(0xFF00B7F2)
val StitchGrey = Color(0xFFD7D7D7)

// Light Theme Mapping
val primaryLight = UnraidOrange
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFFFDBC6)
val onPrimaryContainerLight = Color(0xFF331200)

val secondaryLight = StitchBrown
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFFFDBCF)
val onSecondaryContainerLight = Color(0xFF390C00)

val tertiaryLight = StitchCyan
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFC1E8FF)
val onTertiaryContainerLight = Color(0xFF001E2C)

val backgroundLight = LightGrey
val onBackgroundLight = Color(0xFF1C1B1B)
val surfaceLight = LightGrey
val onSurfaceLight = Color(0xFF1C1B1B)
val surfaceVariantLight = Color(0xFFF4F3F7)
val onSurfaceVariantLight = Color(0xFF44474E)

// Material3's lightColorScheme()/darkColorScheme() default these tonal-elevation roles to the
// baseline M3 neutral palette when left unset, which happens to sit at nearly the same lightness
// as this app's custom background/surface (both are a light near-white grey in light mode, both
// a dark near-black grey in dark mode) - so every Card using surfaceContainerLow/High (Docker
// rows, Main tab device rows, etc.) barely showed up against the screen background. Explicit,
// visibly-separated steps fix that; dynamic color (Android 12+) isn't affected since the system
// already computes a coherent full tonal palette from the wallpaper.
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFFFFFF)
val surfaceContainerLight = Color(0xFFF7F7F8)
val surfaceContainerHighLight = Color(0xFFECECEE)
val surfaceContainerHighestLight = Color(0xFFE3E3E6)

// Dark Theme Mapping
val primaryDark = UnraidOrange
val onPrimaryDark = Color(0xFF552000)
val primaryContainerDark = Color(0xFF783100)
val onPrimaryContainerDark = Color(0xFFFFDBC6)

val secondaryDark = StitchBlue
val onSecondaryDark = Color(0xFF00344F)
val secondaryContainerDark = Color(0xFF004C71)
val onSecondaryContainerDark = Color(0xFFCBE6FF)

val tertiaryDark = StitchGrey
val onTertiaryDark = Color(0xFF303030)
val tertiaryContainerDark = Color(0xFF474747)
val onTertiaryContainerDark = Color(0xFFF0F0F0)

val backgroundDark = DarkGrey
val onBackgroundDark = LightGrey
val surfaceDark = DarkGrey
val onSurfaceDark = LightGrey
val surfaceVariantDark = Color(0xFF303030)
val onSurfaceVariantDark = Color(0xFFC4C6D0)

// See the light-mode comment above `surfaceContainerLowestLight` - same fix, mirrored for dark.
val surfaceContainerLowestDark = Color(0xFF121212)
val surfaceContainerLowDark = Color(0xFF252424)
val surfaceContainerDark = Color(0xFF2A2929)
val surfaceContainerHighDark = Color(0xFF343333)
val surfaceContainerHighestDark = Color(0xFF3F3E3E)

val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
