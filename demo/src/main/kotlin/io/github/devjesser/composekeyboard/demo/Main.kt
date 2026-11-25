package io.github.devjesser.composekeyboard.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.devjesser.composekeyboard.KeyboardPlacement
import io.github.devjesser.composekeyboard.KeyboardTheme
import io.github.devjesser.composekeyboard.LocalSoftKeyboard
import io.github.devjesser.composekeyboard.SoftKeyboardHost

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ComposeKeyboard demo",
        // The size of the target touch terminals.
        state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
    ) {
        DemoApp()
    }
}

@Composable
fun DemoApp() {
    var dark by remember { mutableStateOf(true) }
    var overlay by remember { mutableStateOf(false) }
    var sizeByFraction by remember { mutableStateOf(true) }
    var fraction by remember { mutableStateOf(0.5f) }
    var showLogin by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            SoftKeyboardHost(
                theme = if (dark) KeyboardTheme.Dark else KeyboardTheme.Light,
                placement = if (overlay) KeyboardPlacement.Overlay else KeyboardPlacement.Push,
                // 0.5 is about what the Windows TabTip takes on a 1280x800 screen.
                heightFraction = if (sizeByFraction) fraction else null,
            ) {
                if (showLogin) {
                    LoginDemo(onBack = { showLogin = false })
                } else {
                    DemoForm(
                        dark = dark, onDark = { dark = it },
                        overlay = overlay, onOverlay = { overlay = it },
                        sizeByFraction = sizeByFraction, onSizeByFraction = { sizeByFraction = it },
                        fraction = fraction, onFraction = { fraction = it },
                        onOpenLogin = { showLogin = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoForm(
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    overlay: Boolean,
    onOverlay: (Boolean) -> Unit,
    sizeByFraction: Boolean,
    onSizeByFraction: (Boolean) -> Unit,
    fraction: Float,
    onFraction: (Float) -> Unit,
    onOpenLogin: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tap a field to bring up the keyboard.") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ComposeKeyboard demo", style = MaterialTheme.typography.headlineSmall)
        Text(status)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Dark keyboard"); Switch(dark, onDark)
            Text("Overlay instead of push"); Switch(overlay, onOverlay)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Size as % of window"); Switch(sizeByFraction, onSizeByFraction)
            Slider(
                value = fraction, onValueChange = onFraction, valueRange = 0.3f..0.7f,
                enabled = sizeByFraction, modifier = Modifier.width(240.dp),
            )
            Text(if (sizeByFraction) "${(fraction * 100).toInt()}%" else "fixed key height")
        }
        Button(onClick = onOpenLogin) { Text("Adaptive login demo (reacts to the keyboard)") }

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username") }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { status = "Signed in as \"$username\" (Done pressed)" }),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it },
            label = { Text("Full name (auto-capitalises words)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = date, onValueChange = { date = it },
            label = { Text("Date (numeric pad)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("Notes (multi-line: Enter inserts a new line)") },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Below this much room above the keyboard, the login form switches to its compact layout. */
private val LoginCompactBelow = 400.dp

/**
 * A login screen that reads [LocalSoftKeyboard] to pick its layout: stacked fields when there is
 * plenty of room above the keyboard, username and password side by side when there is not.
 *
 * The choice uses the host height minus the keyboard's expected height, both known before the keyboard
 * appears and neither changing while it slides, so the layout never flips mid-animation. Do not base it
 * on this content's own height: with KeyboardPlacement.Push that shrinks while the keyboard slides in.
 */
@Composable
private fun LoginDemo(onBack: () -> Unit) {
    val keyboard = LocalSoftKeyboard.current
    val spaceAbove = keyboard.hostHeight - keyboard.expectedHeight
    // hostHeight is 0 for the very first frame, until the host has been measured.
    val compact = keyboard.hostHeight > 0.dp && spaceAbove < LoginCompactBelow

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val signIn = { status = "Signed in as \"$username\"" }

    // verticalScroll: Compose scrolls a focused field into view above the keyboard by itself.
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back to the form") }
        Text("Sign in", style = MaterialTheme.typography.headlineSmall)
        Text(
            (if (compact) "Compact layout" else "Stacked layout") +
                ": ${spaceAbove.value.toInt()}dp above the keyboard (compact below ${LoginCompactBelow.value.toInt()}dp)",
        )

        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UsernameField(username, { username = it }, Modifier.weight(1f))
                PasswordField(password, { password = it }, signIn, Modifier.weight(1f))
            }
        } else {
            UsernameField(username, { username = it }, Modifier.fillMaxWidth())
            PasswordField(password, { password = it }, signIn, Modifier.fillMaxWidth())
        }
        Button(onClick = signIn) { Text("Sign in") }
        if (status.isNotEmpty()) Text(status)
    }
}

@Composable
private fun UsernameField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text("Username") }, singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text("Password") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
}
