package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.typezero.couchlink.remote.tv.provider.TvCapability
import dev.typezero.couchlink.remote.tv.provider.TvProviderInput
import dev.typezero.couchlink.remote.tv.provider.TvProviderState
import dev.typezero.couchlink.remote.tv.provider.TvRemoteCommand
import dev.typezero.couchlink.remote.ui.components.PremiumPanel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised
import dev.typezero.couchlink.remote.ui.theme.Raised2
import dev.typezero.couchlink.remote.ui.theme.Silver
import dev.typezero.couchlink.remote.ui.theme.SilverDim
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

private enum class RemoteIcon {
    Power, Input, Home, Back, Settings,
    Up, Down, Left, Right,
    VolumeUp, VolumeDown, Mute,
    ChannelUp, ChannelDown, Live,
    Rewind, PlayPause, FastForward,
    Hdmi, Composite, Antenna,
}

@Composable
internal fun TvRemoteScreen(
    state: TvProviderState,
    onConnect: () -> Unit,
    onCommand: (TvRemoteCommand) -> Unit,
    onInput: (TvProviderInput) -> Unit,
    onHaptic: () -> Unit,
) {
    val enabled = state.connection.ready
    val capabilities = state.capabilities
    var showInputSelector by remember { mutableStateOf(false) }

    fun press(action: () -> Unit) {
        onHaptic()
        action()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TvRemoteHeader(
            state = state,
            onPower = {
                if (TvCapability.Power in capabilities) {
                    press { onCommand(TvRemoteCommand.Power) }
                }
            },
        )

        PremiumPanel {
            SectionHeading("SYSTEM")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RemoteButton(
                    RemoteIcon.Input,
                    "INPUT",
                    enabled && TvCapability.Inputs in capabilities && state.inputs.isNotEmpty(),
                    { press { showInputSelector = true } },
                    Modifier.weight(1f),
                )
                RemoteButton(
                    RemoteIcon.Home,
                    "HOME",
                    enabled && TvCapability.Home in capabilities,
                    { press { onCommand(TvRemoteCommand.Home) } },
                    Modifier.weight(1f),
                )
                RemoteButton(
                    RemoteIcon.Back,
                    "BACK",
                    enabled && TvCapability.Back in capabilities,
                    { press { onCommand(TvRemoteCommand.Back) } },
                    Modifier.weight(1f),
                )
                RemoteButton(
                    RemoteIcon.Settings,
                    "SETTINGS",
                    enabled && TvCapability.Settings in capabilities,
                    { press { onCommand(TvRemoteCommand.Settings) } },
                    Modifier.weight(1f),
                )
            }
        }

        if (TvCapability.Dpad in capabilities) {
            Dpad(
                enabled = enabled,
                onCommand = { command -> press { onCommand(command) } },
            )
        }

        val showVolume = capabilities.any { it in setOf(TvCapability.Volume, TvCapability.Mute) }
        val showChannel = capabilities.any { it in setOf(TvCapability.Channels, TvCapability.LiveTv) }

        if (showVolume || showChannel) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (showVolume) {
                    PremiumPanel(modifier = Modifier.weight(1f)) {
                        SectionHeading("VOLUME")
                        RemoteButton(
                            RemoteIcon.VolumeUp, "VOLUME +",
                            enabled && TvCapability.Volume in capabilities,
                            { press { onCommand(TvRemoteCommand.VolumeUp) } },
                            Modifier.fillMaxWidth(),
                        )
                        RemoteButton(
                            RemoteIcon.Mute, "MUTE",
                            enabled && TvCapability.Mute in capabilities,
                            { press { onCommand(TvRemoteCommand.Mute) } },
                            Modifier.fillMaxWidth(),
                        )
                        RemoteButton(
                            RemoteIcon.VolumeDown, "VOLUME −",
                            enabled && TvCapability.Volume in capabilities,
                            { press { onCommand(TvRemoteCommand.VolumeDown) } },
                            Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (showChannel) {
                    PremiumPanel(modifier = Modifier.weight(1f)) {
                        SectionHeading("CHANNEL")
                        RemoteButton(
                            RemoteIcon.ChannelUp, "CHANNEL +",
                            enabled && TvCapability.Channels in capabilities,
                            { press { onCommand(TvRemoteCommand.ChannelUp) } },
                            Modifier.fillMaxWidth(),
                        )
                        if (TvCapability.LiveTv in capabilities) {
                            RemoteButton(
                                RemoteIcon.Live, "LIVE TV",
                                enabled,
                                { press { onCommand(TvRemoteCommand.LiveTv) } },
                                Modifier.fillMaxWidth(),
                                accent = true,
                            )
                        }
                        RemoteButton(
                            RemoteIcon.ChannelDown, "CHANNEL −",
                            enabled && TvCapability.Channels in capabilities,
                            { press { onCommand(TvRemoteCommand.ChannelDown) } },
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (TvCapability.Playback in capabilities) {
            PremiumPanel {
                SectionHeading("PLAYBACK")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RemoteButton(
                        RemoteIcon.Rewind, "REWIND", enabled,
                        { press { onCommand(TvRemoteCommand.Rewind) } },
                        Modifier.weight(1f),
                    )
                    RemoteButton(
                        RemoteIcon.PlayPause, "PLAY / PAUSE", enabled,
                        { press { onCommand(TvRemoteCommand.PlayPause) } },
                        Modifier.weight(1f),
                        accent = true,
                    )
                    RemoteButton(
                        RemoteIcon.FastForward, "FAST FORWARD", enabled,
                        { press { onCommand(TvRemoteCommand.FastForward) } },
                        Modifier.weight(1f),
                    )
                }
            }
        }

        if (showInputSelector) {
            InputSelectorDialog(
                deviceName = state.selectedDevice?.name ?: "TV",
                inputs = state.inputs,
                onDismiss = { showInputSelector = false },
                onSelect = { input ->
                    showInputSelector = false
                    press { onInput(input) }
                },
            )
        }

        if (!enabled) {
            ReconnectPanel(
                paired = state.pairing.paired,
                tvName = state.selectedDevice?.name ?: "the TV",
                onConnect = { press(onConnect) },
            )
        }
    }
}

@Composable
private fun TvRemoteHeader(
    state: TvProviderState,
    onPower: () -> Unit,
) {
    val statusLabel = when {
        state.connection.ready -> "REMOTE READY"
        state.connection.connecting -> "CONNECTING"
        state.pairing.paired -> "STANDBY"
        else -> "PAIRING REQUIRED"
    }
    val statusColor = when {
        state.connection.ready -> Success
        state.connection.connecting -> Accent
        else -> Muted
    }
    val powerEnabled = state.pairing.paired && TvCapability.Power in state.capabilities

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "TV REMOTE",
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
            )
            Text(
                state.selectedDevice?.name ?: "No TV selected",
                color = TextColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .shadow(7.dp, CircleShape, spotColor = statusColor.copy(alpha = 0.8f))
                        .background(statusColor, CircleShape),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    statusLabel,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
            Text(
                when {
                    state.connection.ready ->
                        "Secure connection • ${state.selectedDevice?.host.orEmpty()}"
                    state.connection.connecting ->
                        "Establishing secure TV connection…"
                    state.pairing.paired ->
                        state.connection.message
                    else ->
                        "Open Settings to select and pair a TV"
                },
                color = Muted,
                fontSize = 11.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(16.dp, CircleShape, spotColor = Accent.copy(alpha = 0.28f))
                .background(
                    Brush.radialGradient(listOf(Color(0xFF222A31), Color(0xFF06090C))),
                    CircleShape,
                )
                .border(1.2.dp, Accent.copy(alpha = 0.95f), CircleShape)
                .padding(3.dp)
                .border(0.7.dp, Accent.copy(alpha = 0.30f), CircleShape)
                .clickable(enabled = powerEnabled, onClick = onPower),
            contentAlignment = Alignment.Center,
        ) {
            RemoteGlyph(
                RemoteIcon.Power,
                if (powerEnabled) Accent else Muted.copy(alpha = 0.5f),
                Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun SectionHeading(label: String) {
    Text(
        label,
        color = Muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.7.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Dpad(
    enabled: Boolean,
    onCommand: (TvRemoteCommand) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(258.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(236.dp)
                .shadow(
                    18.dp,
                    CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.9f),
                    spotColor = Accent.copy(alpha = 0.13f),
                )
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF202830), Color(0xFF080C10), Color(0xFF030506)),
                    ),
                    CircleShape,
                )
                .border(1.25.dp, Accent.copy(alpha = 0.88f), CircleShape)
                .padding(4.dp)
                .border(0.65.dp, SilverDim.copy(alpha = 0.55f), CircleShape),
        )

        DirectionButton(RemoteIcon.Up, enabled, { onCommand(TvRemoteCommand.DpadUp) }, Modifier.align(Alignment.TopCenter))
        DirectionButton(RemoteIcon.Down, enabled, { onCommand(TvRemoteCommand.DpadDown) }, Modifier.align(Alignment.BottomCenter))
        DirectionButton(RemoteIcon.Left, enabled, { onCommand(TvRemoteCommand.DpadLeft) }, Modifier.align(Alignment.CenterStart))
        DirectionButton(RemoteIcon.Right, enabled, { onCommand(TvRemoteCommand.DpadRight) }, Modifier.align(Alignment.CenterEnd))

        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(13.dp, CircleShape, spotColor = Accent.copy(alpha = 0.30f))
                .background(
                    Brush.radialGradient(listOf(Color(0xFF29333C), Color(0xFF090D11))),
                    CircleShape,
                )
                .border(1.4.dp, Accent, CircleShape)
                .clickable(enabled = enabled) { onCommand(TvRemoteCommand.Select) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "OK",
                color = if (enabled) Accent else Muted.copy(alpha = 0.55f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun DirectionButton(
    icon: RemoteIcon,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RemoteGlyph(
            icon,
            if (enabled) TextColor else Muted.copy(alpha = 0.45f),
            Modifier.size(29.dp),
        )
    }
}

@Composable
private fun InputSelectorDialog(
    deviceName: String,
    inputs: List<TvProviderInput>,
    onDismiss: () -> Unit,
    onSelect: (TvProviderInput) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val outerShape = RoundedCornerShape(26.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(
                    24.dp,
                    outerShape,
                    ambientColor = Color.Black.copy(alpha = 0.95f),
                    spotColor = Accent.copy(alpha = 0.18f),
                )
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF171E25), Color(0xFF05080B))),
                    outerShape,
                )
                .border(1.3.dp, Accent.copy(alpha = 0.95f), outerShape)
                .padding(3.dp)
                .border(0.65.dp, Accent.copy(alpha = 0.28f), RoundedCornerShape(23.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Raised2, CircleShape)
                        .border(1.dp, Accent.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    RemoteGlyph(RemoteIcon.Input, Accent, Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "SELECT INPUT",
                        color = TextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                    Text(deviceName, color = Muted, fontSize = 11.sp)
                }
            }

            inputs.forEach { input ->
                val icon = when {
                    input.id.contains("HDMI", ignoreCase = true) -> RemoteIcon.Hdmi
                    input.id.contains("COMPOSITE", ignoreCase = true) ||
                        input.label.contains("Composite", ignoreCase = true) -> RemoteIcon.Composite
                    else -> RemoteIcon.Antenna
                }
                InputSourceButton(icon, input.label, "TV input") { onSelect(input) }
            }

            Text(
                "CANCEL",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun InputSourceButton(
    icon: RemoteIcon,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                8.dp,
                shape,
                ambientColor = Color.Black.copy(alpha = 0.65f),
                spotColor = Color.Transparent,
            )
            .background(
                Brush.verticalGradient(listOf(Color(0xFF171D23), Color(0xFF080B0E))),
                shape,
            )
            .border(1.dp, SilverDim.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                .border(0.8.dp, Accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RemoteGlyph(icon, Silver, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
        Text("›", color = Muted, fontSize = 25.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReconnectPanel(
    paired: Boolean,
    tvName: String,
    onConnect: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF151A1F), Color(0xFF070A0D))),
                shape,
            )
            .border(1.dp, Accent.copy(alpha = 0.55f), shape)
            .clickable(enabled = paired, onClick = onConnect)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (paired) Accent else Muted, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (paired) "Tap to reconnect to $tvName"
            else "Open Settings to select and pair a TV",
            color = TextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (paired) {
            Text(
                "RECONNECT",
                color = Accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
private fun RemoteButton(
    icon: RemoteIcon,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        !enabled -> SilverDim.copy(alpha = 0.25f)
        accent -> Accent.copy(alpha = 0.95f)
        else -> Accent.copy(alpha = 0.55f)
    }
    val iconColor = when {
        !enabled -> Muted.copy(alpha = 0.45f)
        accent -> Accent
        else -> Silver
    }

    Column(
        modifier = modifier
            .height(62.dp)
            .shadow(
                8.dp,
                shape,
                ambientColor = Color.Black.copy(alpha = 0.70f),
                spotColor = if (accent) Accent.copy(alpha = 0.16f) else Color.Transparent,
            )
            .background(
                Brush.verticalGradient(
                    if (enabled) {
                        listOf(Color(0xFF1B2229), Color(0xFF080B0F))
                    } else {
                        listOf(Raised.copy(alpha = 0.55f), Color(0xFF050709))
                    },
                ),
                shape,
            )
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RemoteGlyph(icon, iconColor, Modifier.size(21.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = when {
                !enabled -> Muted.copy(alpha = 0.48f)
                accent -> Accent
                else -> TextColor
            },
            fontSize = if (label.length > 10) 8.5f.sp else 9.5f.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.25.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RemoteGlyph(
    icon: RemoteIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = (size.minDimension * 0.085f).coerceAtLeast(1.5f)
        val thin = stroke * 0.72f
        val c = Offset(size.width / 2f, size.height / 2f)

        fun line(start: Offset, end: Offset, width: Float = stroke) {
            drawLine(color, start, end, width, cap = StrokeCap.Round)
        }

        when (icon) {
            RemoteIcon.Power -> {
                drawArc(
                    color = color,
                    startAngle = -42f,
                    sweepAngle = 264f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.16f),
                    size = Size(size.width * 0.68f, size.height * 0.70f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                line(Offset(c.x, size.height * 0.06f), Offset(c.x, size.height * 0.48f))
            }
            RemoteIcon.Input -> {
                drawRoundRect(
                    color,
                    Offset(size.width * 0.08f, size.height * 0.18f),
                    Size(size.width * 0.64f, size.height * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.08f),
                    style = Stroke(stroke),
                )
                line(Offset(size.width * 0.48f, c.y), Offset(size.width * 0.92f, c.y))
                line(Offset(size.width * 0.74f, size.height * 0.32f), Offset(size.width * 0.92f, c.y))
                line(Offset(size.width * 0.74f, size.height * 0.68f), Offset(size.width * 0.92f, c.y))
            }
            RemoteIcon.Home -> {
                val path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.48f)
                    lineTo(c.x, size.height * 0.12f)
                    lineTo(size.width * 0.90f, size.height * 0.48f)
                    moveTo(size.width * 0.22f, size.height * 0.42f)
                    lineTo(size.width * 0.22f, size.height * 0.88f)
                    lineTo(size.width * 0.78f, size.height * 0.88f)
                    lineTo(size.width * 0.78f, size.height * 0.42f)
                }
                drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            RemoteIcon.Back -> {
                line(Offset(size.width * 0.82f, c.y), Offset(size.width * 0.18f, c.y))
                line(Offset(size.width * 0.18f, c.y), Offset(size.width * 0.45f, size.height * 0.23f))
                line(Offset(size.width * 0.18f, c.y), Offset(size.width * 0.45f, size.height * 0.77f))
            }
            RemoteIcon.Settings -> {
                drawCircle(color, size.minDimension * 0.16f, c, style = Stroke(stroke))
                repeat(8) { index ->
                    val a = Math.toRadians((index * 45.0) - 90.0)
                    val p1 = Offset(
                        c.x + kotlin.math.cos(a).toFloat() * size.width * 0.27f,
                        c.y + kotlin.math.sin(a).toFloat() * size.height * 0.27f,
                    )
                    val p2 = Offset(
                        c.x + kotlin.math.cos(a).toFloat() * size.width * 0.42f,
                        c.y + kotlin.math.sin(a).toFloat() * size.height * 0.42f,
                    )
                    line(p1, p2)
                }
            }
            RemoteIcon.Up, RemoteIcon.Down, RemoteIcon.Left, RemoteIcon.Right -> {
                val points = when (icon) {
                    RemoteIcon.Up -> arrayOf(
                        Offset(size.width * 0.20f, size.height * 0.62f),
                        Offset(c.x, size.height * 0.34f),
                        Offset(size.width * 0.80f, size.height * 0.62f),
                    )
                    RemoteIcon.Down -> arrayOf(
                        Offset(size.width * 0.20f, size.height * 0.38f),
                        Offset(c.x, size.height * 0.66f),
                        Offset(size.width * 0.80f, size.height * 0.38f),
                    )
                    RemoteIcon.Left -> arrayOf(
                        Offset(size.width * 0.62f, size.height * 0.20f),
                        Offset(size.width * 0.34f, c.y),
                        Offset(size.width * 0.62f, size.height * 0.80f),
                    )
                    else -> arrayOf(
                        Offset(size.width * 0.38f, size.height * 0.20f),
                        Offset(size.width * 0.66f, c.y),
                        Offset(size.width * 0.38f, size.height * 0.80f),
                    )
                }
                line(points[0], points[1])
                line(points[1], points[2])
            }
            RemoteIcon.VolumeUp, RemoteIcon.VolumeDown, RemoteIcon.Mute -> {
                val path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.40f)
                    lineTo(size.width * 0.32f, size.height * 0.40f)
                    lineTo(size.width * 0.55f, size.height * 0.20f)
                    lineTo(size.width * 0.55f, size.height * 0.80f)
                    lineTo(size.width * 0.32f, size.height * 0.60f)
                    lineTo(size.width * 0.10f, size.height * 0.60f)
                    close()
                }
                drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
                when (icon) {
                    RemoteIcon.VolumeUp -> {
                        line(Offset(size.width * 0.70f, c.y), Offset(size.width * 0.94f, c.y), thin)
                        line(Offset(size.width * 0.82f, size.height * 0.38f), Offset(size.width * 0.82f, size.height * 0.62f), thin)
                    }
                    RemoteIcon.VolumeDown ->
                        line(Offset(size.width * 0.70f, c.y), Offset(size.width * 0.94f, c.y), thin)
                    else -> {
                        line(Offset(size.width * 0.68f, size.height * 0.34f), Offset(size.width * 0.94f, size.height * 0.66f), thin)
                        line(Offset(size.width * 0.94f, size.height * 0.34f), Offset(size.width * 0.68f, size.height * 0.66f), thin)
                    }
                }
            }
            RemoteIcon.ChannelUp, RemoteIcon.ChannelDown -> {
                drawRoundRect(
                    color,
                    Offset(size.width * 0.08f, size.height * 0.18f),
                    Size(size.width * 0.60f, size.height * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f),
                    style = Stroke(stroke),
                )
                line(Offset(size.width * 0.75f, c.y), Offset(size.width * 0.94f, c.y), thin)
                if (icon == RemoteIcon.ChannelUp) {
                    line(
                        Offset(size.width * 0.845f, size.height * 0.40f),
                        Offset(size.width * 0.845f, size.height * 0.60f),
                        thin,
                    )
                }
            }
            RemoteIcon.Live -> {
                drawRoundRect(
                    color,
                    Offset(size.width * 0.08f, size.height * 0.18f),
                    Size(size.width * 0.84f, size.height * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.08f),
                    style = Stroke(stroke),
                )
                drawCircle(color, size.minDimension * 0.08f, Offset(size.width * 0.28f, c.y))
                line(Offset(size.width * 0.45f, size.height * 0.38f), Offset(size.width * 0.76f, size.height * 0.38f), thin)
                line(Offset(size.width * 0.45f, size.height * 0.60f), Offset(size.width * 0.68f, size.height * 0.60f), thin)
            }
            RemoteIcon.Rewind, RemoteIcon.FastForward -> {
                val reverse = icon == RemoteIcon.Rewind
                fun triangle(centerX: Float) {
                    val path = Path().apply {
                        if (reverse) {
                            moveTo(centerX + size.width * 0.12f, size.height * 0.25f)
                            lineTo(centerX - size.width * 0.12f, c.y)
                            lineTo(centerX + size.width * 0.12f, size.height * 0.75f)
                        } else {
                            moveTo(centerX - size.width * 0.12f, size.height * 0.25f)
                            lineTo(centerX + size.width * 0.12f, c.y)
                            lineTo(centerX - size.width * 0.12f, size.height * 0.75f)
                        }
                        close()
                    }
                    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                triangle(size.width * 0.34f)
                triangle(size.width * 0.66f)
            }
            RemoteIcon.PlayPause -> {
                val path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.20f)
                    lineTo(size.width * 0.48f, c.y)
                    lineTo(size.width * 0.10f, size.height * 0.80f)
                    close()
                }
                drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
                line(Offset(size.width * 0.68f, size.height * 0.22f), Offset(size.width * 0.68f, size.height * 0.78f))
                line(Offset(size.width * 0.88f, size.height * 0.22f), Offset(size.width * 0.88f, size.height * 0.78f))
            }
            RemoteIcon.Hdmi -> {
                drawRoundRect(
                    color,
                    Offset(size.width * 0.08f, size.height * 0.25f),
                    Size(size.width * 0.84f, size.height * 0.50f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f),
                    style = Stroke(stroke),
                )
                repeat(4) { i ->
                    val x = size.width * (0.28f + i * 0.145f)
                    line(Offset(x, size.height * 0.40f), Offset(x, size.height * 0.60f), thin)
                }
            }
            RemoteIcon.Composite -> {
                drawCircle(color, size.minDimension * 0.20f, Offset(size.width * 0.28f, c.y), style = Stroke(stroke))
                drawCircle(color, size.minDimension * 0.20f, Offset(size.width * 0.72f, c.y), style = Stroke(stroke))
                drawCircle(color, size.minDimension * 0.05f, Offset(size.width * 0.28f, c.y))
                drawCircle(color, size.minDimension * 0.05f, Offset(size.width * 0.72f, c.y))
            }
            RemoteIcon.Antenna -> {
                line(Offset(c.x, size.height * 0.38f), Offset(c.x, size.height * 0.88f))
                line(Offset(size.width * 0.28f, size.height * 0.88f), Offset(size.width * 0.72f, size.height * 0.88f))
                drawArc(
                    color = color,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.20f, size.height * 0.12f),
                    size = Size(size.width * 0.60f, size.height * 0.54f),
                    style = Stroke(thin, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = 215f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.32f, size.height * 0.24f),
                    size = Size(size.width * 0.36f, size.height * 0.32f),
                    style = Stroke(thin, cap = StrokeCap.Round),
                )
            }
        }
    }
}
