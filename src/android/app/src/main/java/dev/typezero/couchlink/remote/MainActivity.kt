package dev.typezero.couchlink.remote

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.network.DiscoveredHost
import dev.typezero.couchlink.remote.network.DiscoveryClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val SurfaceColor = Color(0xFF0B0D10)
private val Panel = Color(0xFF15191E)
private val Raised = Color(0xFF1D2229)
private val Raised2 = Color(0xFF252B33)
private val Text = Color(0xFFF5F6F8)
private val Muted = Color(0xFFA5ABB5)
private val Accent = Color(0xFFFF881D)
private val Success = Color(0xFF62D273)
private val Danger = Color(0xFFFF6B6B)

enum class AppScreen {
    Home,
    Touchpad,
    Keyboard,
    Settings,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = SurfaceColor,
                    surface = Panel,
                    primary = Accent,
                ),
            ) {
                CouchLinkApp()
            }
        }
    }
}

@Composable
private fun CouchLinkApp() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val client = remember { DiscoveryClient(context) }
    val scope = rememberCoroutineScope()

    var host by remember {
        mutableStateOf<DiscoveredHost?>(client.lastKnownHost())
    }
    var status by remember {
        mutableStateOf("Listening for CouchLink Host…")
    }
    var connected by remember { mutableStateOf(false) }
    var remoteInputEnabled by remember { mutableStateOf(false) }
    var pairingRequired by remember { mutableStateOf(false) }
    var paired by remember { mutableStateOf(false) }
    var pairingCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(AppScreen.Home) }
    var sensitivity by remember { mutableFloatStateOf(1.35f) }
    var dragLock by remember { mutableStateOf(false) }
    var keyboardText by remember { mutableStateOf("") }
    var sessionJob by remember { mutableStateOf<Job?>(null) }
    var sessionHostKey by remember { mutableStateOf<String?>(null) }
    var statusExpanded by remember { mutableStateOf(false) }
    var pairingRequestedForHostId by remember {
        mutableStateOf<String?>(null)
    }

    val appPrefs = remember {
        context.getSharedPreferences(
            "couchlink_ui",
            Context.MODE_PRIVATE,
        )
    }

    var hapticsEnabled by remember {
        mutableStateOf(
            appPrefs.getBoolean("haptics", true),
        )
    }
    var naturalScrolling by remember {
        mutableStateOf(
            appPrefs.getBoolean("natural_scrolling", true),
        )
    }

    fun releaseDrag() {
        if (!dragLock) {
            return
        }

        dragLock = false

        scope.launch {
            client.sendMouseButton("left", "up")
        }
    }

    fun startSession(discovered: DiscoveredHost) {
        /*
         * The desktop Session Host and Boot Service advertise different ports
         * for the same Windows machine. Endpoint selection belongs inside
         * DiscoveryClient, so a discovery-port change must not restart an
         * otherwise healthy persistent session.
         */
        val hostKey = "${discovered.hostId}@${discovered.address}"

        if (
            sessionJob?.isActive == true &&
            sessionHostKey == hostKey
        ) {
            return
        }

        sessionJob?.cancel()
        client.disconnect()

        sessionHostKey = hostKey
        paired = true

        sessionJob = scope.launch {
            client.runPersistentSession(discovered) {
                    message,
                    isConnected,
                    inputEnabled,
                ->

                /*
                 * DiscoveryClient invokes this callback from its IO context.
                 * Move Compose state updates back onto the remembered UI scope.
                 */
                scope.launch {
                    status = message
                    connected = isConnected
                    remoteInputEnabled = inputEnabled

                    if (!isConnected || !inputEnabled) {
                        releaseDrag()
                    }
                }
            }
        }
    }

    LaunchedEffect(client) {
        client.listen { discovered ->
            /*
             * UDP discovery runs on Dispatchers.IO. Keep all Compose state and
             * session lifecycle decisions on the UI scope.
             */
            scope.launch {
                host = discovered

                if (client.hasTrustedToken(discovered)) {
                    paired = true
                    pairingRequired = false
                    pairingRequestedForHostId = null
                    startSession(discovered)
                    return@launch
                }

                if (
                    pairingRequestedForHostId ==
                    discovered.hostId
                ) {
                    return@launch
                }

                pairingRequestedForHostId = discovered.hostId
                status =
                    "Requesting a new pairing code from " +
                    "${discovered.hostName}…"

                runCatching {
                    client.beginPairing(discovered)
                }.onSuccess { hello ->
                    pairingRequired = hello.pairingRequired
                    paired = hello.trusted

                    status = if (hello.pairingRequired) {
                        "Enter the six-digit code shown on " +
                            "${discovered.hostName}."
                    } else {
                        "Trusted link restored with " +
                            "${discovered.hostName}."
                    }

                    if (hello.trusted) {
                        pairingRequestedForHostId = null
                        startSession(discovered)
                    }
                }.onFailure { error ->
                    pairingRequestedForHostId = null
                    status =
                        "Pairing request failed: " +
                        (error.message ?: "connection error")
                }
            }
        }
    }

    DisposableEffect(client) {
        onDispose {
            client.disconnect()
            sessionJob?.cancel()
            sessionJob = null
            sessionHostKey = null
        }
    }

    Surface(Modifier.fillMaxSize(), color = SurfaceColor) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumHeader(connected, remoteInputEnabled)
                HostCard(host, connected, remoteInputEnabled)
                CompactStatusStrip(
                    status = status,
                    connected = connected,
                    enabled = remoteInputEnabled,
                    expanded = statusExpanded,
                    onToggle = { statusExpanded = !statusExpanded }
                )

                val discovered = host
                if (discovered == null) {
                    PremiumPanel {
                        CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Searching the local network", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("Keep CouchLink Host open on the PC.", color = Muted)
                    }
                } else if (!paired && pairingRequired) {
                    PairingPanel(pairingCode, { pairingCode = it.filter(Char::isDigit).take(6) }, busy) {
                        busy = true
                        scope.launch {
                            runCatching { client.pair(discovered, pairingCode) }
                                .onSuccess {
                                    paired = it.success
                                    pairingRequired = !it.success
                                    status = it.message
                                    if (it.success) startSession(discovered)
                                }.onFailure { status = "Pairing failed: ${it.message}" }
                            busy = false
                        }
                    }
                } else if (connected && discovered.hostState == "SignInRequired") {
                    when (screen) {
                        AppScreen.Home -> PreLoginPanel(discovered)
                        AppScreen.Touchpad -> TouchpadScreen(
                            remoteInputEnabled, sensitivity, { sensitivity = it }, dragLock,
                            onMove = { x, y -> scope.launch { client.sendMouseMove(x, y) } },
                            onButton = { button, action -> scope.launch { client.sendMouseButton(button, action) } },
                            onScroll = { delta -> scope.launch { client.sendMouseScroll(if (naturalScrolling) delta else -delta) } },
                            onDragToggle = {
                                dragLock = !dragLock
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { client.sendMouseButton("left", if (dragLock) "down" else "up") }
                            }
                        )
                        AppScreen.Keyboard -> KeyboardScreen(
                            remoteInputEnabled, keyboardText, { keyboardText = it.filter(Char::isDigit).take(32) },
                            onSend = { val text = keyboardText; keyboardText = ""; scope.launch { client.sendKeyboardText(text) } },
                            onKey = { scope.launch { client.sendKeyPress(it) } },
                            onShortcut = { }
                        )
                        AppScreen.Settings -> SettingsScreen(
                            host = discovered,
                            hapticsEnabled = hapticsEnabled,
                            onHapticsChanged = { hapticsEnabled = it; appPrefs.edit().putBoolean("haptics", it).apply() },
                            naturalScrolling = naturalScrolling,
                            onNaturalScrollingChanged = { naturalScrolling = it; appPrefs.edit().putBoolean("natural_scrolling", it).apply() },
                            onWake = { scope.launch {
                                status = if (client.sendWakeOnLan(discovered)) "Wake packet sent to ${discovered.hostName}." else "Wake-on-LAN is unavailable for this host."
                            } }
                        )
                    }
                } else if (connected || screen == AppScreen.Settings) {
                    when (screen) {
                        AppScreen.Home -> HomeScreen(
                            onLauncher = { launcher -> if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { client.sendLauncherAction(launcher) } },
                            onShortcut = { shortcut -> if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); scope.launch { client.sendShortcut(shortcut) } },
                            inputEnabled = remoteInputEnabled
                        )
                        AppScreen.Touchpad -> TouchpadScreen(
                            remoteInputEnabled, sensitivity, { sensitivity = it }, dragLock,
                            onMove = { x, y -> scope.launch { client.sendMouseMove(x, y) } },
                            onButton = { button, action -> scope.launch { client.sendMouseButton(button, action) } },
                            onScroll = { delta -> scope.launch { client.sendMouseScroll(if (naturalScrolling) delta else -delta) } },
                            onDragToggle = {
                                dragLock = !dragLock
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { client.sendMouseButton("left", if (dragLock) "down" else "up") }
                            }
                        )
                        AppScreen.Keyboard -> KeyboardScreen(
                            remoteInputEnabled, keyboardText, { keyboardText = it.take(1024) },
                            onSend = { val text = keyboardText; keyboardText = ""; scope.launch { client.sendKeyboardText(text) } },
                            onKey = { scope.launch { client.sendKeyPress(it) } },
                            onShortcut = { scope.launch { client.sendShortcut(it) } }
                        )
                        AppScreen.Settings -> SettingsScreen(
                            host = discovered,
                            hapticsEnabled = hapticsEnabled,
                            onHapticsChanged = { hapticsEnabled = it; appPrefs.edit().putBoolean("haptics", it).apply() },
                            naturalScrolling = naturalScrolling,
                            onNaturalScrollingChanged = { naturalScrolling = it; appPrefs.edit().putBoolean("natural_scrolling", it).apply() },
                            onWake = { scope.launch {
                                status = if (client.sendWakeOnLan(discovered)) "Wake packet sent to ${discovered.hostName}. Waiting for Windows…" else "Wake-on-LAN is unavailable for this host."
                            } }
                        )
                    }
                }

            }
            if (host != null) BottomNav(screen) { screen = it }
        }
    }
}

