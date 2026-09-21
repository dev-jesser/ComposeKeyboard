package io.github.devjesser.composekeyboard

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.test.Test
import kotlin.test.assertEquals

class ImeMappingTest {
    @Test
    fun numericKeyboardTypesUseTheNumericPage() {
        listOf(KeyboardType.Number, KeyboardType.Phone, KeyboardType.Decimal, KeyboardType.NumberPassword)
            .forEach { assertEquals(KeyboardPage.Numeric, pageFor(it), "$it") }
    }

    @Test
    fun textualKeyboardTypesUseTheLettersPage() {
        listOf(KeyboardType.Text, KeyboardType.Password, KeyboardType.Email, KeyboardType.Uri)
            .forEach { assertEquals(KeyboardPage.Letters, pageFor(it), "$it") }
    }

    @Test
    fun enterKeyStyleFollowsTheImeAction() {
        assertEquals(EnterKeyStyle.Done, enterKeyStyleFor(ImeOptions(imeAction = ImeAction.Done)))
        assertEquals(EnterKeyStyle.Next, enterKeyStyleFor(ImeOptions(imeAction = ImeAction.Next)))
        assertEquals(EnterKeyStyle.Search, enterKeyStyleFor(ImeOptions(imeAction = ImeAction.Search)))
        assertEquals(EnterKeyStyle.Done, enterKeyStyleFor(ImeOptions(singleLine = true)))
        assertEquals(EnterKeyStyle.Return, enterKeyStyleFor(ImeOptions(singleLine = false)))
    }
}
