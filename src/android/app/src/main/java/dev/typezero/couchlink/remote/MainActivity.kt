package dev.typezero.couchlink.remote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.typezero.couchlink.remote.hid.BluetoothHidController
import dev.typezero.couchlink.remote.hid.BluetoothHidRuntime
import dev.typezero.couchlink.remote.hid.BluetoothHidService
import dev.typezero.couchlink.remote.hid.MouseAction
import dev.typezero.couchlink.remote.hid.MouseButton
import dev.typezero.couchlink.remote.host.LauncherHostRuntime
import dev.typezero.couchlink.remote.model.AppScreen
import dev.typezero.couchlink.remote.model.LauncherId
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import dev.typezero.couchlink.remote.ui.components.BottomNav
import dev.typezero.couchlink.remote.ui.components.ConnectionOverview
import dev.typezero.couchlink.remote.ui.components.PremiumHeader
import dev.typezero.couchlink.remote.ui.screens.HomeScreen
import dev.typezero.couchlink.remote.ui.screens.KeyboardScreen
import dev.typezero.couchlink.remote.ui.screens.SettingsScreen
import dev.typezero.couchlink.remote.ui.screens.TouchpadScreen
import dev.typezero.couchlink.remote.ui.screens.TvRemoteScreen
import dev.typezero.couchlink.remote.ui.theme.CouchLinkTheme
import dev.typezero.couchlink.remote.ui.theme.SurfaceColor

private const val UI_PREFERENCES = "couchlink_ui"
private const val HAPTICS_PREFERENCE = "haptics"
private const val NATURAL_SCROLLING_PREFERENCE = "natural_scrolling"
private const val MAX_KEYBOARD_TEXT_LENGTH = 1_024
private const val DISCOVERABLE_DURATION_SECONDS = 300
private const val EA_DESKTOP_COMMAND =
    "%ProgramFiles%\\Electronic Arts\\EA Desktop\\EA Desktop\\EADesktop.exe"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CouchLinkTheme {
                CouchLinkApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val controller = BluetoothHidRuntime.controller(applicationContext)
        if (controller.hasRequiredPermissions()) {
            BluetoothHidService.start(this)
        }
        controller.start()
    }
}

