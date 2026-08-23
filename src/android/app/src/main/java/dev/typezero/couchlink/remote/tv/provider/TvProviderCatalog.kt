package dev.typezero.couchlink.remote.tv.provider

/**
 * Stable provider identities exposed by CouchLink's TV setup flow.
 *
 * `implemented` describes whether a provider backend currently exists; it is not
 * a capability flag. Individual remote controls are governed by TvCapability.
 */
internal object TvProviderCatalog {
    val providers: List<TvProviderDescriptor> = listOf(
        TvProviderDescriptor(
            id = TvProviderId.GoogleAndroidTv,
            displayName = "Google TV / Android TV",
            implemented = true,
        ),
        TvProviderDescriptor(
            id = TvProviderId.Roku,
            displayName = "Roku",
            implemented = false,
        ),
        TvProviderDescriptor(
            id = TvProviderId.SamsungTizen,
            displayName = "Samsung Tizen",
            implemented = false,
        ),
        TvProviderDescriptor(
            id = TvProviderId.LgWebOs,
            displayName = "LG webOS",
            implemented = false,
        ),
        TvProviderDescriptor(
            id = TvProviderId.FireTv,
            displayName = "Fire TV",
            implemented = false,
        ),
    )

    fun descriptor(id: TvProviderId): TvProviderDescriptor =
        providers.first { it.id == id }
}
