package com.chilisaft.undroaid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.chilisaft.undroaid.R

// Chakra Petch for titles, headers, and primary labels
val chakraPetchFontFamily = FontFamily(
    Font(R.font.chakrapetch_regular, FontWeight.Normal),
    Font(R.font.chakrapetch_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold)
)

// Inter Variable for descriptive text, body, and UI labels
val interFontFamily = FontFamily(
    Font(
        resId = R.font.inter_variable,
    )
)

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    // Display, Headline, and Title use Chakra Petch (High emphasis branding)
    displayLarge = baseline.displayLarge.copy(fontFamily = chakraPetchFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = chakraPetchFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = chakraPetchFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = chakraPetchFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = chakraPetchFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = chakraPetchFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = chakraPetchFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = chakraPetchFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = chakraPetchFontFamily),

    // Body and Labels use Inter (Readability for descriptions)
    bodyLarge = baseline.bodyLarge.copy(fontFamily = interFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = interFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = interFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = interFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = interFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = interFontFamily),
)
