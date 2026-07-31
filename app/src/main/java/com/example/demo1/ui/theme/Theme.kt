package com.example.demo1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.demo1.ColorTheme

private fun getLightColorScheme(
    colorTheme: ColorTheme
) = when (colorTheme) {                                                                                         //Bu, if mantığının çok seçenekli halidir.

    ColorTheme.Neutral -> lightColorScheme(
        primary = Color(0xFF303030),                                                                                //kullanıcı balon rengi
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5E5E5),
        onPrimaryContainer = Color.Black,

        background = Color(0xFFFFFBFF),                                                                   //Uygulamanın genel ekran arka planı.
        onBackground = Color(0xFF1C1B1F),

        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1C1B1F),

        surfaceVariant = Color(0xFFE7E0EC),                                                           //Modelin mesaj balonu bu renkte görünür.
        onSurfaceVariant = Color(0xFF49454F)
    )

    ColorTheme.Purple -> lightColorScheme(
        primary = Color(0xFF6D42C1),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE9DDFF),
        onPrimaryContainer = Color(0xFF25005A),

        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1C1B1F),

        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1C1B1F),

        surfaceVariant = Color(0xFFEDE7F6),
        onSurfaceVariant = Color(0xFF49454F)
    )

    ColorTheme.Blue -> lightColorScheme(
        primary = Color(0xFF146CBA),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7E9FF),
        onPrimaryContainer = Color(0xFF001D36),

        background = Color(0xFFF7FBFF),
        onBackground = Color(0xFF1A1C1E),

        surface = Color(0xFFF7FBFF),
        onSurface = Color(0xFF1A1C1E),

        surfaceVariant = Color(0xFFE0EAF4),
        onSurfaceVariant = Color(0xFF43474E)
    )

    ColorTheme.Pink -> lightColorScheme(
        primary = Color(0xFFB3266E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E8),
        onPrimaryContainer = Color(0xFF3F001F),

        background = Color(0xFFFFF8FB),
        onBackground = Color(0xFF201A1D),

        surface = Color(0xFFFFF8FB),
        onSurface = Color(0xFF201A1D),

        surfaceVariant = Color(0xFFF5E0E8),
        onSurfaceVariant = Color(0xFF51434A)
    )
}

private fun getDarkColorScheme(
    colorTheme: ColorTheme
) = when (colorTheme) {

    ColorTheme.Neutral -> darkColorScheme(
        primary = Color(0xFFD0D0D0),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF454545),
        onPrimaryContainer = Color.White,

        background = Color(0xFF121212),
        onBackground = Color(0xFFE6E1E5),

        surface = Color(0xFF121212),
        onSurface = Color(0xFFE6E1E5),

        surfaceVariant = Color(0xFF2D2D2D),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )

    ColorTheme.Purple -> darkColorScheme(
        primary = Color(0xFFD0B4FF),
        onPrimary = Color(0xFF3D0078),
        primaryContainer = Color(0xFF56328A),
        onPrimaryContainer = Color(0xFFE9DDFF),

        background = Color(0xFF17121E),
        onBackground = Color(0xFFE9E1E9),

        surface = Color(0xFF17121E),
        onSurface = Color(0xFFE9E1E9),

        surfaceVariant = Color(0xFF332B3D),
        onSurfaceVariant = Color(0xFFD0C4D7)
    )

    ColorTheme.Blue -> darkColorScheme(
        primary = Color(0xFF9DCAFF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF174A77),
        onPrimaryContainer = Color(0xFFD7E9FF),

        background = Color(0xFF101820),
        onBackground = Color(0xFFE1E2E8),

        surface = Color(0xFF101820),
        onSurface = Color(0xFFE1E2E8),

        surfaceVariant = Color(0xFF293641),
        onSurfaceVariant = Color(0xFFC3C7CF)
    )

    ColorTheme.Pink -> darkColorScheme(
        primary = Color(0xFFFFB1CF),
        onPrimary = Color(0xFF65003A),
        primaryContainer = Color(0xFF872556),
        onPrimaryContainer = Color(0xFFFFD9E8),

        background = Color(0xFF21151B),
        onBackground = Color(0xFFECE0E5),

        surface = Color(0xFF21151B),
        onSurface = Color(0xFFECE0E5),

        surfaceVariant = Color(0xFF402832),
        onSurfaceVariant = Color(0xFFD8C2CA)
    )
}

@Composable
fun DemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: ColorTheme = ColorTheme.Neutral,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        getDarkColorScheme(colorTheme)
    } else {
        getLightColorScheme(colorTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}