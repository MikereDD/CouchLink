package dev.typezero.couchlink.remote.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import dev.typezero.couchlink.remote.tv.provider.*
import dev.typezero.couchlink.remote.ui.theme.CouchLinkTheme
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w420dp-h1000dp")
class TvDpadGeometryTest {
    @get:Rule val compose = createComposeRule()
    private var fixtureWidth = 395f
    private val density get() = RuntimeEnvironment.getApplication().resources.displayMetrics.density
    private val commands = mutableListOf<TvRemoteCommand>()
    private var haptics = 0

    private fun show(width: Float, font: Float = 1.15f, enabled: Boolean = true, rtl: Boolean = false) {
        fixtureWidth = width
        RuntimeEnvironment.setFontScale(font)
        val state = TvProviderState(
            providerId = TvProviderId.GoogleAndroidTv,
            capabilities = setOf(TvCapability.Dpad),
            connection = TvProviderConnectionState(connected = enabled, ready = enabled),
            message = "Host-only D-pad geometry fixture",
        )
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                CouchLinkTheme {
                    Column(Modifier.width(width.dp).padding(horizontal = 18.dp)) {
                        TvRemoteScreen(state, {}, { commands += it }, {}, { haptics++ })
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(font, RuntimeEnvironment.getApplication().resources.configuration.fontScale, 0.01f)
    }

    private fun ok(): Rect = compose.onNodeWithText("OK").fetchSemanticsNode().boundsInRoot
    private fun directions(): List<Rect> = compose.onAllNodes(hasClickAction() and isEnabled() and !hasText("OK"))
        .fetchSemanticsNodes().map { it.boundsInRoot }.also { assertEquals("Four independent arrows", 4, it.size) }

    private fun verifyGeometry() {
        val ok = ok(); val arrows = directions(); val center = ok.center
        val diameter = minOf(236f, fixtureWidth - 36f) * density
        val radius = diameter / 2f - 1.25f * density
        val all = arrows + ok
        all.forEachIndexed { index, rect ->
            assertTrue("Target $index must be at least 48dp wide", rect.width + 1f >= 48f * density)
            assertTrue("Target $index must be at least 48dp high", rect.height + 1f >= 48f * density)
            listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { corner ->
                assertTrue("Target $index extends outside circular boundary: $rect, radius=$radius",
                    hypot(corner.x-center.x, corner.y-center.y) <= radius + 0.75f)
            }
        }
        all.indices.forEach { a -> (a+1 until all.size).forEach { b ->
            val overlapW = minOf(all[a].right,all[b].right)-maxOf(all[a].left,all[b].left)
            val overlapH = minOf(all[a].bottom,all[b].bottom)-maxOf(all[a].top,all[b].top)
            assertFalse("Touch targets $a and $b overlap", overlapW > 0f && overlapH > 0f)
        } }
        val radii = arrows.map { hypot(it.center.x-center.x, it.center.y-center.y) }
        assertTrue("All arrows must share one radial distance: $radii", radii.max()-radii.min() <= 1.5f)
        val up=arrows.minBy{it.center.y};val down=arrows.maxBy{it.center.y}
        val left=arrows.minBy{it.center.x};val right=arrows.maxBy{it.center.x}
        assertTrue(abs(up.center.x-center.x)<=1f && abs(down.center.x-center.x)<=1f)
        assertTrue(abs(left.center.y-center.y)<=1f && abs(right.center.y-center.y)<=1f)
        assertTrue(abs((up.center.y+down.center.y)/2-center.y)<=1f)
        assertTrue(abs((left.center.x+right.center.x)/2-center.x)<=1f)
    }

    private fun capture(name: String) {
        val ok=ok();val arrows=directions();val dir=File("build/reports/dpad-geometry").apply{mkdirs()}
        val array=JSONArray()
        (arrows+ok).forEach { array.put(JSONObject().put("left",it.left/density).put("top",it.top/density)
            .put("right",it.right/density).put("bottom",it.bottom/density)) }
        File(dir,"$name.json").writeText(JSONObject().put("fixture_width_dp",fixtureWidth)
            .put("font_scale",RuntimeEnvironment.getApplication().resources.configuration.fontScale)
            .put("density",density).put("targets_dp",array).toString(2))
        compose.runOnUiThread {
            val view=android.view.inspector.WindowInspector.getGlobalWindowViews().last()
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val top=(ok.center.y-129f*density).toInt().coerceAtLeast(0)
            val left=(18f*density).toInt()
            val crop=Bitmap.createBitmap(bitmap,left,top,((fixtureWidth-36f)*density).toInt(),(258f*density).toInt())
            File(dir,"$name.png").outputStream().use { assertTrue(crop.compress(Bitmap.CompressFormat.PNG,100,it)) }
            crop.recycle();bitmap.recycle()
        }
    }

    @Test fun normalPixelContainsAllFourTouchTargets() {
        show(395f);capture("dpad-pixel-host");verifyGeometry()
    }

    @Test fun narrowPhoneKeepsTheSameContainedGeometry() {
        show(320f,1f);capture("dpad-narrow-host");verifyGeometry()
    }

    @Test fun largeTextDoesNotMoveArrowTargets() {
        show(320f,1.6f);capture("dpad-large-text-host");verifyGeometry()
    }

    @Test fun veryLargeTextKeepsOkAndArrowsSeparated() {
        show(280f,2f);capture("dpad-very-large-text-host");verifyGeometry()
    }

    @Test fun compactWindowPreservesAccessibleTargets() {
        show(196f,1f);capture("dpad-compact-host");verifyGeometry()
    }

    private fun touchPhysicalDirections() {
        val arrows=directions()
        val physical=listOf(arrows.minBy{it.center.y},arrows.maxBy{it.center.y},
            arrows.minBy{it.center.x},arrows.maxBy{it.center.x})
        physical.forEach { bounds ->
            compose.onRoot().performTouchInput { click(bounds.center) };compose.waitForIdle()
        }
        assertEquals(listOf(TvRemoteCommand.DpadUp,TvRemoteCommand.DpadDown,
            TvRemoteCommand.DpadLeft,TvRemoteCommand.DpadRight),commands)
    }

    @Test fun independentTouchTargetsDispatchExactlyTheIntendedCommand() {
        show(395f);verifyGeometry();touchPhysicalDirections()
        compose.onNodeWithText("OK").performTouchInput { click() };compose.waitForIdle()
        assertEquals(TvRemoteCommand.Select,commands.last());assertEquals(5,commands.size);assertEquals(5,haptics)
        val center=ok().center
        compose.onRoot().performTouchInput { click(Offset(center.x+116f*density,center.y)) }
        compose.waitForIdle();assertEquals(5,commands.size);assertEquals(5,haptics)
        listOf("up","down","left","right").forEach {
            compose.onNodeWithContentDescription("D-pad $it").assertHasClickAction().assertIsDisplayed()
        }
    }

    @Test fun disabledDpadSendsNoCommandsOrHaptics() {
        show(395f,enabled=false)
        listOf("up","down","left","right").forEach {
            compose.onNodeWithContentDescription("D-pad $it").assertIsNotEnabled().performTouchInput { click() }
        }
        compose.onNodeWithText("OK").assertIsNotEnabled().performTouchInput { click() }
        compose.waitForIdle();assertTrue(commands.isEmpty());assertEquals(0,haptics)
    }

    @Test fun rtlStillMapsPhysicalLeftAndRightCorrectly() {
        show(395f,1f,rtl=true);verifyGeometry();touchPhysicalDirections()
    }
}
