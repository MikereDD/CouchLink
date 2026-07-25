package dev.typezero.couchlink.remote.tv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

internal enum class TvInputTarget(val label: String, val appLink: String) {
    HDMI_1("HDMI 1", "content://android.media.tv/passthrough/com.hisense.tv.hitvinput%2F.hdmi.HdmiTvInputService%2FHW4"),
    HDMI_2("HDMI 2", "content://android.media.tv/passthrough/com.hisense.tv.hitvinput%2F.hdmi.HdmiTvInputService%2FHW5"),
    HDMI_3("HDMI 3", "content://android.media.tv/passthrough/com.hisense.tv.hitvinput%2F.hdmi.HdmiTvInputService%2FHW6"),
    COMPOSITE("Composite", "content://android.media.tv/passthrough/com.hisense.tv.hitvinput%2F.composite.CompositeTvInputService%2FHW1"),
    TV("TV", "content://android.media.tv/passthrough/com.hisense.tv.hitvinput%2F.tuner.TunerTvInputService%2FHW0"),
}

internal class TvDiscoveryController(
    context: Context,
) {
    internal data class State(
        val scanning: Boolean = false,
        val devices: List<TvDevice> = emptyList(),
        val selectedDevice: TvDevice? = null,
        val probing: Boolean = false,
        val probe: TvConnectionProbe? = null,
        val pairing: PairingState = PairingState(),
        val remote: TvRemoteClient.Connection = TvRemoteClient.Connection(),
        val wakeMacAddress: String? = null,
        val message: String = "Scan your local network for a Google TV.",
    )

    internal data class PairingState(
        val inProgress: Boolean = false,
        val awaitingCode: Boolean = false,
        val paired: Boolean = false,
        val message: String? = null,
    )

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val discovered = ConcurrentHashMap<String, TvDevice>()
    private val activeListeners = ConcurrentHashMap<String, NsdManager.DiscoveryListener>()
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val pairingClient = TvPairingClient(appContext)
    private val remoteClient = TvRemoteClient(appContext)
    private var pendingPairing: TvPairingClient.PendingPairing? = null
    private var wakeJob: Job? = null

    private val _state = MutableStateFlow(
        State(
            selectedDevice = rememberedDevice(),
            pairing = rememberedDevice()?.let { PairingState(paired = pairingClient.isPaired(it.host)) } ?: PairingState(),
            wakeMacAddress = rememberedWakeMac(rememberedDevice()),
            message = rememberedDevice()?.let {
                "Saved TV: ${it.name} (${it.host}). Scan or test the connection."
            } ?: "Scan your local network for a Google TV.",
        ),
    )
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        val device = _state.value.selectedDevice
        if (device != null && pairingClient.isPaired(device.host)) {
            connectRemote()
        }
    }

    fun startDiscovery() {
        if (_state.value.scanning) return
        discovered.clear()
        _state.value = _state.value.copy(
            scanning = true,
            devices = emptyList(),
            message = "Searching for Android and Google TVs…",
        )

        SERVICE_TYPES.forEach { serviceType ->
            val listener = discoveryListener(serviceType)
            activeListeners[serviceType] = listener
            runCatching {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            }.onFailure { error ->
                activeListeners.remove(serviceType)
                updateDiscoveryFailure(error.message)
            }
        }
    }

    fun stopDiscovery() {
        activeListeners.values.toList().forEach { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        activeListeners.clear()
        _state.value = _state.value.copy(scanning = false)
    }

    fun select(device: TvDevice) {
        preferences.edit()
            .putString(KEY_NAME, device.name)
            .putString(KEY_HOST, device.host)
            .apply()
        ensureKnownWakeMac(device)
        remoteClient.close()
        val paired = pairingClient.isPaired(device.host)
        _state.value = _state.value.copy(
            selectedDevice = device,
            probe = null,
            pairing = PairingState(paired = paired),
            remote = TvRemoteClient.Connection(),
            wakeMacAddress = rememberedWakeMac(device),
            message = "Selected ${device.name} at ${device.host}.",
        )
        if (paired) connectRemote()
    }

    fun selectManual(host: String) {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        if (cleanHost.isBlank()) {
            _state.value = _state.value.copy(message = "Enter the TV IP address or hostname.")
            return
        }
        select(TvDevice(name = "Manual Google TV", host = cleanHost))
    }

    fun probeSelected() {
        val device = _state.value.selectedDevice ?: run {
            _state.value = _state.value.copy(message = "Select a discovered TV or enter its IP address first.")
            return
        }
        if (_state.value.probing) return

        _state.value = _state.value.copy(
            probing = true,
            probe = null,
            message = "Testing Android TV Remote services on ${device.host}…",
        )
        scope.launch {
            val probe = withContext(Dispatchers.IO) {
                TvConnectionProbe(
                    pairingPortReachable = canConnect(device.host, PAIRING_PORT),
                    remotePortReachable = canConnect(device.host, REMOTE_PORT),
                )
            }
            val message = when {
                probe.fullyReachable -> "TV Remote services are reachable. This TV is ready for CouchLink pairing."
                probe.anyReachable -> "One TV Remote service responded. The TV may be waking up or using a different service version."
                else -> "No TV Remote service responded. Confirm the TV is on and both devices are on the same network."
            }
            _state.value = _state.value.copy(
                probing = false,
                probe = probe,
                message = message,
            )
        }
    }


    fun beginPairing() {
        val device = _state.value.selectedDevice ?: run {
            _state.value = _state.value.copy(message = "Select a TV before pairing.")
            return
        }
        if (_state.value.pairing.inProgress || _state.value.pairing.awaitingCode) return
        pendingPairing?.close()
        pendingPairing = null
        _state.value = _state.value.copy(
            pairing = PairingState(inProgress = true, message = "Starting secure pairing…"),
            message = "Starting pairing with ${device.name}…",
        )
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { pairingClient.begin(device.host) }
            }.onSuccess { pending ->
                pendingPairing = pending
                _state.value = _state.value.copy(
                    pairing = PairingState(
                        awaitingCode = true,
                        message = "Enter the six-character code shown on the TV.",
                    ),
                    message = "Pairing code requested from ${device.name}.",
                )
            }.onFailure { error ->
                pendingPairing?.close()
                pendingPairing = null
                _state.value = _state.value.copy(
                    pairing = PairingState(message = error.message ?: "Pairing could not start."),
                    message = "Pairing failed: ${error.message ?: error.javaClass.simpleName}",
                )
            }
        }
    }

    fun finishPairing(code: String) {
        val pending = pendingPairing ?: run {
            _state.value = _state.value.copy(message = "Start pairing before entering a code.")
            return
        }
        if (_state.value.pairing.inProgress) return
        _state.value = _state.value.copy(
            pairing = _state.value.pairing.copy(inProgress = true, message = "Verifying pairing code…"),
        )
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { pairingClient.finish(pending, code) }
            }.onSuccess {
                pendingPairing = null
                _state.value = _state.value.copy(
                    pairing = PairingState(paired = true, message = "Paired securely with this TV."),
                    message = "CouchLink is paired with ${pending.host}.",
                )
                connectRemote()
            }.onFailure { error ->
                pendingPairing?.close()
                pendingPairing = null
                _state.value = _state.value.copy(
                    pairing = PairingState(message = error.message ?: "Pairing failed."),
                    message = "Pairing failed: ${error.message ?: error.javaClass.simpleName}",
                )
            }
        }
    }

    fun connectRemote() {
        val device = _state.value.selectedDevice ?: return
        if (!pairingClient.isPaired(device.host)) {
            _state.value = _state.value.copy(
                remote = TvRemoteClient.Connection(message = "Pair with this TV in Settings first."),
            )
            return
        }
        remoteClient.connect(device.host) { connection ->
            scope.launch {
                _state.value = _state.value.copy(remote = connection)
            }
        }
    }

    fun togglePower() {
        if (_state.value.remote.ready) {
            sendKey(dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode.KEYCODE_POWER)
        } else {
            wakeSelectedTv()
        }
    }

    private fun wakeSelectedTv() {
        if (wakeJob?.isActive == true) return
        val device = _state.value.selectedDevice ?: run {
            _state.value = _state.value.copy(message = "Select a TV before using Wake-on-LAN.")
            return
        }
        val mac = _state.value.wakeMacAddress ?: rememberedWakeMac(device) ?: run {
            _state.value = _state.value.copy(message = "No Wake-on-LAN MAC address is stored for ${device.name}.")
            return
        }
        if (!_state.value.pairing.paired) {
            _state.value = _state.value.copy(message = "Pair with ${device.name} before using the Power button.")
            return
        }

        _state.value = _state.value.copy(
            remote = TvRemoteClient.Connection(connecting = true, message = "Waking ${device.name}…"),
            message = "Sending Wake-on-LAN to ${device.name}…",
        )
        wakeJob = scope.launch {
            val woke = withContext(Dispatchers.IO) {
                sendWakePackets(mac)
                repeat(WAKE_RETRY_COUNT) { attempt ->
                    if (canConnect(device.host, REMOTE_PORT)) return@withContext true
                    if (attempt + 1 < WAKE_RETRY_COUNT) delay(WAKE_RETRY_DELAY_MS)
                }
                false
            }
            if (woke) {
                _state.value = _state.value.copy(message = "${device.name} is awake. Reconnecting TV Remote…")
                connectRemote()
            } else {
                _state.value = _state.value.copy(
                    remote = TvRemoteClient.Connection(message = "Wake packet sent, but the TV Remote service did not return yet."),
                    message = "${device.name} did not reconnect within ${WAKE_TIMEOUT_SECONDS} seconds.",
                )
            }
            wakeJob = null
        }
    }

    fun sendKey(keyCode: dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode) {
        if (!_state.value.remote.ready) {
            _state.value = _state.value.copy(message = "Reconnecting to the TV. Try the command again in a moment.")
            connectRemote()
            return
        }
        // The write is real network I/O and MUST NOT run on the main thread, or Android
        // throws NetworkOnMainThreadException. Dispatch to IO; state updates from the
        // client marshal back to Main via its own callback.
        scope.launch {
            withContext(Dispatchers.IO) { remoteClient.sendKey(keyCode) }
        }
    }

    /** Opens the Google TV Home Live tab instead of the manufacturer's antenna tuner.
     * Google TV does not expose the Live tab as a public remote key, so use a short,
     * deterministic launcher-navigation macro: Home, refocus Home, move to Live, open.
     */
    fun openGoogleTvLive() {
        if (!_state.value.remote.ready) {
            _state.value = _state.value.copy(message = "Reconnect to the TV before opening Google TV Live.")
            connectRemote()
            return
        }
        scope.launch {
            val sequence = listOf(
                dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode.KEYCODE_HOME to 850L,
                dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode.KEYCODE_HOME to 250L,
                dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode.KEYCODE_DPAD_RIGHT to 150L,
                dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode.KEYCODE_DPAD_CENTER to 0L,
            )
            withContext(Dispatchers.IO) {
                sequence.forEach { (key, pauseAfter) ->
                    remoteClient.sendKey(key)
                    if (pauseAfter > 0) delay(pauseAfter)
                }
            }
        }
    }

    fun selectInput(target: TvInputTarget) {
        if (!_state.value.remote.ready) {
            _state.value = _state.value.copy(message = "Reconnect to the TV before changing inputs.")
            connectRemote()
            return
        }
        scope.launch(Dispatchers.IO) {
            remoteClient.launchAppLink(target.appLink)
        }
    }

    fun forgetTv() {
        cancelPairing()
        remoteClient.close()
        preferences.edit().clear().apply()
        appContext.getSharedPreferences("couchlink_tv_remote", Context.MODE_PRIVATE).edit().clear().apply()
        _state.value = State(message = "TV pairing and selection forgotten.")
    }

    fun cancelPairing() {
        pendingPairing?.close()
        pendingPairing = null
        _state.value = _state.value.copy(
            pairing = PairingState(paired = _state.value.selectedDevice?.host?.let(pairingClient::isPaired) == true),
            message = "Pairing cancelled.",
        )
    }

    fun close() {
        stopDiscovery()
        pendingPairing?.close()
        pendingPairing = null
        wakeJob?.cancel()
        wakeJob = null
        remoteClient.close()
        scope.cancel()
    }

    private fun discoveryListener(serviceType: String) = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) = Unit

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            runCatching {
                nsdManager.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit

                        @Suppress("DEPRECATION")
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val hostAddress = info.host?.hostAddress ?: return
                            val device = TvDevice(
                                name = info.serviceName.ifBlank { "Google TV" },
                                host = hostAddress,
                                serviceType = serviceType,
                                advertisedPort = info.port.takeIf { it > 0 },
                            )
                            discovered[hostAddress] = device
                            val devices = discovered.values.sortedBy { it.name.lowercase() }
                            _state.value = _state.value.copy(
                                devices = devices,
                                message = if (devices.size == 1) {
                                    "Found 1 compatible TV."
                                } else {
                                    "Found ${devices.size} compatible TVs."
                                },
                            )
                        }
                    },
                )
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

        override fun onDiscoveryStopped(serviceType: String) {
            activeListeners.remove(serviceType)
            if (activeListeners.isEmpty()) {
                _state.value = _state.value.copy(scanning = false)
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            activeListeners.remove(serviceType)
            runCatching { nsdManager.stopServiceDiscovery(this) }
            updateDiscoveryFailure("NSD error $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            activeListeners.remove(serviceType)
            updateDiscoveryFailure("NSD stop error $errorCode")
        }
    }

    private fun updateDiscoveryFailure(detail: String?) {
        _state.value = _state.value.copy(
            scanning = activeListeners.isNotEmpty(),
            message = "TV discovery could not start${detail?.let { ": $it" } ?: "."}",
        )
    }

    private fun rememberedDevice(): TvDevice? {
        val host = preferences.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: return null
        val name = preferences.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: "Saved Google TV"
        return TvDevice(name = name, host = host)
    }

    private fun ensureKnownWakeMac(device: TvDevice) {
        if (preferences.getString(KEY_WAKE_MAC, null).isNullOrBlank() && device.host == ABADDON_HOST) {
            preferences.edit().putString(KEY_WAKE_MAC, ABADDON_MAC).apply()
        }
    }

    private fun rememberedWakeMac(device: TvDevice?): String? {
        val saved = preferences.getString(KEY_WAKE_MAC, null)?.takeIf { it.isNotBlank() }
        if (saved != null) return saved
        return if (device?.host == ABADDON_HOST) ABADDON_MAC else null
    }

    private fun sendWakePackets(macAddress: String) {
        val mac = macAddress.split(':', '-').map { it.toInt(16).toByte() }.toByteArray()
        require(mac.size == 6) { "Invalid TV MAC address: $macAddress" }
        val payload = ByteArray(6 + 16 * mac.size)
        repeat(6) { payload[it] = 0xFF.toByte() }
        repeat(16) { copy ->
            mac.copyInto(payload, destinationOffset = 6 + copy * mac.size)
        }
        DatagramSocket().use { socket ->
            socket.broadcast = true
            val targets = listOf(
                InetSocketAddress(InetAddress.getByName("255.255.255.255"), 9),
                InetSocketAddress(InetAddress.getByName("255.255.255.255"), 7),
            )
            repeat(WAKE_PACKET_BURSTS) {
                targets.forEach { target -> socket.send(DatagramPacket(payload, payload.size, target)) }
                Thread.sleep(WAKE_PACKET_INTERVAL_MS)
            }
        }
    }

    private fun canConnect(host: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
        }
        true
    }.getOrDefault(false)

    private companion object {
        const val PREFERENCES = "couchlink_tv_remote"
        const val KEY_NAME = "selected_tv_name"
        const val KEY_HOST = "selected_tv_host"
        const val KEY_WAKE_MAC = "selected_tv_wake_mac"
        const val ABADDON_HOST = "192.168.4.39"
        const val ABADDON_MAC = "38:64:07:C2:BF:EE"
        const val PAIRING_PORT = 6467
        const val REMOTE_PORT = 6466
        const val SOCKET_TIMEOUT_MS = 1_500
        const val WAKE_RETRY_COUNT = 15
        const val WAKE_RETRY_DELAY_MS = 2_000L
        const val WAKE_TIMEOUT_SECONDS = 30
        const val WAKE_PACKET_BURSTS = 3
        const val WAKE_PACKET_INTERVAL_MS = 100L
        val SERVICE_TYPES = listOf(
            "_androidtvremote2._tcp.",
            "_androidtvremote._tcp.",
        )
    }
}
