package io.github.devjesser.composekeyboard.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            SoftKeyboardHost(
                theme = if (dark) KeyboardTheme.Dark else KeyboardTheme.Light,
                placement = if (overlay) KeyboardPlacement.Overlay else KeyboardPlacement.Push,
            ) {
                DemoForm(
                    dark = dark, onDark = { dark = it },
                    overlay = overlay, onOverlay = { overlay = it },
                )
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
