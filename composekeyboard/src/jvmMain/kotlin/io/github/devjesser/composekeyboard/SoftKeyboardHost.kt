package io.github.devjesser.composekeyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.text.input.TextEditorState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Tracks which text field currently owns the platform text-input session. */
@OptIn(ExperimentalComposeUiApi::class)
private class ActiveTextInput {
    var request: PlatformTextInputMethodRequest? by mutableStateOf(null)
}

/** Remembers the most recent session so the keyboard keeps its content while animating out. */
@OptIn(ExperimentalComposeUiApi::class)
private class LastRequest {
    var request: PlatformTextInputMethodRequest? = null
}

/**
 * Wrap your app (or a screen) in this once and every text field inside gets an on-screen
 * keyboard: it slides in when a field gains focus and out when focus is lost, the field is
 * dismissed with Done/Go/Search/Send, or the "hide" key is pressed. No per-field changes needed.
 *
 * It works by intercepting Compose's platform text-input sessions, so it follows real focus
 * and reads each field's `KeyboardOptions` (keyboard type, capitalisation, IME action).
 *
 * @param enabled set to false to keep the wrapper in place but never show the keyboard
 *   (for example when a physical keyboard is attached).
 * @param dismissOnOutsideTap clear focus (and hide the keyboard) when the user taps content
 *   that is not a text field.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SoftKeyboardHost(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placement: KeyboardPlacement = KeyboardPlacement.Push,
    layouts: KeyboardLayoutSet = KeyboardLayoutSet.EnglishQwerty,
    theme: KeyboardTheme = KeyboardTheme.Dark,
    dismissOnOutsideTap: Boolean = true,
    content: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val active = remember { ActiveTextInput() }
    val last = remember { LastRequest() }
    val keyboardState = remember { KeyboardState() }

    val interceptor = remember(active) {
        object : PlatformTextInputInterceptor {
            override suspend fun interceptStartInputMethod(
                request: PlatformTextInputMethodRequest,
                nextHandler: PlatformTextInputSession,
            ): Nothing {
                active.request = request
                try {
                    // Keep the platform's own behaviour (IME support) and suspend until the
                    // field loses focus; cancellation runs the finally block.
                    nextHandler.startInputMethod(request)
                } finally {
                    if (active.request === request) active.request = null
                }
            }
        }
    }

    InterceptPlatformTextInput(interceptor) {
        val request = active.request
        if (request != null) last.request = request
        val displayed = last.request

        // Reset the keyboard for each new field and keep auto-capitalisation up to date.
        if (request != null) {
            LaunchedEffect(request) {
                val ime = request.imeOptions
                keyboardState.start(
                    page = pageFor(ime.keyboardType),
                    capitalization = ime.capitalization,
                    textBeforeCursor = request.state.textBeforeCursor(),
                )
                snapshotFlow { request.state.textBeforeCursor() }.collect { before ->
                    keyboardState.autoShift(ime.capitalization, before)
                }
            }
        }

        // Small grace period so moving focus from one field to another does not flicker.
        val wanted = enabled && request != null
        val visible by produceState(initialValue = false, key1 = wanted) {
            if (wanted) value = true else {
                delay(150.milliseconds)
                value = false
            }
        }

        val input = remember(displayed) {
            displayed?.let { DesktopKeyboardInput(it) { focusManager.clearFocus() } }
        }

        val keyboard: @Composable () -> Unit = {
            if (displayed != null && input != null) {
                SoftKeyboard(
                    state = keyboardState,
                    input = input,
                    layouts = layouts,
                    enterKeyStyle = enterKeyStyleFor(displayed.imeOptions),
                    theme = theme,
                )
            }
        }

        val contentModifier = if (dismissOnOutsideTap) {
            // The standard "tap elsewhere to unfocus" idiom. Taps on a text field are consumed by
            // the field itself, so this only fires for taps on everything else.
            Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        } else {
            Modifier
        }

        when (placement) {
            KeyboardPlacement.Push -> Column(modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().then(contentModifier)) { content() }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) { keyboard() }
            }
            KeyboardPlacement.Overlay -> Box(modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().then(contentModifier)) { content() }
                AnimatedVisibility(
                    visible = visible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) { keyboard() }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun TextEditorState.textBeforeCursor(): String {
    val end = selection.min.coerceIn(0, text.length)
    return text.substring(0, end)
}
