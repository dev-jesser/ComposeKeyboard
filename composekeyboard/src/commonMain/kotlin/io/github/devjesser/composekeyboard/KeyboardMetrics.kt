package io.github.devjesser.composekeyboard

import androidx.compose.ui.unit.Dp

/**
 * Height of this page when every key is [KeyboardDimensions.keyHeight] tall, exactly as
 * [SoftKeyboard] lays it out: the rows plus the padding around them.
 */
internal fun KeyboardLayout.naturalHeight(dimensions: KeyboardDimensions): Dp =
    dimensions.keyHeight * rows.size + (dimensions.outerPadding - dimensions.keySpacing / 2) * 2

/** The tallest page of the set; the letters page for the built-in layouts. */
internal fun KeyboardLayoutSet.tallestNaturalHeight(dimensions: KeyboardDimensions): Dp =
    maxOf(letters.naturalHeight(dimensions), symbols.naturalHeight(dimensions), numeric.naturalHeight(dimensions))
