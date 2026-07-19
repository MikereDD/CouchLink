package dev.typezero.couchlink.remote.network

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.coroutineContext

data class DiscoveredHost(
    val hostId: String,
    val hostName: String,
    val hostVersion: String,
    val hostState: String,
    val address: String,
    val sessionPort: Int,
    val pairingRequired: Boolean,
    val macAddress: String,
)

data class HelloResult(
    val hostName: String,
    val hostVersion: String,
    val hostState: String,
    val pairingRequired: Boolean,
    val trusted: Boolean,
)

data class PairResult(val success: Boolean, val message: String)

class DiscoveryClient(context: Context) {
    private val preferences = context.getSharedPreferences("couchlink_pairing", Context.MODE_PRIVATE)
    private val writeMutex = Mutex()
    @Volatile private var activeSocket: Socket? = null

    val clientId: String = preferences.getString("client_id", null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString("client_id", it).apply()
    }

    fun hasTrustedToken(host: DiscoveredHost): Boolean =
        !preferences.getString("token_${host.hostId}", null).isNullOrBlank()

    fun lastKnownHost(): DiscoveredHost? {
        val hostId = preferences.getString("last_host_id", null) ?: return null
        return DiscoveredHost(
            hostId,
            preferences.getString("last_host_name", "Windows PC") ?: "Windows PC",
            preferences.getString("last_host_version", "") ?: "",
            "Offline",
            preferences.getString("last_host_address", "") ?: "",
            preferences.getInt("last_host_port", 45821),
            true,
            preferences.getString("last_host_mac", "") ?: ""
        )
    }

    private fun saveLastHost(host: DiscoveredHost) {
        preferences.edit()
            .putString("last_host_id", host.hostId)
            .putString("last_host_name", host.hostName)
            .putString("last_host_version", host.hostVersion)
            .putString("last_host_address", host.address)
            .putInt("last_host_port", host.sessionPort)
            .putString("last_host_mac", host.macAddress)
            .apply()
    }

