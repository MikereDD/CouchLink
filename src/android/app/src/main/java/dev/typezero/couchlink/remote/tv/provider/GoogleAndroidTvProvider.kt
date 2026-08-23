package dev.typezero.couchlink.remote.tv.provider

import dev.typezero.couchlink.remote.tv.TvDevice
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import dev.typezero.couchlink.remote.tv.TvInputTarget
import dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Provider adapter for CouchLink's existing Google TV / Android TV implementation.
 *
 * This class deliberately delegates protocol behavior to TvDiscoveryController so
 * the proven pairing, TLS, discovery, Wake-on-LAN, reconnect, key delivery, Live TV,
 * and Hisense input-switching behavior remain unchanged while the app moves to the
 * provider-neutral TV Remote architecture.
 */
internal class GoogleAndroidTvProvider(
    private val controller: TvDiscoveryController,
    private val closeControllerOnClose: Boolean = true,
) : TvRemoteProvider {

    override val descriptor: TvProviderDescriptor =
        TvProviderCatalog.descriptor(TvProviderId.GoogleAndroidTv)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(controller.state.value.toProviderState())
    override val state: StateFlow<TvProviderState> = _state.asStateFlow()

    init {
        scope.launch {
            controller.state.collect { googleState ->
                _state.value = googleState.toProviderState()
            }
        }
    }

    override fun startDiscovery() = controller.startDiscovery()

    override fun stopDiscovery() = controller.stopDiscovery()

    override fun select(device: TvProviderDevice) {
        require(device.providerId == TvProviderId.GoogleAndroidTv) {
            "GoogleAndroidTvProvider cannot select ${device.providerId}."
        }

        controller.select(
            TvDevice(
                name = device.name,
                host = device.host,
                serviceType = device.serviceType,
                advertisedPort = device.advertisedPort,
            ),
        )
    }

    override fun selectManual(host: String) = controller.selectManual(host)

    override fun probeSelected() = controller.probeSelected()

    override fun beginPairing() = controller.beginPairing()

    override fun finishPairing(code: String) = controller.finishPairing(code)

    override fun cancelPairing() = controller.cancelPairing()

    override fun connect() = controller.connectRemote()

    override fun send(command: TvRemoteCommand) {
        when (command) {
            TvRemoteCommand.Power -> controller.togglePower()
            TvRemoteCommand.LiveTv -> controller.openGoogleTvLive()
            else -> controller.sendKey(command.toGoogleKeyCode())
        }
    }

    override fun selectInput(input: TvProviderInput) {
        val target = runCatching { TvInputTarget.valueOf(input.id) }
            .getOrElse { error("Unknown Google/Android TV input: ${input.id}") }

        controller.selectInput(target)
    }

    override fun launchApp(app: TvProviderApp) {
        throw UnsupportedOperationException(
            "Google/Android TV app launching is not wired to Remote v2 yet: ${app.displayName}",
        )
    }

    override fun forgetDevice() = controller.forgetTv()

    override fun close() {
        scope.cancel()
        if (closeControllerOnClose) {
            controller.close()
        }
    }

    private fun TvDiscoveryController.State.toProviderState(): TvProviderState =
        TvProviderState(
            providerId = TvProviderId.GoogleAndroidTv,
            capabilities = GOOGLE_CAPABILITIES,
            scanning = scanning,
            devices = devices.map { it.toProviderDevice() },
            selectedDevice = selectedDevice?.toProviderDevice(),
            probing = probing,
            serviceProbes = buildList {
                probe?.let { result ->
                    add(
                        TvProviderServiceProbe(
                            id = "pairing",
                            label = "Pairing service",
                            endpoint = "6467",
                            reachable = result.pairingPortReachable,
                        ),
                    )
                    add(
                        TvProviderServiceProbe(
                            id = "remote",
                            label = "Remote service",
                            endpoint = "6466",
                            reachable = result.remotePortReachable,
                        ),
                    )
                }
            },
            pairing = TvProviderPairingState(
                inProgress = pairing.inProgress,
                awaitingCode = pairing.awaitingCode,
                paired = pairing.paired,
                message = pairing.message,
            ),
            connection = TvProviderConnectionState(
                connecting = remote.connecting,
                connected = remote.connected,
                ready = remote.ready,
                message = remote.message,
                lastCommand = remote.lastCommand,
                commandsSent = remote.commandsSent,
            ),
            inputs = GOOGLE_INPUTS,
            wakeMacAddress = wakeMacAddress,
            message = message,
        )

    private fun TvDevice.toProviderDevice(): TvProviderDevice =
        TvProviderDevice(
            providerId = TvProviderId.GoogleAndroidTv,
            name = name,
            host = host,
            serviceType = serviceType,
            advertisedPort = advertisedPort,
        )

    private fun TvRemoteCommand.toGoogleKeyCode(): RemoteKeyCode =
        when (this) {
            TvRemoteCommand.DpadUp -> RemoteKeyCode.KEYCODE_DPAD_UP
            TvRemoteCommand.DpadDown -> RemoteKeyCode.KEYCODE_DPAD_DOWN
            TvRemoteCommand.DpadLeft -> RemoteKeyCode.KEYCODE_DPAD_LEFT
            TvRemoteCommand.DpadRight -> RemoteKeyCode.KEYCODE_DPAD_RIGHT
            TvRemoteCommand.Select -> RemoteKeyCode.KEYCODE_DPAD_CENTER
            TvRemoteCommand.Back -> RemoteKeyCode.KEYCODE_BACK
            TvRemoteCommand.Home -> RemoteKeyCode.KEYCODE_HOME
            TvRemoteCommand.Menu -> RemoteKeyCode.KEYCODE_MENU
            TvRemoteCommand.Settings -> RemoteKeyCode.KEYCODE_SETTINGS
            TvRemoteCommand.Power -> RemoteKeyCode.KEYCODE_POWER
            TvRemoteCommand.VolumeUp -> RemoteKeyCode.KEYCODE_VOLUME_UP
            TvRemoteCommand.VolumeDown -> RemoteKeyCode.KEYCODE_VOLUME_DOWN
            TvRemoteCommand.Mute -> RemoteKeyCode.KEYCODE_VOLUME_MUTE
            TvRemoteCommand.PlayPause -> RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE
            TvRemoteCommand.Rewind -> RemoteKeyCode.KEYCODE_MEDIA_REWIND
            TvRemoteCommand.FastForward -> RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD
            TvRemoteCommand.ChannelUp -> RemoteKeyCode.KEYCODE_CHANNEL_UP
            TvRemoteCommand.ChannelDown -> RemoteKeyCode.KEYCODE_CHANNEL_DOWN
            TvRemoteCommand.LiveTv -> error("LiveTv is handled as a Google TV navigation macro.")
        }

    private companion object {
        val GOOGLE_CAPABILITIES = setOf(
            TvCapability.Dpad,
            TvCapability.Back,
            TvCapability.Home,
            TvCapability.Menu,
            TvCapability.Settings,
            TvCapability.Power,
            TvCapability.Volume,
            TvCapability.Mute,
            TvCapability.Playback,
            TvCapability.Channels,
            TvCapability.LiveTv,
            TvCapability.Inputs,
            TvCapability.WakeOnLan,
        )

        val GOOGLE_INPUTS = TvInputTarget.entries.map { target ->
            TvProviderInput(
                id = target.name,
                label = target.label,
            )
        }
    }
}
