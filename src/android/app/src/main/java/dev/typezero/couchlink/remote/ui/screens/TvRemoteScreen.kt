package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.tv.TvDevice
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

@Composable
internal fun TvRemoteScreen(
    state: TvDiscoveryController.State,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelect: (TvDevice) -> Unit,
    onSelectManual: (String) -> Unit,
    onProbe: () -> Unit,
    onBeginPairing: () -> Unit,
    onFinishPairing: (String) -> Unit,
    onCancelPairing: () -> Unit,
) {
    var manualHost by remember { mutableStateOf(state.selectedDevice?.host.orEmpty()) }
    var pairingCode by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("TV REMOTE")

        PremiumPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.selectedDevice?.name ?: "No TV selected",
                        color = if (state.selectedDevice != null) TextColor else Muted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.selectedDevice?.host
                            ?: "Discover your Hisense Google TV on the local network.",
                        color = Muted,
                        fontSize = 13.sp,
                    )
                }
                StatusDot(
                    active = state.probe?.fullyReachable == true,
                    busy = state.probing || state.scanning,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = state.message,
                color = if (state.probe?.fullyReachable == true) Success else Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }

        PremiumPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionButton(
                    label = if (state.scanning) "Stop scan" else "Scan for TVs",
                    onClick = if (state.scanning) onStopScan else onScan,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = if (state.probing) "Testing…" else "Test connection",
                    onClick = onProbe,
                    enabled = state.selectedDevice != null && !state.probing,
                    modifier = Modifier.weight(1f),
                )
            }

            state.probe?.let { probe ->
                Spacer(Modifier.height(14.dp))
                ProbeRow("Pairing service", 6467, probe.pairingPortReachable)
                Spacer(Modifier.height(8.dp))
                ProbeRow("Remote-control service", 6466, probe.remotePortReachable)
            }
        }

        if (state.probe?.fullyReachable == true || state.pairing.awaitingCode || state.pairing.paired) {
            SectionLabel("SECURE PAIRING")
            PremiumPanel {
                when {
                    state.pairing.paired -> {
                        Text(
                            text = "PAIRED",
                            color = Success,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = state.pairing.message ?: "CouchLink has a secure identity for this TV.",
                            color = Muted,
                            fontSize = 13.sp,
                        )
                    }
                    state.pairing.awaitingCode -> {
                        Text(
                            text = "Check the TV screen and enter its six-character pairing code.",
                            color = TextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { value ->
                                pairingCode = value.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                            },
                            label = { Text("TV pairing code") },
                            placeholder = { Text("A1B2C3") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ActionButton(
                                label = if (state.pairing.inProgress) "Pairing…" else "Pair TV",
                                onClick = { onFinishPairing(pairingCode) },
                                enabled = pairingCode.length == 6 && !state.pairing.inProgress,
                                modifier = Modifier.weight(1f),
                            )
                            ActionButton(
                                label = "Cancel",
                                onClick = onCancelPairing,
                                enabled = !state.pairing.inProgress,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Pair CouchLink with the TV to authorize remote-control commands. The private key stays in Android Keystore.",
                            color = Muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        ActionButton(
                            label = if (state.pairing.inProgress) "Requesting code…" else "Pair with TV",
                            onClick = onBeginPairing,
                            enabled = !state.pairing.inProgress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                state.pairing.message?.takeIf { !state.pairing.paired }?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = Muted, fontSize = 12.sp)
                }
            }
        }

        if (state.devices.isNotEmpty()) {
            SectionLabel("DISCOVERED TVS")
            state.devices.forEach { device ->
                DeviceCard(
                    device = device,
                    selected = state.selectedDevice?.host == device.host,
                    onClick = { onSelect(device) },
                )
            }
        }

        SectionLabel("MANUAL CONNECTION")
        PremiumPanel {
            Text(
                text = "Use the TV's IP address if local discovery does not find it.",
                color = Muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = manualHost,
                onValueChange = { manualHost = it },
                label = { Text("TV IP address or hostname") },
                placeholder = { Text("192.168.4.x") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            ActionButton(
                label = "Use this TV",
                onClick = { onSelectManual(manualHost) },
                enabled = manualHost.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PremiumPanel {
            Text(
                text = "PAIRING TEST BUILD",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "This build performs the real Google TV certificate pairing exchange. After pairing succeeds, the next build will open the remote-control channel and send live navigation, Home, Back, volume, and mute commands.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: TvDevice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Raised, shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Accent else Accent.copy(alpha = 0.45f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▣", color = if (selected) Accent else TextColor, fontSize = 28.sp)
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, color = TextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(device.host, color = Muted, fontSize = 13.sp)
            device.serviceType?.let { Text(it, color = Muted, fontSize = 10.sp) }
        }
        Text(if (selected) "SELECTED" else "SELECT", color = if (selected) Accent else Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ProbeRow(label: String, port: Int, reachable: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("TCP port $port", color = Muted, fontSize = 11.sp)
        }
        Text(
            text = if (reachable) "REACHABLE" else "NO RESPONSE",
            color = if (reachable) Success else Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusDot(active: Boolean, busy: Boolean) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(Raised, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Accent,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "●",
                color = if (active) Success else Muted,
                fontSize = 24.sp,
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.Black,
            disabledContainerColor = Raised,
            disabledContentColor = Muted,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp,
    )
}
