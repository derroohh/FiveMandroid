package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FivemDarkColorScheme = darkColorScheme(
    primary = FivemOrange,
    onPrimary = FivemWhite,
    secondary = FivemCardGrey,
    onSecondary = FivemWhite,
    background = FivemDarkBg,
    onBackground = FivemWhite,
    surface = FivemDeepGrey,
    onSurface = FivemWhite,
    surfaceVariant = FivemCardGrey,
    onSurfaceVariant = FivemTextMuted,
    outline = FivemBorderGrey,
    tertiary = FivemSuccess,
    error = FivemError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for authentic FiveM Gaming style
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve branded Cfx orange & dark mode
    content: @Composable () -> Unit,
) {
    val colorScheme = FivemDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
