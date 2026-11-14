package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import dev.typezero.couchlink.remote.tv.provider.TvCapability
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.tv.provider.TvProviderRuntime
import dev.typezero.couchlink.remote.tv.provider.TvRemoteProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

internal class TvAppLauncherRuntime(
    private val store: TvAppShortcutStore,
    private val activeProvider: () -> TvRemoteProvider,
) {
    constructor(context: Context, tvProviderRuntime: TvProviderRuntime) : this(
        TvAppShortcutStore(context.applicationContext),
        { tvProviderRuntime.activeProvider.value },
    )

    val shortcuts: StateFlow<List<TvAppShortcut>>
        get() = store.shortcuts
    val icons: TvCustomIconStorage
        get() = store.icons

    val knownServices: List<TvKnownService>
        get() = TvKnownServiceCatalog.services

    fun shortcutsFor(providerId: TvProviderId): List<TvAppShortcut> =
        shortcuts.value.filter { it.providerId == providerId }

    fun addCustomApp(
        displayName: String,
        providerId: TvProviderId,
        launchTarget: String,
        iconDraft: TvIconDraft? = null,
    ): TvAppShortcut =
        store.add(
            displayName = displayName,
            providerId = providerId,
            launchTarget = launchTarget,
            kind = TvAppShortcutKind.CustomApp,
            iconDraft = iconDraft,
        )

    fun addKnownService(serviceId: String, providerId: TvProviderId): TvAppShortcut {
        val service = requireNotNull(TvKnownServiceCatalog.find(serviceId)) {
            "Unknown TV streaming service: $serviceId"
        }
        val launchTarget = requireNotNull(service.launchTargetFor(providerId)) {
            "${service.displayName} does not have a verified launch target for ${providerId.name} yet."
        }

        return store.add(
            displayName = service.displayName,
            providerId = providerId,
            launchTarget = launchTarget,
            kind = TvAppShortcutKind.KnownService,
            knownServiceId = service.id,
        )
    }

    fun update(shortcut: TvAppShortcut) = store.update(shortcut)
    fun setCustomIcon(id: String, draft: TvIconDraft?) = store.setCustomIcon(id, draft)
    fun remove(id: String): Boolean = store.remove(id)
    fun move(id: String, newIndex: Int): Boolean = store.move(id, newIndex)
    fun clear() = store.clear()

    fun launch(shortcut: TvAppShortcut): TvAppLaunchResult {
        val provider = activeProvider()
        val providerId = provider.descriptor.id
        val state = provider.state.value

        if (shortcut.providerId != providerId) return TvAppLaunchResult.WrongProvider
        if (TvCapability.AppLaunch !in state.capabilities) return TvAppLaunchResult.Unsupported
        if (!state.connection.ready) return TvAppLaunchResult.Disconnected

        return try {
            provider.launchApp(shortcut.toProviderApp())
            TvAppLaunchResult.Requested
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            TvAppLaunchResult.Failed
        }
    }
}

/** Requested means submitted to the provider, not sent on the wire or opened. */
internal enum class TvAppLaunchResult { Requested, WrongProvider, Unsupported, Disconnected, Failed }
