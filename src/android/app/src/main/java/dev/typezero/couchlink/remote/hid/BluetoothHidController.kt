package dev.typezero.couchlink.remote.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.Executor

private const val HID_PREFERENCES = "couchlink_bluetooth_hid"
private const val KEY_DESIRED_HOST_ADDRESS = "desired_host_address"
private const val LAUNCH_INPUT_DELAY_MS = 500L
private const val LAUNCH_CONFIRM_DELAY_MS = 350L
private const val KEYSTROKE_INTERVAL_MS = 12L
private const val MAX_SCROLL_DELTA = 480
private val RECONNECT_DELAYS_MS = longArrayOf(1_000L, 2_000L, 5_000L, 10_000L, 15_000L)

/**
 * Android-side Bluetooth HID keyboard/mouse backend.
 *
 * Windows sees the phone as a normal Bluetooth keyboard and mouse, so input is
 * handled by the built-in Windows HID stack and remains available at Winlogon.
 * Launcher, pointer, and keyboard commands are sent directly to the paired
 * Windows PC through the built-in Bluetooth HID stack.
 */
internal class BluetoothHidController(private val context: Context) {
    data class Host(
        val name: String,
        val address: String,
    )

    data class State(
        val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
        val permissionGranted: Boolean = false,
        val bluetoothEnabled: Boolean = false,
        val profileReady: Boolean = false,
        val registered: Boolean = false,
        val connecting: Boolean = false,
        val connected: Boolean = false,
        val connectedHost: Host? = null,
        val pairedHosts: List<Host> = emptyList(),
        val message: String = "Bluetooth HID is not initialized.",
    )

