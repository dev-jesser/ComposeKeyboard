package io.github.devjesser.composekeyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class KeyIcon { Backspace, Shift, ShiftLocked, Enter, Dismiss }

/**
 * Draws key icons as vector paths on a 24x24 grid, so they do not depend on the glyph coverage
 * of whatever font the system falls back to.
 */
internal fun DrawScope.drawKeyIcon(icon: KeyIcon, color: Color) {
    val s = size.minDimension / 24f
    val stroke = Stroke(width = 1.9f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun path(build: Path.() -> Unit): Path = Path().apply(build)
    fun Path.pt(x: Float, y: Float, first: Boolean = false) {
        if (first) moveTo(x * s, y * s) else lineTo(x * s, y * s)
    }

    when (icon) {
        KeyIcon.Backspace -> {
            drawPath(
                path {
                    pt(9f, 5f, first = true); pt(21f, 5f); pt(21f, 19f); pt(9f, 19f); pt(2f, 12f); close()
                },
                color, style = stroke,
            )
            drawPath(path { pt(12.5f, 9f, first = true); pt(17.5f, 15f) }, color, style = stroke)
            drawPath(path { pt(17.5f, 9f, first = true); pt(12.5f, 15f) }, color, style = stroke)
        }
        KeyIcon.Shift, KeyIcon.ShiftLocked -> {
            val arrow = path {
                pt(12f, 3f, first = true); pt(21f, 12f); pt(16f, 12f); pt(16f, 20f)
                pt(8f, 20f); pt(8f, 12f); pt(3f, 12f); close()
            }
            if (icon == KeyIcon.ShiftLocked) drawPath(arrow, color, style = Fill) else drawPath(arrow, color, style = stroke)
        }
        KeyIcon.Enter -> {
            drawPath(path { pt(19f, 5f, first = true); pt(19f, 13f); pt(6f, 13f) }, color, style = stroke)
            drawPath(path { pt(10f, 9f, first = true); pt(6f, 13f); pt(10f, 17f) }, color, style = stroke)
        }
        KeyIcon.Dismiss -> {
            drawPath(path { pt(6f, 8f, first = true); pt(12f, 14f); pt(18f, 8f) }, color, style = stroke)
            drawPath(path { pt(6f, 19f, first = true); pt(18f, 19f) }, color, style = stroke)
        }
    }
}
