package dev.typezero.couchlink.remote.host

import android.content.Context
import android.os.Build
import dev.typezero.couchlink.remote.BuildConfig
import dev.typezero.couchlink.remote.model.AudioOutputDevice
import dev.typezero.couchlink.remote.model.LauncherHostState
import dev.typezero.couchlink.remote.model.LauncherId
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class LauncherHostClient(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("couchlink_launcher_host", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequence = AtomicLong(0)
    private val writeLock = Any()

    private val _state = MutableStateFlow(LauncherHostState())
    val state: StateFlow<LauncherHostState> = _state.asStateFlow()

    private var discoveryJob: Job? = null
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var activeHostId: String? = null
    private var activeHostAddress: String? = null
    private var activeHostPort: Int = SESSION_PORT
    private var lastAttemptHostId: String? = null
    private var lastAttemptAtMs: Long = 0L

    private val clientId: String = preferences.getString(KEY_CLIENT_ID, null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString(KEY_CLIENT_ID, it).apply() }

    fun start() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = scope.launch { discoveryLoop() }
    }

    fun stop() {
        discoveryJob?.cancel()
        disconnect("Launcher host stopped.")
    }

    fun retry() {
        disconnect("Searching for CouchLink Host…")
        discoveryJob?.cancel()
        discoveryJob = scope.launch { discoveryLoop() }
    }

    fun forgetTrustedHost(): Boolean {
        val hostId = activeHostId ?: _state.value.hostId.takeIf(String::isNotBlank) ?: return false
        preferences.edit().remove(tokenKey(hostId)).apply()
        retry()
        return true
    }

    /**
     * Explicitly begin pairing with the currently discovered host. This is the only
     * path that opens a connection to an untrusted host, so pairing never starts
     * without the user asking for it.
     */
    fun pairWithHost(): Boolean {
        val current = _state.value
        if (current.hostId.isBlank() || current.hostAddress.isBlank()) return false
        connect(current.hostId, current.hostName, current.hostAddress, current.hostPort)
        return true
    }

    /**
     * Dismiss an in-progress pairing without pairing. The untrusted session is
     * closed and will not auto-reconnect (only trusted hosts do), so the prompt
     * does not immediately return.
     */
    fun cancelPairing() {
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        closeSocket()
        _state.value = _state.value.copy(
            connecting = false,
            connected = false,
            pairingRequired = false,
            message = "Pairing canceled. Bluetooth input still works.",
        )
    }

    fun submitPairingCode(code: String) {
        val normalized = code.filter(Char::isDigit).take(6)
        if (normalized.length != 6) {
            _state.value = _state.value.copy(message = "Enter the six-digit code shown by the Windows host.")
            return
        }
        scope.launch {
            val sent = sendEnvelope(
                "pair_request",
                JSONObject()
                    .put("clientId", clientId)
                    .put("clientName", deviceName())
                    .put("pairingCode", normalized),
            )
            if (!sent) {
                _state.value = _state.value.copy(
                    connecting = false,
                    pairingRequired = false,
                    message = "Pairing session closed. Tap Pair with Host to try again.",
                )
            }
        }
    }


    fun refreshAudioOutputs(): Boolean {
        if (!_state.value.connected) return false
        _state.value = _state.value.copy(audioLoading = true)
        scope.launch {
            if (!sendEnvelope("audio_output_list", JSONObject())) {
                _state.value = _state.value.copy(audioLoading = false)
            }
        }
        return true
    }

    fun setAudioOutput(endpointId: String): Boolean {
        if (!_state.value.connected || endpointId.isBlank()) return false
        scope.launch {
            sendEnvelope("audio_output_set", JSONObject().put("endpointId", endpointId))
        }
        return true
    }

    fun launch(
        launcher: LauncherId,
        action: String = "launch_or_focus",
        onSendFailed: (() -> Unit)? = null,
    ): Boolean {
        if (!_state.value.connected) return false
        scope.launch {
            val sent = sendEnvelope(
                "launcher_action",
                JSONObject()
                    .put("launcher", launcher.wireName())
                    .put("action", action),
            )
            if (!sent) {
                withContext(Dispatchers.Main.immediate) { onSendFailed?.invoke() }
            }
        }
        return true
    }

    private suspend fun discoveryLoop() {
        while (scope.isActive) {
            try {
                DatagramSocket(null).use { udp ->
                    udp.reuseAddress = true
                    udp.broadcast = true
                    udp.soTimeout = 5000
                    udp.bind(InetSocketAddress(DISCOVERY_PORT))
                    val buffer = ByteArray(8192)
                    while (scope.isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        try {
                            udp.receive(packet)
                        } catch (_: java.net.SocketTimeoutException) {
                            if (!_state.value.connected && !_state.value.connecting && !_state.value.discovered) {
                                _state.value = _state.value.copy(message = "Searching for CouchLink Host…")
                            }
                            continue
                        }
                        val json = JSONObject(String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8))
                        if (!json.optString("product").equals("CouchLink", ignoreCase = true)) continue
                        val hostId = json.optString("hostId")
                        val hostName = json.optString("hostName", "Windows PC")
                        val advertisedAddress = json.optString("address")
                        val address = advertisedAddress.takeIf { it.isNotBlank() && it != "0.0.0.0" }
                            ?: packet.address.hostAddress
                            ?: continue
                        val port = json.optInt("sessionPort", SESSION_PORT)
                        val hasToken = preferences.getString(tokenKey(hostId), null) != null
                        _state.value = _state.value.copy(
                            discovered = true,
                            trusted = hasToken,
                            hostId = hostId,
                            hostName = hostName,
                            hostAddress = address,
                            hostPort = port,
                            message = when {
                                _state.value.connected -> _state.value.message
                                hasToken -> "Found $hostName"
                                else -> "Found $hostName. Tap Pair to enable launching."
                            },
                        )
                        // Only auto-connect to hosts we already trust. Untrusted hosts
                        // must be paired on the user's explicit request, so a CouchLink
                        // PC on the network never forces an unsolicited pairing prompt.
                        if (hasToken && !recentlyAttempted(hostId) &&
                            (connectionJob?.isActive != true || activeHostId != hostId)
                        ) {
                            connect(hostId, hostName, address, port)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.value = _state.value.copy(message = "Host discovery error: ${exception.message ?: "unknown error"}")
                delay(2000)
            }
        }
    }

    private fun connect(hostId: String, hostName: String, address: String, port: Int) {
        connectionJob?.cancel()
        closeSocket()
        activeHostId = hostId
        activeHostAddress = address
        activeHostPort = port
        lastAttemptHostId = hostId
        lastAttemptAtMs = System.currentTimeMillis()
        connectionJob = scope.launch {
            _state.value = _state.value.copy(
                connecting = true,
                connected = false,
                pairingRequired = false,
                hostId = hostId,
                hostName = hostName,
                hostAddress = address,
                hostPort = port,
                message = "Connecting to $hostName…",
            )
            var ownedSocket: Socket? = null
            var ownedOutput: DataOutputStream? = null
            try {
                val tcp = Socket()
                ownedSocket = tcp
                tcp.connect(InetSocketAddress(InetAddress.getByName(address), port), CONNECT_TIMEOUT_MS)
                tcp.tcpNoDelay = true
                tcp.keepAlive = true
                val stream = DataOutputStream(tcp.getOutputStream())
                ownedOutput = stream
                socket = tcp
                output = stream
                val helloSent = sendEnvelope(
                    "hello",
                    JSONObject()
                        .put("clientId", clientId)
                        .put("clientName", deviceName())
                        .put("clientPlatform", "Android")
                        .put("clientVersion", BuildConfig.VERSION_NAME)
                        .put("pairingToken", preferences.getString(tokenKey(hostId), null)),
                    expectedOutput = stream,
                )
                if (!helloSent) throw java.io.IOException("Unable to send host handshake")
                val input = DataInputStream(tcp.getInputStream())
                while (scope.isActive && !tcp.isClosed) {
                    val length = input.readInt()
                    require(length in 1..MAX_FRAME_BYTES) { "Invalid host frame length: $length" }
                    val payload = ByteArray(length)
                    input.readFully(payload)
                    handleEnvelope(JSONObject(String(payload, StandardCharsets.UTF_8)), hostId, hostName)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (activeHostId == hostId && socket === ownedSocket) {
                    _state.value = _state.value.copy(
                        connecting = false,
                        connected = false,
                        pairingRequired = false,
                        message = "Launcher host disconnected. Bluetooth input still works.",
                    )
                }
            } finally {
                if (socket === ownedSocket) {
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    output = null
                    socket = null
                }
                runCatching { ownedOutput?.close() }
                runCatching { ownedSocket?.close() }
            }
        }
    }

    private fun handleEnvelope(envelope: JSONObject, hostId: String, hostName: String) {
        val type = envelope.optString("type")
        val payload = envelope.optJSONObject("payload") ?: JSONObject()
        when (type) {
            "hello_ack" -> {
                val pairingRequired = payload.optBoolean("pairingRequired", true)
                val trusted = payload.optBoolean("trusted", false)
                // If we presented a stored token and the host still says untrusted,
                // the token is stale. Drop it so discovery stops auto-reconnecting
                // and re-prompting; the host becomes a plain opt-in pairing target.
                if (!trusted && preferences.getString(tokenKey(hostId), null) != null) {
                    preferences.edit().remove(tokenKey(hostId)).apply()
                }
                _state.value = _state.value.copy(
                    connecting = !trusted,
                    connected = trusted,
                    trusted = trusted,
                    pairingRequired = pairingRequired,
                    hostId = hostId,
                    hostName = payload.optString("hostName", hostName),
                    hostVersion = payload.optString("hostVersion"),
                    message = if (pairingRequired) {
                        "Enter the six-digit code shown by the Windows host."
                    } else {
                        "Launcher host connected."
                    },
                )
            }
            "pair_result" -> {
                val success = payload.optBoolean("success")
                if (success) {
                    payload.optString("pairingToken").takeIf(String::isNotBlank)?.let { token ->
                        preferences.edit().putString(tokenKey(hostId), token).apply()
                    }
                    _state.value = _state.value.copy(
                        pairingRequired = false,
                        connecting = true,
                        message = "Paired. Opening trusted session…",
                    )
                    // The host authenticates a new TCP session with the issued token.
                    scope.launch {
                        delay(250)
                        connect(hostId, hostName, activeHostAddress ?: return@launch, activeHostPort)
                    }
                } else {
                    _state.value = _state.value.copy(message = payload.optString("message", "Pairing failed."))
                }
            }
            "session_ready" -> {
                _state.value = _state.value.copy(
                    connecting = false,
                    connected = true,
                    trusted = true,
                    pairingRequired = false,
                    message = "Launcher host connected to ${payload.optString("hostName", hostName)}.",
                )
                startHeartbeat(payload.optInt("heartbeatSeconds", 5).coerceIn(2, 15))
                refreshAudioOutputs()
            }
            "audio_output_list_result" -> {
                val devicesJson = payload.optJSONArray("devices")
                val devices = buildList {
                    if (devicesJson != null) {
                        for (index in 0 until devicesJson.length()) {
                            val item = devicesJson.optJSONObject(index) ?: continue
                            val id = item.optString("id")
                            if (id.isBlank()) continue
                            add(AudioOutputDevice(id, item.optString("name", id), item.optBoolean("isDefault")))
                        }
                    }
                }
                _state.value = _state.value.copy(
                    audioOutputs = devices,
                    audioLoading = false,
                    message = payload.optString("error").takeIf { it.isNotBlank() } ?: _state.value.message,
                )
            }
            "audio_output_result" -> {
                val endpointId = payload.optString("endpointId")
                val success = payload.optBoolean("success")
                val devices = if (success) {
                    _state.value.audioOutputs.map { it.copy(isDefault = it.id.equals(endpointId, ignoreCase = true)) }
                } else _state.value.audioOutputs
                _state.value = _state.value.copy(
                    audioOutputs = devices,
                    audioLoading = false,
                    message = payload.optString("message", if (success) "Audio output changed." else "Audio switch failed."),
                )
                if (success) refreshAudioOutputs()
            }
            "launcher_result" -> {
                val launcher = launcherIdFromWireName(payload.optString("launcher"))
                val resultMessage = payload.optString("message", "Launcher command completed.")
                val stateName = payload.optString("state", "unknown")
                val states = _state.value.launcherStates.toMutableMap()
                if (launcher != null) states[launcher] = stateName
                _state.value = _state.value.copy(message = resultMessage, launcherStates = states)
            }
            "error" -> _state.value = _state.value.copy(message = payload.optString("message", "Host protocol error."))
        }
    }

    private fun startHeartbeat(seconds: Int) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _state.value.connected) {
                delay(seconds * 1000L)
                if (!sendEnvelope("ping", JSONObject().put("sequence", sequence.incrementAndGet()))) break
            }
        }
    }

    private fun sendEnvelope(
        type: String,
        payload: JSONObject,
        expectedOutput: DataOutputStream? = null,
    ): Boolean {
        val frame = JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("messageId", UUID.randomUUID().toString())
            .put("type", type)
            .put("sentAtUtc", java.time.Instant.now().toString())
            .put("payload", payload)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        require(frame.size <= MAX_FRAME_BYTES)
        val result = synchronized(writeLock) {
            val stream = output ?: return@synchronized Result.failure<Boolean>(
                java.io.IOException("Launcher host output stream is unavailable"),
            )
            if (expectedOutput != null && stream !== expectedOutput) {
                return@synchronized Result.failure<Boolean>(
                    java.io.IOException("Launcher host session changed before send"),
                )
            }
            runCatching {
                stream.writeInt(frame.size)
                stream.write(frame)
                stream.flush()
                true
            }
        }
        if (result.isFailure) {
            if (_state.value.connected || _state.value.connecting) {
                _state.value = _state.value.copy(
                    connected = false,
                    connecting = false,
                    message = "Launcher host disconnected. Bluetooth input still works.",
                )
            }
            return false
        }
        return true
    }

    private fun disconnect(message: String) {
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        closeSocket()
        _state.value = LauncherHostState(message = message)
    }

    private fun closeSocket() {
        runCatching { output?.close() }
        runCatching { socket?.close() }
        output = null
        socket = null
    }

    // Throttle auto-connect so a discovered-but-unreachable host is not retried on
    // every 2-second advertisement.
    private fun recentlyAttempted(hostId: String): Boolean =
        lastAttemptHostId == hostId &&
            !_state.value.connected &&
            System.currentTimeMillis() - lastAttemptAtMs < RECONNECT_BACKOFF_MS

    private fun deviceName(): String = Build.MODEL?.takeIf(String::isNotBlank) ?: "Android device"
    private fun tokenKey(hostId: String) = "pairing_token_$hostId"

    private fun LauncherId.wireName(): String = when (this) {
        LauncherId.Steam -> "steam"
        LauncherId.Gog -> "gog"
        LauncherId.Xbox -> "xbox"
        LauncherId.Ea -> "ea"
        LauncherId.Ubisoft -> "ubisoft"
        LauncherId.Rockstar -> "rockstar"
        LauncherId.Epic -> "epic"
        LauncherId.Amazon -> "amazon"
    }

    private fun launcherIdFromWireName(value: String): LauncherId? = when (value.lowercase()) {
        "steam" -> LauncherId.Steam
        "gog" -> LauncherId.Gog
        "xbox" -> LauncherId.Xbox
        "ea" -> LauncherId.Ea
        "ubisoft" -> LauncherId.Ubisoft
        "rockstar" -> LauncherId.Rockstar
        "epic" -> LauncherId.Epic
        "amazon" -> LauncherId.Amazon
        else -> null
    }

    private companion object {
        const val PROTOCOL_VERSION = 1
        const val DISCOVERY_PORT = 45820
        const val SESSION_PORT = 45821
        const val CONNECT_TIMEOUT_MS = 5000
        const val RECONNECT_BACKOFF_MS = 4000L
        const val MAX_FRAME_BYTES = 1024 * 1024
        const val KEY_CLIENT_ID = "client_id"
    }
}

