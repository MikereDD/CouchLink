package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.runtime.Composable
import dev.typezero.couchlink.remote.tv.TvDevice
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import dev.typezero.couchlink.remote.tv.TvInputTarget
import dev.typezero.couchlink.remote.tv.TvRemoteClient
import dev.typezero.couchlink.remote.tv.proto.RemoteKeyCode
import dev.typezero.couchlink.remote.tv.provider.TvProviderInput
import dev.typezero.couchlink.remote.tv.provider.TvProviderState
import dev.typezero.couchlink.remote.tv.provider.TvRemoteCommand

/**
 * Temporary compatibility boundary between the provider-neutral TV runtime and
 * the existing CouchLink TV Remote composable.
 *
 * The public-facing call site can now speak TvProviderState/TvRemoteCommand
 * while the existing visual implementation is migrated separately. This keeps
 * the proven remote layout unchanged during the architecture transition.
 */
@Composable
internal fun TvProviderRemoteScreen(
    state: TvProviderState,
    onConnect: () -> Unit,
    onCommand: (TvRemoteCommand) -> Unit,
    onInput: (TvProviderInput) -> Unit,
    onHaptic: () -> Unit,
) {
    TvRemoteScreen(
        state = state.toLegacyState(),
        onConnect = onConnect,
        onPower = { onCommand(TvRemoteCommand.Power) },
        onKey = { key -> onCommand(key.toProviderCommand()) },
        onLiveTv = { onCommand(TvRemoteCommand.LiveTv) },
        onInput = { target ->
            onInput(
                TvProviderInput(
                    id = target.name,
                    label = target.label,
                ),
            )
        },
        onHaptic = onHaptic,
    )
}

private fun TvProviderState.toLegacyState(): TvDiscoveryController.State =
    TvDiscoveryController.State(
        scanning = scanning,
        devices = devices.map { device ->
            TvDevice(
                name = device.name,
                host = device.host,
                serviceType = device.serviceType,
                advertisedPort = device.advertisedPort,
            )
        },
        selectedDevice = selectedDevice?.let { device ->
            TvDevice(
                name = device.name,
                host = device.host,
                serviceType = device.serviceType,
                advertisedPort = device.advertisedPort,
            )
        },
        probing = probing,
        probe = null,
        pairing = TvDiscoveryController.PairingState(
            inProgress = pairing.inProgress,
            awaitingCode = pairing.awaitingCode,
            paired = pairing.paired,
            message = pairing.message,
        ),
        remote = TvRemoteClient.Connection(
            connecting = connection.connecting,
            connected = connection.connected,
            ready = connection.ready,
            message = connection.message,
            lastCommand = connection.lastCommand,
            commandsSent = connection.commandsSent,
        ),
        wakeMacAddress = null,
        message = message,
    )

private fun RemoteKeyCode.toProviderCommand(): TvRemoteCommand =
    when (this) {
        RemoteKeyCode.KEYCODE_DPAD_UP -> TvRemoteCommand.DpadUp
        RemoteKeyCode.KEYCODE_DPAD_DOWN -> TvRemoteCommand.DpadDown
        RemoteKeyCode.KEYCODE_DPAD_LEFT -> TvRemoteCommand.DpadLeft
        RemoteKeyCode.KEYCODE_DPAD_RIGHT -> TvRemoteCommand.DpadRight
        RemoteKeyCode.KEYCODE_DPAD_CENTER -> TvRemoteCommand.Select
        RemoteKeyCode.KEYCODE_BACK -> TvRemoteCommand.Back
        RemoteKeyCode.KEYCODE_HOME -> TvRemoteCommand.Home
        RemoteKeyCode.KEYCODE_MENU -> TvRemoteCommand.Menu
        RemoteKeyCode.KEYCODE_SETTINGS -> TvRemoteCommand.Settings
        RemoteKeyCode.KEYCODE_POWER -> TvRemoteCommand.Power
        RemoteKeyCode.KEYCODE_VOLUME_UP -> TvRemoteCommand.VolumeUp
        RemoteKeyCode.KEYCODE_VOLUME_DOWN -> TvRemoteCommand.VolumeDown
        RemoteKeyCode.KEYCODE_VOLUME_MUTE -> TvRemoteCommand.Mute
        RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE -> TvRemoteCommand.PlayPause
        RemoteKeyCode.KEYCODE_MEDIA_REWIND -> TvRemoteCommand.Rewind
        RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD -> TvRemoteCommand.FastForward
        RemoteKeyCode.KEYCODE_CHANNEL_UP -> TvRemoteCommand.ChannelUp
        RemoteKeyCode.KEYCODE_CHANNEL_DOWN -> TvRemoteCommand.ChannelDown
        else -> error("Unsupported legacy TV key mapping: ${name}")
    }
