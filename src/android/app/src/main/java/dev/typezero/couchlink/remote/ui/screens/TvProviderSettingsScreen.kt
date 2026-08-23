package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.runtime.Composable
import dev.typezero.couchlink.remote.hid.BluetoothHidController
import dev.typezero.couchlink.remote.model.LauncherHostState
import dev.typezero.couchlink.remote.tv.TvConnectionProbe
import dev.typezero.couchlink.remote.tv.TvDevice
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import dev.typezero.couchlink.remote.tv.TvRemoteClient
import dev.typezero.couchlink.remote.tv.provider.TvProviderDevice
import dev.typezero.couchlink.remote.tv.provider.TvProviderState
import dev.typezero.couchlink.remote.update.CouchLinkUpdateManager

/**
 * Compatibility bridge that lets Settings operate entirely through the active
 * TV provider while the existing SettingsScreen TV panel is migrated separately.
 *
 * MainActivity and provider call sites no longer depend on TvDiscoveryController.
 */
@Composable
internal fun TvProviderSettingsScreen(
    hidState: BluetoothHidController.State,
    launcherHostState: LauncherHostState,
    tvState: TvProviderState,
    hapticsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    naturalScrolling: Boolean,
    onNaturalScrollingChanged: (Boolean) -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onRefreshBluetoothHosts: () -> Unit,
    onConnectBluetoothHost: (String) -> Boolean,
    onDisconnectBluetoothHost: () -> Boolean,
    onReconnectLauncherHost: () -> Unit,
    onPairLauncherHost: () -> Boolean,
    onForgetLauncherHost: () -> Boolean,
    onTvScan: () -> Unit,
    onTvStopScan: () -> Unit,
    onTvSelect: (TvProviderDevice) -> Unit,
    onTvSelectManual: (String) -> Unit,
    onTvProbe: () -> Unit,
    onTvBeginPairing: () -> Unit,
    onTvFinishPairing: (String) -> Unit,
    onTvCancelPairing: () -> Unit,
    onTvConnect: () -> Unit,
    onTvForget: () -> Unit,
    updateState: CouchLinkUpdateManager.State,
    onCheckForUpdates: () -> Unit,
    onUpdateChannelChanged: (Boolean) -> Unit,
    onDownloadUpdate: () -> Unit,
    onContinueInstall: () -> Unit,
    onOpenInstallPermission: () -> Unit,
) {
    SettingsScreen(
        hidState = hidState,
        launcherHostState = launcherHostState,
        tvState = tvState.toLegacyState(),
        hapticsEnabled = hapticsEnabled,
        onHapticsChanged = onHapticsChanged,
        naturalScrolling = naturalScrolling,
        onNaturalScrollingChanged = onNaturalScrollingChanged,
        onRequestBluetoothPermission = onRequestBluetoothPermission,
        onMakeDiscoverable = onMakeDiscoverable,
        onRefreshBluetoothHosts = onRefreshBluetoothHosts,
        onConnectBluetoothHost = onConnectBluetoothHost,
        onDisconnectBluetoothHost = onDisconnectBluetoothHost,
        onReconnectLauncherHost = onReconnectLauncherHost,
        onPairLauncherHost = onPairLauncherHost,
        onForgetLauncherHost = onForgetLauncherHost,
        onTvScan = onTvScan,
        onTvStopScan = onTvStopScan,
        onTvSelect = { legacyDevice ->
            onTvSelect(
                TvProviderDevice(
                    providerId = tvState.providerId,
                    name = legacyDevice.name,
                    host = legacyDevice.host,
                    serviceType = legacyDevice.serviceType,
                    advertisedPort = legacyDevice.advertisedPort,
                ),
            )
        },
        onTvSelectManual = onTvSelectManual,
        onTvProbe = onTvProbe,
        onTvBeginPairing = onTvBeginPairing,
        onTvFinishPairing = onTvFinishPairing,
        onTvCancelPairing = onTvCancelPairing,
        onTvConnect = onTvConnect,
        onTvForget = onTvForget,
        updateState = updateState,
        onCheckForUpdates = onCheckForUpdates,
        onUpdateChannelChanged = onUpdateChannelChanged,
        onDownloadUpdate = onDownloadUpdate,
        onContinueInstall = onContinueInstall,
        onOpenInstallPermission = onOpenInstallPermission,
    )
}

private fun TvProviderState.toLegacyState(): TvDiscoveryController.State {
    val pairingProbe = serviceProbes.firstOrNull { it.id == "pairing" }
    val remoteProbe = serviceProbes.firstOrNull { it.id == "remote" }
    val legacyProbe =
        if (pairingProbe != null || remoteProbe != null) {
            TvConnectionProbe(
                pairingPortReachable = pairingProbe?.reachable == true,
                remotePortReachable = remoteProbe?.reachable == true,
            )
        } else {
            null
        }

    return TvDiscoveryController.State(
        scanning = scanning,
        devices = devices.map { it.toLegacyDevice() },
        selectedDevice = selectedDevice?.toLegacyDevice(),
        probing = probing,
        probe = legacyProbe,
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
        wakeMacAddress = wakeMacAddress,
        message = message,
    )
}

private fun TvProviderDevice.toLegacyDevice(): TvDevice =
    TvDevice(
        name = name,
        host = host,
        serviceType = serviceType,
        advertisedPort = advertisedPort,
    )
