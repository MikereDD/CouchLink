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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import dev.typezero.couchlink.remote.tv.provider.TvProviderDescriptor
import dev.typezero.couchlink.remote.tv.provider.TvProviderDevice
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.tv.provider.TvProviderState
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Danger
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised2
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor
import dev.typezero.couchlink.remote.update.CouchLinkUpdateManager

@Composable
internal fun SettingsScreen(
    hidState: BluetoothHidController.State,
    launcherHostState: LauncherHostState,
    tvState: TvProviderState,
    tvProviders: List<TvProviderDescriptor>,
    activeTvProviderId: TvProviderId,
    onTvProviderSelected: (TvProviderId) -> Boolean,
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
    onTvScan: () -> Unit,
    onTvStopScan: () -> Unit,
    onTvSelect: (TvProviderDevice) -> Unit,
    onTvSelectManual: (String) -> Unit,
    onTvProbe: () -> Unit,
    onTvBeginPairing: () -> Unit,
    onTvFinishPairing: (String) -> Unit,
    onTvCancelPairing: () -> Unit,
    onTvConnect: () -> Unit,
    onTvForget: () -> Unit,
    updateState: CouchLinkUpdateManager.State,
    onCheckForUpdates: () -> Unit,
    onUpdateChannelChanged: (Boolean) -> Unit,
    onDownloadUpdate: () -> Unit,
    onContinueInstall: () -> Unit,
    onOpenInstallPermission: () -> Unit,
) {
    val context = LocalContext.current
    var diagnosticsCopied by rememberSaveable { mutableStateOf(false) }
    val buildChannel = if (BuildConfig.DEBUG) "Debug" else "Release"
    val activeProviderName = tvProviders.firstOrNull { it.id == activeTvProviderId }?.displayName ?: activeTvProviderId.name

    val diagnostics = remember(hidState, launcherHostState, tvState, activeProviderName, buildChannel) {
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
            appendLine("Bluetooth status: ${hidState.message}")
            appendLine("Windows Host trusted: ${launcherHostState.trusted}")
            appendLine("Windows Host connected: ${launcherHostState.connected}")
            appendLine("Windows Host PC: ${launcherHostState.hostName.ifBlank { "None" }}")
            appendLine("Windows Host endpoint: ${launcherHostState.hostAddress.ifBlank { "None" }}:${launcherHostState.hostPort}")
            appendLine("Windows Host wake MAC: ${launcherHostState.wakeMacAddress.ifBlank { "Unavailable" }}")
            appendLine("Windows Host waking: ${launcherHostState.waking}")
            appendLine("Windows Host status: ${launcherHostState.message}")
            appendLine("TV provider: $activeProviderName")
            appendLine("TV selected: ${tvState.selectedDevice?.name ?: "None"}")
            appendLine("TV endpoint: ${tvState.selectedDevice?.host ?: "None"}")
            appendLine("TV paired: ${tvState.pairing.paired}")
            appendLine("TV connected: ${tvState.connection.ready}")
            appendLine("TV wake MAC: ${tvState.wakeMacAddress ?: "Unavailable"}")
            append("TV status: ${tvState.message}")
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

    TvProviderSelectorPanel(
        providers = tvProviders,
        activeProviderId = activeTvProviderId,
        onSelect = onTvProviderSelected,
    )

    TvRemoteSettingsPanel(
        state = tvState,
        providerName = activeProviderName,
        onScan = onTvScan,
        onStopScan = onTvStopScan,
        onSelect = onTvSelect,
        onSelectManual = onTvSelectManual,
        onProbe = onTvProbe,
        onBeginPairing = onTvBeginPairing,
        onFinishPairing = onTvFinishPairing,
        onCancelPairing = onTvCancelPairing,
        onConnect = onTvConnect,
        onForget = onTvForget,
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

    UpdateSettingsPanel(
        state = updateState,
        onCheck = onCheckForUpdates,
        onChannelChanged = onUpdateChannelChanged,
        onDownload = onDownloadUpdate,
        onContinueInstall = onContinueInstall,
        onOpenInstallPermission = onOpenInstallPermission,
    )

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
                    Text("Couch", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("Link", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Remote ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 12.sp)
            }
        }

        Text("ABOUT", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            "A local-first Bluetooth keyboard, mouse, launcher, and TV remote for living-room control.",
            color = TextColor,
            fontSize = 14.sp,
        )
        HorizontalDivider(color = Raised2)
        AboutDetailRow("Build", "$buildChannel · ${BuildConfig.VERSION_CODE}")
        AboutDetailRow("Input", "Bluetooth HID")
        AboutDetailRow("TV platform", activeProviderName)
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
                if (diagnosticsCopied) "DIAGNOSTICS COPIED" else "COPY DIAGNOSTICS",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TvProviderSelectorPanel(
    providers: List<TvProviderDescriptor>,
    activeProviderId: TvProviderId,
    onSelect: (TvProviderId) -> Boolean,
) {
    PremiumPanel {
        Text("TV PLATFORM", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            "Choose the TV operating system CouchLink should control.",
            color = Muted,
            fontSize = 12.sp,
        )

        providers.forEach { provider ->
            val active = provider.id == activeProviderId
            OutlinedButton(
                onClick = { onSelect(provider.id) },
                enabled = provider.implemented && !active,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        provider.displayName,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    )
                    Text(
                        when {
                            active -> "ACTIVE"
                            provider.implemented -> "AVAILABLE"
                            else -> "COMING LATER"
                        },
                        color = when {
                            active -> Success
                            provider.implemented -> Accent
                            else -> Muted
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvRemoteSettingsPanel(
    state: TvProviderState,
    providerName: String,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelect: (TvProviderDevice) -> Unit,
    onSelectManual: (String) -> Unit,
    onProbe: () -> Unit,
    onBeginPairing: () -> Unit,
    onFinishPairing: (String) -> Unit,
    onCancelPairing: () -> Unit,
    onConnect: () -> Unit,
    onForget: () -> Unit,
) {
    var manualHost by rememberSaveable { mutableStateOf(state.selectedDevice?.host.orEmpty()) }
    var pairingCode by rememberSaveable { mutableStateOf("") }

    PremiumPanel {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TV Remote", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    state.selectedDevice?.let { "${it.name} • ${it.host}" } ?: "No $providerName device selected",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                when {
                    state.connection.ready -> "CONNECTED"
                    state.connection.connecting -> "CONNECTING"
                    state.pairing.paired -> "PAIRED"
                    else -> "NOT PAIRED"
                },
                color = if (state.connection.ready) Success else Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(state.message, color = Muted, fontSize = 12.sp)
        state.wakeMacAddress?.let { AboutDetailRow("Wake-on-LAN", it) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = if (state.scanning) onStopScan else onScan,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.scanning) "STOP SCAN" else "SCAN")
            }
            OutlinedButton(
                onClick = onProbe,
                enabled = state.selectedDevice != null && !state.probing,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.probing) "TESTING" else "DIAGNOSTICS")
            }
        }

        state.serviceProbes.forEach { probe ->
            val label = if (probe.endpoint.isNullOrBlank()) probe.label else "${probe.label} (${probe.endpoint})"
            AboutDetailRow(label, if (probe.reachable) "Reachable" else "No response")
        }

        if (state.devices.isNotEmpty()) {
            Text("DISCOVERED TVS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            state.devices.forEach { device ->
                OutlinedButton(onClick = { onSelect(device) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.selectedDevice?.host == device.host) {
                            "✓ ${device.name} • ${device.host}"
                        } else {
                            "${device.name} • ${device.host}"
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = manualHost,
            onValueChange = { manualHost = it },
            label = { Text("Manual TV address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { onSelectManual(manualHost) },
            enabled = manualHost.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("USE MANUAL ADDRESS")
        }

        when {
            state.pairing.awaitingCode -> {
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { value ->
                        pairingCode = value.uppercase()
                            .filter { it.isDigit() || it in 'A'..'F' }
                            .take(6)
                    },
                    label = { Text("Code shown on TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onFinishPairing(pairingCode) },
                        enabled = pairingCode.length == 6,
                        modifier = Modifier.weight(1f),
                    ) { Text("PAIR") }
                    OutlinedButton(onClick = onCancelPairing, modifier = Modifier.weight(1f)) {
                        Text("CANCEL")
                    }
                }
            }

            !state.pairing.paired -> Button(
                onClick = onBeginPairing,
                enabled = state.selectedDevice != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("PAIR WITH TV", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            else -> {
                OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("RECONNECT TV REMOTE")
                }
                OutlinedButton(onClick = onForget, modifier = Modifier.fillMaxWidth()) {
                    Text("FORGET TV")
                }
            }
        }
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Windows Launcher Host", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Launches, focuses, closes, and reports launcher state over your local network.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                when {
                    state.connected -> "CONNECTED"
                    state.waking -> "WAKING"
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

        Text(state.message, color = Muted, fontSize = 12.sp)
        AboutDetailRow("PC", state.hostName.ifBlank { "Not discovered" })
        AboutDetailRow(
            "Endpoint",
            if (state.hostAddress.isBlank()) "Not available" else "${state.hostAddress}:${state.hostPort}",
        )
        AboutDetailRow("Host version", state.hostVersion.ifBlank { "Unknown" })
        AboutDetailRow(
            "Wake-on-LAN",
            state.wakeMacAddress.ifBlank { "Unavailable until a trusted Host advertises Ethernet identity" },
        )
        AboutDetailRow("Launcher fallback", "Bluetooth HID when host is offline")

        if (state.discovered && !state.trusted && !state.connected && !state.connecting) {
            Button(
                onClick = { onPair() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text(
                    "PAIR WITH ${state.hostName.ifBlank { "HOST" }.uppercase()}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReconnect, modifier = Modifier.weight(1f)) {
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bluetooth secure input", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Android presents itself as a real keyboard and mouse for Windows sign-in.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                when {
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

        Text(state.message, color = Muted, fontSize = 12.sp)

        if (!state.supported) {
            Text("Android 9 or newer is required.", color = Danger, fontSize = 12.sp)
            return@PremiumPanel
        }

        if (!state.permissionGranted) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("ALLOW BLUETOOTH HID", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            return@PremiumPanel
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMakeDiscoverable, modifier = Modifier.weight(1f)) {
                Text("PAIR WINDOWS")
            }
            OutlinedButton(onClick = onRefreshHosts, modifier = Modifier.weight(1f)) {
                Text("REFRESH")
            }
        }

        if (state.connected) {
            Text(
                "Input route: Bluetooth HID → ${state.connectedHost?.name ?: "Windows PC"}",
                color = Success,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = { onDisconnect() }, modifier = Modifier.fillMaxWidth()) {
                Text("DISCONNECT BLUETOOTH HID")
            }
        } else if (state.pairedHosts.isNotEmpty()) {
            TextButton(onClick = { pairedDevicesExpanded = !pairedDevicesExpanded }) {
                Text(
                    if (pairedDevicesExpanded) "HIDE PAIRED DEVICES" else "CHOOSE PAIRED DEVICE",
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
                            Text(pairedHost.address, color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            Text(
                "Use PAIR WINDOWS, then add “CouchLink Remote” from Windows Bluetooth settings.",
                color = Muted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AboutDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = TextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun UpdateSettingsPanel(
    state: CouchLinkUpdateManager.State,
    onCheck: () -> Unit,
    onChannelChanged: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onContinueInstall: () -> Unit,
    onOpenInstallPermission: () -> Unit,
) {
    val updatePanelContext = LocalContext.current

    PremiumPanel {
        Text("APP UPDATES", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            state.message,
            color = if (state.updateAvailable) Accent else TextColor,
            fontSize = 14.sp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TEST BUILDS", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "Opt in to selected prereleases for volunteer testing.",
                    color = Muted,
                    fontSize = 10.sp,
                )
            }
            Switch(
                checked = state.testChannel,
                onCheckedChange = onChannelChanged,
                enabled = !state.checking && !state.downloading,
            )
        }

        if (state.availableVersion != null) {
            AboutDetailRow("Installed", BuildConfig.VERSION_NAME)
            AboutDetailRow("Available", state.availableVersion)
        }

        Text(state.stage.uppercase(), color = Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)

        if (state.progressPercent != null) {
            LinearProgressIndicator(
                progress = { state.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "DOWNLOAD ${state.progressPercent}%",
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }

        if (state.releaseNotes.isNotBlank() && state.updateAvailable) {
            Text(state.releaseNotes.take(700), color = Muted, fontSize = 11.sp)
        }

        if (state.installPermissionRequired) {
            Button(onClick = onOpenInstallPermission, modifier = Modifier.fillMaxWidth()) {
                Text("ALLOW UPDATE INSTALLS", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onContinueInstall, modifier = Modifier.fillMaxWidth()) {
                Text("INSTALL UPDATE", fontWeight = FontWeight.Bold)
            }
        } else if (state.updateAvailable) {
            Button(
                onClick = onDownload,
                enabled = !state.downloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.downloading) "DOWNLOADING…" else "DOWNLOAD AND INSTALL",
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (state.testChannel) {
            OutlinedButton(
                onClick = { onChannelChanged(false) },
                enabled = !state.checking && !state.downloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("RETURN TO STABLE", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = {
                val report = buildString {
                    appendLine("CouchLink Updater Test Report")
                    appendLine("Remote version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Channel: ${if (state.testChannel) "Test" else "Stable"}")
                    appendLine("Available: ${state.availableVersion ?: "None"}")
                    appendLine("Stage: ${state.stage}")
                    appendLine("Status: ${state.message}")
                    appendLine("Progress: ${state.progressPercent ?: 0}%")
                }
                updatePanelContext.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("CouchLink updater report", report))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("COPY TEST REPORT", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onCheck,
            enabled = !state.checking && !state.downloading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.checking) "CHECKING…" else "CHECK FOR UPDATES", fontWeight = FontWeight.Bold)
        }

        Text(
            if (state.testChannel) {
                "Test channel selected. Prereleases still require official assets, GitHub SHA-256, and APK certificate verification."
            } else {
                "Stable channel selected. Downloads require official assets, GitHub SHA-256, and APK certificate verification."
            },
            color = Muted,
            fontSize = 10.sp,
        )
    }
}
