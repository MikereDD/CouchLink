package dev.typezero.couchlink.remote.tv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

internal class TvDiscoveryController(
    context: Context,
) {
    internal data class State(
        val scanning: Boolean = false,
        val devices: List<TvDevice> = emptyList(),
        val selectedDevice: TvDevice? = null,
        val probing: Boolean = false,
        val probe: TvConnectionProbe? = null,
        val message: String = "Scan your local network for a Google TV.",
    )

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val discovered = ConcurrentHashMap<String, TvDevice>()
    private val activeListeners = ConcurrentHashMap<String, NsdManager.DiscoveryListener>()
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        State(
            selectedDevice = rememberedDevice(),
            message = rememberedDevice()?.let {
                "Saved TV: ${it.name} (${it.host}). Scan or test the connection."
            } ?: "Scan your local network for a Google TV.",
        ),
    )
    val state: StateFlow<State> = _state.asStateFlow()

    fun startDiscovery() {
        if (_state.value.scanning) return
        discovered.clear()
        _state.value = _state.value.copy(
            scanning = true,
            devices = emptyList(),
            message = "Searching for Android and Google TVs…",
        )

        SERVICE_TYPES.forEach { serviceType ->
            val listener = discoveryListener(serviceType)
            activeListeners[serviceType] = listener
            runCatching {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            }.onFailure { error ->
                activeListeners.remove(serviceType)
                updateDiscoveryFailure(error.message)
            }
        }
    }

    fun stopDiscovery() {
        activeListeners.values.toList().forEach { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        activeListeners.clear()
        _state.value = _state.value.copy(scanning = false)
    }

    fun select(device: TvDevice) {
        preferences.edit()
            .putString(KEY_NAME, device.name)
            .putString(KEY_HOST, device.host)
            .apply()
        _state.value = _state.value.copy(
            selectedDevice = device,
            probe = null,
            message = "Selected ${device.name} at ${device.host}.",
        )
    }

    fun selectManual(host: String) {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        if (cleanHost.isBlank()) {
            _state.value = _state.value.copy(message = "Enter the TV IP address or hostname.")
            return
        }
        select(TvDevice(name = "Manual Google TV", host = cleanHost))
    }

    fun probeSelected() {
        val device = _state.value.selectedDevice ?: run {
            _state.value = _state.value.copy(message = "Select a discovered TV or enter its IP address first.")
            return
        }
        if (_state.value.probing) return

        _state.value = _state.value.copy(
            probing = true,
            probe = null,
            message = "Testing Android TV Remote services on ${device.host}…",
        )
        scope.launch {
            val probe = withContext(Dispatchers.IO) {
                TvConnectionProbe(
                    pairingPortReachable = canConnect(device.host, PAIRING_PORT),
                    remotePortReachable = canConnect(device.host, REMOTE_PORT),
                )
            }
            val message = when {
                probe.fullyReachable -> "TV Remote services are reachable. This TV is ready for CouchLink pairing."
                probe.anyReachable -> "One TV Remote service responded. The TV may be waking up or using a different service version."
                else -> "No TV Remote service responded. Confirm the TV is on and both devices are on the same network."
            }
            _state.value = _state.value.copy(
                probing = false,
                probe = probe,
                message = message,
            )
        }
    }

    fun close() {
        stopDiscovery()
        scope.cancel()
    }

    private fun discoveryListener(serviceType: String) = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) = Unit

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            runCatching {
                nsdManager.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit

                        @Suppress("DEPRECATION")
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val hostAddress = info.host?.hostAddress ?: return
                            val device = TvDevice(
                                name = info.serviceName.ifBlank { "Google TV" },
                                host = hostAddress,
                                serviceType = serviceType,
                                advertisedPort = info.port.takeIf { it > 0 },
                            )
                            discovered[hostAddress] = device
                            val devices = discovered.values.sortedBy { it.name.lowercase() }
                            _state.value = _state.value.copy(
                                devices = devices,
                                message = if (devices.size == 1) {
                                    "Found 1 compatible TV."
                                } else {
                                    "Found ${devices.size} compatible TVs."
                                },
                            )
                        }
                    },
                )
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

        override fun onDiscoveryStopped(serviceType: String) {
            activeListeners.remove(serviceType)
            if (activeListeners.isEmpty()) {
                _state.value = _state.value.copy(scanning = false)
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            activeListeners.remove(serviceType)
            runCatching { nsdManager.stopServiceDiscovery(this) }
            updateDiscoveryFailure("NSD error $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            activeListeners.remove(serviceType)
            updateDiscoveryFailure("NSD stop error $errorCode")
        }
    }

    private fun updateDiscoveryFailure(detail: String?) {
        _state.value = _state.value.copy(
            scanning = activeListeners.isNotEmpty(),
            message = "TV discovery could not start${detail?.let { ": $it" } ?: "."}",
        )
    }

    private fun rememberedDevice(): TvDevice? {
        val host = preferences.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: return null
        val name = preferences.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: "Saved Google TV"
        return TvDevice(name = name, host = host)
    }

    private fun canConnect(host: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
        }
        true
    }.getOrDefault(false)

    private companion object {
        const val PREFERENCES = "couchlink_tv_remote"
        const val KEY_NAME = "selected_tv_name"
        const val KEY_HOST = "selected_tv_host"
        const val PAIRING_PORT = 6467
        const val REMOTE_PORT = 6466
        const val SOCKET_TIMEOUT_MS = 1_500
        val SERVICE_TYPES = listOf(
            "_androidtvremote2._tcp.",
            "_androidtvremote._tcp.",
        )
    }
}
