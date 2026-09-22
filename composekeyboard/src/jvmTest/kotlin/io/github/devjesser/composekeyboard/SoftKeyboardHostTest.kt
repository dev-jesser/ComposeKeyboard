package io.github.devjesser.composekeyboard

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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
