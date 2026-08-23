package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.tv.launcher.TvAppShortcut
import dev.typezero.couchlink.remote.tv.launcher.TvAppShortcutKind
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised2
import dev.typezero.couchlink.remote.ui.theme.SilverDim
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

@Composable
internal fun TvAppLauncherTray(
    shortcuts: List<TvAppShortcut>,
    enabled: Boolean,
    providerDisplayName: String,
    onLaunch: (TvAppShortcut) -> Unit,
    onAddCustomApp: (displayName: String, launchTarget: String) -> Unit,
    onRemove: (TvAppShortcut) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var shortcutToManage by remember { mutableStateOf<TvAppShortcut?>(null) }

    PremiumPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TV APPS",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.7.sp,
                )
                Text(
                    if (shortcuts.isEmpty()) {
                        "Build your own launcher tray"
                    } else {
                        "${shortcuts.size} saved • $providerDisplayName"
                    },
                    color = TextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                "+ ADD APP",
                color = Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        if (shortcuts.isEmpty()) {
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF151B20),
                                Color(0xFF080B0E),
                            ),
                        ),
                        shape,
                    )
                    .border(1.dp, SilverDim.copy(alpha = 0.45f), shape)
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Text(
                    "NO APPS ADDED",
                    color = TextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Add only the streaming services and custom TV apps you actually use.",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                shortcuts.forEach { shortcut ->
                    TvAppShortcutTile(
                        shortcut = shortcut,
                        enabled = enabled,
                        onClick = { onLaunch(shortcut) },
                        onManage = { shortcutToManage = shortcut },
                    )
                }

                AddAppTile(onClick = { showAddDialog = true })
            }
        }

        if (!enabled && shortcuts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect the TV Remote before launching apps.",
                color = Muted,
                fontSize = 10.sp,
            )
        }
    }

    if (showAddDialog) {
        AddCustomTvAppDialog(
            providerDisplayName = providerDisplayName,
            onDismiss = { showAddDialog = false },
            onSave = { name, target ->
                onAddCustomApp(name, target)
                showAddDialog = false
            },
        )
    }

    shortcutToManage?.let { shortcut ->
        ManageTvAppDialog(
            shortcut = shortcut,
            onDismiss = { shortcutToManage = null },
            onRemove = {
                onRemove(shortcut)
                shortcutToManage = null
            },
        )
    }
}

@Composable
private fun TvAppShortcutTile(
    shortcut: TvAppShortcut,
    enabled: Boolean,
    onClick: () -> Unit,
    onManage: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier = Modifier
            .width(118.dp)
            .height(82.dp)
            .shadow(
                8.dp,
                shape,
                ambientColor = Color.Black.copy(alpha = 0.70f),
                spotColor = Accent.copy(alpha = if (enabled) 0.12f else 0f),
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2128), Color(0xFF080B0E)),
                ),
                shape,
            )
            .border(
                1.dp,
                if (enabled) Accent.copy(alpha = 0.60f) else SilverDim.copy(alpha = 0.30f),
                shape,
            ),
    ) {
        Column(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Raised2, RoundedCornerShape(9.dp))
                    .border(0.7.dp, Accent.copy(alpha = 0.55f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    shortcut.displayName.take(1).uppercase(),
                    color = if (enabled) Accent else Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column {
                Text(
                    shortcut.displayName,
                    color = if (enabled) TextColor else Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    when (shortcut.kind) {
                        TvAppShortcutKind.KnownService -> "SERVICE"
                        TvAppShortcutKind.CustomApp -> "CUSTOM APP"
                    },
                    color = Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        Text(
            "⋮",
            color = Muted,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable(onClick = onManage)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AddAppTile(
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier = Modifier
            .width(92.dp)
            .height(82.dp)
            .background(Color(0xFF090D10), shape)
            .border(1.dp, Accent.copy(alpha = 0.42f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "+",
            color = Accent,
            fontSize = 25.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 25.sp,
        )
        Text(
            "ADD APP",
            color = Muted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.55.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddCustomTvAppDialog(
    providerDisplayName: String,
    onDismiss: () -> Unit,
    onSave: (displayName: String, launchTarget: String) -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var launchTarget by rememberSaveable { mutableStateOf("") }
    val canSave = displayName.trim().isNotEmpty() && launchTarget.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "ADD TV APP",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Add a custom app shortcut for $providerDisplayName. Known-service presets will appear as their direct launch targets are hardware-verified.",
                    fontSize = 12.sp,
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(80) },
                    label = { Text("App name") },
                    placeholder = { Text("Syncler") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = launchTarget,
                    onValueChange = { launchTarget = it.take(2048) },
                    label = { Text("Launch URI / app link") },
                    placeholder = { Text("scheme://... or https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "The launch target is sent through the TV provider's app-launch transport. CouchLink does not use ADB on the TV.",
                    color = Muted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(displayName.trim(), launchTarget.trim())
                },
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
    )
}

@Composable
private fun ManageTvAppDialog(
    shortcut: TvAppShortcut,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                shortcut.displayName,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Manage this TV app shortcut.")
                Text(
                    shortcut.launchTarget,
                    color = Muted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRemove) {
                Text("REMOVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
    )
}
