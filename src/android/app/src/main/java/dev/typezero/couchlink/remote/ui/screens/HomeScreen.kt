package dev.typezero.couchlink.remote.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.hid.WindowsShortcut
import dev.typezero.couchlink.remote.model.AudioFavoriteSlot
import dev.typezero.couchlink.remote.model.AudioOutputFavorite
import dev.typezero.couchlink.remote.model.LauncherHostState
import dev.typezero.couchlink.remote.model.LauncherId
import dev.typezero.couchlink.remote.ui.components.SectionLabel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Cyan
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Purple
import dev.typezero.couchlink.remote.ui.theme.Silver
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

private data class SecondaryLauncherSpec(
    val label: String,
    val launcher: LauncherId,
    @param:DrawableRes val iconRes: Int,
)

private data class CommandSpec(
    val label: String,
    val shortcut: WindowsShortcut,
    val glyph: CommandGlyphType,
)

private enum class CommandGlyphType {
    AltTab,
    Task,
    Show,
    Close,
}

private val secondaryLauncherRows = listOf(
    listOf(
        SecondaryLauncherSpec("Xbox", LauncherId.Xbox, R.drawable.launcher_xbox),
        SecondaryLauncherSpec("EA app", LauncherId.Ea, R.drawable.launcher_ea),
        SecondaryLauncherSpec("Ubisoft", LauncherId.Ubisoft, R.drawable.launcher_ubisoft),
    ),
    listOf(
        SecondaryLauncherSpec("Rockstar", LauncherId.Rockstar, R.drawable.launcher_rockstar),
        SecondaryLauncherSpec("Epic", LauncherId.Epic, R.drawable.launcher_epic),
        SecondaryLauncherSpec("Amazon", LauncherId.Amazon, R.drawable.launcher_amazon),
    ),
)

private val commandDeck = listOf(
    CommandSpec("Alt-Tab", WindowsShortcut.AltTab, CommandGlyphType.AltTab),
    CommandSpec("Task", WindowsShortcut.TaskManager, CommandGlyphType.Task),
    CommandSpec("Show", WindowsShortcut.ShowDesktop, CommandGlyphType.Show),
    CommandSpec("Close", WindowsShortcut.CloseWindow, CommandGlyphType.Close),
)

@Composable
internal fun HomeScreen(
    launcherHostState: LauncherHostState,
    onLauncher: (LauncherId) -> Unit,
    onShortcut: (WindowsShortcut) -> Unit,
    launcherEnabled: Boolean,
    inputEnabled: Boolean,
    onRetryLauncherHost: () -> Unit,
    onWakePc: () -> Unit,
    onRefreshAudioOutputs: () -> Unit,
    onAudioOutput: (String) -> Unit,
    onSetAudioFavorite: (AudioFavoriteSlot, String) -> Unit,
    onClearAudioFavorite: (AudioFavoriteSlot) -> Unit,
) {
    LauncherHostStatus(launcherHostState, onRetryLauncherHost, onWakePc)
    SectionLabel("LAUNCHERS")
    if (!launcherEnabled) {
        Text(
            text = "Connect the Windows launcher host or Bluetooth input to use launchers.",
            color = Muted,
            fontSize = 11.sp,
        )
    }

    PrimaryLauncherTile(
        iconRes = R.drawable.launcher_steam,
        title = "Steam",
        subtitle = "Launch Big Picture",
        running = launcherHostState.launcherStates[LauncherId.Steam] == "running",
        enabled = launcherEnabled,
        onClick = { onLauncher(LauncherId.Steam) },
    )

    FeaturedLauncherTile(
        iconRes = R.drawable.launcher_gog,
        title = "GOG Galaxy",
        subtitle = "Open your DRM-free library",
        enabled = launcherEnabled,
        onClick = { onLauncher(LauncherId.Gog) },
    )

    secondaryLauncherRows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { launcher ->
                SecondaryLauncherTile(
                    iconRes = launcher.iconRes,
                    label = launcher.label,
                    enabled = launcherEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onLauncher(launcher.launcher) },
                )
            }
        }
    }

    Spacer(Modifier.height(2.dp))
    SectionLabel("AUDIO OUTPUT")
    AudioOutputPanel(
        state = launcherHostState,
        onRefresh = onRefreshAudioOutputs,
        onSelect = onAudioOutput,
        onSetFavorite = onSetAudioFavorite,
        onClearFavorite = onClearAudioFavorite,
    )

    Spacer(Modifier.height(2.dp))
    SectionLabel("COMMAND DECK")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        commandDeck.forEach { command ->
            CommandDeckTile(
                glyph = command.glyph,
                label = command.label,
                enabled = inputEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onShortcut(command.shortcut) },
            )
        }
    }
}

