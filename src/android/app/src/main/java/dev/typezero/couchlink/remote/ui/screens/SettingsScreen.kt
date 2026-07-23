package dev.typezero.couchlink.remote.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.BuildConfig
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.hid.BluetoothHidController
import dev.typezero.couchlink.remote.model.LauncherHostState
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Danger
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised2
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

@Composable
internal fun SettingsScreen(
    hidState: BluetoothHidController.State,
    launcherHostState: LauncherHostState,
    hapticsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    naturalScrolling: Boolean,
    onNaturalScrollingChanged: (Boolean) -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onRefreshBluetoothHosts: () -> Unit,
    onConnectBluetoothHost: (String) -> Boolean,
    onDisconnectBluetoothHost: () -> Boolean,
    onReconnectLauncherHost: () -> Unit,
    onPairLauncherHost: () -> Boolean,
    onForgetLauncherHost: () -> Boolean,
) {
    val context = LocalContext.current
    var diagnosticsCopied by rememberSaveable { mutableStateOf(false) }
    val buildChannel = if (BuildConfig.DEBUG) "Debug" else "Release"

    val diagnostics = remember(hidState, launcherHostState, buildChannel) {
        buildString {
            appendLine("CouchLink Remote")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Build channel: $buildChannel")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Bluetooth HID supported: ${hidState.supported}")
            appendLine("Bluetooth permission granted: ${hidState.permissionGranted}")
            appendLine("Bluetooth enabled: ${hidState.bluetoothEnabled}")
            appendLine("Bluetooth HID registered: ${hidState.registered}")
            appendLine("Bluetooth HID connected: ${hidState.connected}")
            appendLine("Bluetooth HID device: ${hidState.connectedHost?.name ?: "None"}")
            append("Bluetooth status: ${hidState.message}")
        }
    }

    Text(
        text = "Living-room settings",
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )

    BluetoothHidPanel(
        state = hidState,
        onRequestPermission = onRequestBluetoothPermission,
        onMakeDiscoverable = onMakeDiscoverable,
        onRefreshHosts = onRefreshBluetoothHosts,
        onConnectHost = onConnectBluetoothHost,
        onDisconnect = onDisconnectBluetoothHost,
    )


    WindowsLauncherHostPanel(
        state = launcherHostState,
        onReconnect = onReconnectLauncherHost,
        onPair = onPairLauncherHost,
        onForget = onForgetLauncherHost,
    )

    PremiumPanel {
        PreferenceSwitchRow(
            title = "Premium haptics",
            description = "Tactile confirmation for controls.",
            checked = hapticsEnabled,
            onCheckedChange = onHapticsChanged,
        )
        PreferenceSwitchRow(
            title = "Natural scrolling",
            description = "Match modern touchpad direction.",
            checked = naturalScrolling,
            onCheckedChange = onNaturalScrollingChanged,
        )
    }

    PremiumPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.couchlink_logo),
                contentDescription = "CouchLink",
                modifier = Modifier.size(58.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Row {
                    Text(
                        text = "Couch",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Link",
                        color = Accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Remote ${BuildConfig.VERSION_NAME}",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }

        Text(
            text = "ABOUT",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "A local-first Bluetooth keyboard, mouse, and launcher remote for Windows living-room PCs.",
            color = TextColor,
            fontSize = 14.sp,
        )
        HorizontalDivider(color = Raised2)
        AboutDetailRow("Build", "$buildChannel · ${BuildConfig.VERSION_CODE}")
        AboutDetailRow("Input", "Bluetooth HID")
        AboutDetailRow("Privacy", "No cloud account or telemetry")

        Button(
            onClick = {
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                    ClipData.newPlainText("CouchLink diagnostics", diagnostics),
                )
                diagnosticsCopied = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Raised2,
                contentColor = TextColor,
            ),
        ) {
            Text(
                text = if (diagnosticsCopied) {
                    "DIAGNOSTICS COPIED"
                } else {
                    "COPY DIAGNOSTICS"
                },
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "Keyboard, mouse, touchpad, shortcuts, and Windows sign-in use Bluetooth HID. " +
                "The optional Windows Launcher Host handles launcher control and status over the local network.",
            color = Muted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun WindowsLauncherHostPanel(
    state: LauncherHostState,
    onReconnect: () -> Unit,
    onPair: () -> Boolean,
    onForget: () -> Boolean,
) {
    PremiumPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Windows Launcher Host",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Launches, focuses, closes, and reports launcher state over your local network.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = when {
                    state.connected -> "CONNECTED"
                    state.connecting -> "CONNECTING"
                    state.discovered && !state.trusted -> "PAIR"
                    state.discovered -> "FOUND"
                    else -> "OFFLINE"
                },
                color = if (state.connected) Success else Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = state.message,
            color = Muted,
            fontSize = 12.sp,
        )

        AboutDetailRow("PC", state.hostName.ifBlank { "Not discovered" })
        AboutDetailRow(
            "Endpoint",
            if (state.hostAddress.isBlank()) "Not available" else "${state.hostAddress}:${state.hostPort}",
        )
        AboutDetailRow("Host version", state.hostVersion.ifBlank { "Unknown" })
        AboutDetailRow("Launcher fallback", "Bluetooth HID when host is offline")

        if (state.discovered && !state.trusted && !state.connected && !state.connecting) {
            Button(
                onClick = { onPair() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text(
                    text = "PAIR WITH ${state.hostName.ifBlank { "HOST" }.uppercase()}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onReconnect,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.connected) "RECONNECT" else "FIND HOST")
            }
            OutlinedButton(
                onClick = { onForget() },
                enabled = state.hostId.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("FORGET HOST")
            }
        }

        Text(
            text = "Bluetooth HID remains the input route even when the launcher host is disconnected.",
            color = Muted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = Muted,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AboutDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Muted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = TextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun BluetoothHidPanel(
    state: BluetoothHidController.State,
    onRequestPermission: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onRefreshHosts: () -> Unit,
    onConnectHost: (String) -> Boolean,
    onDisconnect: () -> Boolean,
) {
    var pairedDevicesExpanded by rememberSaveable { mutableStateOf(false) }

    PremiumPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Bluetooth secure input",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Android presents itself as a real keyboard and mouse for Windows sign-in.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = when {
                    state.connected -> "CONNECTED"
                    state.connecting -> "CONNECTING"
                    state.registered -> "READY"
                    else -> "OFF"
                },
                color = if (state.connected) Success else Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = state.message,
            color = Muted,
            fontSize = 12.sp,
        )

        if (!state.supported) {
            Text(
                text = "Android 9 or newer is required.",
                color = Danger,
                fontSize = 12.sp,
            )
            return@PremiumPanel
        }

        if (!state.permissionGranted) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text(
                    text = "ALLOW BLUETOOTH HID",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
            return@PremiumPanel
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onMakeDiscoverable,
                modifier = Modifier.weight(1f),
            ) {
                Text("PAIR WINDOWS")
            }
            OutlinedButton(
                onClick = onRefreshHosts,
                modifier = Modifier.weight(1f),
            ) {
                Text("REFRESH")
            }
        }

        if (state.connected) {
            Text(
                text = "Input route: Bluetooth HID → ${state.connectedHost?.name ?: "Windows PC"}",
                color = Success,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(
                onClick = { onDisconnect() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("DISCONNECT BLUETOOTH HID")
            }
        } else if (state.pairedHosts.isNotEmpty()) {
            TextButton(
                onClick = { pairedDevicesExpanded = !pairedDevicesExpanded },
            ) {
                Text(
                    text = if (pairedDevicesExpanded) {
                        "HIDE PAIRED DEVICES"
                    } else {
                        "CHOOSE PAIRED DEVICE"
                    },
                    color = Accent,
                )
            }

            if (pairedDevicesExpanded) {
                state.pairedHosts.forEach { pairedHost ->
                    OutlinedButton(
                        onClick = { onConnectHost(pairedHost.address) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(pairedHost.name)
                            Text(
                                text = pairedHost.address,
                                color = Muted,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Use PAIR WINDOWS, then add “CouchLink Remote” from Windows Bluetooth settings.",
                color = Muted,
                fontSize = 11.sp,
            )
        }
    }
}
