package io.github.devjesser.composekeyboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyboardLayoutTest {
    private val layouts = KeyboardLayoutSet.EnglishQwerty

    private fun assertRowsAlign(page: KeyboardPage) {
        val totals = layouts.layoutFor(page).rows.map { row -> row.sumOf { it.weight.toDouble() } }
        totals.forEach { assertEquals(totals.first(), it, 1e-6, "every row on $page must have the same total weight: $totals") }
    }

    @Test fun lettersRowsAlign() = assertRowsAlign(KeyboardPage.Letters)
    @Test fun symbolsRowsAlign() = assertRowsAlign(KeyboardPage.Symbols)
    @Test fun numericRowsAlign() = assertRowsAlign(KeyboardPage.Numeric)

    @Test
    fun everyPageCanBackspaceAndDismissAndEnter() {
        KeyboardPage.entries.forEach { page ->
            val actions = layouts.layoutFor(page).rows.flatten().filterIsInstance<SoftKey.Action>().map { it.action }
            assertTrue(KeyAction.Backspace in actions, "$page needs Backspace")
            assertTrue(KeyAction.Enter in actions, "$page needs Enter")
            assertTrue(KeyAction.Dismiss in actions, "$page needs Dismiss")
        }
    }

    @Test
    fun lettersPageHasAllLettersAndUsernamePunctuation() {
        val typed = layouts.letters.rows.flatten().filterIsInstance<SoftKey.Character>().map { it.lower }.toSet()
        ('a'..'z').forEach { assertTrue(it.toString() in typed, "missing letter $it") }
        listOf("@", "_", "-", ".", ",", "0", "9").forEach { assertTrue(it in typed, "missing $it") }
    }

    @Test
    fun numericPageHasDateSeparators() {
        val typed = layouts.numeric.rows.flatten().filterIsInstance<SoftKey.Character>().map { it.lower }.toSet()
        ("0123456789./-").forEach { assertTrue(it.toString() in typed, "missing $it") }
    }
}
