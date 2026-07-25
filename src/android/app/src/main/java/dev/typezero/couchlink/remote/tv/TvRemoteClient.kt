package dev.typezero.couchlink.remote.tv

import android.content.Context
import dev.typezero.couchlink.remote.BuildConfig
import dev.typezero.couchlink.remote.tv.proto.RemoteConfigure
import dev.typezero.couchlink.remote.tv.proto.RemoteDeviceInfo
import dev.typezero.couchlink.remote.tv.proto.RemoteDirection
import dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode
import dev.typezero.couchlink.remote.tv.proto.RemoteKeyInject
import dev.typezero.couchlink.remote.tv.proto.RemoteMessage
import dev.typezero.couchlink.remote.tv.proto.RemotePingResponse
import dev.typezero.couchlink.remote.tv.proto.RemoteSetActive
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.Principal
import java.security.SecureRandom
import java.security.cert.X509Certificate
import android.util.Log
import java.io.File
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

internal class TvRemoteClient(private val context: Context) : AutoCloseable {
    internal data class Connection(
        val connecting: Boolean = false,
        val connected: Boolean = false,
        val ready: Boolean = false,
        val message: String = "TV remote is offline.",
        val lastCommand: String? = null,
        val commandsSent: Long = 0,
        val trace: String = "No wire activity yet.",
    )

    @Volatile private var socket: SSLSocket? = null
    @Volatile private var output: BufferedOutputStream? = null
    @Volatile private var readerThread: Thread? = null
    private val writeLock = Any()
    private var onState: ((Connection) -> Unit)? = null
    @Volatile private var negotiatedFeatures: Int = 0
    @Volatile private var currentConnection = Connection()
    // Monotonic generation token. Each connect() bumps it; a reader thread only
    // reports state/errors while it still owns the current epoch. This prevents a
    // superseded thread (torn down by a reconnect) from clobbering the live
    // connection with a spurious "Socket closed" message.
    @Volatile private var epoch = 0

    fun connect(host: String, stateCallback: (Connection) -> Unit) {
        close()
        val myEpoch = ++epoch
        onState = stateCallback
        updateState(Connection(connecting = true, message = "Connecting to $host…"))
        Thread({ runConnection(host, myEpoch) }, "CouchLink-TV-Remote").apply {
            isDaemon = true
            start()
            readerThread = this
        }
    }

    fun sendKey(keyCode: RemoteKeyCode): Boolean {
        val stream = output ?: return false
        // Reference remote clients advertise a fixed capability code and do NOT gate
        // key delivery on the TV's advertised mask, which varies by firmware. Refusing
        // here caused every press to fail and trigger a reconnect, surfacing as
        // "Socket closed". We only need a live output stream to send a key.

        val keyTap = keyMessage(keyCode, RemoteDirection.SHORT)
        return runCatching {
            synchronized(writeLock) {
                writeMessage(stream, keyTap, "KEY:${keyCode.name}:SHORT")
            }
            updateState(
                currentConnection.copy(
                    lastCommand = keyCode.name,
                    commandsSent = currentConnection.commandsSent + 1,
                    message = "Sent ${keyCode.name.removePrefix("KEYCODE_")}",
                    trace = currentConnection.trace,
                ),
            )
            true
        }.getOrElse {
            updateState(currentConnection.copy(connected = false, ready = false, message = "TV command failed: ${it.message ?: it.javaClass.simpleName}"))
            false
        }
    }

    private fun keyMessage(keyCode: RemoteKeyCode, direction: RemoteDirection): RemoteMessage =
        RemoteMessage.newBuilder()
            .setRemoteKeyInject(
                RemoteKeyInject.newBuilder()
                    .setKeyCode(keyCode)
                    .setDirection(direction),
            )
            .build()

    override fun close() {
        epoch++
        runCatching { socket?.close() }
        socket = null
        output = null
        readerThread = null
        negotiatedFeatures = 0
        currentConnection = Connection()
    }