@Composable
private fun CouchLinkApp() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hidController = remember(context) {
        BluetoothHidRuntime.controller(context.applicationContext)
    }
    val hidState by hidController.state.collectAsState()
    val launcherHost = remember(context) { LauncherHostRuntime.client(context.applicationContext) }
    val launcherHostState by launcherHost.state.collectAsState()
    val tvDiscovery = remember(context) { TvDiscoveryController(context.applicationContext) }
    val tvState by tvDiscovery.state.collectAsState()
    var pairingCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(launcherHost) {
        // LauncherHostRuntime is process-wide. Keep its trusted session alive across
        // Activity recreation instead of stopping it when Compose disposes on rotation.
        launcherHost.start()
    }
    var pendingDiscoverability by remember { mutableStateOf(false) }

    DisposableEffect(tvDiscovery) {
        onDispose { tvDiscovery.close() }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && hidController.hasRequiredPermissions()) {
            BluetoothHidService.start(context)
            hidController.start()
        } else {
            hidController.start()
        }
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        BluetoothHidService.start(context)
        hidController.refreshPairedHosts()
        hidController.start()
    }

    val advertisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingDiscoverability) {
            pendingDiscoverability = false
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    DISCOVERABLE_DURATION_SECONDS,
                )
            discoverableLauncher.launch(intent)
        } else {
            pendingDiscoverability = false
        }
    }

    var screenName by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    val screen = runCatching { AppScreen.valueOf(screenName) }.getOrDefault(AppScreen.Home)
    var sensitivity by rememberSaveable { mutableFloatStateOf(1.35f) }
    var dragLock by remember { mutableStateOf(false) }
    var keyboardText by rememberSaveable { mutableStateOf("") }
    var statusExpanded by rememberSaveable { mutableStateOf(false) }

    val appPreferences = remember(context) {
        context.getSharedPreferences(UI_PREFERENCES, Context.MODE_PRIVATE)
    }
    var hapticsEnabled by remember {
        mutableStateOf(appPreferences.getBoolean(HAPTICS_PREFERENCE, true))
    }
    var naturalScrolling by remember {
        mutableStateOf(appPreferences.getBoolean(NATURAL_SCROLLING_PREFERENCE, true))
    }

    fun requestBluetoothHidPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            BluetoothHidService.start(context)
            hidController.start()
        }
    }

    fun makeBluetoothDiscoverable() {
        // Making the phone discoverable is the only action that needs
        // BLUETOOTH_ADVERTISE. Request it here instead of gating all HID input on
        // it, so core keyboard/mouse use keeps working even if advertise is denied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hidController.hasAdvertisePermission()
        ) {
            pendingDiscoverability = true
            advertisePermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
            return
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                DISCOVERABLE_DURATION_SECONDS,
            )
        discoverableLauncher.launch(intent)
    }

    fun releaseDrag() {
        if (!dragLock) return
        dragLock = false
        hidController.mouseButton(MouseButton.Left, MouseAction.Up)
    }

    LaunchedEffect(hidState.connected, screen) {
        if (!hidState.connected || screen != AppScreen.Touchpad) {
            releaseDrag()
        }
    }

    if (launcherHostState.pairingRequired) {
        AlertDialog(
            onDismissRequest = {
                launcherHost.cancelPairing()
                pairingCode = ""
            },
            title = { Text("Pair with ${launcherHostState.hostName.ifBlank { "Windows Host" }}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the six-digit code shown in the CouchLink Windows host.")
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it.filter(Char::isDigit).take(6) },
                        label = { Text("Pairing code") },
                        singleLine = true,
                    )
                    Text(launcherHostState.message)
                }
            },
            confirmButton = {
                Button(
                    enabled = pairingCode.length == 6,
                    onClick = {
                        launcherHost.submitPairingCode(pairingCode)
                        pairingCode = ""
                    },
                ) { Text("Pair") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        launcherHost.cancelPairing()
                        pairingCode = ""
                    },
                ) { Text("Not now") }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 18.dp,
                        top = 8.dp,
                        end = 18.dp,
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PremiumHeader(bluetoothState = hidState)
                ConnectionOverview(
                    bluetoothState = hidState,
                    expanded = statusExpanded,
                    onToggle = { statusExpanded = !statusExpanded },
                )

                when (screen) {
                    AppScreen.Home -> HomeScreen(
                        launcherHostState = launcherHostState,
                        launcherEnabled = launcherHostState.connected || hidState.connected,
                        inputEnabled = hidState.connected,
                        onLauncher = { launcher ->
                            if (hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (!launcherHost.launch(
                                    launcher = launcher,
                                    onSendFailed = { hidController.launchLauncher(launcher) },
                                )
                            ) {
                                hidController.launchLauncher(launcher)
                            }
                        },
                        onRetryLauncherHost = launcherHost::retry,
                        onShortcut = { shortcut ->
                            if (hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            hidController.pressShortcut(shortcut)
                        },
                    )

                    AppScreen.Touchpad -> TouchpadScreen(
                        enabled = hidState.connected,
                        sensitivity = sensitivity,
                        onSensitivityChanged = { sensitivity = it },
                        dragLock = dragLock,
                        onMove = { deltaX, deltaY ->
                            hidController.moveRelative(deltaX, deltaY)
                        },
                        onButton = { button, action ->
                            hidController.mouseButton(button, action)
                        },
                        onScroll = { delta ->
                            hidController.scroll(if (naturalScrolling) delta else -delta)
                        },
                        onDragToggle = {
                            dragLock = !dragLock
                            if (hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            hidController.mouseButton(
                                MouseButton.Left,
                                if (dragLock) MouseAction.Down else MouseAction.Up,
                            )
                        },
                    )

                    AppScreen.Keyboard -> KeyboardScreen(
                        enabled = hidState.connected,
                        text = keyboardText,
                        onTextChanged = { keyboardText = it.take(MAX_KEYBOARD_TEXT_LENGTH) },
                        onSend = {
                            val textToSend = keyboardText
                            if (textToSend.isNotEmpty() && hidController.sendText(textToSend)) {
                                keyboardText = ""
                            }
                        },
                        onKey = { key -> hidController.pressKey(key) },
                        onShortcut = { shortcut -> hidController.pressShortcut(shortcut) },
                    )

                    AppScreen.TvRemote -> TvRemoteScreen(
                        state = tvState,
                        onConnect = tvDiscovery::connectRemote,
                        onKey = tvDiscovery::sendKey,
                    )

                    AppScreen.Settings -> SettingsScreen(
                        hidState = hidState,
                        launcherHostState = launcherHostState,
                        tvState = tvState,
                        hapticsEnabled = hapticsEnabled,
                        onHapticsChanged = { enabled ->
                            hapticsEnabled = enabled
                            appPreferences.edit()
                                .putBoolean(HAPTICS_PREFERENCE, enabled)
                                .apply()
                        },
                        naturalScrolling = naturalScrolling,
                        onNaturalScrollingChanged = { enabled ->
                            naturalScrolling = enabled
                            appPreferences.edit()
                                .putBoolean(NATURAL_SCROLLING_PREFERENCE, enabled)
                                .apply()
                        },
                        onRequestBluetoothPermission = ::requestBluetoothHidPermission,
                        onMakeDiscoverable = ::makeBluetoothDiscoverable,
                        onRefreshBluetoothHosts = hidController::refreshPairedHosts,
                        onConnectBluetoothHost = hidController::connect,
                        onDisconnectBluetoothHost = hidController::disconnect,
                        onReconnectLauncherHost = launcherHost::retry,
                        onPairLauncherHost = launcherHost::pairWithHost,
                        onForgetLauncherHost = launcherHost::forgetTrustedHost,
                        onTvScan = tvDiscovery::startDiscovery,
                        onTvStopScan = tvDiscovery::stopDiscovery,
                        onTvSelect = tvDiscovery::select,
                        onTvSelectManual = tvDiscovery::selectManual,
                        onTvProbe = tvDiscovery::probeSelected,
                        onTvBeginPairing = tvDiscovery::beginPairing,
                        onTvFinishPairing = tvDiscovery::finishPairing,
                        onTvCancelPairing = tvDiscovery::cancelPairing,
                        onTvConnect = tvDiscovery::connectRemote,
                        onTvForget = tvDiscovery::forgetTv,
                    )
                }
            }

            BottomNav(
                current = screen,
                onSelect = { destination -> screenName = destination.name },
            )
        }
    }
}

private fun BluetoothHidController.launchLauncher(
    launcher: LauncherId,
): Boolean = when (launcher) {
    LauncherId.Steam -> launchWindowsRunCommand("steam://open/bigpicture")
    LauncherId.Ea -> launchWindowsRunCommand(EA_DESKTOP_COMMAND)
    else -> launchWindowsApp(launcher.displayName)
}
