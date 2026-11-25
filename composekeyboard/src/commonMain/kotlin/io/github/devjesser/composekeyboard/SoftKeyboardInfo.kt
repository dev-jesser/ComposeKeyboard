package io.github.devjesser.composekeyboard

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * What the on-screen keyboard is doing, for the content inside a `SoftKeyboardHost`.
 *
 * Read it through [LocalSoftKeyboard]. The same instance is provided for as long as the host is in
 * the composition, and each property is backed by snapshot state, so only the code that reads a
 * property is recomposed when it changes (not the whole screen).
 */
@Stable
interface SoftKeyboardInfo {
    /**
     * True from the moment the keyboard is wanted (a text field has focus) until it is gone. It is
     * the value that drives the slide animation, so it flips at the start of the animation, and it
     * stays true for a short grace period when focus moves between two fields.
     */
    val isVisible: Boolean

    /**
     * The height the keyboard has, or will have, once fully shown. It is known BEFORE the keyboard
     * appears, so a screen can choose its layout up front instead of switching halfway through the
     * slide-in animation. `0.dp` when the host is disabled, and for the first frame when the host
     * is sized with `heightFraction` and has not been measured yet.
     *
     * With `heightFraction` this is exactly the keyboard height. Without it the keyboard is as tall
     * as its keys make it, which depends on the page, so this is the height of the tallest page
     * (the letters page); the symbols and numeric pages are one row shorter.
     */
    val expectedHeight: Dp

    /**
     * How much of [expectedHeight] is on screen right now, animated. Use it for effects and for
     * [softKeyboardPadding], not for choosing a layout: it changes on every frame of the animation.
     * It follows the slide animation closely but is a separate animation, so treat it as
     * approximate.
     */
    val currentHeight: Dp

    /**
     * The full height of the `SoftKeyboardHost`, whether or not the keyboard is showing. `0.dp` for
     * the first frame, until the host has been measured.
     *
     * `hostHeight - expectedHeight` is the space left above the keyboard, and it does not depend on
     * the placement or on the animation. It is the stable thing to base a layout choice on; the
     * live height of your own content is not, because with `KeyboardPlacement.Push` it shrinks
     * while the keyboard slides in.
     */
    val hostHeight: Dp
}

/**
 * The state of the nearest `SoftKeyboardHost`. Outside a host (previews, tests, or a machine with a
 * physical keyboard and no host) it reports a keyboard that is hidden and 0.dp tall, so screens
 * that read it keep working.
 */
val LocalSoftKeyboard: ProvidableCompositionLocal<SoftKeyboardInfo> =
    staticCompositionLocalOf { HiddenSoftKeyboardInfo }

private object HiddenSoftKeyboardInfo : SoftKeyboardInfo {
    override val isVisible: Boolean = false
    override val expectedHeight: Dp = 0.dp
    override val currentHeight: Dp = 0.dp
    override val hostHeight: Dp = 0.dp
}

/** [SoftKeyboardInfo] that reads straight from the host's state, so readers recompose precisely. */
internal class SoftKeyboardInfoImpl(
    private val visible: State<Boolean>,
    private val expected: State<Dp>,
    private val current: State<Dp>,
    private val host: State<Dp>,
) : SoftKeyboardInfo {
    override val isVisible: Boolean get() = visible.value
    override val expectedHeight: Dp get() = expected.value
    override val currentHeight: Dp get() = current.value
    override val hostHeight: Dp get() = host.value
}

/**
 * Leaves room at the bottom for the keyboard, like `imePadding()`: the content is laid out in the
 * space above the keyboard and follows it as it slides in and out.
 *
 * Use it for content in a host with `KeyboardPlacement.Overlay`, where the keyboard covers the
 * content; put it on the scrollable container so a focused field is scrolled into view above the
 * keys. It is not needed with `KeyboardPlacement.Push`, where the host already shrinks the content.
 * Outside a host it does nothing.
 */
fun Modifier.softKeyboardPadding(): Modifier = composed {
    val keyboard = LocalSoftKeyboard.current
    Modifier.layout { measurable, constraints ->
        // Read inside the layout block, so the animation re-lays out instead of recomposing.
        val bottom = keyboard.currentHeight.roundToPx().coerceAtLeast(0)
        val placeable = measurable.measure(constraints.offset(vertical = -bottom))
        layout(placeable.width, placeable.height + bottom) {
            placeable.placeRelative(0, 0)
        }
    }
}
