package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import java.util.UUID
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

    fun add(
        displayName: String,
        providerId: TvProviderId,
        launchTarget: String,
        kind: TvAppShortcutKind,
        knownServiceId: String? = null,
    ): TvAppShortcut {
        val shortcut = TvAppShortcut(
            id = UUID.randomUUID().toString(),
            displayName = requireName(displayName),
            providerId = providerId,
            launchTarget = requireTarget(launchTarget),
            kind = kind,
            knownServiceId = knownServiceId?.trim()?.takeIf(String::isNotEmpty),
        )
        persist(_shortcuts.value + shortcut)
        return shortcut
    }

    fun update(shortcut: TvAppShortcut) {
        val clean = shortcut.copy(
            displayName = requireName(shortcut.displayName),
            launchTarget = requireTarget(shortcut.launchTarget),
            knownServiceId = shortcut.knownServiceId?.trim()?.takeIf(String::isNotEmpty),
        )
        val current = _shortcuts.value
        val index = current.indexOfFirst { it.id == clean.id }
        require(index >= 0) { "Unknown TV app shortcut: ${clean.id}" }

        val next = current.toMutableList().apply { this[index] = clean }
        persist(next)
    }

    fun remove(id: String): Boolean {
        val next = _shortcuts.value.filterNot { it.id == id }
        if (next.size == _shortcuts.value.size) return false
        persist(next)
        return true
    }

    fun move(id: String, newIndex: Int): Boolean {
        val current = _shortcuts.value
        val from = current.indexOfFirst { it.id == id }
        if (from < 0 || current.isEmpty()) return false

        val target = newIndex.coerceIn(0, current.lastIndex)
        if (from == target) return true

        val next = current.toMutableList()
        val item = next.removeAt(from)
        next.add(target, item)
        persist(next)
        return true
    }

    fun clear() {
        persist(emptyList())
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
                                .put("knownServiceId", shortcut.knownServiceId ?: JSONObject.NULL),
                        )
                    }
                },
            )
            .toString()

        preferences.edit().putString(KEY_PAYLOAD, payload).apply()
        _shortcuts.value = shortcuts.toList()
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
                        knownServiceId = item
                            .optString("knownServiceId")
                            .trim()
                            .takeIf(String::isNotEmpty),
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
