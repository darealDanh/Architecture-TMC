package com.tmc.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TmcColors {
    val Primary = Color(0xFF0B57D0)
    val PrimaryContainer = Color(0xFFD3E3FD)
    val OnPrimaryContainer = Color(0xFF041E49)
    val Background = Color(0xFFF8F9FC)
    val NavigationBar = Color(0xFFEDF0F5)
    val OnSurface = Color(0xFF1A1C1E)
    val OnSurfaceVariant = Color(0xFF5C5F66)
    val Outline = Color(0xFF74777F)
    val OutlineVariant = Color(0xFFC4C6CF)
    val Divider = Color(0xFFEEF0F3)
    val Track = Color(0xFFE3E8EF)
    val Avatar = Color(0xFFC2E7FF)
    val OnAvatar = Color(0xFF004A77)
    val Ok = Color(0xFF137333)
    val OkContainer = Color(0xFFE6F4EA)
    val Warning = Color(0xFF7A4F00)
    val WarningContainer = Color(0xFFFEF3D6)
    val Error = Color(0xFFB3261E)
    val ErrorContainer = Color(0xFFFCE8E6)
}

private val colorScheme = lightColorScheme(
    primary = TmcColors.Primary,
    onPrimary = Color.White,
    primaryContainer = TmcColors.PrimaryContainer,
    onPrimaryContainer = TmcColors.OnPrimaryContainer,
    secondaryContainer = TmcColors.PrimaryContainer,
    onSecondaryContainer = TmcColors.OnPrimaryContainer,
    background = TmcColors.Background,
    onBackground = TmcColors.OnSurface,
    surface = TmcColors.Background,
    onSurface = TmcColors.OnSurface,
    surfaceVariant = TmcColors.NavigationBar,
    onSurfaceVariant = TmcColors.OnSurfaceVariant,
    surfaceContainer = TmcColors.NavigationBar,
    outline = TmcColors.Outline,
    outlineVariant = TmcColors.OutlineVariant,
    error = TmcColors.Error,
)

private val typography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
)

@Composable
fun TmcTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
