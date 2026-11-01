package com.expenso.app.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class AppColors(
    val income: Color,
    val incomeSoft: Color,
    val expense: Color,
    val expenseSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val overdue: Color,
    val overdueSoft: Color,
    val muted: Color,
    val line: Color,
    val surfaceRaised: Color,
    val indigoSoft: Color,
    val isDark: Boolean
)

private val LightColors = AppColors(
    income = IncomeGreen,
    incomeSoft = IncomeGreenSoft,
    expense = ExpenseRed,
    expenseSoft = ExpenseRedSoft,
    warning = Amber,
    warningSoft = AmberSoft,
    overdue = OverdueRed,
    overdueSoft = ExpenseRedSoft,
    muted = InkMuted,
    line = LineSoft,
    surfaceRaised = Color(0xFFF8F9FC),
    indigoSoft = IndigoSoft,
    isDark = false
)

private val DarkColors = AppColors(
    income = Color(0xFF4ADE9D),
    incomeSoft = Color(0xFF123529),
    expense = Color(0xFFFF7B80),
    expenseSoft = Color(0xFF3A1D20),
    warning = Color(0xFFF5C451),
    warningSoft = Color(0xFF3A2E12),
    overdue = Color(0xFFFF7B80),
    overdueSoft = Color(0xFF3A1D20),
    muted = DarkMuted,
    line = DarkLine,
    surfaceRaised = DarkSurfaceRaised,
    indigoSoft = Color(0xFF272350),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = Color(0xFF2C2599),
    secondary = Color(0xFF6B7080),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F1F6),
    onSecondaryContainer = Ink,
    tertiary = IncomeGreen,
    onTertiary = Color.White,
    background = OffWhite,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3F4F8),
    onSurfaceVariant = InkMuted,
    outline = LineSoft,
    outlineVariant = Color(0xFFF0F1F5),
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = Color(0xFF9B1C1F),
    surfaceTint = Indigo,
    scrim = Color.Black
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8B86FF),
    onPrimary = Color(0xFF16123F),
    primaryContainer = Color(0xFF2C2768),
    onPrimaryContainer = Color(0xFFE7E5FF),
    secondary = Color(0xFFA2A8B8),
    onSecondary = Color(0xFF111219),
    secondaryContainer = Color(0xFF242733),
    onSecondaryContainer = DarkInk,
    tertiary = Color(0xFF4ADE9D),
    onTertiary = Color(0xFF062B1D),
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF1D202B),
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    outlineVariant = Color(0xFF242734),
    error = Color(0xFFFF7B80),
    onError = Color(0xFF3A0A0C),
    errorContainer = Color(0xFF3A1D20),
    onErrorContainer = Color(0xFFFFD9DA),
    surfaceTint = Color(0xFF8B86FF),
    scrim = Color.Black
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ExpensoTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val appColors = if (dark) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.setBackgroundDrawable(ColorDrawable(scheme.background.toArgb()))
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = FinTypography,
            shapes = AppShapes
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background)
            ) {
                content()
            }
        }
    }
}
