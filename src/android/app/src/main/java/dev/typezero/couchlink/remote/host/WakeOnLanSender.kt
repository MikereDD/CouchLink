package dev.typezero.couchlink.remote.host

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.Collections

internal object WakeOnLanSender {
    fun normalizeMacAddress(value: String?): String? {
        val compact = value
            ?.filter(Char::isLetterOrDigit)
            ?.uppercase()
            .orEmpty()

        if (!compact.matches(Regex("[0-9A-F]{12}")) ||
            compact == "000000000000" ||
            compact == "FFFFFFFFFFFF"
        ) {
            return null
        }

        return compact.chunked(2).joinToString(":")
    }

    fun buildMagicPacket(macAddress: String): ByteArray {
        val normalized = normalizeMacAddress(macAddress)
            ?: throw IllegalArgumentException("Invalid Wake-on-LAN MAC address.")
        val mac = normalized.split(':').map { it.toInt(16).toByte() }.toByteArray()
        return ByteArray(6 + 16 * mac.size).also { packet ->
            repeat(6) { packet[it] = 0xFF.toByte() }
            repeat(16) { copy ->
                mac.copyInto(packet, destinationOffset = 6 + copy * mac.size)
            }
        }
    }

    fun send(
        macAddress: String,
        bursts: Int = DEFAULT_BURSTS,
        intervalMilliseconds: Long = DEFAULT_INTERVAL_MS,
    ) {
        require(bursts > 0) { "Wake packet burst count must be positive." }
        require(intervalMilliseconds >= 0L) { "Wake packet interval cannot be negative." }

        val payload = buildMagicPacket(macAddress)
        val targets = broadcastAddresses().flatMap { address ->
            WAKE_PORTS.map { port -> InetSocketAddress(address, port) }
        }

        DatagramSocket().use { socket ->
            socket.broadcast = true
            var successfulSends = 0
            var lastFailure: Throwable? = null

            repeat(bursts) { burst ->
                targets.forEach { target ->
                    runCatching {
                        socket.send(DatagramPacket(payload, payload.size, target))
                    }.onSuccess {
                        successfulSends += 1
                    }.onFailure { failure ->
                        lastFailure = failure
                    }
                }
                if (burst + 1 < bursts && intervalMilliseconds > 0L) {
                    Thread.sleep(intervalMilliseconds)
                }
            }

            if (successfulSends == 0) {
                throw IllegalStateException(
                    "No Wake-on-LAN broadcast could be sent.",
                    lastFailure,
                )
            }
        }
    }

    private fun broadcastAddresses(): Set<InetAddress> = buildSet {
        add(InetAddress.getByName("255.255.255.255"))

        runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces()).forEach { network ->
                if (!network.isUp || network.isLoopback) return@forEach
                network.interfaceAddresses
                    .mapNotNull { it.broadcast }
                    .forEach(::add)
            }
        }
    }

    private val WAKE_PORTS = listOf(9, 7)
    private const val DEFAULT_BURSTS = 10
    private const val DEFAULT_INTERVAL_MS = 500L
}
