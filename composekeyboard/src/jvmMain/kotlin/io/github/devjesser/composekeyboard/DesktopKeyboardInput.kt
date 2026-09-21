package io.github.devjesser.composekeyboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType

/**
 * Sends key presses to the text field that currently owns the platform text-input session,
 * using the same edit commands a real input method would.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class DesktopKeyboardInput(
    private val request: PlatformTextInputMethodRequest,
    private val dismissKeyboard: () -> Unit,
) : KeyboardInput {

    override fun commit(text: String) {
        request.onEditCommand(listOf(CommitTextCommand(text, 1)))
    }

    override fun backspace() {
        request.onEditCommand(listOf(BackspaceCommand()))
    }

    override fun enter() {
        val ime = request.imeOptions
        val action = ime.imeAction
        val performsAction = ime.singleLine || (action != ImeAction.Default && action != ImeAction.None)
        if (!performsAction) {
            // Multi-line field without an explicit action: Enter inserts a line break.
            commit("\n")
            return
        }
        request.onImeAction?.invoke(action)
        if (dismissesKeyboard(action, ime.singleLine)) dismissKeyboard()
    }

    override fun dismiss() = dismissKeyboard()
}

private fun dismissesKeyboard(action: ImeAction, singleLine: Boolean): Boolean = when (action) {
    ImeAction.Default -> singleLine
    ImeAction.Done, ImeAction.Go, ImeAction.Search, ImeAction.Send -> true
    else -> false
}

internal fun enterKeyStyleFor(ime: ImeOptions): EnterKeyStyle = when (ime.imeAction) {
    ImeAction.Done -> EnterKeyStyle.Done
    ImeAction.Next, ImeAction.Previous -> EnterKeyStyle.Next
    ImeAction.Go -> EnterKeyStyle.Go
    ImeAction.Search -> EnterKeyStyle.Search
    ImeAction.Send -> EnterKeyStyle.Send
    ImeAction.Default -> if (ime.singleLine) EnterKeyStyle.Done else EnterKeyStyle.Return
    else -> EnterKeyStyle.Return
}

internal fun pageFor(type: KeyboardType): KeyboardPage = when (type) {
    KeyboardType.Number, KeyboardType.Phone, KeyboardType.Decimal, KeyboardType.NumberPassword -> KeyboardPage.Numeric
    else -> KeyboardPage.Letters
}
