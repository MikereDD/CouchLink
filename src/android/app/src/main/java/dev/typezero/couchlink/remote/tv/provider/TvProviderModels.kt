package dev.typezero.couchlink.remote.tv.provider

internal enum class TvProviderId {
    GoogleAndroidTv,
    Roku,
    SamsungTizen,
    LgWebOs,
    FireTv,
    AppleTv,
}

internal data class TvProviderDescriptor(
    val id: TvProviderId,
    val displayName: String,
    val implemented: Boolean,
)

internal enum class TvCapability {
    Dpad,
    Back,
    Home,
    Menu,
    Settings,
    Power,
    Volume,
    Mute,
    Playback,
    Channels,
    LiveTv,
    Inputs,
    TextInput,
    AppLaunch,
}

internal enum class TvRemoteCommand {
    DpadUp,
    DpadDown,
    DpadLeft,
    DpadRight,
    Select,
    Back,
    Home,
    Menu,
    Settings,
    Power,
    VolumeUp,
    VolumeDown,
    Mute,
    PlayPause,
    Rewind,
    FastForward,
    ChannelUp,
    ChannelDown,
    LiveTv,
}

internal data class TvProviderDevice(
    val providerId: TvProviderId,
    val name: String,
    val host: String,
    val serviceType: String? = null,
    val advertisedPort: Int? = null,
)

internal data class TvProviderInput(
    val id: String,
    val label: String,
)

internal data class TvProviderPairingState(
    val inProgress: Boolean = false,
    val awaitingCode: Boolean = false,
    val paired: Boolean = false,
    val message: String? = null,
)

internal data class TvProviderConnectionState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val ready: Boolean = false,
    val message: String = "TV remote is offline.",
    val lastCommand: String? = null,
    val commandsSent: Long = 0,
)

internal data class TvProviderState(
    val providerId: TvProviderId,
    val capabilities: Set<TvCapability>,
    val scanning: Boolean = false,
    val devices: List<TvProviderDevice> = emptyList(),
    val selectedDevice: TvProviderDevice? = null,
    val probing: Boolean = false,
    val pairing: TvProviderPairingState = TvProviderPairingState(),
    val connection: TvProviderConnectionState = TvProviderConnectionState(),
    val inputs: List<TvProviderInput> = emptyList(),
    val message: String,
)
