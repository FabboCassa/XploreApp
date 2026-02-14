package org.xplore.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Xplore Color Palette ──────────────────────────────────────────
//  Culture-inspired: deep indigo base, warm amber accent, refined neutrals

private val XploreDark = darkColorScheme(
    primary = Color(0xFF9FA8DA),          // Soft lavender
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF283593),
    onPrimaryContainer = Color(0xFFE8EAF6),
    secondary = Color(0xFFFFD54F),         // Warm amber accent
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFFF9A825),
    onSecondaryContainer = Color(0xFF1B1B1B),
    tertiary = Color(0xFF80CBC4),           // Teal accent
    onTertiary = Color(0xFF004D40),
    background = Color(0xFF0D1117),         // Deep dark
    onBackground = Color(0xFFE8EAF6),
    surface = Color(0xFF161B22),            // Slightly lighter dark
    onSurface = Color(0xFFE8EAF6),
    surfaceVariant = Color(0xFF1E2530),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF37474F),
    error = Color(0xFFEF5350),
    onError = Color(0xFFFFFFFF),
)

private val XploreLight = lightColorScheme(
    primary = Color(0xFF3949AB),            // Indigo
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC5CAE9),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFFF9A825),           // Amber
    onSecondary = Color(0xFF1B1B1B),
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary = Color(0xFF00897B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF546E7A),
    outline = Color(0xFFB0BEC5),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
)

// ── Typography ────────────────────────────────────────────────────

private val XploreTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Shapes ────────────────────────────────────────────────────────

private val XploreShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ── Theme Composable ──────────────────────────────────────────────

@Composable
fun XploreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) XploreDark else XploreLight

    MaterialTheme(
        colorScheme = colorScheme,
        typography = XploreTypography,
        shapes = XploreShapes,
        content = content,
    )
}
