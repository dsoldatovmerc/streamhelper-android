package com.streamhelper.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * RN src/theme/typography.ts mapped onto Material 3 slots:
 * Title1 → headlineMedium, Title2 → titleLarge, Title3 → titleMedium,
 * Headline → titleSmall, Body → bodyLarge, Callout → bodyMedium.
 */
private val Default = Typography()

val AppTypography = Typography(
    headlineMedium = Default.headlineMedium.copy(fontSize = 28.sp),
    titleLarge = Default.titleLarge.copy(fontSize = 22.sp),
    titleMedium = Default.titleMedium.copy(fontSize = 20.sp),
    titleSmall = Default.titleSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Default.bodyLarge.copy(fontSize = 17.sp),
    bodyMedium = Default.bodyMedium.copy(fontSize = 16.sp),
)