    private val bluetoothManager =
        context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val executor = Executor { runnable ->
        context.mainExecutor.execute(runnable)
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var mouseButtons: Byte = 0
    private var registering = false
    private var profileRequestPending = false
    private val handler = Handler(Looper.getMainLooper())
    private val preferences = context.getSharedPreferences(
        HID_PREFERENCES,
        Context.MODE_PRIVATE,
    )
    private var desiredHostAddress: String? =
        preferences.getString(KEY_DESIRED_HOST_ADDRESS, null)
    private var reconnectAttempts = 0
    private val reconnectRunnable = Runnable { attemptReconnect() }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            profileRequestPending = false
            hidDevice = proxy as BluetoothHidDevice
            publish(
                profileReady = true,
                message = "Bluetooth HID profile ready.",
            )
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            profileRequestPending = false
            hidDevice = null
            connectedDevice = null
            registering = false
            publish(
                profileReady = false,
                registered = false,
                connecting = false,
                connected = false,
                connectedHost = null,
                message = "Bluetooth HID profile disconnected.",
            )
            if (!desiredHostAddress.isNullOrBlank()) scheduleReconnect()
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(
            pluggedDevice: BluetoothDevice?,
            registered: Boolean,
        ) {
            registering = false
            publish(
                registered = registered,
                message = if (registered) {
                    "Bluetooth HID is ready. Pair or connect a Windows PC."
                } else {
                    "Bluetooth HID registration was released."
                },
            )
            refreshPairedHosts()
            if (registered) scheduleReconnect(immediate = true)
        }

        override fun onConnectionStateChanged(
            device: BluetoothDevice,
            state: Int,
        ) {
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> {
                    publish(
                        connecting = true,
                        connected = false,
                        connectedHost = hostOf(device),
                        message = "Connecting Bluetooth HID to ${safeName(device)}…",
                    )
                }

                BluetoothProfile.STATE_CONNECTED -> {
                    handler.removeCallbacks(reconnectRunnable)
                    reconnectAttempts = 0
                    connectedDevice = device
                    desiredHostAddress = device.address
                    preferences.edit()
                        .putString(KEY_DESIRED_HOST_ADDRESS, device.address)
                        .apply()
                    publish(
                        connecting = false,
                        connected = true,
                        connectedHost = hostOf(device),
                        message = "Bluetooth HID connected to ${safeName(device)}.",
                    )
                    releaseAll()
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    publish(
                        connecting = false,
                        message = "Disconnecting Bluetooth HID…",
                    )
                }

                else -> {
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                    }
                    val shouldReconnect = !desiredHostAddress.isNullOrBlank()
                    publish(
                        connecting = false,
                        connected = false,
                        connectedHost = null,
                        message = if (shouldReconnect) {
                            "Bluetooth HID disconnected. Reconnecting…"
                        } else {
                            "Bluetooth HID disconnected."
                        },
                    )
                    if (shouldReconnect) scheduleReconnect()
                }
            }
        }

        override fun onGetReport(
            device: BluetoothDevice,
            type: Byte,
            id: Byte,
            bufferSize: Int,
        ) {
            val report = when (id.toInt()) {
                HidReport.KEYBOARD_ID -> ByteArray(HidReport.KEYBOARD_SIZE)
                HidReport.MOUSE_ID -> ByteArray(HidReport.MOUSE_SIZE)
                else -> null
            }
            val proxy = hidDevice ?: return
            if (report == null) {
                proxy.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            } else {
                proxy.replyReport(device, type, id, report)
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            // The descriptor supports report mode. Boot mode is accepted by the
            // platform, and the simple keyboard/mouse reports remain compatible.
            releaseAll()
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            handler.removeCallbacks(reconnectRunnable)
            desiredHostAddress = null
            preferences.edit().remove(KEY_DESIRED_HOST_ADDRESS).apply()
            connectedDevice = null
            publish(
                connecting = false,
                connected = false,
                connectedHost = null,
                message = "Windows removed the Bluetooth HID pairing.",
            )
            refreshPairedHosts()
        }
    }

    /**
     * Core HID input (registering, connecting, and sending reports) only needs
     * BLUETOOTH_CONNECT. BLUETOOTH_ADVERTISE is required solely for the
     * discoverability request used during first-time pairing, so it is checked
     * separately in [hasAdvertisePermission] rather than gating all input.
     */
    fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.checkSelfPermission(
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAdvertisePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.checkSelfPermission(
            Manifest.permission.BLUETOOTH_ADVERTISE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            publish(message = "Bluetooth HID requires Android 9 or newer.")
            return
        }
        if (!hasRequiredPermissions()) {
            publish(
                permissionGranted = false,
                message = "Nearby devices permission is required for Bluetooth HID.",
            )
            return
        }

        val currentAdapter = adapter
        if (currentAdapter == null) {
            publish(
                permissionGranted = true,
                bluetoothEnabled = false,
                message = "This Android device has no Bluetooth adapter.",
            )
            return
        }
        if (!currentAdapter.isEnabled) {
            publish(
                permissionGranted = true,
                bluetoothEnabled = false,
                message = "Turn on Bluetooth to use secure HID input.",
            )
            return
        }

        val currentState = _state.value
        if (currentState.connected && currentState.connectedHost != null) {
            publish(
                permissionGranted = true,
                bluetoothEnabled = true,
                message = "Bluetooth HID profile active.",
            )
            refreshPairedHosts()
            return
        }

        publish(
            permissionGranted = true,
            bluetoothEnabled = true,
            message = when {
                currentState.registered -> "Bluetooth HID profile active."
                currentState.profileReady -> "Registering Bluetooth HID input…"
                else -> "Opening Android Bluetooth HID profile…"
            },
        )
        refreshPairedHosts()
        when {
            hidDevice != null -> registerApp()
            profileRequestPending -> Unit
            else -> {
                profileRequestPending = currentAdapter.getProfileProxy(
                    context,
                    profileListener,
                    BluetoothProfile.HID_DEVICE,
                )
                if (!profileRequestPending) {
                    publish(message = "Android could not open the Bluetooth HID profile.")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        handler.removeCallbacks(reconnectRunnable)
        releaseAll()
        val proxy = hidDevice
        proxy?.unregisterApp()
        if (proxy != null) {
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
        }
        profileRequestPending = false
        hidDevice = null
        connectedDevice = null
        registering = false
        publish(
            profileReady = false,
            registered = false,
            connecting = false,
            connected = false,
            connectedHost = null,
            message = "Bluetooth HID stopped.",
        )
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val proxy = hidDevice ?: return
        if (_state.value.registered || registering) return
        registering = true

        val settings = BluetoothHidDeviceAppSdpSettings(
            "CouchLink Remote",
            "CouchLink Bluetooth keyboard and mouse",
            "Typezero",
            (BluetoothHidDevice.SUBCLASS1_COMBO.toInt() or
                BluetoothHidDevice.SUBCLASS2_UNCATEGORIZED.toInt()).toByte(),
            HidReport.REPORT_DESCRIPTOR,
        )

        val accepted = proxy.registerApp(
            settings,
            null,
            null,
            executor,
            callback,
        )
        if (!accepted) registering = false
        publish(
            message = if (accepted) {
                "Registering CouchLink as a Bluetooth keyboard and mouse…"
            } else {
                "Android rejected Bluetooth HID registration."
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedHosts() {
        if (!hasRequiredPermissions()) return
        val hosts = adapter?.bondedDevices
            ?.map(::hostOf)
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            .orEmpty()
        publish(pairedHosts = hosts)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        if (!hasRequiredPermissions()) return false
        val proxy = hidDevice ?: return false
        if (!_state.value.registered) return false
        val device = runCatching { adapter?.getRemoteDevice(address) }
            .getOrNull()
            ?: return false
        desiredHostAddress = address
        preferences.edit().putString(KEY_DESIRED_HOST_ADDRESS, address).apply()
        publish(
            connecting = true,
            message = "Requesting Bluetooth HID connection to ${safeName(device)}…",
        )
        val accepted = proxy.connect(device)
        if (!accepted) {
            publish(
                connecting = false,
                connectedHost = null,
                message = "Windows did not accept the Bluetooth HID connection request.",
            )
        }
        return accepted
    }

    @SuppressLint("MissingPermission")
    fun disconnect(): Boolean {
        handler.removeCallbacks(reconnectRunnable)
        desiredHostAddress = null
        reconnectAttempts = 0
        preferences.edit().remove(KEY_DESIRED_HOST_ADDRESS).apply()
        val device = connectedDevice
        if (device == null) {
            publish(
                connecting = false,
                connected = false,
                connectedHost = null,
                message = "Bluetooth HID disconnected.",
            )
            return false
        }
        val accepted = hidDevice?.disconnect(device) == true
        if (!accepted) {
            publish(
                connecting = false,
                message = "Android could not disconnect the Bluetooth HID session.",
            )
        }
        return accepted
    }

    @SuppressLint("MissingPermission")
    private fun attemptReconnect() {
        if (!hasRequiredPermissions()) return
        if (_state.value.connected || _state.value.connecting) return
        if (!_state.value.registered) {
            start()
            return
        }
        val address = desiredHostAddress ?: return
        val bonded = adapter?.bondedDevices
            ?.firstOrNull { it.address == address }
            ?: return
        publish(
            connecting = true,
            connectedHost = hostOf(bonded),
            message = "Reconnecting Bluetooth HID to ${safeName(bonded)}…",
        )
        val accepted = hidDevice?.connect(bonded) == true
        if (!accepted) {
            publish(connecting = false, message = "Bluetooth reconnect will retry.")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        val address = desiredHostAddress ?: return
        if (address.isBlank() || _state.value.connected) return
        handler.removeCallbacks(reconnectRunnable)
        val delay = if (immediate) {
            0L
        } else {
            val step = reconnectAttempts.coerceAtMost(RECONNECT_DELAYS_MS.lastIndex)
            reconnectAttempts = (step + 1).coerceAtMost(RECONNECT_DELAYS_MS.lastIndex)
            RECONNECT_DELAYS_MS[step]
        }
        handler.postDelayed(reconnectRunnable, delay)
    }

    fun moveRelative(deltaX: Int, deltaY: Int): Boolean {
        val dx = deltaX.coerceIn(-127, 127).toByte()
        val dy = deltaY.coerceIn(-127, 127).toByte()
        return sendReport(
            HidReport.MOUSE_ID,
            byteArrayOf(mouseButtons, dx, dy, 0),
        )
    }

    fun scroll(delta: Int): Boolean {
        // The HID wheel field is a single signed byte, so a fast swipe cannot be
        // expressed as one large value. Emit the scroll as a series of reports,
        // each carrying at most a full-scale +/-127 step, so momentum is
        // preserved instead of being flattened to a single notch.
        if (delta == 0) {
            return sendReport(HidReport.MOUSE_ID, byteArrayOf(mouseButtons, 0, 0, 0))
        }
        var remaining = delta.coerceIn(-MAX_SCROLL_DELTA, MAX_SCROLL_DELTA)
        var allSent = true
        while (remaining != 0) {
            val chunk = if (remaining > 0) {
                remaining.coerceAtMost(127)
            } else {
                remaining.coerceAtLeast(-127)
            }
            val sent = sendReport(
                HidReport.MOUSE_ID,
                byteArrayOf(mouseButtons, 0, 0, chunk.toByte()),
            )
            if (!sent) {
                allSent = false
                break
            }
            remaining -= chunk
        }
        return allSent
    }

    fun mouseButton(
        button: MouseButton,
        action: MouseAction,
    ): Boolean {
        val mask = button.mask
        return when (action) {
            MouseAction.Down -> {
                mouseButtons = (mouseButtons.toInt() or mask).toByte()
                sendMouseState()
            }

            MouseAction.Up -> {
                mouseButtons = (mouseButtons.toInt() and mask.inv()).toByte()
                sendMouseState()
            }

            MouseAction.Click -> {
                val pressed = (mouseButtons.toInt() or mask).toByte()
                val pressSent = sendReport(
                    HidReport.MOUSE_ID,
                    byteArrayOf(pressed, 0, 0, 0),
                )
                val releaseSent = sendMouseState()
                pressSent && releaseSent
            }
        }
    }

    /**
     * Types [text] on the paired host. Every character is validated up front, so
     * an unmappable character rejects the whole request without sending a partial
     * string. Keystrokes are then paced by [KEYSTROKE_INTERVAL_MS] to avoid
     * overrunning the Bluetooth send queue and silently dropping characters on
     * long input. Returns true once the text is accepted and queued.
     */
    fun sendText(text: String): Boolean = sendText(text, onComplete = null)

    private fun sendText(text: String, onComplete: (() -> Unit)?): Boolean {
        if (text.isEmpty() || !_state.value.connected) return false
        val strokes = ArrayList<KeyStroke>(text.length)
        for (character in text) {
            val stroke = HidKeyMap.fromCharacter(character) ?: return false
            strokes.add(stroke)
        }
        dispatchStrokes(strokes, 0, onComplete)
        return true
    }

    private fun dispatchStrokes(
        strokes: List<KeyStroke>,
        index: Int,
        onComplete: (() -> Unit)?,
    ) {
        if (index >= strokes.size) {
            onComplete?.invoke()
            return
        }
        if (!_state.value.connected) return
        val stroke = strokes[index]
        press(stroke.modifiers, stroke.usage)
        handler.postDelayed(
            { dispatchStrokes(strokes, index + 1, onComplete) },
            KEYSTROKE_INTERVAL_MS,
        )
    }

    fun pressKey(key: RemoteKey): Boolean = press(0, key.usage)

    fun pressShortcut(shortcut: WindowsShortcut): Boolean =
        press(shortcut.modifiers, shortcut.usage)

    /**
     * Opens the Windows Start menu and searches for an installed application.
     * Used by launcher tiles that rely on Windows Start Search.
     */
    fun launchWindowsApp(searchName: String): Boolean {
        val query = searchName.trim()
        if (query.isEmpty() || !_state.value.connected) return false
        if (query.any { HidKeyMap.fromCharacter(it) == null }) return false

        // Tap the Windows key, allow Start Search to become ready, type the
        // application name, then press Enter. Delays avoid losing characters
        // while the Start menu is animating open.
        if (!press(HidModifier.LEFT_GUI, 0)) return false
        handler.postDelayed({
            if (!_state.value.connected) return@postDelayed
            sendText(query) {
                handler.postDelayed({
                    if (_state.value.connected) pressKey(RemoteKey.Enter)
                }, LAUNCH_CONFIRM_DELAY_MS)
            }
        }, LAUNCH_INPUT_DELAY_MS)
        return true
    }

    /**
     * Opens the Windows Run dialog and executes a command or registered URI.
     * Used for exact Bluetooth-only launch actions such as Steam Big Picture.
     */
    fun launchWindowsRunCommand(command: String): Boolean {
        val query = command.trim()
        if (query.isEmpty() || !_state.value.connected) return false
        if (query.any { HidKeyMap.fromCharacter(it) == null }) return false

        // Win+R opens the Run dialog. Give it time to become active before
        // typing the command, then press Enter.
        if (!press(HidModifier.LEFT_GUI, 0x15)) return false
        handler.postDelayed({
            if (!_state.value.connected) return@postDelayed
            sendText(query) {
                handler.postDelayed({
                    if (_state.value.connected) pressKey(RemoteKey.Enter)
                }, LAUNCH_CONFIRM_DELAY_MS)
            }
        }, LAUNCH_INPUT_DELAY_MS)
        return true
    }

    fun releaseAll() {
        mouseButtons = 0
        sendReport(HidReport.MOUSE_ID, byteArrayOf(0, 0, 0, 0))
        sendReport(HidReport.KEYBOARD_ID, ByteArray(HidReport.KEYBOARD_SIZE))
    }

    private fun sendMouseState(): Boolean = sendReport(
        HidReport.MOUSE_ID,
        byteArrayOf(mouseButtons, 0, 0, 0),
    )

    private fun press(modifiers: Int, usage: Int): Boolean {
        val down = byteArrayOf(
            modifiers.toByte(),
            0,
            usage.toByte(),
            0,
            0,
            0,
            0,
            0,
        )
        val downSent = sendReport(HidReport.KEYBOARD_ID, down)
        val upSent = sendReport(HidReport.KEYBOARD_ID, ByteArray(HidReport.KEYBOARD_SIZE))
        return downSent && upSent
    }

    @SuppressLint("MissingPermission")
    private fun sendReport(id: Int, data: ByteArray): Boolean {
        val proxy = hidDevice ?: return false
        val device = connectedDevice ?: return false
        if (!_state.value.connected) return false
        return proxy.sendReport(device, id, data)
    }

    private fun publish(
        supported: Boolean = _state.value.supported,
        permissionGranted: Boolean = _state.value.permissionGranted,
        bluetoothEnabled: Boolean = _state.value.bluetoothEnabled,
        profileReady: Boolean = _state.value.profileReady,
        registered: Boolean = _state.value.registered,
        connecting: Boolean = _state.value.connecting,
        connected: Boolean = _state.value.connected,
        connectedHost: Host? = _state.value.connectedHost,
        pairedHosts: List<Host> = _state.value.pairedHosts,
        message: String = _state.value.message,
    ) {
        _state.value = State(
            supported = supported,
            permissionGranted = permissionGranted,
            bluetoothEnabled = bluetoothEnabled,
            profileReady = profileReady,
            registered = registered,
            connecting = connecting,
            connected = connected,
            connectedHost = connectedHost,
            pairedHosts = pairedHosts,
            message = message,
        )
    }

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice): String =
        runCatching { device.name }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: device.address

    private fun hostOf(device: BluetoothDevice) = Host(
        name = safeName(device),
        address = device.address,
    )
}