@Composable
private fun AudioOutputPanel(
    state: LauncherHostState,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onSetFavorite: (AudioFavoriteSlot, String) -> Unit,
    onClearFavorite: (AudioFavoriteSlot) -> Unit,
) {
    var editingFavorite by remember { mutableStateOf<AudioFavoriteSlot?>(null) }
    var showOutputs by remember { mutableStateOf(false) }
    val currentOutput = state.audioOutputs.firstOrNull { it.isDefault }
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090D11), shape)
            .border(1.dp, Accent.copy(alpha = 0.72f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = currentOutput?.name?.let(::compactAudioOutputName) ?:
                        if (state.connected) "No active output reported" else "Connect Windows Host",
                    color = TextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Text(
                    text = if (state.connected) "CURRENT WINDOWS OUTPUT" else "WINDOWS HOST OFFLINE",
                    color = if (state.connected) Success else Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (state.audioLoading) "LOADING" else "REFRESH",
                color = Accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = state.connected && !state.audioLoading, onClick = onRefresh),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AudioFavoriteCard(
                label = "HEADPHONES",
                favorite = state.favoriteHeadphones,
                state = state,
                modifier = Modifier.weight(1f),
                onSelect = onSelect,
                onEdit = { editingFavorite = AudioFavoriteSlot.Headphones },
            )
            AudioFavoriteCard(
                label = "TV / DISPLAY",
                favorite = state.favoriteTvDisplay,
                state = state,
                modifier = Modifier.weight(1f),
                onSelect = onSelect,
                onEdit = { editingFavorite = AudioFavoriteSlot.TvDisplay },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF05080B), RoundedCornerShape(12.dp))
                .border(1.dp, Silver.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .clickable(enabled = state.connected) {
                    showOutputs = true
                    onRefresh()
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("CHOOSE AUDIO OUTPUT", color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("›", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showOutputs) {
        AlertDialog(
            onDismissRequest = { showOutputs = false },
            containerColor = Color(0xFF090D11),
            titleContentColor = TextColor,
            textContentColor = TextColor,
            title = {
                Column {
                    Text("Audio Output", fontWeight = FontWeight.Bold)
                    Text(
                        currentOutput?.name?.let(::compactAudioOutputName) ?: "No active output",
                        color = Muted,
                        fontSize = 10.sp,
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    state.audioOutputs.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (device.isDefault) Accent.copy(alpha = 0.12f) else Color(0xFF05080B), RoundedCornerShape(11.dp))
                                .border(1.dp, if (device.isDefault) Accent else Silver.copy(alpha = 0.20f), RoundedCornerShape(11.dp))
                                .clickable(enabled = state.connected && !device.isDefault) {
                                    onSelect(device.id)
                                    showOutputs = false
                                }
                                .padding(horizontal = 11.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (device.isDefault) "●" else "○", color = if (device.isDefault) Success else Muted, fontSize = 11.sp)
                            Spacer(Modifier.width(9.dp))
                            Text(
                                compactAudioOutputName(device.name),
                                color = TextColor,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onRefresh) { Text("REFRESH", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showOutputs = false }) { Text("CLOSE", color = Muted) }
            },
        )
    }

    editingFavorite?.let { slot ->
        AlertDialog(
            onDismissRequest = { editingFavorite = null },
            containerColor = Color(0xFF090D11),
            titleContentColor = TextColor,
            textContentColor = TextColor,
            title = {
                Text(if (slot == AudioFavoriteSlot.Headphones) "Choose Headphones" else "Choose TV / Display")
            },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    state.audioOutputs.forEach { device ->
                        Text(
                            text = compactAudioOutputName(device.name),
                            color = TextColor,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF05080B), RoundedCornerShape(10.dp))
                                .border(1.dp, Silver.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                                .clickable {
                                    onSetFavorite(slot, device.id)
                                    editingFavorite = null
                                }
                                .padding(11.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingFavorite = null }) { Text("CANCEL", color = Accent) }
            },
            dismissButton = {
                val assigned = if (slot == AudioFavoriteSlot.Headphones) {
                    state.favoriteHeadphones.assigned
                } else {
                    state.favoriteTvDisplay.assigned
                }
                if (assigned) {
                    TextButton(onClick = {
                        onClearFavorite(slot)
                        editingFavorite = null
                    }) { Text("CLEAR", color = Muted) }
                }
            },
        )
    }
}

@Composable
private fun AudioFavoriteCard(
    label: String,
    favorite: AudioOutputFavorite,
    state: LauncherHostState,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
    onEdit: () -> Unit,
) {
    val device = state.audioOutputs.firstOrNull { it.id.equals(favorite.endpointId, ignoreCase = true) }
    val available = device != null
    val isDefault = device?.isDefault == true
    val title = when {
        !favorite.assigned -> "Not set"
        favorite.name.isNotBlank() -> compactAudioOutputName(favorite.name)
        else -> "Saved output"
    }
    Column(
        modifier = modifier
            .background(if (isDefault) Accent.copy(alpha = 0.12f) else Color(0xFF05080B), RoundedCornerShape(12.dp))
            .border(1.dp, if (isDefault) Accent else Silver.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .clickable(enabled = favorite.assigned && available && state.connected) { onSelect(favorite.endpointId) }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("EDIT", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onEdit))
        }
        Text(title, color = if (favorite.assigned && !available) Muted else TextColor, fontSize = 10.sp, maxLines = 2)
        Text(
            when {
                !favorite.assigned -> "Choose an output"
                available && isDefault -> "ACTIVE"
                available -> "TAP TO SWITCH"
                else -> "UNAVAILABLE"
            },
            color = when {
                available && isDefault -> Success
                available -> Silver
                else -> Muted
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun compactAudioOutputName(name: String): String = name
    .removeSuffix(" (NVIDIA High Definition Audio)")
    .removeSuffix(" (SteelSeries Sonar Virtual Audio Device)")
    .replace(" (Realtek(R) Audio)", " (Realtek)")
    .trim()

@Composable
private fun LauncherHostStatus(
    state: LauncherHostState,
    onRetry: () -> Unit,
    onWake: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val statusColor = when {
        state.connected -> Success
        state.pairingRequired || state.connecting || state.waking -> Accent
        else -> Muted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090D11), shape)
            .border(1.dp, Accent.copy(alpha = 0.72f), shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("●", color = statusColor, fontSize = 12.sp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (state.connected) "WINDOWS LAUNCHER HOST" else "WINDOWS HOST",
                color = TextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.message,
                color = Muted,
                fontSize = 10.sp,
                maxLines = 2,
            )
        }
        if (!state.connected) {
            val wakeAvailable = state.trusted && state.wakeMacAddress.isNotBlank()
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        state.waking -> "WAKING"
                        state.connecting -> "CONNECTING"
                        wakeAvailable -> "WAKE PC"
                        else -> "RETRY"
                    },
                    color = Accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        enabled = !state.connecting && !state.waking,
                        onClick = if (wakeAvailable) onWake else onRetry,
                    ),
                )
                if (wakeAvailable && !state.connecting && !state.waking) {
                    Text(
                        text = "RETRY",
                        color = Muted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clickable(onClick = onRetry),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryLauncherTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    HeroLauncherTile(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        accent = Cyan,
        height = 124.dp,
        iconSize = 88.dp,
        titleSize = 28.sp,
        running = running,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun FeaturedLauncherTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    HeroLauncherTile(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        accent = Purple,
        height = 108.dp,
        iconSize = 76.dp,
        titleSize = 23.sp,
        running = false,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun HeroLauncherTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    height: Dp,
    iconSize: Dp,
    titleSize: TextUnit,
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val alpha = if (enabled) 1f else 0.42f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer(alpha = alpha)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.76f),
                spotColor = Color.Black.copy(alpha = 0.34f),
            )
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF111820), Color(0xFF030608), Color(0xFF0B1117)),
                ),
                shape = shape,
            )
            .border(1.35.dp, Accent.copy(alpha = 0.96f), shape)
            .padding(2.dp)
            .border(0.75.dp, Accent.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 1.dp.toPx())
            drawArc(
                color = accent.copy(alpha = 0.08f),
                startAngle = 212f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(size.width * 0.42f, size.height * -0.34f),
                size = Size(size.width * 0.78f, size.height * 1.20f),
                style = stroke,
            )
            drawArc(
                color = Silver.copy(alpha = 0.05f),
                startAngle = 203f,
                sweepAngle = 148f,
                useCenter = false,
                topLeft = Offset(size.width * 0.50f, size.height * -0.20f),
                size = Size(size.width * 0.64f, size.height * 1.02f),
                style = stroke,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCradle(
                iconRes = iconRes,
                description = title,
                size = iconSize,
                accent = accent,
            )
            Spacer(Modifier.width(17.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextColor,
                        fontSize = titleSize,
                        lineHeight = titleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (running) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "●",
                            color = Success,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = accent,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Text(
                text = "›",
                color = accent.copy(alpha = 0.96f),
                fontSize = 43.sp,
                lineHeight = 43.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun IconCradle(
    @DrawableRes iconRes: Int,
    description: String,
    size: Dp,
    accent: Color,
) {
    val shape = RoundedCornerShape(23.dp)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, shape, spotColor = Color.Black.copy(alpha = 0.42f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF202830), Color(0xFF030608)),
                ),
                shape = shape,
            )
            .border(1.15.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(4.dp)
            .border(0.65.dp, Accent.copy(alpha = 0.34f), RoundedCornerShape(20.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
private fun SecondaryLauncherTile(
    @DrawableRes iconRes: Int,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(19.dp)
    val alpha = if (enabled) 1f else 0.42f

    Column(
        modifier = modifier
            .height(112.dp)
            .graphicsLayer(alpha = alpha)
            .shadow(9.dp, shape, ambientColor = Color.Black.copy(alpha = 0.68f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF151C23), Color(0xFF040709)),
                ),
                shape = shape,
            )
            .border(1.1.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(2.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconCradle(
            iconRes = iconRes,
            description = label,
            size = 68.dp,
            accent = Accent,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = TextColor.copy(alpha = 0.88f),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommandDeckTile(
    glyph: CommandGlyphType,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val alpha = if (enabled) 1f else 0.38f

    Column(
        modifier = modifier
            .height(90.dp)
            .graphicsLayer(alpha = alpha)
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.68f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF182129), Color(0xFF040709)),
                ),
                shape = shape,
            )
            .border(1.1.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(2.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CommandGlyph(
            kind = glyph,
            color = TextColor.copy(alpha = alpha),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = TextColor.copy(alpha = alpha * 0.86f),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommandGlyph(
    kind: CommandGlyphType,
    color: Color,
) {
    Canvas(Modifier.size(36.dp)) {
        val strokeWidth = 2.3.dp.toPx()
        val thinStrokeWidth = 1.8.dp.toPx()

        when (kind) {
            CommandGlyphType.AltTab -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.10f, size.height * 0.29f),
                    size = Size(size.width * 0.54f, size.height * 0.47f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(strokeWidth),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.36f, size.height * 0.12f),
                    size = Size(size.width * 0.54f, size.height * 0.47f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(strokeWidth),
                )
            }

            CommandGlyphType.Task -> {
                repeat(3) { index ->
                    val y = size.height * (0.22f + index * 0.27f)
                    drawLine(
                        color,
                        Offset(size.width * 0.12f, y),
                        Offset(size.width * 0.19f, y + size.height * 0.07f),
                        thinStrokeWidth,
                    )
                    drawLine(
                        color,
                        Offset(size.width * 0.19f, y + size.height * 0.07f),
                        Offset(size.width * 0.29f, y - size.height * 0.05f),
                        thinStrokeWidth,
                    )
                    drawLine(
                        color,
                        Offset(size.width * 0.39f, y),
                        Offset(size.width * 0.88f, y),
                        strokeWidth,
                    )
                }
            }

            CommandGlyphType.Show -> {
                val eye = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.50f)
                    quadraticTo(
                        size.width * 0.50f,
                        size.height * 0.10f,
                        size.width * 0.92f,
                        size.height * 0.50f,
                    )
                    quadraticTo(
                        size.width * 0.50f,
                        size.height * 0.90f,
                        size.width * 0.08f,
                        size.height * 0.50f,
                    )
                    close()
                }
                drawPath(eye, color, style = Stroke(strokeWidth))
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.12f,
                    center = center,
                    style = Stroke(strokeWidth),
                )
            }

            CommandGlyphType.Close -> {
                drawLine(
                    color,
                    Offset(size.width * 0.17f, size.height * 0.17f),
                    Offset(size.width * 0.83f, size.height * 0.83f),
                    strokeWidth,
                )
                drawLine(
                    color,
                    Offset(size.width * 0.83f, size.height * 0.17f),
                    Offset(size.width * 0.17f, size.height * 0.83f),
                    strokeWidth,
                )
            }
        }
    }
}
