package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.hid.RemoteKey
import dev.typezero.couchlink.remote.hid.WindowsShortcut
import dev.typezero.couchlink.remote.ui.components.ActionButton
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised2

private data class KeySpec(
    val label: String,
    val key: RemoteKey,
)

private data class ShortcutSpec(
    val label: String,
    val shortcut: WindowsShortcut,
)

private val keyRows = listOf(
    listOf(
        KeySpec("ESC", RemoteKey.Escape),
        KeySpec("TAB", RemoteKey.Tab),
        KeySpec("⌫", RemoteKey.Backspace),
        KeySpec("ENTER", RemoteKey.Enter),
    ),
    listOf(
        KeySpec("←", RemoteKey.Left),
        KeySpec("↑", RemoteKey.Up),
        KeySpec("↓", RemoteKey.Down),
        KeySpec("→", RemoteKey.Right),
    ),
)

private val windowsShortcuts = listOf(
    ShortcutSpec("ALT+TAB", WindowsShortcut.AltTab),
    ShortcutSpec("DESKTOP", WindowsShortcut.ShowDesktop),
    ShortcutSpec("TASK MGR", WindowsShortcut.TaskManager),
)

@Composable
internal fun KeyboardScreen(
    enabled: Boolean,
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onKey: (RemoteKey) -> Unit,
    onShortcut: (WindowsShortcut) -> Unit,
) {
    PremiumPanel {
        Text(
            text = "Keyboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Send text or use the essential Windows keys below.",
            color = Muted,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { Text("Type text to send") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Button(
            onClick = onSend,
            enabled = enabled && text.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = Raised2,
                disabledContentColor = Muted,
            ),
        ) {
            Text(
                text = "SEND TEXT",
                color = if (enabled && text.isNotEmpty()) Color.Black else Muted,
                fontWeight = FontWeight.Bold,
            )
        }

        KeyRows(
            enabled = enabled,
            onKey = onKey,
        )

        Text(
            text = "Windows shortcuts",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            windowsShortcuts.forEach { shortcut ->
                ActionButton(
                    label = shortcut.label,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    onShortcut(shortcut.shortcut)
                }
            }
        }
    }
}

@Composable
private fun KeyRows(
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
) {
    keyRows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            row.forEach { key ->
                ActionButton(
                    label = key.label,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    onKey(key.key)
                }
            }
        }
    }
}
