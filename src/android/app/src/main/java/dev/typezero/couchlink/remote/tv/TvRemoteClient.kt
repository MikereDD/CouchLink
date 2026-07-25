package dev.typezero.couchlink.remote.tv

import android.content.Context
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
    )

    @Volatile private var socket: SSLSocket? = null
    @Volatile private var output: BufferedOutputStream? = null
    @Volatile private var readerThread: Thread? = null
    private val writeLock = Any()
    private var onState: ((Connection) -> Unit)? = null

    fun connect(host: String, stateCallback: (Connection) -> Unit) {
        close()
        onState = stateCallback
        stateCallback(Connection(connecting = true, message = "Connecting to $host…"))
        Thread({ runConnection(host) }, "CouchLink-TV-Remote").apply {
            isDaemon = true
            start()
            readerThread = this
        }
    }

    fun sendKey(keyCode: RemoteKeyCode): Boolean {
        val stream = output ?: return false
        val message = RemoteMessage.newBuilder()
            .setRemoteKeyInject(
                RemoteKeyInject.newBuilder()
                    .setKeyCode(keyCode)
                    .setDirection(RemoteDirection.SHORT),
            ).build()
        return runCatching {
            synchronized(writeLock) { writeMessage(stream, message) }
            true
        }.getOrElse {
            onState?.invoke(Connection(message = "TV command failed: ${it.message ?: it.javaClass.simpleName}"))
            false
        }
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
        output = null
        readerThread = null
    }

    private fun runConnection(host: String) {
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
            onState?.invoke(Connection(connected = true, message = "Connected to $host. Waiting for TV…"))

            while (!remoteSocket.isClosed) {
                val message = readMessage(input)
                handleMessage(message, stream)
            }
        } catch (error: Throwable) {
            if (socket != null || error !is java.net.SocketException) {
                onState?.invoke(Connection(message = "TV remote offline: ${error.message ?: error.javaClass.simpleName}"))
            }
        } finally {
            runCatching { socket?.close() }
            socket = null
            output = null
        }
    }

    private fun handleMessage(message: RemoteMessage, stream: BufferedOutputStream) {
        when {
            message.hasRemoteConfigure() -> {
                val supported = message.remoteConfigure.code1
                val active = supported and REQUESTED_FEATURES
                val reply = RemoteMessage.newBuilder().setRemoteConfigure(
                    RemoteConfigure.newBuilder()
                        .setCode1(active)
                        .setDeviceInfo(
                            RemoteDeviceInfo.newBuilder()
                                .setUnknown1(1)
                                .setUnknown2("1")
                                .setPackageName("dev.typezero.couchlink.remote")
                                .setAppVersion("1.2-dev.3"),
                        ),
                ).build()
                synchronized(writeLock) { writeMessage(stream, reply) }
            }
            message.hasRemoteSetActive() -> {
                val reply = RemoteMessage.newBuilder()
                    .setRemoteSetActive(RemoteSetActive.newBuilder().setActive(REQUESTED_FEATURES))
                    .build()
                synchronized(writeLock) { writeMessage(stream, reply) }
            }
            message.hasRemotePingRequest() -> {
                val reply = RemoteMessage.newBuilder()
                    .setRemotePingResponse(
                        RemotePingResponse.newBuilder().setVal1(message.remotePingRequest.val1),
                    ).build()
                synchronized(writeLock) { writeMessage(stream, reply) }
            }
            message.hasRemoteStart() -> {
                onState?.invoke(
                    Connection(
                        connected = true,
                        ready = message.remoteStart.started,
                        message = if (message.remoteStart.started) "TV remote ready." else "TV is in standby.",
                    ),
                )
            }
        }
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

    private fun writeMessage(output: BufferedOutputStream, message: RemoteMessage) {
        val payload = message.toByteArray()
        writeVarInt(output, payload.size)
        output.write(payload)
        output.flush()
    }

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
        const val REQUESTED_FEATURES = 1 or 2 or 32 or 64 or 512
        const val PAIRING_PREFERENCES = "couchlink_tv_remote"
        const val KEY_SERVER_FINGERPRINT = "paired_tv_server_fingerprint"
        val TRUST_SERVER: TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}
