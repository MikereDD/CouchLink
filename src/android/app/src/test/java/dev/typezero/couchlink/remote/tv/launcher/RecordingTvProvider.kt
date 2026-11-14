package dev.typezero.couchlink.remote.tv.launcher

import dev.typezero.couchlink.remote.tv.provider.*
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory provider contract only; owns no controller, socket, or device. */
internal class RecordingTvProvider : TvRemoteProvider {
    override val descriptor = TvProviderCatalog.descriptor(TvProviderId.GoogleAndroidTv)
    override val state = MutableStateFlow(TvProviderState(
        providerId = descriptor.id,
        capabilities = setOf(TvCapability.AppLaunch),
        connection = TvProviderConnectionState(connected = true, ready = true),
        message = "Test provider",
    ))
    val requests = mutableListOf<TvProviderApp>()
    var failure: RuntimeException? = null
    override fun launchApp(app: TvProviderApp) { failure?.let { throw it }; requests += app }
    override fun startDiscovery() = error("Unexpected discovery")
    override fun stopDiscovery() = Unit
    override fun select(device: TvProviderDevice) = error("Unexpected selection")
    override fun selectManual(host: String) = error("Unexpected selection")
    override fun probeSelected() = error("Unexpected probe")
    override fun beginPairing() = error("Unexpected pairing")
    override fun finishPairing(code: String) = error("Unexpected pairing")
    override fun cancelPairing() = Unit
    override fun connect() = error("Unexpected connection")
    override fun send(command: TvRemoteCommand) = error("Unexpected remote command")
    override fun selectInput(input: TvProviderInput) = error("Unexpected input")
    override fun forgetDevice() = error("Unexpected forget")
    override fun close() = Unit
}
