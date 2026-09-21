# ComposeKeyboard

An on-screen (soft) keyboard for **Compose Multiplatform Desktop**, written entirely in Compose.
Built for touch-only Windows terminals where there is no physical keyboard, as a replacement for
launching `TabTip.exe` / `osk.exe` through native code.

> **Status: 0.1.0-SNAPSHOT, work in progress.** The Desktop (JVM) target is the only one supported.
> It relies on Compose's experimental platform text-input APIs, see [How it works](#how-it-works)
> and [Limitations](#limitations).

## Features

- **Attach once, works for every text field.** Wrap your UI in `SoftKeyboardHost`. The keyboard slides
  in when a field gains focus and out when it loses focus. No per-field code.
- **Reads each field's `KeyboardOptions`**: number/phone/decimal fields get a numeric pad, everything else
  gets QWERTY; `KeyboardCapitalization` drives auto-shift; `ImeAction` labels and drives the Enter key.
- **Touch-friendly**: large keys, Shift with double-tap caps lock, press-and-hold Backspace repeat,
  letters / symbols / numeric pages, dark and light themes.
- **No native code, no `TabTip.exe` path dependency.** Pure Kotlin, so it behaves the same on any Windows
  version (and on macOS / Linux).
- **Foundation only.** No dependency on Material, so it fits whichever design system you use
  (the demo uses Material 3; `OutlinedTextField` from Material 2 works the same).
- **Push or overlay** placement, custom key layouts, custom theme.

## Requirements

| | |
|---|---|
| JDK | 17 or newer |
| Kotlin | 2.x (the Compose compiler Gradle plugin is required) |
| Compose Multiplatform | 1.12.x (targets 1.12.0) |
| Gradle | 9.x (wrapper included) |

## Installation

The artifact coordinates are `io.github.dev-jesser:composekeyboard`.

```kotlin
// build.gradle.kts of your Compose Desktop / KMP module
kotlin {
    sourceSets {
        jvmMain.dependencies {            // or `desktopMain` if that is what your target is called
            implementation("io.github.dev-jesser:composekeyboard:0.1.0-SNAPSHOT")
        }
    }
}
```

Until it is published, install it locally or to your own repository:

```bash
# Local machine (add mavenLocal() to your project's repositories)
./gradlew :composekeyboard:publishToMavenLocal

# Your internal Nexus / GitLab package registry: put this in ~/.gradle/gradle.properties
#   composekeyboard.publishUrl=https://nexus.example.com/repository/maven-releases/
#   composekeyboard.publishUser=...
#   composekeyboard.publishPassword=...
./gradlew :composekeyboard:publishAllPublicationsToInternalRepository
```

## Quick start

```kotlin
import io.github.devjesser.composekeyboard.SoftKeyboardHost

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        SoftKeyboardHost {
            MyApp()          // every text field in here gets the keyboard
        }
    }
}
```

That is all. Your existing fields keep working as they are:

```kotlin
OutlinedTextField(
    value = username,
    onValueChange = { username = it },
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),   // Enter key shows "Next"
)

OutlinedTextField(
    value = password,
    onValueChange = { password = it },
    visualTransformation = PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { signIn() }),        // runs when Enter/Done is pressed
)

OutlinedTextField(                                                    // names: capitalise each word
    value = name, onValueChange = { name = it },
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
)

OutlinedTextField(                                                    // dates / numbers: numeric pad
    value = date, onValueChange = { date = it },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
)
```

### Options

```kotlin
SoftKeyboardHost(
    enabled = true,                                   // false: never show it (e.g. a physical keyboard is attached)
    placement = KeyboardPlacement.Push,               // Push (content shrinks) or Overlay (floats over content)
    layouts = KeyboardLayoutSet.EnglishQwerty,        // bring your own pages/keys
    theme = KeyboardTheme.Dark,                       // KeyboardTheme.Light, or KeyboardTheme(colors, dimensions)
    dismissOnOutsideTap = true,                       // tapping non-text-field content unfocuses
) { /* content */ }
```

### When the keyboard hides

- The field loses focus (tap elsewhere, or focus moves to a non-text element).
- **Done / Go / Search / Send** is pressed (single-line fields treat the default action as Done).
- The **hide** key (chevron) is pressed.

Moving between fields (`Next`, or tapping another field) keeps the keyboard up and re-targets it.
In a **multi-line** field with no explicit `ImeAction`, Enter inserts a line break.

| `KeyboardType` | Page |
|---|---|
| `Number`, `Phone`, `Decimal`, `NumberPassword` | Numeric pad (digits, `.`, `/`, `-`) |
| everything else (`Text`, `Password`, `Email`, `Uri`, ...) | QWERTY (`@ _ - . ,` are on the main page; symbols via `?123`) |

Tip: `PasswordVisualTransformation` alone does not set the keyboard type. Add
`keyboardType = KeyboardType.Password` as shown above.

## Try it: the demo

```bash
./gradlew :demo:run
```

Opens a 1280x800 window (the target terminal size) with a login form, name, date and notes fields, and
switches for theme and placement. IDE previews for every keyboard page are in
[`demo/.../Previews.kt`](demo/src/main/kotlin/io/github/devjesser/composekeyboard/demo/Previews.kt)
(use the gutter icon next to `@Preview` in IntelliJ IDEA with the Kotlin Multiplatform plugin).

## How it works

Compose exposes `InterceptPlatformTextInput`, which lets a parent see every text-input session that a
text field opens when it gains focus, and forwards it to the platform as usual. `SoftKeyboardHost`
installs one interceptor and:

1. shows the keyboard while a session is active and hides it (after a short grace period so moving
   between fields does not flicker) when it ends;
2. reads the field's `ImeOptions` from the session request to choose the page, auto-capitalisation and
   Enter key;
3. types by sending the same edit commands a real input method would (`CommitTextCommand`,
   `BackspaceCommand`, and `onImeAction` for Enter), so it works with any `BasicTextField`-based field,
   including `TextField` / `OutlinedTextField`, with selections, cursor movement and password masking
   behaving normally.

Keys use raw pointer input instead of `clickable`, so pressing a key never steals focus from the field.

The keyboard UI (`SoftKeyboard`, `KeyboardState`, layouts, theme) lives in `commonMain` and does not depend
on any text field. Only the interception lives in `jvmMain`, because the request type is platform
specific.

## Limitations

- **Experimental API surface.** The members of `PlatformTextInputMethodRequest` on Desktop are marked
  `@ExperimentalComposeUiApi`; they may change between Compose Multiplatform releases. The library pins a
  tested version and the test suite is meant to catch breakage when you upgrade.
- **One window.** The keyboard lives inside the window that contains `SoftKeyboardHost`. Text fields inside
  a separate `Dialog` / `DialogWindow` / `Popup` window are not covered by the host of the main window; put a
  `SoftKeyboardHost` inside the dialog's content too.
- **Touch input reaches Compose as mouse events on Windows**, so multi-touch typing (two fingers at once) is
  not supported and keys trigger when the finger presses (as reported by the OS).
- English QWERTY only for now; the layout model is data (`KeyboardLayoutSet`) so adding one is easy.

## Migrating from a TabTip / `osk` wrapper

Delete the native DLL and the JNI/JNA glue, remove the calls that open and close the keyboard from your
field focus handlers, and wrap your root composable in `SoftKeyboardHost`.

## Development

```bash
./gradlew :composekeyboard:jvmTest      # unit tests + headless Compose UI tests
./gradlew :demo:run                     # demo app
./gradlew :composekeyboard:publishToMavenLocal
```

Project layout:

```
composekeyboard/           the library (Kotlin Multiplatform, jvm target)
  src/commonMain           keys, layouts, KeyboardState, theme, SoftKeyboard composable
  src/jvmMain              SoftKeyboardHost + adapter to Compose's text-input session
  src/commonTest, jvmTest  logic tests and end-to-end UI tests
demo/                      runnable Compose Desktop app + @Preview functions
```

Behind a corporate proxy: point `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` at your
internal Gradle distribution mirror, and add your Maven/plugin mirrors through an init script in
`~/.gradle/init.d/`. The build files use only `gradlePluginPortal()`, `google()` and `mavenCentral()`.

## License

Apache License 2.0, see [LICENSE](LICENSE).
