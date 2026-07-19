package dev.typezero.couchlink.remote.network

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
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

data class PairResult(
    val success: Boolean,
    val message: String,
)

class DiscoveryClient(context: Context) {
    private val preferences =
        context.getSharedPreferences("couchlink_pairing", Context.MODE_PRIVATE)

    private val writeMutex = Mutex()

    @Volatile
    private var activeSocket: Socket? = null

    val clientId: String =
        preferences.getString("client_id", null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit()
                    .putString("client_id", it)
                    .apply()
            }

    fun hasTrustedToken(host: DiscoveredHost): Boolean =
        !preferences.getString("token_${host.hostId}", null).isNullOrBlank()

    fun lastKnownHost(): DiscoveredHost? {
        val hostId = preferences.getString("last_host_id", null) ?: return null

        return DiscoveredHost(
            hostId = hostId,
            hostName = preferences.getString(
                "last_host_name",
                "Windows PC",
            ) ?: "Windows PC",
            hostVersion = preferences.getString(
                "last_host_version",
                "",
            ) ?: "",
            hostState = "Offline",
            address = preferences.getString(
                "last_host_address",
                "",
            ) ?: "",
            sessionPort = preferences.getInt(
                "last_host_port",
                DESKTOP_PORT,
            ),
            pairingRequired = true,
            macAddress = preferences.getString(
                "last_host_mac",
                "",
            ) ?: "",
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

    suspend fun sendWakeOnLan(
        host: DiscoveredHost,
    ): Boolean = withContext(Dispatchers.IO) {
        val clean = host.macAddress
            .replace(":", "")
            .replace("-", "")

        if (clean.length != 12) {
            return@withContext false
        }

        val mac = ByteArray(6) { index ->
            clean.substring(
                index * 2,
                index * 2 + 2,
            ).toInt(16).toByte()
        }

        val packet = ByteArray(102) {
            0xFF.toByte()
        }

        for (index in 0 until 16) {
            mac.copyInto(
                destination = packet,
                destinationOffset = 6 + index * 6,
            )
        }

        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(
                DatagramPacket(
                    packet,
                    packet.size,
                    InetAddress.getByName("255.255.255.255"),
                    9,
                ),
            )
        }

        true
    }

    fun disconnect() {
        runCatching {
            activeSocket?.close()
        }

        activeSocket = null
    }

    suspend fun listen(
        onHost: (DiscoveredHost) -> Unit,
    ) = withContext(Dispatchers.IO) {
        DatagramSocket(null).use { socket ->
            socket.reuseAddress = true
            socket.bind(
                InetSocketAddress(DISCOVERY_PORT),
            )
            socket.soTimeout = 2500

            while (isActive) {
                try {
                    val buffer = ByteArray(4096)
                    val packet = DatagramPacket(
                        buffer,
                        buffer.size,
                    )

                    socket.receive(packet)

                    val json = JSONObject(
                        String(
                            packet.data,
                            0,
                            packet.length,
                            Charsets.UTF_8,
                        ),
                    )

                    if (json.optString("product") != "CouchLink") {
                        continue
                    }

                    onHost(
                        DiscoveredHost(
                            hostId = json.getString("hostId"),
                            hostName = json.getString("hostName"),
                            hostVersion = json.getString("hostVersion"),
                            hostState = json.getString("hostState"),
                            address = packet.address.hostAddress
                                ?: json.getString("address"),
                            sessionPort = json.getInt("sessionPort"),
                            pairingRequired =
                                json.getBoolean("pairingRequired"),
                            macAddress =
                                json.optString("macAddress"),
                        ).also {
                            saveLastHost(it)
                        },
                    )
                } catch (_: java.net.SocketTimeoutException) {
                    // Continue listening.
                }
            }
        }
    }

    suspend fun beginPairing(
        host: DiscoveredHost,
    ): HelloResult = withContext(Dispatchers.IO) {
        request(
            host = host,
            type = "hello",
            payloadBody = helloPayload(host),
        ) { response ->
            decodeHello(response)
        }
    }

    suspend fun pair(
        host: DiscoveredHost,
        code: String,
    ): PairResult = withContext(Dispatchers.IO) {
        request(
            host = host,
            type = "pair_request",
            payloadBody = JSONObject()
                .put("clientId", clientId)
                .put("clientName", Build.MODEL)
                .put("pairingCode", code),
        ) { response ->
            require(
                response.getString("type") == "pair_result",
            ) {
                "Unexpected response: ${response.getString("type")}"
            }

            val body = response.getJSONObject("payload")
            val success = body.getBoolean("success")

            if (success) {
                preferences.edit()
                    .putString(
                        "token_${host.hostId}",
                        body.getString("pairingToken"),
                    )
                    .apply()
            }

            PairResult(
                success = success,
                message = body.getString("message"),
            )
        }
    }

    suspend fun sendMouseMove(
        deltaX: Int,
        deltaY: Int,
    ) = sendActive(
        type = "mouse_move",
        payload = JSONObject()
            .put(
                "deltaX",
                deltaX.coerceIn(-250, 250),
            )
            .put(
                "deltaY",
                deltaY.coerceIn(-250, 250),
            ),
    )

    suspend fun sendMouseButton(
        button: String,
        action: String = "click",
    ) = sendActive(
        type = "mouse_button",
        payload = JSONObject()
            .put("button", button)
            .put("action", action),
    )

    suspend fun sendMouseScroll(
        delta: Int,
    ) = sendActive(
        type = "mouse_scroll",
        payload = JSONObject()
            .put(
                "delta",
                delta.coerceIn(-1200, 1200),
            ),
    )

    suspend fun sendKeyboardText(
        text: String,
    ) = sendActive(
        type = "keyboard_text",
        payload = JSONObject()
            .put(
                "text",
                text.take(1024),
            ),
    )

    suspend fun sendKeyPress(
        key: String,
    ) = sendActive(
        type = "key_press",
        payload = JSONObject()
            .put("key", key),
    )

    suspend fun sendShortcut(
        shortcut: String,
    ) = sendActive(
        type = "shortcut",
        payload = JSONObject()
            .put("shortcut", shortcut),
    )

    suspend fun sendLauncherAction(
        launcher: String,
        action: String = "launch",
    ) = sendActive(
        type = "launcher_action",
        payload = JSONObject()
            .put("launcher", launcher)
            .put("action", action),
    )

    suspend fun runPersistentSession(
        host: DiscoveredHost,
        onStatus: (
            message: String,
            connected: Boolean,
            remoteInputEnabled: Boolean,
        ) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var attempt = 0

        while (coroutineContext.isActive) {
            attempt++

            var connectedThisPass = false

            val candidatePorts = listOf(
                PRELOGIN_PORT,
                host.sessionPort,
                DESKTOP_PORT,
            ).distinct()

            for (port in candidatePorts) {
                if (!coroutineContext.isActive) {
                    break
                }

                try {
                    onStatus(
                        if (attempt == 1) {
                            "Connecting to ${host.hostName}:$port…"
                        } else {
                            "Reconnecting to ${host.hostName}:$port…"
                        },
                        false,
                        false,
                    )

                    Socket().use { socket ->
                        activeSocket = socket

                        Log.i(
                            SESSION_LOG_TAG,
                            "Attempting ${host.address}:$port",
                        )

                        socket.connect(
                            InetSocketAddress(
                                InetAddress.getByName(host.address),
                                port,
                            ),
                            3000,
                        )

                        socket.soTimeout = 12_000
                        socket.tcpNoDelay = true

                        writeEnvelope(
                            socket = socket,
                            type = "hello",
                            payloadBody = helloPayload(host),
                        )

                        val hello = decodeHello(
                            readEnvelope(socket),
                        )

                        Log.i(
                            SESSION_LOG_TAG,
                            "Hello from port $port: " +
                                "state=${hello.hostState}, " +
                                "trusted=${hello.trusted}",
                        )

                        require(hello.trusted) {
                            "Host requires pairing again."
                        }

                        /*
                         * The Boot Service remains reachable after login for
                         * diagnostics. Do not remain attached to it once the
                         * desktop Session Host is available.
                         */
                        if (
                            port == PRELOGIN_PORT &&
                            hello.hostState != "SignInRequired"
                        ) {
                            Log.i(
                                SESSION_LOG_TAG,
                                "Desktop available; leaving " +
                                    "pre-login port $port",
                            )

                            invalidateActiveSocket(socket)
                            return@use
                        }

                        val ready = readEnvelope(socket)

                        require(
                            ready.getString("type") == "session_ready",
                        ) {
                            "Host did not open a persistent session."
                        }

                        val body = ready.getJSONObject("payload")

                        val heartbeatSeconds = body
                            .optInt("heartbeatSeconds", 5)
                            .coerceIn(2, 10)

                        var inputEnabled =
                            body.optBoolean(
                                "remoteInputEnabled",
                                false,
                            )

                        onStatus(
                            "Connected to ${hello.hostName}:$port",
                            true,
                            inputEnabled,
                        )

                        Log.i(
                            SESSION_LOG_TAG,
                            "Persistent session connected on port $port",
                        )

                        attempt = 0
                        connectedThisPass = true

                        var sequence = 0L

                        while (
                            coroutineContext.isActive &&
                            !socket.isClosed
                        ) {
                            sequence++

                            writeMutex.withLock {
                                writeEnvelope(
                                    socket = socket,
                                    type = "ping",
                                    payloadBody = JSONObject()
                                        .put(
                                            "sequence",
                                            sequence,
                                        ),
                                )
                            }

                            val response = readEnvelope(socket)

                            when (response.getString("type")) {
                                "pong" -> {
                                    inputEnabled = response
                                        .getJSONObject("payload")
                                        .optBoolean(
                                            "remoteInputEnabled",
                                            false,
                                        )

                                    onStatus(
                                        "Connected to ${hello.hostName}:$port " +
                                            "• heartbeat $sequence",
                                        true,
                                        inputEnabled,
                                    )
                                }

                                "error" -> {
                                    val payload =
                                        response.getJSONObject("payload")
                                    val code =
                                        payload.optString("code")

                                    if (
                                        port == PRELOGIN_PORT &&
                                        code == "desktop_session_available"
                                    ) {
                                        Log.i(
                                            SESSION_LOG_TAG,
                                            "Desktop session became available; " +
                                                "leaving pre-login port $port",
                                        )

                                        invalidateActiveSocket(socket)
                                        break
                                    }

                                    val message = payload.optString(
                                        "message",
                                        "CouchLink host returned an error.",
                                    )

                                    error(
                                        if (code.isBlank()) {
                                            message
                                        } else {
                                            "$code: $message"
                                        },
                                    )
                                }

                                else -> {
                                    error(
                                        "Unexpected heartbeat response: " +
                                            response.getString("type"),
                                    )
                                }
                            }

                            delay(
                                heartbeatSeconds * 1000L,
                            )
                        }
                    }
                } catch (error: Exception) {
                    Log.w(
                        SESSION_LOG_TAG,
                        "Port $port failed",
                        error,
                    )

                    activeSocket?.let {
                        invalidateActiveSocket(it)
                    }

                    if (!coroutineContext.isActive) {
                        break
                    }

                    onStatus(
                        "${host.hostName}:$port unavailable: " +
                            (error.message ?: "connection lost"),
                        false,
                        false,
                    )

                    continue
                } finally {
                    val socket = activeSocket

                    if (
                        socket != null &&
                        socket.isClosed
                    ) {
                        activeSocket = null
                    }
                }

                /*
                 * A completed session should restart endpoint selection from
                 * the beginning.
                 */
                if (connectedThisPass) {
                    break
                }
            }

            if (!coroutineContext.isActive) {
                break
            }

            delay(
                if (connectedThisPass) {
                    750
                } else {
                    1500
                },
            )
        }
    }

    private suspend fun sendActive(
        type: String,
        payload: JSONObject,
    ) = withContext(Dispatchers.IO) {
        val socket = activeSocket
            ?: return@withContext

        if (
            socket.isClosed ||
            !socket.isConnected
        ) {
            return@withContext
        }

        try {
            writeMutex.withLock {
                if (
                    activeSocket !== socket ||
                    socket.isClosed ||
                    !socket.isConnected
                ) {
                    return@withLock
                }

                writeEnvelope(
                    socket = socket,
                    type = type,
                    payloadBody = payload,
                )
            }
        } catch (_: IOException) {
            /*
             * A pre-login broker can deliberately close the socket during the
             * handoff to the desktop Session Host. Treat that as a reconnect
             * signal instead of allowing the exception to crash Compose.
             */
            invalidateActiveSocket(socket)
        } catch (_: IllegalStateException) {
            invalidateActiveSocket(socket)
        }
    }

    private fun invalidateActiveSocket(
        socket: Socket,
    ) {
        if (activeSocket === socket) {
            activeSocket = null
        }

        runCatching {
            socket.close()
        }
    }

    private fun helloPayload(
        host: DiscoveredHost,
    ) = JSONObject()
        .put("clientId", clientId)
        .put("clientName", Build.MODEL)
        .put("clientPlatform", "Android")
        .put("clientVersion", "0.1-dev.12.1")
        .put(
            "pairingToken",
            preferences.getString(
                "token_${host.hostId}",
                null,
            ),
        )

    private fun decodeHello(
        response: JSONObject,
    ): HelloResult {
        require(
            response.getString("type") == "hello_ack",
        ) {
            "Unexpected response: ${response.getString("type")}"
        }

        val body = response.getJSONObject("payload")

        return HelloResult(
            hostName = body.getString("hostName"),
            hostVersion = body.getString("hostVersion"),
            hostState = body.getString("hostState"),
            pairingRequired =
                body.getBoolean("pairingRequired"),
            trusted = body.getBoolean("trusted"),
        )
    }

    private fun <T> request(
        host: DiscoveredHost,
        type: String,
        payloadBody: JSONObject,
        decode: (JSONObject) -> T,
    ): T {
        Socket().use { socket ->
            socket.connect(
                InetSocketAddress(
                    InetAddress.getByName(host.address),
                    host.sessionPort,
                ),
                4000,
            )

            socket.soTimeout = 5000
            socket.tcpNoDelay = true

            writeEnvelope(
                socket = socket,
                type = type,
                payloadBody = payloadBody,
            )

            return decode(
                readEnvelope(socket),
            )
        }
    }

    private fun writeEnvelope(
        socket: Socket,
        type: String,
        payloadBody: JSONObject,
    ) {
        val envelope = JSONObject()
            .put("protocolVersion", 1)
            .put(
                "messageId",
                UUID.randomUUID().toString(),
            )
            .put("type", type)
            .put(
                "sentAtUtc",
                Instant.now().toString(),
            )
            .put("payload", payloadBody)

        val bytes = envelope
            .toString()
            .toByteArray(Charsets.UTF_8)

        socket.getOutputStream().apply {
            write(
                ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putInt(bytes.size)
                    .array(),
            )

            write(bytes)
            flush()
        }
    }

    private fun readEnvelope(
        socket: Socket,
    ): JSONObject {
        val input = socket.getInputStream()

        val responseSize = ByteBuffer
            .wrap(input.readExactly(4))
            .order(ByteOrder.BIG_ENDIAN)
            .int

        require(
            responseSize in 1..1_048_576,
        ) {
            "Invalid CouchLink frame size: $responseSize"
        }

        return JSONObject(
            String(
                input.readExactly(responseSize),
                Charsets.UTF_8,
            ),
        )
    }

    private fun java.io.InputStream.readExactly(
        count: Int,
    ): ByteArray {
        val result = ByteArray(count)
        var offset = 0

        while (offset < count) {
            val read = read(
                result,
                offset,
                count - offset,
            )

            if (read < 0) {
                error(
                    "Connection ended inside a CouchLink frame",
                )
            }

            offset += read
        }

        return result
    }

    companion object {
        private const val SESSION_LOG_TAG =
            "CouchLinkSession"

        const val DISCOVERY_PORT = 45820
        const val DESKTOP_PORT = 45821
        const val PRELOGIN_PORT = 45822
    }
}