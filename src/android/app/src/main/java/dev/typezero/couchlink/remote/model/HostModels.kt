package dev.typezero.couchlink.remote.model

internal data class AudioOutputDevice(
    val id: String,
    val name: String,
    val isDefault: Boolean,
)

internal data class LauncherHostState(
    val discovered: Boolean = false,
    val trusted: Boolean = false,
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val pairingRequired: Boolean = false,
    val hostId: String = "",
    val hostName: String = "",
    val hostAddress: String = "",
    val hostPort: Int = 45821,
    val hostVersion: String = "",
    val message: String = "Searching for CouchLink Host…",
    val launcherStates: Map<LauncherId, String> = emptyMap(),
    val audioOutputs: List<AudioOutputDevice> = emptyList(),
    val audioLoading: Boolean = false,
)
