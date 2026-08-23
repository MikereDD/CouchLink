package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import dev.typezero.couchlink.remote.tv.provider.TvCapability
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.tv.provider.TvProviderRuntime
import kotlinx.coroutines.flow.StateFlow

internal class TvAppLauncherRuntime(
    context: Context,
    private val tvProviderRuntime: TvProviderRuntime,
) {
    private val store = TvAppShortcutStore(context.applicationContext)

    val shortcuts: StateFlow<List<TvAppShortcut>>
        get() = store.shortcuts

    val knownServices: List<TvKnownService>
        get() = TvKnownServiceCatalog.services

    fun shortcutsFor(providerId: TvProviderId): List<TvAppShortcut> =
        shortcuts.value.filter { it.providerId == providerId }

    fun addCustomApp(
        displayName: String,
        providerId: TvProviderId,
        launchTarget: String,
    ): TvAppShortcut =
        store.add(
            displayName = displayName,
            providerId = providerId,
            launchTarget = launchTarget,
            kind = TvAppShortcutKind.CustomApp,
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
    fun remove(id: String): Boolean = store.remove(id)
    fun move(id: String, newIndex: Int): Boolean = store.move(id, newIndex)
    fun clear() = store.clear()

    fun launch(shortcut: TvAppShortcut): Boolean {
        val provider = tvProviderRuntime.activeProvider.value
        val providerId = tvProviderRuntime.activeProviderId.value
        val state = provider.state.value

        if (shortcut.providerId != providerId) return false
        if (TvCapability.AppLaunch !in state.capabilities) return false
        if (!state.connection.ready) return false

        provider.launchApp(shortcut.toProviderApp())
        return true
    }
}
