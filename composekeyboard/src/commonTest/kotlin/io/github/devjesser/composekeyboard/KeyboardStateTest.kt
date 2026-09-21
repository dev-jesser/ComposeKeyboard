package io.github.devjesser.composekeyboard

import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

private class RecordingInput : KeyboardInput {
    val events = mutableListOf<String>()
    override fun commit(text: String) { events += "commit:$text" }
    override fun backspace() { events += "backspace" }
    override fun enter() { events += "enter" }
    override fun dismiss() { events += "dismiss" }
}

@OptIn(ExperimentalTime::class)
class KeyboardStateTest {
    private val time = TestTimeSource()
    private val state = KeyboardState(time)
    private val input = RecordingInput()

    private val a = SoftKey.Character("a")
    private val shift = SoftKey.Action(KeyAction.Shift)

    private fun start(cap: KeyboardCapitalization = KeyboardCapitalization.None, before: String = "") =
        state.start(KeyboardPage.Letters, cap, before)

    @Test
    fun lowercaseByDefault() {
        start()
        state.onKey(a, input)
        assertEquals(listOf("commit:a"), input.events)
    }

    @Test
    fun shiftCapitalisesOneLetterThenTurnsOff() {
        start()
        state.onKey(shift, input)
        assertEquals(ShiftState.Once, state.shift)
        state.onKey(a, input)
        state.onKey(a, input)
        assertEquals(listOf("commit:A", "commit:a"), input.events)
        assertEquals(ShiftState.Off, state.shift)
    }

    @Test
    fun quickDoubleTapLocksCapsAndThirdTapReleases() {
        start()
        state.onKey(shift, input)
        time += 100.milliseconds
        state.onKey(shift, input)
        assertEquals(ShiftState.Locked, state.shift)
        state.onKey(a, input)
        state.onKey(a, input)
        assertEquals(listOf("commit:A", "commit:A"), input.events)
        state.onKey(shift, input)
        assertEquals(ShiftState.Off, state.shift)
    }

    @Test
    fun slowSecondTapTurnsShiftOff() {
        start()
        state.onKey(shift, input)
        time += 2_000.milliseconds
        state.onKey(shift, input)
        assertEquals(ShiftState.Off, state.shift)
    }

    @Test
    fun shiftDoesNotAffectSymbolsPage() {
        start()
        state.onKey(shift, input)
        state.onKey(SoftKey.Action(KeyAction.ShowSymbols), input)
        state.onKey(SoftKey.Character("x"), input)
        assertEquals(KeyboardPage.Symbols, state.page)
        assertEquals(listOf("commit:x"), input.events)
    }

    @Test
    fun actionKeysAreForwarded() {
        start()
        state.onKey(SoftKey.Action(KeyAction.Backspace), input)
        state.onKey(SoftKey.Action(KeyAction.Enter), input)
        state.onKey(SoftKey.Action(KeyAction.Space), input)
        state.onKey(SoftKey.Action(KeyAction.Dismiss), input)
        assertEquals(listOf("backspace", "enter", "commit: ", "dismiss"), input.events)
    }

    @Test
    fun wordsCapitalisationShiftsAtStartAndAfterSpace() {
        start(KeyboardCapitalization.Words)
        assertEquals(ShiftState.Once, state.shift)
        state.onKey(a, input) // consumes the shift
        state.autoShift(KeyboardCapitalization.Words, "A")
        assertEquals(ShiftState.Off, state.shift)
        state.autoShift(KeyboardCapitalization.Words, "Anna ")
        assertEquals(ShiftState.Once, state.shift)
    }

    @Test
    fun sentencesCapitalisationNeedsPunctuationAndSpace() {
        start(KeyboardCapitalization.Sentences)
        assertEquals(ShiftState.Once, state.shift) // empty field
        state.onKey(a, input)
        state.autoShift(KeyboardCapitalization.Sentences, "Hello")
        assertEquals(ShiftState.Off, state.shift)
        state.autoShift(KeyboardCapitalization.Sentences, "Hello.")
        assertEquals(ShiftState.Off, state.shift)
        state.autoShift(KeyboardCapitalization.Sentences, "Hello. ")
        assertEquals(ShiftState.Once, state.shift)
    }

    @Test
    fun charactersCapitalisationLocksCaps() {
        start(KeyboardCapitalization.Characters)
        assertEquals(ShiftState.Locked, state.shift)
    }

    @Test
    fun autoShiftNeverTurnsShiftOff() {
        start()
        state.onKey(shift, input) // user turns shift on manually
        state.autoShift(KeyboardCapitalization.Words, "hello")
        assertEquals(ShiftState.Once, state.shift)
    }
}
