package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import java.util.UUID
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent, ordered library of user-selected TV launcher shortcuts.
 *
 * SharedPreferences is intentionally sufficient here: this is a small user-curated
 * list, not a media database. The serialized payload is versioned so future fields
 * can be migrated without throwing away a user's tray.
 */
internal class TvAppShortcutStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val _shortcuts = MutableStateFlow(load())
    val shortcuts: StateFlow<List<TvAppShortcut>> = _shortcuts.asStateFlow()
    val icons = TvCustomIconStorage(context)
    private var iconCleanupSafe = true

    init { synchronized(TvCustomIconStorage.LOCK) { icons.prune(persistedIconReferences()) } }

    fun add(
        displayName: String,
        providerId: TvProviderId,
        launchTarget: String,
        kind: TvAppShortcutKind,
        knownServiceId: String? = null,
        iconDraft: TvIconDraft? = null,
    ): TvAppShortcut = synchronized(TvCustomIconStorage.LOCK) {
        require(kind == TvAppShortcutKind.CustomApp || iconDraft == null) { "Only custom apps can have a user icon." }
        val serviceId = knownServiceId?.trim()?.takeIf(String::isNotEmpty)
        if (kind == TvAppShortcutKind.KnownService) {
            requireNotNull(serviceId) { "A known service needs a stable service ID." }
            _shortcuts.value.firstOrNull {
                it.providerId == providerId && it.knownServiceId == serviceId
            }?.let { return@synchronized it }
        }
        val name = requireName(displayName)
        val target = requireNewTarget(launchTarget)
        val icon = iconDraft?.let(icons::write)
        val shortcut = TvAppShortcut(
            id = UUID.randomUUID().toString(),
            displayName = name,
            providerId = providerId,
            launchTarget = target,
            kind = kind,
            knownServiceId = knownServiceId?.trim()?.takeIf(String::isNotEmpty),
            iconReference = icon,
        )
        try { persist(_shortcuts.value + shortcut) }
        catch (failure: Exception) { pruneAfterFailedSave(); throw failure }
        shortcut
    }

    fun update(shortcut: TvAppShortcut) = synchronized(TvCustomIconStorage.LOCK) {
        val clean = shortcut.copy(
            displayName = requireName(shortcut.displayName),
            launchTarget = requireTarget(shortcut.launchTarget),
            knownServiceId = shortcut.knownServiceId?.trim()?.takeIf(String::isNotEmpty),
        )
        val current = _shortcuts.value
        val index = current.indexOfFirst { it.id == clean.id }
        require(index >= 0) { "Unknown TV app shortcut: ${clean.id}" }
        require(current[index].kind == TvAppShortcutKind.CustomApp || clean.iconReference == current[index].iconReference) {
            "Only custom apps can change their icon."
        }
        if (clean.knownServiceId != null) {
            require(current.none {
                it.id != clean.id && it.providerId == clean.providerId && it.knownServiceId == clean.knownServiceId
            }) { "This service is already in your launcher." }
        }

        val next = current.toMutableList().apply { this[index] = clean }
        persist(next)
    }

    fun remove(id: String): Boolean = synchronized(TvCustomIconStorage.LOCK) {
        val next = _shortcuts.value.filterNot { it.id == id }
        if (next.size == _shortcuts.value.size) return@synchronized false
        persist(next)
        true
    }

    fun move(id: String, newIndex: Int): Boolean = synchronized(TvCustomIconStorage.LOCK) {
        val current = _shortcuts.value
        val item = current.firstOrNull { it.id == id } ?: return@synchronized false
        // Indices belong to the visible provider list. Other providers retain
        // their exact slots as well as their relative ordering.
        val slots = current.indices.filter { current[it].providerId == item.providerId }
        val visible = slots.map { current[it] }.toMutableList()
        val from = visible.indexOfFirst { it.id == id }
        val target = newIndex.coerceIn(0, visible.lastIndex)
        if (from == target) return@synchronized true
        visible.add(target, visible.removeAt(from))
        val next = current.toMutableList()
        slots.forEachIndexed { position, slot -> next[slot] = visible[position] }
        persist(next)
        true
    }

    fun clear() = synchronized(TvCustomIconStorage.LOCK) {
        persist(emptyList())
    }

    fun setCustomIcon(id: String, draft: TvIconDraft?) = synchronized(TvCustomIconStorage.LOCK) {
        val current = _shortcuts.value
        val index = current.indexOfFirst { it.id == id }
        require(index >= 0 && current[index].kind == TvAppShortcutKind.CustomApp) { "Choose a saved custom app." }
        val reference = draft?.let(icons::write)
        try {
            persist(current.toMutableList().apply { this[index] = this[index].copy(iconReference = reference) })
        } catch (failure: Exception) { pruneAfterFailedSave(); throw failure }
    }

    private fun pruneAfterFailedSave() {
        if (!iconCleanupSafe) return
        // Keep both possible owners if a storage failure made durability uncertain.
        val persisted = persistedIconReferences() ?: return
        icons.prune(persisted + _shortcuts.value.mapNotNull { it.iconReference })
    }

    private fun persistedIconReferences(): Set<String>? {
        val raw = preferences.getString(KEY_PAYLOAD, null) ?: return emptySet()
        return runCatching {
            val root = JSONObject(raw)
            require(root.optInt("version") == SCHEMA_VERSION)
            val array = requireNotNull(root.optJSONArray("shortcuts"))
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.takeUnless { it.isNull("iconReference") }
                    ?.optString("iconReference")?.takeIf { it.isNotBlank() }
            }.toSet()
        }.getOrNull()
    }

    private fun persist(shortcuts: List<TvAppShortcut>) {
        val payload = JSONObject()
            .put("version", SCHEMA_VERSION)
            .put(
                "shortcuts",
                JSONArray().apply {
                    shortcuts.forEach { shortcut ->
                        put(
                            JSONObject()
                                .put("id", shortcut.id)
                                .put("displayName", shortcut.displayName)
                                .put("providerId", shortcut.providerId.name)
                                .put("launchTarget", shortcut.launchTarget)
                                .put("kind", shortcut.kind.name)
                                .put("knownServiceId", shortcut.knownServiceId ?: JSONObject.NULL)
                                .put("iconReference", shortcut.iconReference ?: JSONObject.NULL),
                        )
                    }
                },
            )
            .toString()

        // Commit metadata before pruning images, so process death cannot restore
        // an old reference whose file was already removed.
        val previousPayload = preferences.getString(KEY_PAYLOAD, null)
        if (!preferences.edit().putString(KEY_PAYLOAD, payload).commit()) {
            // A failed commit may still have updated SharedPreferences' memory.
            // Restore the previous payload before deciding which files are owned.
            iconCleanupSafe = preferences.edit().putString(KEY_PAYLOAD, previousPayload).commit()
            throw IOException("Could not save shortcuts.")
        }
        _shortcuts.value = shortcuts.toList()
        iconCleanupSafe = true
        icons.prune(shortcuts.mapNotNull { it.iconReference }.toSet())
    }

    private fun load(): List<TvAppShortcut> {
        val raw = preferences.getString(KEY_PAYLOAD, null) ?: return emptyList()

        return runCatching {
            val root = JSONObject(raw)
            when (root.optInt("version", 0)) {
                SCHEMA_VERSION -> decodeV1(root)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun decodeV1(root: JSONObject): List<TvAppShortcut> {
        val array = root.optJSONArray("shortcuts") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val shortcut = runCatching {
                    TvAppShortcut(
                        id = item.getString("id").trim().also {
                            require(it.isNotEmpty()) { "Shortcut ID is blank." }
                        },
                        displayName = requireName(item.getString("displayName")),
                        providerId = TvProviderId.valueOf(item.getString("providerId")),
                        launchTarget = requireTarget(item.getString("launchTarget")),
                        kind = TvAppShortcutKind.valueOf(item.getString("kind")),
                        knownServiceId = if (item.isNull("knownServiceId")) null else item
                            .optString("knownServiceId")
                            .trim()
                            .takeIf(String::isNotEmpty),
                        iconReference = if (item.isNull("iconReference")) null else item
                            .optString("iconReference").trim().takeIf(String::isNotEmpty),
                    )
                }.getOrNull()

                if (shortcut != null && none { it.id == shortcut.id }) {
                    add(shortcut)
                }
            }
        }
    }

    private fun requireName(value: String): String =
        value.trim().also {
            require(it.isNotEmpty()) { "TV app shortcut name cannot be blank." }
            require(it.length <= MAX_NAME_LENGTH) {
                "TV app shortcut name cannot exceed $MAX_NAME_LENGTH characters."
            }
        }

    private fun requireNewTarget(value: String): String =
        requireTarget(value).also { target ->
            val uri = runCatching { URI(target) }.getOrNull()
            require(uri != null && !uri.scheme.isNullOrBlank()) {
                "Enter a complete launch URI, such as https://example.com/ or app://open."
            }
            if (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
                require(!uri.host.isNullOrBlank()) { "The app link needs a valid web address." }
            }
        }

    private fun requireTarget(value: String): String =
        value.trim().also {
            require(it.isNotEmpty()) { "TV app launch target cannot be blank." }
            require(it.length <= MAX_TARGET_LENGTH) {
                "TV app launch target cannot exceed $MAX_TARGET_LENGTH characters."
            }
        }

    private companion object {
        const val PREFERENCES = "couchlink_tv_app_launcher"
        const val KEY_PAYLOAD = "shortcut_library"
        const val SCHEMA_VERSION = 1
        const val MAX_NAME_LENGTH = 80
        const val MAX_TARGET_LENGTH = 2_048
    }
}
