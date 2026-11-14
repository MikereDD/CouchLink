package dev.typezero.couchlink.remote.tv.launcher

import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import org.junit.Assert.*
import org.junit.Test

class TvKnownServiceCatalogTest {
    @Test fun googlePresetsContainOnlyTheThreeProvenTargets() {
        assertEquals(
            mapOf("netflix" to "https://www.netflix.com/browse", "apple-tv" to "https://tv.apple.com/", "hulu" to "hulu://video/"),
            TvKnownServiceCatalog.supportedBy(TvProviderId.GoogleAndroidTv)
                .associate { it.id to it.launchTargetFor(TvProviderId.GoogleAndroidTv) },
        )
    }
    @Test fun unimplementedProvidersHaveNoPresets() {
        TvProviderId.entries.filter { it != TvProviderId.GoogleAndroidTv }.forEach {
            assertTrue(TvKnownServiceCatalog.supportedBy(it).isEmpty())
        }
    }
}
