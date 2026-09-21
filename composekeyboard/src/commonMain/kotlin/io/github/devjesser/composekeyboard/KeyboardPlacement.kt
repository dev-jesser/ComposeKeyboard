package io.github.devjesser.composekeyboard

/** How the keyboard is laid out relative to your content. */
enum class KeyboardPlacement {
    /** The content area shrinks to make room, so a focused field is never hidden behind the keys. */
    Push,

    /** The keyboard floats over the bottom of the content. */
    Overlay,
}
