package xyz.ecys.vocab.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import xyz.ecys.vocab.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val SpaceGrotesk = FontFamily(
    Font(resId = R.font.space_grotesk_light, weight = FontWeight.Light),
    Font(resId = R.font.space_grotesk_regular, weight = FontWeight.Normal),
    Font(resId = R.font.space_grotesk_medium, weight = FontWeight.Medium),
    Font(resId = R.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.space_grotesk_bold, weight = FontWeight.Bold)
)

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = Primary80,          // Main accent color (currently light blue)
    onPrimary = Background,       // Text/icons on primary color
    primaryContainer = Primary40, // Used for containers with primary color
    onPrimaryContainer = White,   // Text/icons on primary containers
    
    // Secondary colors
    secondary = Secondary80,      // Secondary accent color
    onSecondary = Background,     // Text/icons on secondary color
    secondaryContainer = Secondary40, // Used for containers with secondary color
    onSecondaryContainer = White, // Text/icons on secondary containers
    
    // Tertiary colors (using secondary colors since we removed pink)
    tertiary = Secondary80,
    onTertiary = Background,
    tertiaryContainer = Secondary40,
    onTertiaryContainer = White,
    
    // Background colors
    background = Background,      // Main background (dark blue-black)
    onBackground = White,         // Text/icons on background
    surface = Surface,           // Surface color (slightly lighter than background)
    onSurface = White,           // Text/icons on surface
    surfaceVariant = Surface,    // Alternative surface color
    onSurfaceVariant = White.copy(alpha = 0.7f), // Text/icons on surface variant
    
    // Other colors
    error = Error,               // Error color (red)
    onError = White,             // Text/icons on error color
    errorContainer = Error.copy(alpha = 0.2f), // Container with error color
    onErrorContainer = Error,    // Text/icons on error container
    
    // Outline
    outline = White.copy(alpha = 0.2f), // Used for borders/dividers
    outlineVariant = White.copy(alpha = 0.1f)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    secondary = Secondary40,
    tertiary = Secondary40,  // Removed pink, using secondary color
    background = Background,
    surface = Surface,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = White,
    onSurface = White,
    error = Error,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun VocabularyBoosterTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set system bars color
    val systemUiController = rememberSystemUiController()
    val backgroundColor = Color(0xFF05080D)
    
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = backgroundColor,
            darkIcons = false
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography.copy(
            displayLarge = Typography.displayLarge.copy(fontFamily = SpaceGrotesk),
            displayMedium = Typography.displayMedium.copy(fontFamily = SpaceGrotesk),
            displaySmall = Typography.displaySmall.copy(fontFamily = SpaceGrotesk),
            headlineLarge = Typography.headlineLarge.copy(fontFamily = SpaceGrotesk),
            headlineMedium = Typography.headlineMedium.copy(fontFamily = SpaceGrotesk),
            headlineSmall = Typography.headlineSmall.copy(fontFamily = SpaceGrotesk),
            titleLarge = Typography.titleLarge.copy(fontFamily = SpaceGrotesk),
            titleMedium = Typography.titleMedium.copy(fontFamily = SpaceGrotesk),
            titleSmall = Typography.titleSmall.copy(fontFamily = SpaceGrotesk),
            bodyLarge = Typography.bodyLarge.copy(fontFamily = SpaceGrotesk),
            bodyMedium = Typography.bodyMedium.copy(fontFamily = SpaceGrotesk),
            bodySmall = Typography.bodySmall.copy(fontFamily = SpaceGrotesk),
            labelLarge = Typography.labelLarge.copy(fontFamily = SpaceGrotesk),
            labelMedium = Typography.labelMedium.copy(fontFamily = SpaceGrotesk),
            labelSmall = Typography.labelSmall.copy(fontFamily = SpaceGrotesk)
        ),
        content = content
    )
}