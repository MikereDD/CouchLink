package dev.typezero.couchlink.remote.tv.launcher

import dev.typezero.couchlink.remote.tv.provider.TvProviderApp
import dev.typezero.couchlink.remote.tv.provider.TvProviderId

/**
 * User-curated shortcut shown in CouchLink's TV application launcher.
 *
 * Entries remain stored independently of the currently active TV provider so
 * changing TV platforms never destroys the user's launcher configuration.
 */
internal data class TvAppShortcut(
    val id: String,
    val displayName: String,
    val providerId: TvProviderId,
    val launchTarget: String,
    val kind: TvAppShortcutKind,
    val knownServiceId: String? = null,
) {
    fun toProviderApp(): TvProviderApp =
        TvProviderApp(
            id = id,
            displayName = displayName,
            launchTarget = launchTarget,
        )
}

internal enum class TvAppShortcutKind {
    KnownService,
    CustomApp,
}
