package io.github.devjesser.composekeyboard

import androidx.compose.runtime.Immutable

/** What a non-character key does when pressed. */
enum class KeyAction {
    Shift,
    Backspace,
    Enter,
    Space,
    ShowLetters,
    ShowSymbols,
    Dismiss,
}

/** A single cell in a keyboard row. [weight] is the key's relative width within its row. */
sealed interface SoftKey {
    val weight: Float

    /** Stable identifier, also used as the UI test tag (`key:<id>`). */
    val id: String

    /** A key that types text. [upper] is used while Shift is active on the letters page. */
    data class Character(
        val lower: String,
        val upper: String = lower.uppercase(),
        override val weight: Float = 1f,
    ) : SoftKey {
        override val id: String get() = lower
    }

    /** A key that performs an [action]. Repeatable keys (Backspace) repeat while held. */
    data class Action(
        val action: KeyAction,
        override val weight: Float = 1f,
        val repeatable: Boolean = false,
    ) : SoftKey {
        override val id: String get() = action.name
    }

    /** Empty space, used to stagger rows. */
    data class Gap(override val weight: Float) : SoftKey {
        override val id: String get() = "gap"
    }
}

/**
 * One page of keys. All rows should add up to the same total weight so the columns line up
 * (a unit test guards this for the built-in layouts).
 *
 * @param widthFraction fraction of the available width the page occupies (the numeric pad is narrower).
 */
@Immutable
class KeyboardLayout(
    val rows: List<List<SoftKey>>,
    val widthFraction: Float = 1f,
)

enum class KeyboardPage { Letters, Symbols, Numeric }

/** The pages that make up a keyboard. Provide your own to change keys or add a language. */
@Immutable
class KeyboardLayoutSet(
    val letters: KeyboardLayout,
    val symbols: KeyboardLayout,
    val numeric: KeyboardLayout,
) {
    fun layoutFor(page: KeyboardPage): KeyboardLayout = when (page) {
        KeyboardPage.Letters -> letters
        KeyboardPage.Symbols -> symbols
        KeyboardPage.Numeric -> numeric
    }

    companion object {
        /** US English QWERTY with a number row, symbols page and numeric pad. */
        val EnglishQwerty: KeyboardLayoutSet = KeyboardLayoutSet(
            letters = KeyboardLayout(
                rows = listOf(
                    chars("1234567890") + SoftKey.Action(KeyAction.Backspace, 1.5f, repeatable = true),
                    chars("qwertyuiop") + SoftKey.Character("@", weight = 0.75f) + SoftKey.Character("_", weight = 0.75f),
                    listOf<SoftKey>(SoftKey.Gap(0.5f)) + chars("asdfghjkl") + SoftKey.Action(KeyAction.Enter, 2f),
                    listOf<SoftKey>(SoftKey.Action(KeyAction.Shift, 1.5f)) + chars("zxcvbnm") + chars(",.-"),
                    listOf(
                        SoftKey.Action(KeyAction.ShowSymbols, 1.5f),
                        SoftKey.Action(KeyAction.Space, 8.5f),
                        SoftKey.Action(KeyAction.Dismiss, 1.5f),
                    ),
                ),
            ),
            // Symbols on the left, a numpad on the right, always together in one page — the
            // layout of the Windows TabTip's "&123" page. Each row's keys still add up to the
            // same total weight (7 symbol columns + 3 numpad columns + 1.5 for the right-hand
            // action key = 11.5), so it lines up like every other page; no separate "block"
            // concept was needed for this.
            symbols = KeyboardLayout(
                rows = listOf(
                    chars("!@#\$%&*") + chars("123") + SoftKey.Action(KeyAction.Backspace, 1.5f, repeatable = true),
                    chars("()-_=+~") + chars("456") + SoftKey.Gap(1.5f),
                    chars("\\;:\"'/^") + chars("789") + SoftKey.Action(KeyAction.Enter, 1.5f),
                    listOf(SoftKey.Action(KeyAction.ShowLetters, 1f), SoftKey.Action(KeyAction.Space, 6f)) +
                        listOf(SoftKey.Character("0", weight = 2f), SoftKey.Character(".")) +
                        SoftKey.Action(KeyAction.Dismiss, 1.5f),
                    ),
                ),
            ),
            numeric = KeyboardLayout(
                rows = listOf(
                    chars("123") + SoftKey.Action(KeyAction.Backspace, repeatable = true),
                    chars("456") + SoftKey.Action(KeyAction.Enter),
                    chars("789") + SoftKey.Action(KeyAction.Dismiss),
                    chars("0./-"),
                ),
                widthFraction = 0.42f,
            ),
        )

        private fun chars(text: String, weight: Float = 1f): List<SoftKey> =
            text.map { SoftKey.Character(it.toString(), weight = weight) }
    }
}
