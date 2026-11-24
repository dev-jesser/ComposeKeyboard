package io.github.devjesser.composekeyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** What the Enter key shows. Icon for [Return]; a text label for the others. */
enum class EnterKeyStyle(internal val label: String?) {
    Return(null),
    Done("Done"),
    Next("Next"),
    Go("Go"),
    Search("Search"),
    Send("Send"),
}

private const val RepeatDelayMs = 400L
private const val RepeatIntervalMs = 55L

/**
 * The keyboard UI. It is stateless with respect to any text field: key presses are reported to
 * [input] and pages / shift live in [state]. Use `SoftKeyboardHost` (jvmMain) to attach it to
 * real text fields, or use this directly for previews and tests.
 *
 * Keys deliberately use raw pointer input rather than `clickable`, so pressing them never
 * takes keyboard focus away from the text field being edited.
 *
 * @param height total height of the keyboard. When specified, every page has exactly this height
 *   and its rows share it equally (pages with fewer rows get taller keys), so the keyboard never
 *   changes height when switching pages. When left unspecified, each key is
 *   `theme.dimensions.keyHeight` tall and a page's height follows from its number of rows.
 *   `SoftKeyboardHost(heightFraction = ...)` computes this from the window size for you.
 */
@Composable
fun SoftKeyboard(
    state: KeyboardState,
    input: KeyboardInput,
    modifier: Modifier = Modifier,
    layouts: KeyboardLayoutSet = KeyboardLayoutSet.EnglishQwerty,
    enterKeyStyle: EnterKeyStyle = EnterKeyStyle.Return,
    theme: KeyboardTheme = KeyboardTheme.Dark,
    height: Dp = Dp.Unspecified,
) {
    val layout = layouts.layoutFor(state.page)
    val fixedHeight = height.isSpecified
    val dimensions = theme.dimensions
    val cellPadding = dimensions.keySpacing / 2

    Box(
        modifier = modifier
            .testTag("softKeyboard")
            .fillMaxWidth()
            .then(if (fixedHeight) Modifier.height(height) else Modifier)
            .background(theme.colors.background)
            // Swallow taps on the keyboard background so they never reach content underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(dimensions.outerPadding - cellPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxWidth(layout.widthFraction).then(if (fixedHeight) Modifier.fillMaxHeight() else Modifier)) {
            layout.rows.forEach { row ->
                Row(Modifier.fillMaxWidth().then(if (fixedHeight) Modifier.weight(1f) else Modifier)) {
                    row.forEach { key ->
                        val cell = Modifier.weight(key.weight)
                            .then(if (fixedHeight) Modifier.fillMaxHeight() else Modifier.height(dimensions.keyHeight))
                        if (key is SoftKey.Gap) {
                            Spacer(cell)
                        } else {
                            KeyButton(key, state, input, enterKeyStyle, theme, cell.padding(cellPadding))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: SoftKey,
    state: KeyboardState,
    input: KeyboardInput,
    enterKeyStyle: EnterKeyStyle,
    theme: KeyboardTheme,
    modifier: Modifier,
) {
    val colors = theme.colors
    // Keyed on [key]: composable slots are reused when the page changes (e.g. the "ABC" key's slot
    // becomes the Shift key's), and a stale "pressed" flag would otherwise stay stuck on the new key.
    var pressed by remember(key) { mutableStateOf(false) }
    val feedback = remember(key) { PressFeedback() }
    // The input target changes whenever another text field takes focus; always use the latest.
    val currentInput by rememberUpdatedState(input)

    val isEnter = key is SoftKey.Action && key.action == KeyAction.Enter
    val shiftActive = key is SoftKey.Action && key.action == KeyAction.Shift && state.shift != ShiftState.Off
    val container = when {
        pressed -> colors.keyPressed
        isEnter || shiftActive -> colors.accent
        key is SoftKey.Action -> colors.actionKey
        else -> colors.key
    }
    val content = if (isEnter || shiftActive) colors.onAccent else colors.onKey

    Box(
        modifier = modifier
            .testTag("key:" + key.id)
            .clip(RoundedCornerShape(theme.dimensions.cornerRadius))
            .background(container)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        val press = feedback.pressStarted()
                        pressed = true
                        // Fire on press (not release) so fast typing feels immediate.
                        state.onKey(key, currentInput)
                        if (key is SoftKey.Action && key.repeatable) {
                            coroutineScope {
                                val repeater = launch {
                                    delay(RepeatDelayMs.milliseconds)
                                    while (true) {
                                        state.onKey(key, currentInput)
                                        delay(RepeatIntervalMs.milliseconds)
                                    }
                                }
                                tryAwaitRelease()
                                repeater.cancel()
                            }
                        } else {
                            tryAwaitRelease()
                        }
                        // Touch screens can deliver down and up in the same instant; hold the pressed
                        // look long enough to be seen. A newer press of this key takes over from us.
                        val wait = feedback.remainingVisibleTime(press)
                        if (wait > Duration.ZERO) delay(wait)
                        if (feedback.mayClear(press)) pressed = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            is SoftKey.Character -> {
                val shifted = state.shift != ShiftState.Off && state.page == KeyboardPage.Letters
                KeyLabel(if (shifted) key.upper else key.lower, content, theme.dimensions.keyFontSize)
            }
            is SoftKey.Action -> when (key.action) {
                KeyAction.Shift -> KeyIconView(
                    if (state.shift == ShiftState.Locked) KeyIcon.ShiftLocked else KeyIcon.Shift,
                    content,
                )
                KeyAction.Backspace -> KeyIconView(KeyIcon.Backspace, content)
                KeyAction.Dismiss -> KeyIconView(KeyIcon.Dismiss, content)
                KeyAction.Enter -> {
                    val label = enterKeyStyle.label
                    if (label == null) KeyIconView(KeyIcon.Enter, content)
                    else KeyLabel(label, content, theme.dimensions.labelFontSize)
                }
                KeyAction.ShowSymbols -> KeyLabel("?123", content, theme.dimensions.labelFontSize)
                KeyAction.ShowLetters -> KeyLabel("ABC", content, theme.dimensions.labelFontSize)
                KeyAction.Space -> KeyLabel("Space", content, theme.dimensions.labelFontSize)
            }
            is SoftKey.Gap -> Unit
        }
    }
}

@Composable
private fun KeyLabel(text: String, color: Color, fontSize: TextUnit) {
    BasicText(
        text = text,
        style = TextStyle(color = color, fontSize = fontSize, textAlign = TextAlign.Center),
    )
}

@Composable
private fun KeyIconView(icon: KeyIcon, color: Color) {
    Canvas(Modifier.size(28.dp)) { drawKeyIcon(icon, color) }
}
