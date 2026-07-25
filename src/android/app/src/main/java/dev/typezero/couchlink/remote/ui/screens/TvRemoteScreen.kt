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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import dev.typezero.couchlink.remote.tv.TvInputTarget
import dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

@Composable
internal fun TvRemoteScreen(
    state: TvDiscoveryController.State,
    onConnect: () -> Unit,
    onPower: () -> Unit,
    onKey: (RemoteKeyCode) -> Unit,
    onLiveTv: () -> Unit,
    onInput: (TvInputTarget) -> Unit,
) {
    val enabled = state.remote.ready
    var showInputSelector by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("TV REMOTE", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(state.selectedDevice?.name ?: "No TV selected", color = TextColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        state.remote.ready -> "Connected • ${state.selectedDevice?.host.orEmpty()}"
                        state.remote.connecting -> "Connecting…"
                        state.pairing.paired -> state.remote.message
                        else -> "Pair a TV in Settings"
                    },
                    color = if (enabled) Success else Muted,
                    fontSize = 12.sp,
                )
                if (state.remote.ready && state.remote.lastCommand != null) {
                    Text(
                        "${state.remote.message} • ${state.remote.commandsSent}",
                        color = Muted,
                        fontSize = 10.sp,
                    )
                }
            }
            RemoteButton(
                label = "⏻",
                enabled = state.pairing.paired,
                onClick = onPower,
                modifier = Modifier.size(54.dp),
                circular = true,
                accentText = true,
            )
        }

        PremiumPanel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RemoteButton("INPUT", enabled, { showInputSelector = true }, Modifier.weight(1f))
                RemoteButton("HOME", enabled, { onKey(RemoteKeyCode.KEYCODE_HOME) }, Modifier.weight(1f))
                RemoteButton("BACK", enabled, { onKey(RemoteKeyCode.KEYCODE_BACK) }, Modifier.weight(1f))
                RemoteButton("SETTINGS", enabled, { onKey(RemoteKeyCode.KEYCODE_SETTINGS) }, Modifier.weight(1f))
            }
        }

        Dpad(enabled = enabled, onKey = onKey)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PremiumPanel(modifier = Modifier.weight(1f)) {
                Text("VOLUME", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                RemoteButton("＋", enabled, { onKey(RemoteKeyCode.KEYCODE_VOLUME_UP) }, Modifier.fillMaxWidth())
                RemoteButton("MUTE", enabled, { onKey(RemoteKeyCode.KEYCODE_VOLUME_MUTE) }, Modifier.fillMaxWidth())
                RemoteButton("－", enabled, { onKey(RemoteKeyCode.KEYCODE_VOLUME_DOWN) }, Modifier.fillMaxWidth())
            }
            PremiumPanel(modifier = Modifier.weight(1f)) {
                Text("CHANNEL", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                RemoteButton("CH ＋", enabled, { onKey(RemoteKeyCode.KEYCODE_CHANNEL_UP) }, Modifier.fillMaxWidth())
                RemoteButton("GOOGLE LIVE", enabled, onLiveTv, Modifier.fillMaxWidth())
                RemoteButton("CH －", enabled, { onKey(RemoteKeyCode.KEYCODE_CHANNEL_DOWN) }, Modifier.fillMaxWidth())
            }
        }

        PremiumPanel {
            Text("PLAYBACK", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RemoteButton("−1 MIN", enabled, { onKey(RemoteKeyCode.KEYCODE_MEDIA_REWIND) }, Modifier.weight(1f))
                RemoteButton("PLAY/PAUSE", enabled, { onKey(RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE) }, Modifier.weight(1f))
                RemoteButton("+1 MIN", enabled, { onKey(RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD) }, Modifier.weight(1f))
            }
        }


        if (showInputSelector) {
            InputSelectorDialog(
                onDismiss = { showInputSelector = false },
                onSelect = { key ->
                    showInputSelector = false
                    onInput(key)
                },
            )
        }

        if (!enabled) {
            PremiumPanel {
                Text(
                    if (state.pairing.paired) "Tap here to reconnect to ${state.selectedDevice?.name ?: "the TV"}." else "Open Settings to discover and pair your Google TV.",
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.pairing.paired, onClick = onConnect)
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Dpad(enabled: Boolean, onKey: (RemoteKeyCode) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .background(Raised, CircleShape)
                .border(1.dp, Accent.copy(alpha = 0.55f), CircleShape),
        )
        RemoteButton("▲", enabled, { onKey(RemoteKeyCode.KEYCODE_DPAD_UP) }, Modifier.align(Alignment.TopCenter).size(72.dp), circular = true)
        RemoteButton("▼", enabled, { onKey(RemoteKeyCode.KEYCODE_DPAD_DOWN) }, Modifier.align(Alignment.BottomCenter).size(72.dp), circular = true)
        RemoteButton("◀", enabled, { onKey(RemoteKeyCode.KEYCODE_DPAD_LEFT) }, Modifier.align(Alignment.CenterStart).size(72.dp), circular = true)
        RemoteButton("▶", enabled, { onKey(RemoteKeyCode.KEYCODE_DPAD_RIGHT) }, Modifier.align(Alignment.CenterEnd).size(72.dp), circular = true)
        RemoteButton("OK", enabled, { onKey(RemoteKeyCode.KEYCODE_DPAD_CENTER) }, Modifier.size(92.dp), circular = true, accentText = true)
    }
}

@Composable
private fun InputSelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (TvInputTarget) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Raised,
        title = { Text("SELECT INPUT", color = TextColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a Hisense hardware input.", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RemoteButton("HDMI 1", true, { onSelect(TvInputTarget.HDMI_1) }, Modifier.weight(1f))
                    RemoteButton("HDMI 2", true, { onSelect(TvInputTarget.HDMI_2) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RemoteButton("HDMI 3", true, { onSelect(TvInputTarget.HDMI_3) }, Modifier.weight(1f))
                    RemoteButton("COMPOSITE", true, { onSelect(TvInputTarget.COMPOSITE) }, Modifier.weight(1f))
                }
                RemoteButton("TV / ANTENNA", true, { onSelect(TvInputTarget.TV) }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Text("CANCEL", color = Accent, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        },
    )
}

@Composable
private fun RemoteButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    accentText: Boolean = false,
) {
    val shape = if (circular) CircleShape else RoundedCornerShape(15.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .background(if (enabled) Raised else Raised.copy(alpha = 0.55f), shape)
            .border(1.dp, if (enabled) Accent.copy(alpha = 0.65f) else Muted.copy(alpha = 0.25f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when {
                !enabled -> Muted.copy(alpha = 0.6f)
                accentText -> Accent
                else -> TextColor
            },
            fontSize = if (label.length <= 2) 20.sp else 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