    suspend fun sendWakeOnLan(host: DiscoveredHost): Boolean = withContext(Dispatchers.IO) {
        val clean = host.macAddress.replace(":", "").replace("-", "")
        if (clean.length != 12) return@withContext false
        val mac = ByteArray(6) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        val packet = ByteArray(102) { 0xFF.toByte() }
        for (i in 0 until 16) mac.copyInto(packet, 6 + i * 6)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName("255.255.255.255"), 9))
        }
        true
    }

    fun disconnect() {
        runCatching { activeSocket?.close() }
        activeSocket = null
    }

    suspend fun listen(onHost: (DiscoveredHost) -> Unit) = withContext(Dispatchers.IO) {
        DatagramSocket(null).use { socket ->
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(DISCOVERY_PORT))
            socket.soTimeout = 2500
            while (isActive) {
                try {
                    val buffer = ByteArray(4096)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val json = JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8))
                    if (json.optString("product") != "CouchLink") continue
                    onHost(DiscoveredHost(
                        hostId = json.getString("hostId"),
                        hostName = json.getString("hostName"),
                        hostVersion = json.getString("hostVersion"),
                        hostState = json.getString("hostState"),
                        address = packet.address.hostAddress ?: json.getString("address"),
                        sessionPort = json.getInt("sessionPort"),
                        pairingRequired = json.getBoolean("pairingRequired"),
                        macAddress = json.optString("macAddress"),
                    ).also { saveLastHost(it) })
                } catch (_: java.net.SocketTimeoutException) { }
            }
        }
    }

    suspend fun beginPairing(host: DiscoveredHost): HelloResult = withContext(Dispatchers.IO) {
        request(host, "hello", helloPayload(host)) { response -> decodeHello(response) }
    }

    suspend fun pair(host: DiscoveredHost, code: String): PairResult = withContext(Dispatchers.IO) {
        request(host, "pair_request", JSONObject()
            .put("clientId", clientId)
            .put("clientName", Build.MODEL)
            .put("pairingCode", code)) { response ->
            require(response.getString("type") == "pair_result") { "Unexpected response: ${response.getString("type")}" }
            val body = response.getJSONObject("payload")
            val success = body.getBoolean("success")
            if (success) preferences.edit().putString("token_${host.hostId}", body.getString("pairingToken")).apply()
            PairResult(success, body.getString("message"))
        }
    }

    suspend fun sendMouseMove(deltaX: Int, deltaY: Int) = sendActive("mouse_move", JSONObject()
        .put("deltaX", deltaX.coerceIn(-250, 250))
        .put("deltaY", deltaY.coerceIn(-250, 250)))

    suspend fun sendMouseButton(button: String, action: String = "click") =
        sendActive("mouse_button", JSONObject().put("button", button).put("action", action))

    suspend fun sendMouseScroll(delta: Int) =
        sendActive("mouse_scroll", JSONObject().put("delta", delta.coerceIn(-1200, 1200)))

    suspend fun sendKeyboardText(text: String) =
        sendActive("keyboard_text", JSONObject().put("text", text.take(1024)))

    suspend fun sendKeyPress(key: String) =
        sendActive("key_press", JSONObject().put("key", key))

    suspend fun sendShortcut(shortcut: String) =
        sendActive("shortcut", JSONObject().put("shortcut", shortcut))

    suspend fun sendLauncherAction(launcher: String, action: String = "launch") =
        sendActive("launcher_action", JSONObject().put("launcher", launcher).put("action", action))

    suspend fun runPersistentSession(
        host: DiscoveredHost,
        onStatus: (message: String, connected: Boolean, remoteInputEnabled: Boolean) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var attempt = 0
        while (coroutineContext.isActive) {
            attempt++
            var connectedThisPass = false
            val candidatePorts = listOf(PRELOGIN_PORT, host.sessionPort, DESKTOP_PORT).distinct()

            for (port in candidatePorts) {
                if (!coroutineContext.isActive) break
                try {
                    onStatus(
                        if (attempt == 1) "Connecting to ${host.hostName}:$port…"
                        else "Reconnecting to ${host.hostName}:$port…",
                        false,
                        false
                    )

                    Socket().use { socket ->
                        activeSocket = socket
                        socket.connect(InetSocketAddress(InetAddress.getByName(host.address), port), 3000)
                        socket.soTimeout = 12_000
                        socket.tcpNoDelay = true

                        writeEnvelope(socket, "hello", helloPayload(host))
                        val hello = decodeHello(readEnvelope(socket))
                        require(hello.trusted) { "Host requires pairing again." }

                        // The boot broker deliberately stays reachable after login for diagnostics.
                        // Do not remain attached to it once a desktop session exists; immediately
                        // try the normal Session Host instead.
                        if (port == PRELOGIN_PORT && hello.hostState != "SignInRequired") {
                            invalidateActiveSocket(socket)
                            return@use
                        }

                        val ready = readEnvelope(socket)
                        require(ready.getString("type") == "session_ready") { "Host did not open a persistent session." }
                        val body = ready.getJSONObject("payload")
                        val heartbeatSeconds = body.optInt("heartbeatSeconds", 5).coerceIn(2, 10)
                        var inputEnabled = body.optBoolean("remoteInputEnabled", false)
                        onStatus("Connected to ${hello.hostName}:$port", true, inputEnabled)
                        attempt = 0
                        connectedThisPass = true

                        var sequence = 0L
                        while (coroutineContext.isActive && !socket.isClosed) {
                            sequence++
                            writeMutex.withLock { writeEnvelope(socket, "ping", JSONObject().put("sequence", sequence)) }
                            val pong = readEnvelope(socket)
                            require(pong.getString("type") == "pong") { "Unexpected heartbeat response: ${pong.getString("type")}" }
                            inputEnabled = pong.getJSONObject("payload").optBoolean("remoteInputEnabled", false)
                            onStatus("Connected to ${hello.hostName}:$port • heartbeat $sequence", true, inputEnabled)
                            delay(heartbeatSeconds * 1000L)
                        }
                    }
                } catch (error: Exception) {
                    activeSocket?.let { invalidateActiveSocket(it) }
                    if (!coroutineContext.isActive) break
                    onStatus("${host.hostName}:$port unavailable: ${error.message ?: "connection lost"}", false, false)
                    continue
                } finally {
                    val socket = activeSocket
                    if (socket != null && socket.isClosed) activeSocket = null
                }

                // A completed session should restart endpoint selection from the beginning.
                if (connectedThisPass) break
            }

            if (!coroutineContext.isActive) break
            delay(if (connectedThisPass) 750 else 1500)
        }
    }

    private suspend fun sendActive(type: String, payload: JSONObject) = withContext(Dispatchers.IO) {
        val socket = activeSocket ?: return@withContext
        if (socket.isClosed || !socket.isConnected) return@withContext

        try {
            writeMutex.withLock {
                if (activeSocket !== socket || socket.isClosed || !socket.isConnected) return@withLock
                writeEnvelope(socket, type, payload)
            }
        } catch (_: IOException) {
            // A pre-login broker can deliberately close the socket during the
            // handoff to the desktop Session Host. Treat that as a reconnect
            // signal instead of allowing the exception to crash Compose.
            invalidateActiveSocket(socket)
        } catch (_: IllegalStateException) {
            invalidateActiveSocket(socket)
        }
    }

    private fun invalidateActiveSocket(socket: Socket) {
        if (activeSocket === socket) activeSocket = null
        runCatching { socket.close() }
    }

    private fun helloPayload(host: DiscoveredHost) = JSONObject()
        .put("clientId", clientId)
        .put("clientName", Build.MODEL)
        .put("clientPlatform", "Android")
        .put("clientVersion", "0.1-dev.12.1")
        .put("pairingToken", preferences.getString("token_${host.hostId}", null))

    private fun decodeHello(response: JSONObject): HelloResult {
        require(response.getString("type") == "hello_ack") { "Unexpected response: ${response.getString("type")}" }
        val body = response.getJSONObject("payload")
        return HelloResult(body.getString("hostName"), body.getString("hostVersion"), body.getString("hostState"), body.getBoolean("pairingRequired"), body.getBoolean("trusted"))
    }

    private fun <T> request(host: DiscoveredHost, type: String, payloadBody: JSONObject, decode: (JSONObject) -> T): T {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(InetAddress.getByName(host.address), host.sessionPort), 4000)
            socket.soTimeout = 5000
            socket.tcpNoDelay = true
            writeEnvelope(socket, type, payloadBody)
            return decode(readEnvelope(socket))
        }
    }

    private fun writeEnvelope(socket: Socket, type: String, payloadBody: JSONObject) {
        val envelope = JSONObject()
            .put("protocolVersion", 1)
            .put("messageId", UUID.randomUUID().toString())
            .put("type", type)
            .put("sentAtUtc", Instant.now().toString())
            .put("payload", payloadBody)
        val bytes = envelope.toString().toByteArray(Charsets.UTF_8)
        socket.getOutputStream().apply {
            write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bytes.size).array())
            write(bytes)
            flush()
        }
    }

    private fun readEnvelope(socket: Socket): JSONObject {
        val input = socket.getInputStream()
        val responseSize = ByteBuffer.wrap(input.readExactly(4)).order(ByteOrder.BIG_ENDIAN).int
        require(responseSize in 1..1_048_576) { "Invalid CouchLink frame size: $responseSize" }
        return JSONObject(String(input.readExactly(responseSize), Charsets.UTF_8))
    }

    private fun java.io.InputStream.readExactly(count: Int): ByteArray {
        val result = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = read(result, offset, count - offset)
            if (read < 0) error("Connection ended inside a CouchLink frame")
            offset += read
        }
        return result
    }

    companion object {
        const val DISCOVERY_PORT = 45820
        const val DESKTOP_PORT = 45821
        const val PRELOGIN_PORT = 45822
    }
}
