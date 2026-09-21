package io.github.devjesser.composekeyboard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
class KeyboardColors(
    val background: Color,
    val key: Color,
    val actionKey: Color,
    val keyPressed: Color,
    val accent: Color,
    val onKey: Color,
    val onAccent: Color,
)

@Immutable
class KeyboardDimensions(
    val keyHeight: Dp = 56.dp,
    val keySpacing: Dp = 6.dp,
    val cornerRadius: Dp = 8.dp,
    val outerPadding: Dp = 8.dp,
    val keyFontSize: TextUnit = 22.sp,
    val labelFontSize: TextUnit = 16.sp,
)

@Immutable
class KeyboardTheme(
    val colors: KeyboardColors,
    val dimensions: KeyboardDimensions = KeyboardDimensions(),
) {
    fun copy(
        colors: KeyboardColors = this.colors,
        dimensions: KeyboardDimensions = this.dimensions,
    ): KeyboardTheme = KeyboardTheme(colors, dimensions)

    companion object {
        val Dark: KeyboardTheme = KeyboardTheme(
            KeyboardColors(
                background = Color(0xFF1E1E1E),
                key = Color(0xFF3B3B3B),
                actionKey = Color(0xFF2C2C2C),
                keyPressed = Color(0xFF666666),
                accent = Color(0xFF0A64AD),
                onKey = Color(0xFFF2F2F2),
                onAccent = Color.White,
            ),
        )

        val Light: KeyboardTheme = KeyboardTheme(
            KeyboardColors(
                background = Color(0xFFD5D7DC),
                key = Color(0xFFFFFFFF),
                actionKey = Color(0xFFB9BCC4),
                keyPressed = Color(0xFF9EA2AB),
                accent = Color(0xFF0A64AD),
                onKey = Color(0xFF1B1B1B),
                onAccent = Color.White,
            ),
        )
    }
}
