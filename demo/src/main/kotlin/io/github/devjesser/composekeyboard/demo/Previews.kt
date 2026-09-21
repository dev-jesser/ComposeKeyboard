package io.github.devjesser.composekeyboard.demo

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.devjesser.composekeyboard.EnterKeyStyle
import io.github.devjesser.composekeyboard.KeyboardInput
import io.github.devjesser.composekeyboard.KeyboardPage
import io.github.devjesser.composekeyboard.KeyboardState
import io.github.devjesser.composekeyboard.KeyboardTheme
import io.github.devjesser.composekeyboard.SoftKeyboard

// IDE previews for each page and theme. Open this file in IntelliJ IDEA and use the gutter icon
// next to @Preview (Kotlin Multiplatform plugin). Sizes match the 1280x800 target screen.

private object NoOpInput : KeyboardInput {
    override fun commit(text: String) = Unit
    override fun backspace() = Unit
    override fun enter() = Unit
    override fun dismiss() = Unit
}

@Composable
private fun PreviewKeyboard(
    page: KeyboardPage,
    theme: KeyboardTheme,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    enterKeyStyle: EnterKeyStyle = EnterKeyStyle.Return,
) {
    val state = remember { KeyboardState().apply { start(page, capitalization, "") } }
    SoftKeyboard(
        state = state,
        input = NoOpInput,
        theme = theme,
        enterKeyStyle = enterKeyStyle,
        modifier = Modifier.width(1280.dp),
    )
}

@Preview
@Composable
fun LettersDarkPreview() = PreviewKeyboard(KeyboardPage.Letters, KeyboardTheme.Dark)

@Preview
@Composable
fun LettersShiftedPreview() =
    PreviewKeyboard(KeyboardPage.Letters, KeyboardTheme.Dark, KeyboardCapitalization.Words, EnterKeyStyle.Done)

@Preview
@Composable
fun LettersLightPreview() = PreviewKeyboard(KeyboardPage.Letters, KeyboardTheme.Light)

@Preview
@Composable
fun SymbolsPreview() = PreviewKeyboard(KeyboardPage.Symbols, KeyboardTheme.Dark)

@Preview
@Composable
fun NumericPreview() = PreviewKeyboard(KeyboardPage.Numeric, KeyboardTheme.Dark, enterKeyStyle = EnterKeyStyle.Next)
