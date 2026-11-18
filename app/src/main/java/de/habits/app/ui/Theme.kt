package de.habits.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Indigo = Color(0xFF4F6BED)

@Composable
fun HabitsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Indigo)
        else -> lightColorScheme(primary = Indigo)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * Outcome colours from the Okabe–Ito palette: blue vs. vermillion stays distinguishable with red–green colour
 * blindness and also differs in brightness. "Partial" is a light tint of success. Calendar cells add symbols
 * (✓ / ✕) so colour is never the only cue.
 */
object Status {
    val success = Color(0xFF0072B2)
    val partial = Color(0x660072B2)
    val fail = Color(0xFFD55E00)
}

/** Per-habit accent colours, referenced by index. */
val habitColors = listOf(
    Color(0xFF4F6BED), Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFFFFA726),
    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF29B6F6), Color(0xFF8D6E63),
)

fun habitColor(index: Int) = habitColors[index.mod(habitColors.size)]
