package com.chilisaft.undroaid.ui.theme
import androidx.compose.ui.graphics.Color

/**
 * The app's brand identity (see branding/ and NOTES.md's "App icon / branding exploration") -
 * forest tones, independent of Unraid's orange and Android's green. Every color below is derived
 * from a tonal ramp built around these three: [ForestPine] is the brand primary (also the app
 * icon's case color), [ForestGold] is the signature accent (the mark's "wink"), and [ForestInk]
 * is both the darkest step of the primary ramp *and* the app icon's ink color - not a
 * coincidence, they're the same hue at different tones.
 */
val ForestInk = Color(0xFF16321F)
val ForestPine = Color(0xFF2F6B48)
val ForestGold = Color(0xFFE3A94A)
val ForestPaper = Color(0xFFEEF2EA)

// --- Primary tonal ramp (forest green, ~hue 150) - ForestInk sits at P20, ForestPine at P40 ---
private val P10 = Color(0xFF0B1F13)
private val P20 = ForestInk
private val P30 = Color(0xFF204A2D)
private val P40 = ForestPine
private val P80 = Color(0xFFA8D4B7)
private val P90 = Color(0xFFCDEAD8)

// --- Secondary tonal ramp (muted moss - cooler/less saturated than primary, for variety) ---
private val S10 = Color(0xFF17211B)
private val S30 = Color(0xFF364637)
private val S40 = Color(0xFF4A5C4B)
private val S80 = Color(0xFFB7C9B7)
private val S90 = Color(0xFFD3E2D2)

// --- Tertiary tonal ramp (the wink's gold) - ForestGold sits at T80, used directly as the
// light-theme tertiary rather than a computed tone 40, so the brand-exact color shows up front
// and center (this is also the "Paused" indicator color on the Docker/VMs tabs) ---
private val T10 = Color(0xFF2B1900)
private val T30 = Color(0xFF623C00)
private val T40 = Color(0xFF8A5A0A)
private val T80 = ForestGold
private val T90 = Color(0xFFFFDFAE)
private val T95 = Color(0xFFF0C374)

// --- Neutral ramp (a faint green-gray bias, not pure gray - ForestPaper sits near the top) ---
private val N10 = Color(0xFF10150E)
private val N17 = Color(0xFF181D16)
private val N20 = Color(0xFF1C211D)
private val N24 = Color(0xFF272C26)
private val N30 = Color(0xFF313630)
private val N87 = Color(0xFFD7DED4)
private val N92 = ForestPaper
private val N94 = Color(0xFFF1F5EF)
private val N96 = Color(0xFFF6F9F4)
private val N98 = Color(0xFFFBFDFA)

// --- Neutral-variant ramp (surfaceVariant/outline family - a step more saturated than neutral) ---
private val NV30 = Color(0xFF414940)
private val NV50 = Color(0xFF717971)
private val NV60 = Color(0xFF8B938A)
private val NV80 = Color(0xFFC4CCC1)
private val NV90 = Color(0xFFE0E8DD)

// Error stays close to M3's standard baseline red rather than a forest tint - errors need to
// read as unambiguously "wrong" at a glance, which a warm/earthy reskin would undercut.
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

// ============================== Light scheme ==============================

val primaryLight = P40
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = P90
val onPrimaryContainerLight = P20
val inversePrimaryLight = P80

val primaryFixedLight = P90
val primaryFixedDimLight = P80
val onPrimaryFixedLight = P20
val onPrimaryFixedVariantLight = P30

val secondaryLight = S40
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = S90
val onSecondaryContainerLight = S10

val secondaryFixedLight = S90
val secondaryFixedDimLight = S80
val onSecondaryFixedLight = S10
val onSecondaryFixedVariantLight = S30

val tertiaryLight = T80
val onTertiaryLight = Color(0xFF402D00)
val tertiaryContainerLight = T90
val onTertiaryContainerLight = T10

val tertiaryFixedLight = T90
val tertiaryFixedDimLight = T80
val onTertiaryFixedLight = T10
val onTertiaryFixedVariantLight = T30

val backgroundLight = N92
val onBackgroundLight = N10
val surfaceLight = N92
val onSurfaceLight = N10
val surfaceVariantLight = NV90
val onSurfaceVariantLight = NV30
val outlineLight = NV50
val outlineVariantLight = NV80
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = N24
val inverseOnSurfaceLight = N92
val surfaceDimLight = N87
val surfaceBrightLight = N92

// Material3's lightColorScheme()/darkColorScheme() default these tonal-elevation roles to the
// baseline M3 neutral palette when left unset, which happens to sit at nearly the same lightness
// as this app's custom background/surface (both are a light near-white grey in light mode, both
// a dark near-black grey in dark mode) - so every Card using surfaceContainerLow/High (Docker
// rows, Main tab device rows, etc.) barely showed up against the screen background. Explicit,
// visibly-separated steps fix that; dynamic color (Android 12+) isn't affected since the system
// already computes a coherent full tonal palette from the wallpaper.
val surfaceContainerLowestLight = N98
val surfaceContainerLowLight = N96
val surfaceContainerLight = N94
val surfaceContainerHighLight = Color(0xFFE9EFE6)
val surfaceContainerHighestLight = Color(0xFFE1E8DD)

// ============================== Dark scheme ==============================

val primaryDark = P80
val onPrimaryDark = P10
val primaryContainerDark = P30
val onPrimaryContainerDark = P90
val inversePrimaryDark = P40

// "Fixed" roles are the same across both themes by design (M3 spec) - a surface that wants to
// stay visually consistent regardless of light/dark, e.g. this app's Dashboard hero gradient.
val primaryFixedDark = P90
val primaryFixedDimDark = P80
val onPrimaryFixedDark = P20
val onPrimaryFixedVariantDark = P30

val secondaryDark = S80
val onSecondaryDark = S10
val secondaryContainerDark = S30
val onSecondaryContainerDark = S90

val secondaryFixedDark = S90
val secondaryFixedDimDark = S80
val onSecondaryFixedDark = S10
val onSecondaryFixedVariantDark = S30

val tertiaryDark = T95
val onTertiaryDark = Color(0xFF452B00)
val tertiaryContainerDark = Color(0xFF644300)
val onTertiaryContainerDark = T90

val tertiaryFixedDark = T90
val tertiaryFixedDimDark = T80
val onTertiaryFixedDark = T10
val onTertiaryFixedVariantDark = T30

val backgroundDark = N10
val onBackgroundDark = Color(0xFFE1E8DD)
val surfaceDark = N10
val onSurfaceDark = Color(0xFFE1E8DD)
val surfaceVariantDark = NV30
val onSurfaceVariantDark = NV80
val outlineDark = NV60
val outlineVariantDark = NV30
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE5EBE2)
val inverseOnSurfaceDark = N24
val surfaceDimDark = N10
val surfaceBrightDark = N30

// See the light-mode comment above `surfaceContainerLowestLight` - same fix, mirrored for dark.
// Dark-theme containers step *up* in lightness as elevation increases (M3 convention), the
// opposite direction from light mode, but the goal is the same: visibly lift cards off the page.
val surfaceContainerLowestDark = Color(0xFF0B0F0A)
val surfaceContainerLowDark = N17
val surfaceContainerDark = N20
val surfaceContainerHighDark = N24
val surfaceContainerHighestDark = N30
