package com.streamhelper.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light-only for now. Dark mode is deferred (see PLAN.md): add a darkColorScheme and
// switch on isSystemInDarkTheme() here. Dynamic (wallpaper) color is intentionally off.
private val LightColors = lightColorScheme(
    primary = AppColors.Primary1,
    onPrimary = AppColors.White,
    secondary = AppColors.Primary2,
    onSecondary = AppColors.White,
    tertiary = AppColors.Primary3,
    onTertiary = AppColors.Black,
    error = AppColors.Danger,
    onError = AppColors.White,
    background = AppColors.White,
    onBackground = AppColors.Black,
    surface = AppColors.White,
    onSurface = AppColors.Black,
    outline = AppColors.Gray,
)

@Composable
fun StreamHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
