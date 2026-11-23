package com.blank.gallery.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

internal data class BlankColorPalette(
    val pageBackground: Color,
    val cardBackground: Color,
    val secondarySurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val divider: Color,
    val accent: Color,
    val danger: Color,
    val glass: Color
)

internal object BlankThemeColors {
    val Light = BlankColorPalette(
        pageBackground = Color(0xFFF5F5F7),
        cardBackground = Color.White,
        secondarySurface = Color(0xFFF1F4F4),
        primaryText = Color(0xFF1A1A1A),
        secondaryText = Color(0xFF888888),
        tertiaryText = Color(0xFFAAAAAA),
        divider = Color(0xFFF0F0F0),
        accent = Color(0xFF7FB5B1),
        danger = Color(0xFFFF3B30),
        glass = Color.White.copy(alpha = 0.85f)
    )

    val Dark = BlankColorPalette(
        pageBackground = Color(0xFF171B1B),
        cardBackground = Color(0xFF222929),
        secondarySurface = Color(0xFF2B3433),
        primaryText = Color(0xFFE6ECEA),
        secondaryText = Color(0xFFA0AAA7),
        tertiaryText = Color(0xFF788581),
        divider = Color(0xFF394340),
        accent = Color(0xFF83BDB5),
        danger = Color(0xFFFF6259),
        glass = Color(0xE6222929)
    )
}

private val Palette = BlankThemeColors.Light

internal val PageBackground = Palette.pageBackground
internal val MainText = Palette.primaryText
internal val ThemeMint = Palette.accent
internal val MutedText = Palette.secondaryText
internal val GlassWhite = Palette.glass

@Composable
internal fun Modifier.blankGlassSurface(
    hazeState: HazeState,
    shape: Shape,
    glassAlpha: Float = 0.80f
): Modifier = this
    .hazeEffect(
        state = hazeState,
        style = HazeStyle(
            backgroundColor = PageBackground,
            tints = emptyList(),
            blurRadius = 20.dp,
            noiseFactor = 0f
        )
    )
    .clip(shape)
    .background(GlassWhite.copy(alpha = glassAlpha.coerceIn(0f, 1f)))
    .border(0.3.dp, Palette.divider, shape)
