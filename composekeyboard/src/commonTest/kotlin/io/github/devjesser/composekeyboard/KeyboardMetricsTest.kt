package io.github.devjesser.composekeyboard

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardMetricsTest {
    private val dimensions = KeyboardDimensions()
    private val layouts = KeyboardLayoutSet.EnglishQwerty

    @Test
    fun lettersPageIsFiveKeyRowsPlusPadding() {
        // 5 rows x 56dp + 2 x (8dp outer padding - 3dp half spacing)
        assertEquals(290f, layouts.letters.naturalHeight(dimensions).value, 0.01f)
    }

    @Test
    fun fourRowPagesAreOneRowShorter() {
        assertEquals(234f, layouts.symbols.naturalHeight(dimensions).value, 0.01f)
        assertEquals(234f, layouts.numeric.naturalHeight(dimensions).value, 0.01f)
    }

    @Test
    fun tallestPageIsTheLettersPage() {
        assertEquals(290f, layouts.tallestNaturalHeight(dimensions).value, 0.01f)
    }
}
