package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.typezero.couchlink.remote.tv.launcher.*
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.ui.theme.CouchLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h800dp")
class TvAppLauncherTrayTest {
    @get:Rule val compose = createComposeRule()
    private val shortcut = TvAppShortcut("netflix-saved", "Netflix", TvProviderId.GoogleAndroidTv, "https://www.netflix.com/browse", TvAppShortcutKind.KnownService, "netflix")
    @Test fun cardStartsCollapsedAndOpensStreamingSheet() {
        compose.setContent {
            CouchLinkTheme { TvAppLauncherTray(listOf(shortcut), true, "Google TV", {}, { _, _, _ -> }, {},
                providerId = TvProviderId.GoogleAndroidTv, supportsAppLaunch = true, onAddKnownService = {}, onMove = { _, _ -> }) }
        }
        compose.onNodeWithText("Netflix").assertDoesNotExist()
        compose.onNodeWithText("STREAMING APPS").performClick()
        compose.onNodeWithText("Netflix").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close streaming apps").performClick()
        compose.onNodeWithText("Netflix").assertDoesNotExist()
    }
    @Test fun originalGenericIconIsBundledWithoutThirdPartyMarks() {
        val context = RuntimeEnvironment.getApplication()
        val generic = context.resources.getIdentifier("ic_tv_app_generic", "drawable", context.packageName)
        assertTrue("Missing original generic app icon", generic != 0)
        assertNotNull(context.getDrawable(generic))
        listOf("tv_service_netflix", "tv_service_apple_tv", "tv_service_hulu").forEach {
            assertEquals("Uncleared graphic must not be bundled: $it", 0,
                context.resources.getIdentifier(it, "drawable", context.packageName))
        }
    }
}
