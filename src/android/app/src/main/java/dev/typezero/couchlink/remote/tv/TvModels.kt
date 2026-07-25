package dev.typezero.couchlink.remote.tv

internal data class TvDevice(
    val name: String,
    val host: String,
    val serviceType: String? = null,
    val advertisedPort: Int? = null,
)

internal data class TvConnectionProbe(
    val pairingPortReachable: Boolean = false,
    val remotePortReachable: Boolean = false,
) {
    val anyReachable: Boolean
        get() = pairingPortReachable || remotePortReachable

    val fullyReachable: Boolean
        get() = pairingPortReachable && remotePortReachable
}
