package dev.typezero.couchlink.remote.tv.launcher

import dev.typezero.couchlink.remote.tv.provider.TvProviderId

internal data class TvKnownService(
    val id: String,
    val displayName: String,
    val launchTargets: Map<TvProviderId, String> = emptyMap(),
) {
    fun launchTargetFor(providerId: TvProviderId): String? =
        launchTargets[providerId]?.trim()?.takeIf(String::isNotEmpty)

    fun supports(providerId: TvProviderId): Boolean =
        launchTargetFor(providerId) != null
}

internal object TvKnownServiceCatalog {
    val services: List<TvKnownService> = listOf(
        TvKnownService("youtube", "YouTube"),
        TvKnownService("netflix", "Netflix"),
        TvKnownService("prime-video", "Prime Video"),
        TvKnownService("disney-plus", "Disney+"),
        TvKnownService("hulu", "Hulu"),
        TvKnownService("max", "Max"),
        TvKnownService("plex", "Plex"),
        TvKnownService("jellyfin", "Jellyfin"),
    )

    fun find(id: String): TvKnownService? =
        services.firstOrNull { it.id == id }

    fun supportedBy(providerId: TvProviderId): List<TvKnownService> =
        services.filter { it.supports(providerId) }
}
