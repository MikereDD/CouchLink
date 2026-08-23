package dev.typezero.couchlink.remote.tv.provider

import android.content.Context
import dev.typezero.couchlink.remote.tv.TvDiscoveryController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Owns the active TV provider and the user's provider selection.
 *
 * Only implemented providers can become active. A saved provider that is no
 * longer available falls back to Google/Android TV so existing CouchLink users
 * keep the current TV Remote behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TvProviderRuntime(
    context: Context,
    private val sharedGoogleController: TvDiscoveryController? = null,
) : AutoCloseable {

    private val appContext = context.applicationContext
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val initialProviderId = rememberedImplementedProvider()
    private val initialProvider = createProvider(initialProviderId)

    private val _activeProviderId = MutableStateFlow(initialProviderId)
    val activeProviderId: StateFlow<TvProviderId> = _activeProviderId.asStateFlow()

    private val _activeProvider = MutableStateFlow(initialProvider)
    val activeProvider: StateFlow<TvRemoteProvider> = _activeProvider.asStateFlow()

    val providerState: StateFlow<TvProviderState> =
        _activeProvider
            .flatMapLatest { provider -> provider.state }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = initialProvider.state.value,
            )

    val providers: List<TvProviderDescriptor>
        get() = TvProviderCatalog.providers

    /**
     * Selects an implemented provider.
     *
     * Returns false when the provider is known to the catalog but does not yet
     * have a backend. The current provider remains active in that case.
     */
    fun selectProvider(providerId: TvProviderId): Boolean {
        val descriptor = TvProviderCatalog.descriptor(providerId)
        if (!descriptor.implemented) return false
        if (_activeProviderId.value == providerId) return true

        val next = createProvider(providerId)
        val previous = _activeProvider.value

        _activeProviderId.value = providerId
        _activeProvider.value = next
        preferences.edit().putString(KEY_ACTIVE_PROVIDER, providerId.name).apply()

        previous.close()
        return true
    }

    override fun close() {
        _activeProvider.value.close()
        scope.cancel()
    }

    private fun rememberedImplementedProvider(): TvProviderId {
        val saved = preferences.getString(KEY_ACTIVE_PROVIDER, null)
            ?.let { name -> runCatching { TvProviderId.valueOf(name) }.getOrNull() }

        val resolved = saved
            ?.takeIf { id -> TvProviderCatalog.descriptor(id).implemented }
            ?: DEFAULT_PROVIDER

        if (saved != resolved) {
            preferences.edit().putString(KEY_ACTIVE_PROVIDER, resolved.name).apply()
        }

        return resolved
    }

    private fun createProvider(providerId: TvProviderId): TvRemoteProvider =
        when (providerId) {
            TvProviderId.GoogleAndroidTv -> {
                val controller = sharedGoogleController ?: TvDiscoveryController(appContext)
                GoogleAndroidTvProvider(
                    controller = controller,
                    closeControllerOnClose = sharedGoogleController == null,
                )
            }

            TvProviderId.Roku,
            TvProviderId.SamsungTizen,
            TvProviderId.LgWebOs,
            TvProviderId.FireTv,
            TvProviderId.AppleTv ->
                error("TV provider ${providerId.name} is not implemented yet.")
        }

    private companion object {
        const val PREFERENCES = "couchlink_tv_provider"
        const val KEY_ACTIVE_PROVIDER = "active_provider"
        val DEFAULT_PROVIDER = TvProviderId.GoogleAndroidTv
    }
}