    private fun runConnection(host: String, myEpoch: Int) {
        try {
            val identity = TvTlsIdentityStore(context).loadOrCreate()
            val sslContext = SSLContext.getInstance("TLSv1.2").apply {
                init(arrayOf(SingleIdentityKeyManager(identity)), arrayOf(TRUST_SERVER), SecureRandom())
            }
            val remoteSocket = (sslContext.socketFactory.createSocket() as SSLSocket).apply {
                enabledProtocols = arrayOf("TLSv1.2")
                connect(InetSocketAddress(host, REMOTE_PORT), CONNECT_TIMEOUT_MS)
                soTimeout = 0
                startHandshake()
            }
            verifyPinnedCertificate(remoteSocket, host)
            val input = BufferedInputStream(remoteSocket.inputStream)
            val stream = BufferedOutputStream(remoteSocket.outputStream)
            socket = remoteSocket
            output = stream
            updateState(Connection(connected = true, message = "Connected to $host. Waiting for TV…"))

            while (!remoteSocket.isClosed) {
                val message = readMessage(input)
                traceInbound(message)
                handleMessage(message, stream)
            }
        } catch (error: Throwable) {
            // Only the current connection attempt is allowed to report an error.
            // A superseded thread (its socket closed by a reconnect) stays silent
            // instead of overwriting the live state with "Socket closed".
            if (myEpoch == epoch) {
                updateState(currentConnection.copy(connected = false, ready = false, message = "TV remote offline: ${error.message ?: error.javaClass.simpleName}"))
            }
        } finally {
            if (myEpoch == epoch) {
                runCatching { socket?.close() }
                socket = null
                output = null
            }
        }
    }

    private fun handleMessage(message: RemoteMessage, stream: BufferedOutputStream) {
        when {
            message.hasRemoteConfigure() -> {
                // Advertise a fixed capability code (like the reference clients) rather
                // than ANDing with the TV's advertised mask, whose bit layout varies by
                // firmware. model/vendor are populated because some Google TV builds are
                // pickier about the configure reply.
                negotiatedFeatures = REQUESTED_FEATURES
                val reply = RemoteMessage.newBuilder().setRemoteConfigure(
                    RemoteConfigure.newBuilder()
                        .setCode1(REQUESTED_FEATURES)
                        .setDeviceInfo(
                            RemoteDeviceInfo.newBuilder()
                                .setModel(android.os.Build.MODEL)
                                .setVendor(android.os.Build.MANUFACTURER)
                                .setUnknown1(1)
                                .setUnknown2("1")
                                .setPackageName(context.packageName)
                                .setAppVersion(BuildConfig.VERSION_NAME),
                        ),
                ).build()
                synchronized(writeLock) { writeMessage(stream, reply, "CONFIGURE:${REQUESTED_FEATURES}") }
            }
            message.hasRemoteSetActive() -> {
                val reply = RemoteMessage.newBuilder()
                    .setRemoteSetActive(RemoteSetActive.newBuilder().setActive(negotiatedFeatures))
                    .build()
                synchronized(writeLock) { writeMessage(stream, reply, "SET_ACTIVE:$negotiatedFeatures") }
            }
            message.hasRemotePingRequest() -> {
                val reply = RemoteMessage.newBuilder()
                    .setRemotePingResponse(
                        RemotePingResponse.newBuilder().setVal1(message.remotePingRequest.val1),
                    ).build()
                synchronized(writeLock) { writeMessage(stream, reply, "PING_RESPONSE:${message.remotePingRequest.val1}") }
            }
            message.hasRemoteError() -> {
                updateState(currentConnection.copy(message = "TV rejected the last remote message."))
            }
            message.hasRemoteStart() -> {
                updateState(
                    currentConnection.copy(
                        connected = true,
                        ready = message.remoteStart.started,
                        message = if (message.remoteStart.started) "TV remote ready." else "TV is in standby.",
                    ),
                )
            }
        }
    }

    private fun updateState(connection: Connection) {
        currentConnection = connection
        onState?.invoke(connection)
    }