@Composable
private fun PreLoginPanel(host: DiscoveredHost) {
    PremiumPanel {
        Text("WINDOWS SIGN-IN", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Connected before login", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "${host.hostName} is awake and waiting at the Windows sign-in screen.",
            color = Muted,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(14.dp))
        Surface(color = Raised2, shape = RoundedCornerShape(14.dp)) {
            Text(
                "Secure sign-in input is intentionally unavailable in this foundation build.",
                modifier = Modifier.padding(14.dp),
                color = Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("CouchLink will hand off automatically after Windows login.", color = Success, fontSize = 13.sp)
    }
}

@Composable
private fun PremiumHeader(connected: Boolean, inputEnabled: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(dev.typezero.couchlink.remote.R.drawable.couchlink_logo),
            contentDescription = "CouchLink",
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.width(12.dp))
        Row {
            Text("Couch", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Text("Link", color = Accent, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Text(
            when { !connected -> "CONNECTING"; inputEnabled -> "READY"; else -> "INPUT LOCKED" },
            color = when { !connected -> Muted; inputEnabled -> Success; else -> Danger },
            fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HostCard(host: DiscoveredHost?, connected: Boolean, inputEnabled: Boolean) {
    if (host == null) return
    Box(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp))
            .border(1.dp, if (connected) Success.copy(alpha = .8f) else Accent.copy(alpha = .8f), RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Raised, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text("PC", color = Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(host.hostName, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Text("${host.hostState}  •  ${host.address}", color = if (connected) Success else Muted, fontSize = 12.sp)
            }
            if (connected) Text(if (inputEnabled) "●" else "○", color = if (inputEnabled) Success else Danger, fontSize = 18.sp)
        }
    }
}

@Composable
private fun PairingPanel(code: String, onCode: (String) -> Unit, busy: Boolean, onPair: () -> Unit) {
    PremiumPanel {
        Text("Secure pairing", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        Text("Enter the six-digit code shown by CouchLink Host.", color = Muted)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(code, onCode, label = { Text("Pairing code") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
        Button(onClick = onPair, enabled = code.length == 6 && !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
            Text("TRUST THIS DEVICE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeScreen(onLauncher: (String) -> Unit, onShortcut: (String) -> Unit, inputEnabled: Boolean) {
    Text("Launchers", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

    PrimaryLauncherTile(
        iconRes = R.drawable.launcher_steam,
        title = "Steam",
        subtitle = "Launch Big Picture",
        running = false,
        onClick = { onLauncher("steam") }
    )

    FeaturedLauncherTile(
        iconRes = R.drawable.launcher_gog,
        title = "GOG Galaxy",
        subtitle = "Open your DRM-free library",
        onClick = { onLauncher("gog") }
    )

    val launchers = listOf(
        Triple("Xbox", "xbox", R.drawable.launcher_xbox),
        Triple("EA app", "ea", R.drawable.launcher_ea),
        Triple("Ubisoft", "ubisoft", R.drawable.launcher_ubisoft),
        Triple("Rockstar", "rockstar", R.drawable.launcher_rockstar),
        Triple("Epic", "epic", R.drawable.launcher_epic),
        Triple("Amazon", "amazon", R.drawable.launcher_amazon)
    )
    launchers.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (label, id, icon) ->
                SecondaryLauncherTile(
                    iconRes = icon,
                    label = label,
                    modifier = Modifier.weight(1f),
                    onClick = { onLauncher(id) }
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Text("Command Deck", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val commands = listOf(
        Triple("Alt+Tab", "alt_tab", R.drawable.command_alt_tab),
        Triple("Task Manager", "task_manager", R.drawable.command_task),
        Triple("Show Desktop", "show_desktop", R.drawable.command_desktop),
        Triple("Close Window", "close_window", R.drawable.command_close)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        commands.forEach { (label, id, icon) ->
            CommandDeckTile(
                iconRes = icon,
                label = label,
                enabled = inputEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onShortcut(id) }
            )
        }
    }
}

@Composable
private fun PrimaryLauncherTile(
    iconRes: Int,
    title: String,
    subtitle: String,
    running: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().height(104.dp)
            .shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = .52f), spotColor = Accent.copy(alpha = .22f))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF27445F), Color(0xFF15283B))),
                shape
            )
            .border(1.4.dp, Accent.copy(alpha = .95f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(70.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = .55f))
                    .background(Color(0xFF101820), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(24.dp))
                    .padding(7.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    if (running) {
                        Spacer(Modifier.width(8.dp))
                        Text("●", color = Success, fontSize = 12.sp)
                    }
                }
                Text(subtitle, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("›", color = Text, fontSize = 36.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun FeaturedLauncherTile(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.fillMaxWidth().height(88.dp)
            .shadow(9.dp, shape, ambientColor = Color.Black.copy(alpha = .48f), spotColor = Color(0xFFB86CFF).copy(alpha = .16f))
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2037), Color(0xFF17151E))), shape)
            .border(1.2.dp, Color(0xFFB86CFF).copy(alpha = .72f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp)
                    .shadow(7.dp, RoundedCornerShape(19.dp), ambientColor = Color.Black.copy(alpha = .5f))
                    .background(Color(0xFF17131D), RoundedCornerShape(19.dp))
                    .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(19.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(iconRes), title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, color = Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFFD7A8FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text("›", color = Text, fontSize = 31.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun SecondaryLauncherTile(
    iconRes: Int,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.height(104.dp)
            .shadow(7.dp, shape, ambientColor = Color.Black.copy(alpha = .46f))
            .background(Brush.verticalGradient(listOf(Color(0xFF242A32), Color(0xFF171B21))), shape)
            .border(1.dp, Color.White.copy(alpha = .075f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(54.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun CommandDeckTile(
    iconRes: Int,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val alpha = if (enabled) 1f else .38f
    Column(
        modifier.height(94.dp)
            .shadow(if (enabled) 6.dp else 0.dp, shape, ambientColor = Color.Black.copy(alpha = .42f))
            .background(Brush.verticalGradient(listOf(Raised2.copy(alpha = alpha), Raised.copy(alpha = alpha))), shape)
            .border(1.dp, Color.White.copy(alpha = .06f * alpha), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(42.dp),
            alpha = alpha
        )
        Spacer(Modifier.height(5.dp))
        Text(label, color = Text.copy(alpha = alpha), textAlign = TextAlign.Center, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

@Composable
private fun TouchpadScreen(
    enabled: Boolean, sensitivity: Float, setSensitivity: (Float) -> Unit, dragLock: Boolean,
    onMove: (Int, Int) -> Unit, onButton: (String, String) -> Unit, onScroll: (Int) -> Unit, onDragToggle: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().height(330.dp).background(Panel, RoundedCornerShape(24.dp))
            .border(1.dp, if (enabled) Accent else Raised2, RoundedCornerShape(24.dp))
            .pointerInput(enabled, sensitivity) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, drag ->
                    change.consume(); val boost = if (abs(drag.x) + abs(drag.y) > 18f) 1.25f else 1f
                    onMove((drag.x * sensitivity * boost).roundToInt(), (drag.y * sensitivity * boost).roundToInt())
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onTap = { onButton("left", "click") }, onDoubleTap = { onButton("left", "click"); onButton("left", "click") }, onLongPress = { onButton("right", "click") })
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(); val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val dy = pressed.map { it.positionChange().y }.average().toFloat()
                            if (abs(dy) >= .8f) { pressed.forEach { it.consume() }; onScroll((-dy * 9f).roundToInt().coerceIn(-480, 480)) }
                        }
                    }
                }
            }, contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOUCHPAD", color = if (enabled) Text else Muted, fontWeight = FontWeight.Bold)
            Text(if (enabled) "Move • tap • hold • two-finger scroll" else "Enable remote input on Windows", color = Muted, fontSize = 12.sp)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sensitivity", color = Muted, fontSize = 12.sp)
        Slider(sensitivity, setSensitivity, Modifier.weight(1f).padding(horizontal = 8.dp), enabled = enabled, valueRange = .65f..2.4f)
        Text(String.format("%.2f×", sensitivity), fontSize = 12.sp)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton("LEFT", enabled, Modifier.weight(1f)) { onButton("left", "click") }
        ActionButton(if (dragLock) "RELEASE" else "DRAG LOCK", enabled, Modifier.weight(1f), dragLock) { onDragToggle() }
        ActionButton("RIGHT", enabled, Modifier.weight(1f)) { onButton("right", "click") }
    }
}

@Composable
private fun KeyboardScreen(enabled: Boolean, text: String, setText: (String) -> Unit, onSend: () -> Unit, onKey: (String) -> Unit, onShortcut: (String) -> Unit) {
    PremiumPanel {
        Text("Keyboard", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Send text or use the essential Windows keys below.", color = Muted)
        OutlinedTextField(text, setText, label = { Text("Type text to send") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(onClick = onSend, enabled = enabled && text.isNotEmpty(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Raised2, disabledContentColor = Muted)) {
            Text("SEND TEXT", color = if (enabled && text.isNotEmpty()) Color.Black else Muted, fontWeight = FontWeight.Bold)
        }
        KeyRows(enabled, onKey)
        Text("Windows shortcuts", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALT+TAB" to "alt_tab", "DESKTOP" to "show_desktop", "TASK MGR" to "task_manager").forEach { (label, id) ->
                ActionButton(label, enabled, Modifier.weight(1f)) { onShortcut(id) }
            }
        }
    }
}

@Composable
private fun KeyRows(enabled: Boolean, onKey: (String) -> Unit) {
    listOf(listOf("ESC" to "escape", "TAB" to "tab", "⌫" to "backspace", "ENTER" to "enter"), listOf("←" to "left", "↑" to "up", "↓" to "down", "→" to "right")).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (label, key) -> ActionButton(label, enabled, Modifier.weight(1f)) { onKey(key) } }
        }
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, modifier: Modifier = Modifier, active: Boolean = false, action: () -> Unit) {
    Button(action, enabled = enabled, modifier = modifier.height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Accent else Raised), contentPadding = PaddingValues(5.dp), shape = RoundedCornerShape(14.dp)) {
        Text(label, color = if (active) Color.Black else Text, textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompactStatusStrip(
    status: String,
    connected: Boolean,
    enabled: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val stateColor = when {
        !connected -> Muted
        enabled -> Success
        else -> Accent
    }
    val stateLabel = when {
        !connected -> "Connecting"
        enabled -> "Connected · Input enabled"
        else -> "Connected · Input locked"
    }

    Column(
        Modifier.fillMaxWidth()
            .background(Raised, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = stateColor, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(stateLabel, color = Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "⌃" else "⌄", color = Muted, fontSize = 14.sp)
        }
        if (expanded) {
            Text(status, color = Muted, fontSize = 11.sp)
            Text(
                if (connected) "Tap to collapse connection details." else "CouchLink is searching for the trusted host.",
                color = Muted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun BottomNav(current: AppScreen, onSelect: (AppScreen) -> Unit) {
    val destinations = listOf(
        Triple(AppScreen.Home, "⌂", "Home"),
        Triple(AppScreen.Touchpad, "◉", "Touchpad"),
        Triple(AppScreen.Keyboard, "⌨", "Keyboard"),
        Triple(AppScreen.Settings, "⚙", "Settings")
    )

    Surface(color = Color(0xFF101318), tonalElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { (screen, icon, label) ->
                val selected = current == screen
                TextButton(
                    onClick = { onSelect(screen) },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (selected) Accent else Muted),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(icon, fontSize = 17.sp, lineHeight = 18.sp)
                        Text(
                            label,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    host: DiscoveredHost,
    hapticsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    naturalScrolling: Boolean,
    onNaturalScrollingChanged: (Boolean) -> Unit,
    onWake: () -> Unit,
) {
    Text("Living-room settings", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    PremiumPanel {
        Text("Wake-on-LAN", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Wake ${host.hostName} from sleep or soft-off, then reconnect automatically.", color = Muted)
        Button(
            onClick = onWake,
            enabled = host.macAddress.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text("WAKE PC", color = Color.Black, fontWeight = FontWeight.Bold) }
        Text(if (host.macAddress.isBlank()) "No compatible network adapter was advertised." else "Adapter ready · ${host.macAddress.chunked(2).joinToString(":")}", color = Muted, fontSize = 11.sp)
    }
    PremiumPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Premium haptics", fontWeight = FontWeight.SemiBold); Text("Tactile confirmation for controls.", color = Muted, fontSize = 12.sp) }
            Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChanged)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Natural scrolling", fontWeight = FontWeight.SemiBold); Text("Match modern touchpad direction.", color = Muted, fontSize = 12.sp) }
            Switch(checked = naturalScrolling, onCheckedChange = onNaturalScrollingChanged)
        }
    }
    PremiumPanel {
        Text("Host", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(host.hostName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("${host.address}:${host.sessionPort}", color = Muted)
        Text("CouchLink Remote v0.1-dev.10.1", color = Accent, fontSize = 12.sp)
    }
}

@Composable
private fun PremiumPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}