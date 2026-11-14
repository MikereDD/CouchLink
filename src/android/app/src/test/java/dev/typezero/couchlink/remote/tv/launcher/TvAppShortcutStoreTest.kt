package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TvAppShortcutStoreTest {
    private val google = TvProviderId.GoogleAndroidTv
    private lateinit var context: Context
    private lateinit var store: TvAppShortcutStore
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("couchlink_tv_app_launcher", 0).edit().clear().commit()
        store = TvAppShortcutStore(context)
    }
    @Test fun addingKnownServiceTwiceReturnsOriginalEvenIfNameChanges() {
        val first = store.add("Netflix", google, "https://www.netflix.com/browse", TvAppShortcutKind.KnownService, "netflix")
        val second = store.add("Localized name", google, "https://www.netflix.com/browse", TvAppShortcutKind.KnownService, "netflix")
        assertEquals(first.id, second.id)
        assertEquals(listOf(first), TvAppShortcutStore(context).shortcuts.value)
    }
    @Test fun providerOwnershipAndCustomNamesDoNotCollide() {
        store.add("Netflix", google, "netflix://custom", TvAppShortcutKind.CustomApp)
        store.add("Netflix", google, "https://www.netflix.com/browse", TvAppShortcutKind.KnownService, "netflix")
        store.add("Netflix", TvProviderId.Roku, "roku://example", TvAppShortcutKind.KnownService, "netflix")
        assertEquals(3, TvAppShortcutStore(context).shortcuts.value.size)
    }
    @Test fun schemaOneNullServiceIdLoadsAsNullAndPreservesCustomTarget() {
        val raw = """{"version":1,"shortcuts":[{"id":"old","displayName":"Personal app","providerId":"GoogleAndroidTv","launchTarget":"legacy target without scheme","kind":"CustomApp","knownServiceId":null}]}"""
        context.getSharedPreferences("couchlink_tv_app_launcher", 0).edit().putString("shortcut_library", raw).commit()
        val loaded = TvAppShortcutStore(context).shortcuts.value.single()
        assertEquals("old", loaded.id)
        assertNull(loaded.knownServiceId)
        assertEquals("legacy target without scheme", loaded.launchTarget)
    }
    @Test fun removeAndMovePersistAcrossNewStoreInstances() {
        val a = store.add("A", google, "a://open", TvAppShortcutKind.CustomApp)
        val b = store.add("B", google, "b://open", TvAppShortcutKind.CustomApp)
        val c = store.add("C", google, "c://open", TvAppShortcutKind.CustomApp)
        assertTrue(store.move(c.id, 0))
        assertTrue(store.remove(b.id))
        assertEquals(listOf(c.id, a.id), TvAppShortcutStore(context).shortcuts.value.map { it.id })
        assertFalse(store.remove("missing"))
        assertFalse(store.move("missing", 0))
    }
    @Test fun movingVisibleShortcutDoesNotMoveOtherProviderSlots() {
        val hidden = store.add("Other", TvProviderId.Roku, "other://open", TvAppShortcutKind.CustomApp)
        val a = store.add("A", google, "a://open", TvAppShortcutKind.CustomApp)
        val b = store.add("B", google, "b://open", TvAppShortcutKind.CustomApp)
        store.move(b.id, 0)
        assertEquals(listOf(hidden.id, b.id, a.id), TvAppShortcutStore(context).shortcuts.value.map { it.id })
        store.move(b.id, 99)
        assertEquals(listOf(hidden.id, a.id, b.id), store.shortcuts.value.map { it.id })
    }
    @Test fun optionalIconReferenceSurvivesLoadingAndUpdatingSchemaOne() {
        val raw = """{"version":1,"shortcuts":[{"id":"old","displayName":"Saved","providerId":"GoogleAndroidTv","launchTarget":"saved://open","kind":"CustomApp","iconReference":"future:my-icon"}]}"""
        context.getSharedPreferences("couchlink_tv_app_launcher", 0).edit().putString("shortcut_library", raw).commit()
        val reloaded = TvAppShortcutStore(context)
        reloaded.update(reloaded.shortcuts.value.single().copy(displayName = "Renamed"))
        val payload = org.json.JSONObject(requireNotNull(context.getSharedPreferences("couchlink_tv_app_launcher", 0).getString("shortcut_library", "")))
        assertEquals("future:my-icon", payload.getJSONArray("shortcuts").getJSONObject(0).optString("iconReference"))
    }
    @Test fun duplicateCannotBeIntroducedThroughUpdate() {
        store.add("Netflix", google, "netflix://open", TvAppShortcutKind.KnownService, "netflix")
        val other = store.add("Hulu", google, "hulu://video/", TvAppShortcutKind.KnownService, "hulu")
        assertThrows(IllegalArgumentException::class.java) { store.update(other.copy(knownServiceId = "netflix")) }
        assertEquals(2, store.shortcuts.value.size)
    }
    @Test fun newlyAddedTargetsRequireAUriSchemeWithoutRejectingLegacyData() {
        listOf("not a uri", "https://", "https://bad host/path", "://missing", "bad scheme://open").forEach { target ->
            assertThrows(IllegalArgumentException::class.java) { store.add("Invalid", google, target, TvAppShortcutKind.CustomApp) }
        }
        assertTrue(store.shortcuts.value.isEmpty())
        store.add("Valid", google, "custom-app://open", TvAppShortcutKind.CustomApp)
        assertEquals(1, store.shortcuts.value.size)
    }
    @Test fun malformedIndividualRecordDoesNotHideValidNeighbor() {
        val raw = """{"version":1,"shortcuts":[{"id":"bad"},{"id":"good","displayName":"Saved","providerId":"GoogleAndroidTv","launchTarget":"saved://open","kind":"CustomApp"}]}"""
        context.getSharedPreferences("couchlink_tv_app_launcher", 0).edit().putString("shortcut_library", raw).commit()
        assertEquals(listOf("good"), TvAppShortcutStore(context).shortcuts.value.map { it.id })
    }
}
