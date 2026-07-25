package dev.typezero.couchlink.remote.tv

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.polo.wire.protobuf.PoloProto
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

internal class TvPairingClient(
    private val context: Context,
) {
    internal data class PendingPairing(
        val host: String,
        val socket: SSLSocket,
        val input: BufferedInputStream,
        val output: BufferedOutputStream,
        val serverCertificate: X509Certificate,
    ) : AutoCloseable {
        override fun close() = socket.close()
    }

    fun begin(host: String, clientName: String = "CouchLink"): PendingPairing {
        ensureClientIdentity()
        val sslContext = buildSslContext()
        val socket = (sslContext.socketFactory.createSocket() as SSLSocket).apply {
            enabledProtocols = enabledProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
            connect(InetSocketAddress(host, PAIRING_PORT), CONNECT_TIMEOUT_MS)
            soTimeout = IO_TIMEOUT_MS
            startHandshake()
        }
        val input = BufferedInputStream(socket.inputStream)
        val output = BufferedOutputStream(socket.outputStream)
        val serverCertificate = socket.session.peerCertificates.first() as X509Certificate

        val request = baseMessage().toBuilder()
            .setPairingRequest(
                PoloProto.PairingRequest.newBuilder()
                    .setClientName(clientName)
                    .setServiceName("atvremote"),
            ).build()
        writeMessage(output, request)

        val ack = readMessage(input)
        checkOk(ack)
        check(ack.hasPairingRequestAck()) { "TV did not acknowledge the pairing request." }

        val encoding = PoloProto.Options.Encoding.newBuilder()
            .setType(PoloProto.Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
            .setSymbolLength(6)
            .build()
        val options = baseMessage().toBuilder()
            .setOptions(
                PoloProto.Options.newBuilder()
                    .setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                    .addInputEncodings(encoding),
            ).build()
        writeMessage(output, options)

        val serverOptions = readMessage(input)
        checkOk(serverOptions)
        check(serverOptions.hasOptions()) { "TV did not return pairing options." }

        val configuration = baseMessage().toBuilder()
            .setConfiguration(
                PoloProto.Configuration.newBuilder()
                    .setClientRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                    .setEncoding(encoding),
            ).build()
        writeMessage(output, configuration)

        val configurationAck = readMessage(input)
        checkOk(configurationAck)
        check(configurationAck.hasConfigurationAck()) { "TV did not accept the pairing configuration." }

        return PendingPairing(host, socket, input, output, serverCertificate)
    }

    fun finish(pending: PendingPairing, code: String): String {
        val normalized = code.trim().uppercase()
        require(normalized.matches(Regex("[0-9A-F]{6}"))) {
            "Enter the six-character hexadecimal code shown on the TV."
        }

        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val clientCertificate = keyStore.getCertificate(CLIENT_ALIAS) as X509Certificate
        val secret = computeSecret(clientCertificate, pending.serverCertificate, normalized)

        val message = baseMessage().toBuilder()
            .setSecret(PoloProto.Secret.newBuilder().setSecret(com.google.protobuf.ByteString.copyFrom(secret)))
            .build()
        writeMessage(pending.output, message)

        val ack = readMessage(pending.input)
        checkOk(ack)
        check(ack.hasSecretAck()) { "The TV rejected the pairing code." }

        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(pending.serverCertificate.encoded)
            .joinToString("") { "%02x".format(it) }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_PAIRED_HOST, pending.host)
            .putString(KEY_SERVER_FINGERPRINT, fingerprint)
            .apply()
        pending.close()
        return fingerprint
    }

    fun isPaired(host: String): Boolean {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PAIRED_HOST, null) == host &&
            !prefs.getString(KEY_SERVER_FINGERPRINT, null).isNullOrBlank()
    }

    private fun ensureClientIdentity() {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (keyStore.containsAlias(CLIENT_ALIAS)) return

        val start = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.YEAR, 20) }
        val spec = KeyGenParameterSpec.Builder(
            CLIENT_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(javax.security.auth.x500.X500Principal("CN=CouchLink TV Remote"))
            .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            .setCertificateNotBefore(start.time)
            .setCertificateNotAfter(end.time)
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE).apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    private fun buildSslContext(): SSLContext {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, null)
        }
        val managers = factory.keyManagers.map { manager ->
            if (manager is X509ExtendedKeyManager) ForcedAliasKeyManager(manager, CLIENT_ALIAS) else manager
        }.toTypedArray()
        return SSLContext.getInstance("TLS").apply {
            init(managers, arrayOf(TRUST_PAIRING_SERVER), SecureRandom())
        }
    }

    private fun computeSecret(
        clientCertificate: X509Certificate,
        serverCertificate: X509Certificate,
        code: String,
    ): ByteArray {
        val clientKey = clientCertificate.publicKey as java.security.interfaces.RSAPublicKey
        val serverKey = serverCertificate.publicKey as java.security.interfaces.RSAPublicKey
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(unsignedBytes(clientKey.modulus))
        digest.update(exponentBytes(clientKey.publicExponent))
        digest.update(unsignedBytes(serverKey.modulus))
        digest.update(exponentBytes(serverKey.publicExponent))
        digest.update(hexToBytes(code.substring(2)))
        val result = digest.digest()
        require((result[0].toInt() and 0xff) == code.substring(0, 2).toInt(16)) {
            "The pairing code does not match this TV. Check the code and try again."
        }
        return result
    }

    private fun unsignedBytes(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return if (bytes.size > 1 && bytes[0].toInt() == 0) bytes.copyOfRange(1, bytes.size) else bytes
    }

    private fun exponentBytes(value: BigInteger): ByteArray {
        val hex = value.toString(16).uppercase().let { if (it.length % 2 == 0) it else "0$it" }
        return hexToBytes(hex)
    }

    private fun hexToBytes(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun baseMessage(): PoloProto.OuterMessage = PoloProto.OuterMessage.newBuilder()
        .setProtocolVersion(2)
        .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)
        .build()

    private fun checkOk(message: PoloProto.OuterMessage) {
        check(message.status == PoloProto.OuterMessage.Status.STATUS_OK) {
            "TV pairing failed with status ${message.status}."
        }
    }

    private fun writeMessage(output: BufferedOutputStream, message: PoloProto.OuterMessage) {
        val payload = message.toByteArray()
        writeVarInt(output, payload.size)
        output.write(payload)
        output.flush()
    }

    private fun readMessage(input: BufferedInputStream): PoloProto.OuterMessage {
        val size = readVarInt(input)
        require(size in 1..MAX_MESSAGE_SIZE) { "Invalid pairing message size: $size" }
        return PoloProto.OuterMessage.parseFrom(input.readNBytesExact(size))
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
            check(next >= 0) { "TV closed the pairing connection." }
            result = result or ((next and 0x7f) shl shift)
            if (next and 0x80 == 0) return result
            shift += 7
        }
        error("Invalid pairing message length.")
    }

    private fun BufferedInputStream.readNBytesExact(size: Int): ByteArray {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = read(bytes, offset, size - offset)
            check(read >= 0) { "TV closed the pairing connection." }
            offset += read
        }
        return bytes
    }

    private class ForcedAliasKeyManager(
        private val delegate: X509ExtendedKeyManager,
        private val alias: String,
    ) : X509ExtendedKeyManager() {
        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: java.net.Socket?) = alias
        override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: javax.net.ssl.SSLEngine?) = alias
        override fun getCertificateChain(alias: String?) = delegate.getCertificateChain(this.alias)
        override fun getPrivateKey(alias: String?) = delegate.getPrivateKey(this.alias)
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(alias)
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: java.net.Socket?) = delegate.chooseServerAlias(keyType, issuers, socket)
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = delegate.getServerAliases(keyType, issuers)
        override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: javax.net.ssl.SSLEngine?) = delegate.chooseEngineServerAlias(keyType, issuers, engine)
    }

    private companion object {
        const val PAIRING_PORT = 6467
        const val CONNECT_TIMEOUT_MS = 5_000
        const val IO_TIMEOUT_MS = 15_000
        const val MAX_MESSAGE_SIZE = 1024 * 1024
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val CLIENT_ALIAS = "couchlink_tv_remote_identity_v1"
        const val PREFERENCES = "couchlink_tv_remote"
        const val KEY_PAIRED_HOST = "paired_tv_host"
        const val KEY_SERVER_FINGERPRINT = "paired_tv_server_fingerprint"

        val TRUST_PAIRING_SERVER = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}
