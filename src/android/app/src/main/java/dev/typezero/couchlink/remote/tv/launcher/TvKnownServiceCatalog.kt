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
        // Previously hardware-verified on Google TV. A sent request is not proof
        // that an application opened; repeat device validation for this milestone.
        TvKnownService("netflix", "Netflix", mapOf(TvProviderId.GoogleAndroidTv to "https://www.netflix.com/browse")),
        TvKnownService("apple-tv", "Apple TV", mapOf(TvProviderId.GoogleAndroidTv to "https://tv.apple.com/")),
        TvKnownService("hulu", "Hulu", mapOf(TvProviderId.GoogleAndroidTv to "hulu://video/")),
        // Retain existing identifiers without offering unverified launch targets.
        TvKnownService("youtube", "YouTube"),
        TvKnownService("prime-video", "Prime Video"),
        TvKnownService("disney-plus", "Disney+"),
        TvKnownService("max", "Max"),
        TvKnownService("plex", "Plex"),
        TvKnownService("jellyfin", "Jellyfin"),
    )

    fun find(id: String): TvKnownService? =
        services.firstOrNull { it.id == id }

    fun supportedBy(providerId: TvProviderId): List<TvKnownService> =
        services.filter { it.supports(providerId) }
}
