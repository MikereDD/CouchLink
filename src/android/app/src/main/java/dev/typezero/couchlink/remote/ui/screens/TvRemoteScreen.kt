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
) {
    var manualHost by remember { mutableStateOf(state.selectedDevice?.host.orEmpty()) }

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
                text = "FIRST TEST BUILD",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "This build verifies discovery and network reachability before CouchLink stores a TV certificate or sends commands. Pairing and the full remote controls are the next step after your A6H passes this test.",
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
