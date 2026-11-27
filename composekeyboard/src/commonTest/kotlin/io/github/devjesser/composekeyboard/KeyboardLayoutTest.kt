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
    @Test fun moreSymbolsRowsAlign() = assertRowsAlign(KeyboardPage.MoreSymbols)

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

    @Test
    fun symbolsPageHasBothSymbolsAndANumpadTogether() {
        val typed = layouts.symbols.rows.flatten().filterIsInstance<SoftKey.Character>().map { it.lower }.toSet()
        // Numpad digits...
        ('0'..'9').forEach { assertTrue(it.toString() in typed, "missing digit $it") }
        // ...alongside symbols, on the same page (the TabTip "&123" style).
        listOf("!", "@", "#", "\$", "%", "&", "(", ")", "-", "_", "=", "+", "\"", "'", "/", "^")
            .forEach { assertTrue(it in typed, "missing symbol $it") }
    }

    @Test
    fun everyPrintableAsciiCharacterCanBeTyped() {
        // Passwords can contain any of these, so no key may be missing from the built-in pages.
        val typeable = KeyboardPage.entries
            .flatMap { page -> layouts.layoutFor(page).rows.flatten() }
            .filterIsInstance<SoftKey.Character>()
            .flatMap { listOf(it.lower, it.upper) } // upper: what Shift types on the letters page
            .toSet()
        val missing = ('!'..'~').map { it.toString() }.filter { it !in typeable }
        assertTrue(missing.isEmpty(), "no key types: ${missing.joinToString(" ")}")
    }

    @Test
    fun spaceCanBeTypedOnEveryTextPage() {
        // The numeric pad is for number and date fields and deliberately has no Space.
        (KeyboardPage.entries - KeyboardPage.Numeric).forEach { page ->
            val actions = layouts.layoutFor(page).rows.flatten().filterIsInstance<SoftKey.Action>().map { it.action }
            assertTrue(KeyAction.Space in actions, "$page needs Space")
        }
    }

    @Test
    fun theSymbolsPagesLinkToEachOtherAndBackToTheLetters() {
        fun actionsOf(page: KeyboardPage) =
            layouts.layoutFor(page).rows.flatten().filterIsInstance<SoftKey.Action>().map { it.action }
        assertTrue(KeyAction.ShowSymbols in actionsOf(KeyboardPage.Letters))
        assertTrue(KeyAction.ShowMoreSymbols in actionsOf(KeyboardPage.Symbols))
        assertTrue(KeyAction.ShowSymbols in actionsOf(KeyboardPage.MoreSymbols))
        assertTrue(KeyAction.ShowLetters in actionsOf(KeyboardPage.Symbols))
        assertTrue(KeyAction.ShowLetters in actionsOf(KeyboardPage.MoreSymbols))
    }
}
