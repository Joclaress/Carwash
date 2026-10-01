package com.example.carwash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = SecondaryDark,
    tertiary = TertiaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,          // 0xFFF44336 Red
    onPrimary = Color.White,
    primaryContainer = PrimaryRedContainer,
    onPrimaryContainer = OnPrimaryRedContainer,
    secondary = SecondaryLight,
    tertiary = TertiaryLight
)

@Composable
fun CarwashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
