package io.github.devjesser.composekeyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end tests: real text fields inside a SoftKeyboardHost, driven only through the keyboard's keys.
 * They run headless with `./gradlew :composekeyboard:jvmTest`.
 */
@OptIn(ExperimentalTestApi::class)
class SoftKeyboardHostTest {

    private fun ComposeUiTest.focus(tag: String) {
        onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
    }

    private fun ComposeUiTest.press(id: String) {
        onNodeWithTag("key:$id").performTouchInput { click() }
    }

    private fun ComposeUiTest.awaitKey(id: String) {
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("key:$id").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.awaitKeyGone(id: String) {
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("key:$id").fetchSemanticsNodes().isEmpty() }
    }

    private fun ComposeUiTest.boundsHeight(tag: String?): Float {
        val node = if (tag == null) onRoot() else onNodeWithTag(tag)
        val bounds = node.getBoundsInRoot()
        return (bounds.bottom - bounds.top).value
    }

    @Test
    fun keyboardAppearsOnFocusAndTypesIntoTheField() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            SoftKeyboardHost {
                BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
            }
        }
        onNodeWithTag("key:h").assertDoesNotExist()

        focus("field")
        awaitKey("h")
        press("h")
        press("i")
        press("Backspace")
        press("y")
        waitForIdle()

        assertEquals("hy", text)
    }

    @Test
    fun numericFieldGetsTheNumericPad() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            SoftKeyboardHost {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.testTag("field"),
                )
            }
        }
        focus("field")
        awaitKey("/") // only exists on the numeric pad
        onNodeWithTag("key:q").assertDoesNotExist()

        press("1")
        press("2")
        press("/")
        waitForIdle()
        assertEquals("12/", text)
    }

    @Test
    fun wordsCapitalisationShiftsTheFirstLetter() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            SoftKeyboardHost {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.testTag("field"),
                )
            }
        }
        focus("field")
        awaitKey("j")
        waitForIdle()
        press("j")
        press("o")
        waitForIdle()
        assertEquals("Jo", text)
    }

    @Test
    fun doneKeyRunsTheKeyboardActionAndHidesTheKeyboard() = runComposeUiTest {
        var text by mutableStateOf("")
        var done = false
        setContent {
            SoftKeyboardHost {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { done = true }),
                    modifier = Modifier.testTag("field"),
                )
            }
        }
        focus("field")
        awaitKey("h")
        press("Enter")
        waitForIdle()

        assertTrue(done, "the field's onDone action should have run")
        awaitKeyGone("h")
    }

    @Test
    fun hideKeyClearsFocusAndHidesTheKeyboard() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            SoftKeyboardHost {
                BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
            }
        }
        focus("field")
        awaitKey("h")
        press("Dismiss")
        awaitKeyGone("h")
    }

    @Test
    fun movingFocusRetargetsTheKeyboardToTheNewField() = runComposeUiTest {
        var first by mutableStateOf("")
        var second by mutableStateOf("")
        setContent {
            SoftKeyboardHost {
                BasicTextField(value = first, onValueChange = { first = it }, modifier = Modifier.testTag("first"))
                BasicTextField(value = second, onValueChange = { second = it }, modifier = Modifier.testTag("second"))
            }
        }
        focus("first")
        awaitKey("a")
        press("a")
        waitForIdle()

        focus("second")
        waitForIdle()
        press("b")
        waitForIdle()

        assertEquals("a", first)
        assertEquals("b", second)
    }

    @Test
    fun disabledHostNeverShowsTheKeyboard() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            SoftKeyboardHost(enabled = false) {
                BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
            }
        }
        focus("field")
        waitForIdle()
        onNodeWithTag("key:h").assertDoesNotExist()
    }

    @Test
    fun heightFractionSizesEveryPageRelativeToTheHost() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            SoftKeyboardHost(heightFraction = 0.5f) {
                BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
            }
        }
        focus("field")
        awaitKey("h")
        waitForIdle()

        fun heightOf(tag: String?): Float {
            val node = if (tag == null) onRoot() else onNodeWithTag(tag)
            val bounds = node.getBoundsInRoot()
            return (bounds.bottom - bounds.top).value
        }

        val host = heightOf(null)
        assertEquals(host * 0.5f, heightOf("softKeyboard"), 2f, "letters page should be half the host height")

        // The symbols page has fewer rows but must keep the same total height.
        press("ShowSymbols")
        awaitKey("ShowLetters")
        waitForIdle()
        assertEquals(host * 0.5f, heightOf("softKeyboard"), 2f, "symbols page should be half the host height")
    }

    // ---- LocalSoftKeyboard ----

    @Test
    fun outsideAHostTheKeyboardInfoReportsHidden() = runComposeUiTest {
        var info: SoftKeyboardInfo? = null
        setContent { info = LocalSoftKeyboard.current }
        waitForIdle()

        val keyboard = checkNotNull(info)
        assertFalse(keyboard.isVisible)
        assertEquals(0f, keyboard.expectedHeight.value)
        assertEquals(0f, keyboard.currentHeight.value)
        assertEquals(0f, keyboard.hostHeight.value)
    }

    private fun checkInfoFollowsTheKeyboard(placement: KeyboardPlacement, fraction: Float?) = runComposeUiTest {
        var text by mutableStateOf("")
        var info: SoftKeyboardInfo? = null
        setContent {
            SoftKeyboardHost(placement = placement, heightFraction = fraction) {
                info = LocalSoftKeyboard.current
                BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
            }
        }
        waitForIdle()
        val keyboard = checkNotNull(info)

        // Before any field has focus: hidden, but it already knows how tall it will be.
        val host = boundsHeight(null)
        assertFalse(keyboard.isVisible)
        assertEquals(host, keyboard.hostHeight.value, 1f, "host height")
        val expectedBefore = if (fraction != null) {
            host * fraction
        } else {
            KeyboardLayoutSet.EnglishQwerty.tallestNaturalHeight(KeyboardDimensions()).value
        }
        assertEquals(expectedBefore, keyboard.expectedHeight.value, 1f, "expected height before the keyboard shows")
        assertEquals(0f, keyboard.currentHeight.value, 1f, "nothing is on screen yet")

        // A field gets focus: it becomes visible and the height on screen grows to the expected one.
        focus("field")
        awaitKey("h")
        assertTrue(keyboard.isVisible)
        waitUntil(timeoutMillis = 5_000) { abs(keyboard.currentHeight.value - keyboard.expectedHeight.value) < 1f }
        assertEquals(boundsHeight("softKeyboard"), keyboard.expectedHeight.value, 1f, "expected vs real height")
        assertEquals(expectedBefore, keyboard.expectedHeight.value, 1f, "expected height does not change on show")

        // The hide key: not visible any more (after the grace period) and nothing is on screen.
        press("Dismiss")
        waitUntil(timeoutMillis = 5_000) { !keyboard.isVisible }
        waitUntil(timeoutMillis = 5_000) { keyboard.currentHeight.value < 1f }
    }

    @Test
    fun infoFollowsTheKeyboardWithPushAndHeightFraction() = checkInfoFollowsTheKeyboard(KeyboardPlacement.Push, 0.5f)

    @Test
    fun infoFollowsTheKeyboardWithOverlayAndHeightFraction() =
        checkInfoFollowsTheKeyboard(KeyboardPlacement.Overlay, 0.5f)

    @Test
    fun infoFollowsTheKeyboardWithPushAndKeyHeight() = checkInfoFollowsTheKeyboard(KeyboardPlacement.Push, null)

    @Test
    fun infoFollowsTheKeyboardWithOverlayAndKeyHeight() = checkInfoFollowsTheKeyboard(KeyboardPlacement.Overlay, null)

    private fun checkDisabledHostReportsNoKeyboard(placement: KeyboardPlacement) = runComposeUiTest {
        var text by mutableStateOf("")
        var info: SoftKeyboardInfo? = null
        setContent {
            SoftKeyboardHost(enabled = false, placement = placement, heightFraction = 0.5f) {
                info = LocalSoftKeyboard.current
                BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
            }
        }
        focus("field")
        waitForIdle()

        val keyboard = checkNotNull(info)
        assertFalse(keyboard.isVisible)
        assertEquals(0f, keyboard.expectedHeight.value)
        assertEquals(0f, keyboard.currentHeight.value, 0.5f)
    }

    @Test
    fun disabledPushHostReportsNoKeyboard() = checkDisabledHostReportsNoKeyboard(KeyboardPlacement.Push)

    @Test
    fun disabledOverlayHostReportsNoKeyboard() = checkDisabledHostReportsNoKeyboard(KeyboardPlacement.Overlay)

    @Test
    fun softKeyboardPaddingKeepsOverlayContentAboveTheKeyboard() = runComposeUiTest {
        var text by mutableStateOf("")
        var info: SoftKeyboardInfo? = null
        setContent {
            SoftKeyboardHost(placement = KeyboardPlacement.Overlay, heightFraction = 0.5f) {
                info = LocalSoftKeyboard.current
                Box(Modifier.fillMaxSize().softKeyboardPadding()) {
                    Box(Modifier.fillMaxSize().testTag("content")) {
                        BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.testTag("field"))
                    }
                }
            }
        }
        waitForIdle()
        val keyboard = checkNotNull(info)
        val host = boundsHeight(null)
        assertEquals(host, boundsHeight("content"), 1f, "content fills the host while the keyboard is hidden")

        focus("field")
        awaitKey("h")
        waitUntil(timeoutMillis = 5_000) { abs(keyboard.currentHeight.value - keyboard.expectedHeight.value) < 1f }
        waitForIdle()
        assertEquals(
            host - keyboard.expectedHeight.value, boundsHeight("content"), 2f,
            "content is laid out above the keyboard",
        )
    }
}
