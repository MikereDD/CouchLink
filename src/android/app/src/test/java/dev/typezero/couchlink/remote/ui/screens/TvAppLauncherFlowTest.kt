package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.tv.launcher.*
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.ui.theme.CouchLinkTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)

@Config(sdk = [35], qualifiers = "w393dp-h800dp")
class TvAppLauncherFlowTest {
    @get:Rule val compose = createComposeRule()
    private val google = TvProviderId.GoogleAndroidTv
    private lateinit var store: TvAppShortcutStore
    private lateinit var runtime: TvAppLauncherRuntime
    private lateinit var provider: RecordingTvProvider
    @Before fun setup() {
        store = TvAppShortcutStore(RuntimeEnvironment.getApplication())
        store.clear()
        provider = RecordingTvProvider()
        runtime = TvAppLauncherRuntime(store) { provider }
    }
    private fun show(ready: Boolean = true, supported: Boolean = true, connecting: Boolean = false, fontScale: Float = 1f) {
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            val shortcuts by store.shortcuts.collectAsState()
            CouchLinkTheme {

                TvAppLauncherTray(shortcuts, ready, "Google TV", { runtime.launch(it) },
                    { name, target, icon -> runtime.addCustomApp(name, google, target, icon) }, { runtime.remove(it.id) },
                    providerId = google, supportsAppLaunch = supported, connecting = connecting,
                    onAddKnownService = { runtime.addKnownService(it, google) }, onMove = { item, index -> runtime.move(item.id, index) })
            }
        }
    }
    private fun open() { compose.onNodeWithText("STREAMING APPS").performClick() }
    private fun captureWindow(name: String) {
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = android.view.inspector.WindowInspector.getGlobalWindowViews().last()
            assertTrue(view.isShown && view.width > 0 && view.height > 0)
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val directory = java.io.File("build/reports/streaming-launcher").apply { mkdirs() }
            java.io.File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }
    @Test fun threeColumnLayoutRendersGenericIconsAndServiceLabels() {
        listOf("netflix", "apple-tv", "hulu").forEach { runtime.addKnownService(it, google) }
        runtime.addCustomApp("My TV App", google, "personal-app://open")
        show()
        captureWindow("streaming-card-host")
        open()
        listOf("Netflix", "Apple TV", "Hulu", "My TV App").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        val bounds = listOf("Netflix", "Apple TV", "Hulu").map { compose.onNodeWithContentDescription("Launch $it").fetchSemanticsNode().boundsInRoot }
        assertEquals(bounds[0].width, bounds[1].width, 1f)
        assertEquals(bounds[1].width, bounds[2].width, 1f)
        assertTrue(bounds[0].left < bounds[1].left && bounds[1].left < bounds[2].left)
        captureWindow("streaming-sheet-host")
    }
    @Test @Config(qualifiers = "w360dp-h640dp") fun narrowPhoneWithLargeTextKeepsActionsReachable() {
        listOf("netflix", "apple-tv", "hulu").forEach { runtime.addKnownService(it, google) }
        show(fontScale = 1.6f)
        assertEquals(1.6f, RuntimeEnvironment.getApplication().resources.configuration.fontScale, 0.01f)
        open()
        compose.onNodeWithText("+ ADD APP").assertIsDisplayed()
        compose.onNodeWithText("Hulu").performScrollTo().assertIsDisplayed()
        captureWindow("streaming-sheet-large-text-host")
    }
    private fun add() { compose.onNodeWithText("+ ADD APP").performClick() }
    @Test fun normalTapRequestsLaunchAndDismissesWithoutEnteringManagement() {
        val saved = runtime.addKnownService("hulu", google)
        show(); open()
        compose.onNodeWithText("Move earlier").assertDoesNotExist()
        compose.onNodeWithContentDescription("Launch Hulu").performClick()
        compose.onNodeWithText("Hulu").assertDoesNotExist()
        assertEquals(listOf(saved.toProviderApp()), provider.requests)
    }
    @Test fun knownServiceChoiceAddsPresetAndDisablesDuplicate() {
        show(); open(); add()
        compose.onNodeWithText("Known Service").performClick()
        compose.onNodeWithText("Netflix").performClick()
        assertEquals("https://www.netflix.com/browse", store.shortcuts.value.single().launchTarget)
        add()
        compose.onNodeWithText("Known Service").performClick()
        compose.onNode(hasText("Netflix") and hasText("Added")).assertIsNotEnabled()
        compose.onNodeWithText("Added").assertIsDisplayed()
        assertEquals(1, store.shortcuts.value.size)
        compose.onNodeWithText("Prime Video").assertDoesNotExist()
        compose.onNodeWithText("Apple TV").assertIsDisplayed()
        compose.onNodeWithText("Hulu").assertIsDisplayed()
    }
    private fun settleDialogLayout() {
        // Robolectric does not perform draw passes. Drive the pending dialog
        // measure/layout explicitly, without disabling Compose's idle checks.
        // Advance virtual frames through the text-field label transition while
        // servicing native dialog layouts; no wall-clock sleep or skipped checks.
        repeat(40) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnUiThread {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                fun measure(view: android.view.View) {
                    if (view is androidx.compose.ui.platform.ViewRootForTest && view.hasPendingMeasureOrLayout) {
                        view.measureAndLayoutForTest()
                    }
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) measure(view.getChildAt(i))
                }
                android.view.inspector.WindowInspector.getGlobalWindowViews().forEach(::measure)
            }
        }
    }
    private fun replaceFieldText(label: String, text: String) {
        // Focus and input use the normal Compose text actions. Service each
        // native dialog layout before the next synchronized action.
        compose.onNodeWithText(label).performClick()
        settleDialogLayout()
        compose.onNodeWithText(label).performTextClearance()
        settleDialogLayout()
        compose.onNodeWithText(label).performTextInput(text)
        settleDialogLayout()
        compose.onNodeWithText(label).assertTextContains(text)
    }
    @Test fun textFieldRenderingPositiveControl() {
        compose.setContent {
            CouchLinkTheme {
                var visible by remember { mutableStateOf(false) }
                androidx.compose.material3.TextButton(onClick = { visible = true }) { androidx.compose.material3.Text("Open field probe") }
                if (visible) androidx.compose.material3.AlertDialog(
                    onDismissRequest = {},
                    text = { androidx.compose.material3.OutlinedTextField(value = "", onValueChange = {}, label = { androidx.compose.material3.Text("Field probe") }) },
                    confirmButton = { androidx.compose.material3.TextButton(onClick = {}) { androidx.compose.material3.Text("Probe action") } },
                )
            }
        }
        compose.onNodeWithText("Open field probe").performClick()
        settleDialogLayout()
        compose.onNodeWithText("Field probe").assertIsDisplayed()
    }
    @Test fun customFormShowsInvalidUriErrorAndThenSavesValidShortcut() {
        show(); open(); add()
        compose.onNodeWithText("Custom App").performClick()
        settleDialogLayout()
        compose.onNodeWithText("ADD").assertIsDisplayed().assertIsNotEnabled()
        println("CUSTOM_EMPTY_ADD\n" + compose.onNodeWithText("ADD").printToString())
        replaceFieldText("App name", "My app")
        replaceFieldText("Launch URI / app link", "not a uri")
        compose.onNodeWithText("ADD").assertIsDisplayed().assertIsEnabled().performClick()
        settleDialogLayout()
        compose.onNodeWithText("Enter a complete launch URI, such as https://example.com/ or app://open.").assertIsDisplayed()
        assertTrue(store.shortcuts.value.isEmpty())
        replaceFieldText("Launch URI / app link", "personal-app://open")
        compose.onNodeWithText("ADD").assertIsDisplayed().assertIsEnabled().performClick()
        settleDialogLayout()
        assertEquals("personal-app://open", store.shortcuts.value.single().launchTarget)
        assertEquals(TvAppShortcutKind.CustomApp, store.shortcuts.value.single().kind)
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(store.shortcuts.value.single()))
        compose.onNodeWithText("My app").assertIsDisplayed()
    }
    @Test fun managementReordersAndRemovesWhileDisconnected() {
        val first = runtime.addKnownService("netflix", google)
        val second = runtime.addKnownService("hulu", google)
        show(ready = false); open()
        compose.onNodeWithContentDescription("Launch Netflix").assertIsNotEnabled()
        compose.onNodeWithText("MANAGE").performClick()
        compose.onNodeWithContentDescription("Manage Netflix").performClick()
        compose.onNodeWithText("Move earlier").assertIsNotEnabled()
        compose.onNodeWithText("Move later").performClick()
        assertEquals(listOf(second.id, first.id), store.shortcuts.value.map { it.id })
        compose.onNodeWithText("Move later").assertIsNotEnabled()
        compose.onNodeWithText("Remove app").performClick()
        assertEquals(listOf(second.id), TvAppShortcutStore(RuntimeEnvironment.getApplication()).shortcuts.value.map { it.id })
        assertTrue(provider.requests.isEmpty())
    }
    @Test fun unsupportedCapabilityExplainsWhyLaunchingIsUnavailable() {
        runtime.addKnownService("netflix", google)
        show(ready = false, supported = false); open()
        compose.onNodeWithText("+ ADD APP").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Launch Netflix").assertIsNotEnabled()
        compose.onAllNodesWithText("App launching is not supported by this TV provider.").assertCountEquals(2)
        compose.onNodeWithText("MANAGE").assertIsEnabled()
    }
    @Test fun connectingStateIsNotReportedAsReady() {
        show(ready = false, connecting = true)
        compose.onNodeWithText("Connecting to TV…").assertIsDisplayed()
    }
    @Test fun iconResolutionUsesOriginalGenericForAllShortcutKinds() {
        val known = runtime.addKnownService("netflix", google)
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(known.copy(displayName = "Renamed")))
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(runtime.addKnownService("apple-tv", google)))
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(runtime.addKnownService("hulu", google)))
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(known.copy(knownServiceId = "unknown")))
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(known.copy(iconReference = "future:override")))
        assertEquals(R.drawable.ic_tv_app_generic, streamingShortcutIcon(known.copy(kind = TvAppShortcutKind.CustomApp)))
    }
}