    private fun verifyPinnedCertificate(socket: SSLSocket, host: String) {
        val expected = context.getSharedPreferences(PAIRING_PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_SERVER_FINGERPRINT, null)
            ?: error("No paired TV certificate is stored.")
        val certificate = socket.session.peerCertificates.firstOrNull() as? X509Certificate
            ?: error("The TV did not provide a certificate.")
        val actual = MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, ignoreCase = true)) {
            "The certificate from $host does not match the paired TV."
        }
    }

    private fun writeMessage(output: BufferedOutputStream, message: RemoteMessage, label: String = "MESSAGE") {
        val payload = message.toByteArray()
        val hex = payload.toHex()
        recordTrace("TX $label [$hex]")
        updateState(currentConnection.copy(trace = "TX $label [$hex]"))
        writeVarInt(output, payload.size)
        output.write(payload)
        output.flush()
    }


    private fun traceInbound(message: RemoteMessage) {
        val payload = message.toByteArray()
        val type = when {
            message.hasRemoteConfigure() -> "CONFIGURE:${message.remoteConfigure.code1}"
            message.hasRemoteSetActive() -> "SET_ACTIVE:${message.remoteSetActive.active}"
            message.hasRemotePingRequest() -> "PING_REQUEST:${message.remotePingRequest.val1}"
            message.hasRemotePingResponse() -> "PING_RESPONSE:${message.remotePingResponse.val1}"
            message.hasRemoteStart() -> "START:${message.remoteStart.started}"
            message.hasRemoteError() -> "ERROR:${message.remoteError.value}"
            else -> "OTHER"
        }
        val trace = "RX $type [${payload.toHex()}]"
        recordTrace(trace)
        updateState(currentConnection.copy(trace = trace))
    }

    private fun recordTrace(line: String) {
        if (!BuildConfig.DEBUG) return

        Log.d(TAG, line)
        runCatching {
            val traceFile = File(context.filesDir, TRACE_FILE)
            val entry = "${System.currentTimeMillis()} $line\n"

            if (traceFile.exists() && traceFile.length() + entry.toByteArray().size > MAX_TRACE_BYTES) {
                val existing = traceFile.readBytes()
                val keepFrom = (existing.size - TRACE_RETAIN_BYTES).coerceAtLeast(0)
                val retained = existing.copyOfRange(keepFrom, existing.size)
                traceFile.writeBytes(retained)
            }

            traceFile.appendText(entry)
        }
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private fun readMessage(input: BufferedInputStream): RemoteMessage {
        val size = readVarInt(input)
        require(size in 1..MAX_MESSAGE_SIZE) { "Invalid TV message size: $size" }
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val count = input.read(bytes, offset, size - offset)
            check(count >= 0) { "TV closed the remote connection." }
            offset += count
        }
        return RemoteMessage.parseFrom(bytes)
    }

    private fun writeVarInt(output: BufferedOutputStream, value: Int) {
        var remaining = value
        while (true) {
            if (remaining and 0x7f.inv() == 0) {
                output.write(remaining)
                return
            }
            output.write((remaining and 0x7f) or 0x80)
            remaining = remaining ushr 7
        }
    }

    private fun readVarInt(input: BufferedInputStream): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val next = input.read()
            check(next >= 0) { "TV closed the remote connection." }
            result = result or ((next and 0x7f) shl shift)
            if (next and 0x80 == 0) return result
            shift += 7
        }
        error("Invalid TV message length.")
    }

    private class SingleIdentityKeyManager(
        private val identity: TvTlsIdentityStore.Identity,
    ) : X509ExtendedKeyManager() {
        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: java.net.Socket?) = ALIAS
        override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: javax.net.ssl.SSLEngine?) = ALIAS
        override fun getCertificateChain(alias: String?) = arrayOf(identity.certificate)
        override fun getPrivateKey(alias: String?) = identity.privateKey
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(ALIAS)
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: java.net.Socket?) = null
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = null
        override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: javax.net.ssl.SSLEngine?) = null
        private companion object { const val ALIAS = "couchlink-tv" }
    }

    private companion object {
        const val REMOTE_PORT = 6466
        const val CONNECT_TIMEOUT_MS = 5_000
        const val MAX_MESSAGE_SIZE = 4 * 1024 * 1024
        const val FEATURE_KEY = 2
        // Android TV Remote v2 feature mask used by the Google TV app family:
        // KEY(2) | IME(4) | VOICE(8) | POWER(32) | VOLUME(64) | APP_LINK(512) = 622.
        const val REQUESTED_FEATURES = 622
        const val TAG = "CouchLinkTvWire"
        const val TRACE_FILE = "couchlink-tv-wire.txt"
        const val MAX_TRACE_BYTES = 256 * 1024
        const val TRACE_RETAIN_BYTES = 192 * 1024
        const val PAIRING_PREFERENCES = "couchlink_tv_remote"
        const val KEY_SERVER_FINGERPRINT = "paired_tv_server_fingerprint"
        val TRUST_SERVER: TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}
