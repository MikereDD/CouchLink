package dev.typezero.couchlink.remote.tv.provider

import kotlinx.coroutines.flow.StateFlow

/**
 * Boundary between CouchLink's shared TV Remote experience and a TV-OS-specific
 * local-control implementation.
 *
 * Provider implementations own protocol details, pairing credentials, discovery,
 * connection/session handling, command mapping, and provider diagnostics.
 *
 * The shared UI consumes only this contract and capability/state models.
 */
internal interface TvRemoteProvider : AutoCloseable {
    val descriptor: TvProviderDescriptor
    val state: StateFlow<TvProviderState>

    fun startDiscovery()
    fun stopDiscovery()

    fun select(device: TvProviderDevice)
    fun selectManual(host: String)
    fun probeSelected()

    fun beginPairing()
    fun finishPairing(code: String)
    fun cancelPairing()

    fun connect()
    fun send(command: TvRemoteCommand)
    fun selectInput(input: TvProviderInput)

    fun forgetDevice()

    override fun close()
}
