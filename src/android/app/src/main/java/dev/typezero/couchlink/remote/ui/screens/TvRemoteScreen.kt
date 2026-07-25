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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
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
    onKey: (RemoteKeyCode) -> Unit,
) {
    val enabled = state.remote.ready
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
            }
            RemoteButton(
                label = "⏻",
                enabled = state.pairing.paired,
                onClick = { if (enabled) onKey(RemoteKeyCode.KEYCODE_POWER) else onConnect() },
                modifier = Modifier.size(54.dp),
                circular = true,
                accentText = true,
            )
        }

        PremiumPanel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RemoteButton("INPUT", enabled, { onKey(RemoteKeyCode.KEYCODE_TV_INPUT) }, Modifier.weight(1f))
                RemoteButton("HOME", enabled, { onKey(RemoteKeyCode.KEYCODE_HOME) }, Modifier.weight(1f))
                RemoteButton("BACK", enabled, { onKey(RemoteKeyCode.KEYCODE_BACK) }, Modifier.weight(1f))
                RemoteButton("MENU", enabled, { onKey(RemoteKeyCode.KEYCODE_MENU) }, Modifier.weight(1f))
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
                RemoteButton("GUIDE", enabled, { onKey(RemoteKeyCode.KEYCODE_GUIDE) }, Modifier.fillMaxWidth())
                RemoteButton("CH －", enabled, { onKey(RemoteKeyCode.KEYCODE_CHANNEL_DOWN) }, Modifier.fillMaxWidth())
            }
        }

        PremiumPanel {
            Text("PLAYBACK", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RemoteButton("⏪", enabled, { onKey(RemoteKeyCode.KEYCODE_MEDIA_REWIND) }, Modifier.weight(1f))
                RemoteButton("▶Ⅱ", enabled, { onKey(RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE) }, Modifier.weight(1f))
                RemoteButton("⏩", enabled, { onKey(RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD) }, Modifier.weight(1f))
            }
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
