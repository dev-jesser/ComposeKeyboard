package io.github.devjesser.composekeyboard

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Receives the result of key presses. Implemented by the platform integration. */
interface KeyboardInput {
    /** Insert [text] at the cursor (replacing any selection). */
    fun commit(text: String)

    /** Delete the selection, or the character before the cursor. */
    fun backspace()

    /** The Enter / action key was pressed. */
    fun enter()

    /** The "hide keyboard" key was pressed. */
    fun dismiss()
}

enum class ShiftState {
    Off,

    /** Capitalise the next letter only. */
    Once,

    /** Caps lock. */
    Locked,
}

/**
 * Pure keyboard logic: current page, shift / caps-lock and auto-capitalisation.
 * It has no dependency on any text field, which keeps it easy to unit test.
 */
@Stable
class KeyboardState(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    var page: KeyboardPage by mutableStateOf(KeyboardPage.Letters)
        private set

    var shift: ShiftState by mutableStateOf(ShiftState.Off)
        private set

    private var lastShiftTap: TimeMark? = null

    /** Reset for a new text field. */
    fun start(
        page: KeyboardPage,
        capitalization: KeyboardCapitalization,
        textBeforeCursor: String,
    ) {
        this.page = page
        shift = ShiftState.Off
        lastShiftTap = null
        autoShift(capitalization, textBeforeCursor)
    }

    /**
     * Turns Shift on when the field's capitalisation mode asks for it. It only ever switches
     * Shift *on* (never off), so it cannot fight the user's own Shift presses.
     */
    fun autoShift(capitalization: KeyboardCapitalization, textBeforeCursor: String) {
        if (page != KeyboardPage.Letters || shift == ShiftState.Locked) return
        when (capitalization) {
            KeyboardCapitalization.Characters -> shift = ShiftState.Locked
            KeyboardCapitalization.Words ->
                if (shift == ShiftState.Off && startsWord(textBeforeCursor)) shift = ShiftState.Once
            KeyboardCapitalization.Sentences ->
                if (shift == ShiftState.Off && startsSentence(textBeforeCursor)) shift = ShiftState.Once
            else -> Unit
        }
    }

    fun onKey(key: SoftKey, input: KeyboardInput) {
        when (key) {
            is SoftKey.Character -> {
                val shifted = shift != ShiftState.Off && page == KeyboardPage.Letters
                input.commit(if (shifted) key.upper else key.lower)
                if (shift == ShiftState.Once) shift = ShiftState.Off
            }
            is SoftKey.Action -> when (key.action) {
                KeyAction.Shift -> onShiftTapped()
                KeyAction.Backspace -> input.backspace()
                KeyAction.Enter -> input.enter()
                KeyAction.Space -> input.commit(" ")
                KeyAction.ShowLetters -> page = KeyboardPage.Letters
                KeyAction.ShowSymbols -> page = KeyboardPage.Symbols
                KeyAction.Dismiss -> input.dismiss()
            }
            is SoftKey.Gap -> Unit
        }
    }

    private fun onShiftTapped() {
        val previous = lastShiftTap
        lastShiftTap = timeSource.markNow()
        shift = when (shift) {
            ShiftState.Off -> ShiftState.Once
            // A quick second tap means caps lock.
            ShiftState.Once ->
                if (previous != null && previous.elapsedNow() < DoubleTapWindow) ShiftState.Locked else ShiftState.Off
            ShiftState.Locked -> ShiftState.Off
        }
    }

    private fun startsWord(before: String): Boolean = before.isEmpty() || before.last().isWhitespace()

    private fun startsSentence(before: String): Boolean {
        if (before.isBlank()) return true
        if (!before.last().isWhitespace()) return false
        val last = before.trimEnd().last()
        return last == '.' || last == '!' || last == '?'
    }

    private companion object {
        val DoubleTapWindow = 400.milliseconds
    }
}
