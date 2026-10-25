package com.chilisaft.undroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.chilisaft.undroid.R

// Chakra Petch for titles, headers, and primary labels. Its heaviest cut is Bold - there's no
// Black, so don't ask for FontWeight.Black on a title style (it'd be synthesized).
val chakraPetchFontFamily = FontFamily(
    Font(R.font.chakrapetch_regular, FontWeight.Normal),
    Font(R.font.chakrapetch_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold)
)

/**
 * One entry per weight, each pinning the variable font's `wght` axis. A single un-weighted
 * `Font(R.font.inter_variable)` only ever renders the file's default (Regular) instance - any
 * SemiBold/Bold request then falls back to synthesized (smeared) bold instead of Inter's real cut.
 */
@OptIn(ExperimentalTextApi::class)
private fun interFont(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

// Inter Variable for descriptive text, body, and UI labels
val interFontFamily = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold),
    interFont(FontWeight.ExtraBold)
)

// Default Material 3 typography values
val baseline = Typography()

/**
 * Weights are part of the scale, not something each call site sets - screens should pick a style
 * and stop there. Only reach for an explicit `fontWeight` to emphasize a body-text run (e.g. a
 * list row's name), and do that inside a shared component rather than per screen.
 */
val AppTypography = Typography(
    // Display, Headline, and Title use Chakra Petch (High emphasis branding)
    displayLarge = baseline.displayLarge.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = baseline.displayMedium.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = baseline.displaySmall.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = baseline.titleLarge.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = baseline.titleMedium.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.Bold),
    titleSmall = baseline.titleSmall.copy(fontFamily = chakraPetchFontFamily, fontWeight = FontWeight.SemiBold),

    // Body and Labels use Inter (Readability for descriptions)
    bodyLarge = baseline.bodyLarge.copy(fontFamily = interFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = interFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = interFontFamily),
    // labelLarge is what Material3 buttons use - left at the baseline Medium weight.
    labelLarge = baseline.labelLarge.copy(fontFamily = interFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = interFontFamily, fontWeight = FontWeight.Bold),
    labelSmall = baseline.labelSmall.copy(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold),
)
