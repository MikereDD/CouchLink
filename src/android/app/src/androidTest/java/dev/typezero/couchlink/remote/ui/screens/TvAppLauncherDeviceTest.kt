package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typezero.couchlink.remote.tv.launcher.TvAppShortcut
import dev.typezero.couchlink.remote.tv.launcher.TvAppShortcutKind
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.ui.theme.CouchLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-only fixture. Does not create TV controllers, connect, or change saved app data. */
@RunWith(AndroidJUnit4::class)
class TvAppLauncherDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val google = TvProviderId.GoogleAndroidTv
    private val requests = mutableListOf<TvAppShortcut>()
    private val knownAdded = mutableListOf<String>()
    private var customAdded: Pair<String, String>? = null
    private fun show(shortcuts: List<TvAppShortcut> = emptyList()) {
        compose.setContent {
            CouchLinkTheme {
                TvAppLauncherTray(shortcuts, true, "Google TV", { requests += it },
                    { name, uri, _ -> customAdded = name to uri }, {}, providerId = google,
                    supportsAppLaunch = true, onAddKnownService = { knownAdded += it }, onMove = { _, _ -> })
            }
        }
        compose.onNodeWithText("STREAMING APPS").performClick()
    }
    @Test fun knownServiceUsesStableIdWithoutTypingUri() {
        show()
        compose.onNodeWithText("+ ADD APP").performClick()
        compose.onNodeWithText("Known Service").performClick()
        compose.onNodeWithText("Apple TV").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("apple-tv"), knownAdded); assertTrue(requests.isEmpty()) }
    }
    @Test fun customFormSupportsNormalFocusTypingAndSave() {
        show()
        compose.onNodeWithText("+ ADD APP").performClick()
        compose.onNodeWithText("Custom App").performClick()
        compose.onNodeWithText("ADD").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("App name").performTextInput("My TV App")
        compose.onNodeWithText("Launch URI / app link").performTextInput("personal-app://open")
        compose.onNodeWithText("ADD").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("My TV App" to "personal-app://open", customAdded) }
        compose.onNodeWithText("Launch URI / app link").assertDoesNotExist()
    }
    @Test fun normalTapSubmitsOneRequestAndDismisses() {
        val hulu = TvAppShortcut("test-hulu", "Hulu", google, "hulu://video/", TvAppShortcutKind.KnownService, "hulu")
        show(listOf(hulu))
        compose.onNodeWithContentDescription("Launch Hulu").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf(hulu), requests) }
        compose.onNodeWithText("Hulu").assertDoesNotExist()
    }
}
