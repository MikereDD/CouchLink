package dev.typezero.couchlink.remote.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import dev.typezero.couchlink.remote.tv.launcher.*
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.ui.theme.CouchLinkTheme
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="w393dp-h800dp")
class TvCustomIconUiTest {
    @get:Rule val compose=createComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()
    private val google=TvProviderId.GoogleAndroidTv
    private lateinit var store:TvAppShortcutStore
    private lateinit var runtime:TvAppLauncherRuntime
    private var nextUri:Uri?=null
    private var launches=0
    private val registry=object:ActivityResultRegistry(){
        override fun <I,O> onLaunch(requestCode:Int,contract:ActivityResultContract<I,O>,input:I,options:ActivityOptionsCompat?){
            val intent=contract.createIntent(context,input)
            assertEquals(Intent.ACTION_OPEN_DOCUMENT,intent.action)
            assertEquals(setOf("image/png","image/jpeg","image/webp"),intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.toSet())
            launches++
            dispatchResult(requestCode,nextUri)
        }
    }
    @Before fun setup(){
        RuntimeEnvironment.setFontScale(1f)
        context.getSharedPreferences("couchlink_tv_app_launcher",0).edit().clear().commit()
        store=TvAppShortcutStore(context);runtime=TvAppLauncherRuntime(store){RecordingTvProvider()}
    }
    private fun image(color:Int=Color.CYAN):ByteArray{
        val bitmap=Bitmap.createBitmap(300,180,Bitmap.Config.ARGB_8888).apply{eraseColor(color)}
        Canvas(bitmap).drawRect(150f,0f,300f,180f,Paint().apply{this.color=Color.rgb(60,70,180)})
        val bytes=ByteArrayOutputStream().also{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()
        bitmap.recycle();return bytes
    }
    private fun select(bytes:ByteArray=image()){
        nextUri=Uri.parse("content://couchlink-test/"+java.util.UUID.randomUUID())
        Shadows.shadowOf(context.contentResolver).registerInputStream(nextUri!!,ByteArrayInputStream(bytes))
        compose.onNodeWithText("ADD ICON").performClick()
        awaitDescription("Selected icon preview")
    }
    private fun draft()=(store.icons.prepare(ByteArrayInputStream(image())) as TvIconImportResult.Ready).draft
    private fun managed()=File(context.noBackupFilesDir,"tv-shortcut-icons").listFiles()?.filter{it.extension=="png"}.orEmpty()
    private fun show(){
        val owner=object:ActivityResultRegistryOwner{override val activityResultRegistry=registry}
        compose.setContent{
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner){
                val shortcuts by store.shortcuts.collectAsState()
                CouchLinkTheme{
                    TvAppLauncherTray(shortcuts,true,"Google TV",{},
                        {name,uri,icon->runtime.addCustomApp(name,google,uri,icon)}, {runtime.remove(it.id)},
                        providerId=google,supportsAppLaunch=true,onAddKnownService={runtime.addKnownService(it,google)},
                        onMove={item,index->runtime.move(item.id,index)},iconStorage=store.icons,
                        onSetCustomIcon={item,icon->runtime.setCustomIcon(item.id,icon)})
                }
            }
        }
        compose.onNodeWithText("STREAMING APPS").performClick();settle()
    }
    private fun settle(){
        repeat(40){
            compose.mainClock.advanceTimeByFrame()
            compose.runOnUiThread{
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                fun layout(v:android.view.View){
                    if(v is androidx.compose.ui.platform.ViewRootForTest && v.hasPendingMeasureOrLayout)v.measureAndLayoutForTest()
                    if(v is android.view.ViewGroup)for(i in 0 until v.childCount)layout(v.getChildAt(i))
                }
                android.view.inspector.WindowInspector.getGlobalWindowViews().forEach(::layout)
            }
        }
    }
    private fun awaitDescription(text:String){
        compose.waitUntil(15_000){settle();compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription(text).assertIsDisplayed()
    }
    private fun openCustom(){
        compose.onNodeWithText("+ ADD APP").performClick();compose.onNodeWithText("Custom App").performClick();settle()
    }
    private fun edit(name:String="Personal Player"){
        if(compose.onAllNodesWithText("MANAGE").fetchSemanticsNodes().isNotEmpty())compose.onNodeWithText("MANAGE").performClick()
        compose.onNodeWithContentDescription("Manage $name").performClick()
        compose.onNodeWithText("EDIT ICON").performClick();settle()
    }
    private fun capture(name:String){
        settle();compose.runOnUiThread{
            val view=android.view.inspector.WindowInspector.getGlobalWindowViews().last()
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap));val dir=File("build/reports/streaming-launcher").apply{mkdirs()}
            File(dir,"$name.png").outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle()
        }
    }
    @Test fun pickerPreviewSaveAndFreshStoreRestoreCustomImage(){
        show();openCustom();select()
        assertEquals(1,launches);assertTrue(managed().isEmpty())
        // Match the established host-dialog focus/layout protocol; keep normal text actions.
        compose.onNodeWithText("App name").performClick()

        settle();compose.onNodeWithText("App name").performTextInput("Personal Player");settle()
        compose.onNodeWithText("Launch URI / app link").performClick();settle()
        compose.onNodeWithText("Launch URI / app link").performTextInput("personal-player://working");settle()
        capture("d2-create-preview-host")
        compose.onNodeWithText("ADD").performClick()
        compose.waitUntil(15_000){store.shortcuts.value.size==1};settle()
        val saved=store.shortcuts.value.single()
        assertEquals("personal-player://working",saved.launchTarget)
        assertEquals(saved,TvAppShortcutStore(context).shortcuts.value.single())
        awaitDescription("Custom app icon");capture("d2-custom-tile-host")
        assertEquals(1,managed().size)
    }
    @Test fun replacementCancelThenSavePreservesOtherFieldsAndRemovesOldFile(){
        val saved=runtime.addCustomApp("Personal Player",google,"personal-player://working",draft())
        runtime.addCustomApp("Other",google,"other://open")
        val before=store.shortcuts.value;show();edit();select(image(Color.GREEN));capture("d2-edit-preview-host")
        compose.onNodeWithText("CANCEL").performClick();settle()
        assertEquals(before,store.shortcuts.value);assertEquals(1,managed().size)
        edit();select(image(Color.GREEN));compose.onNodeWithText("SAVE ICON").performClick()
        compose.waitUntil(15_000){store.shortcuts.value.first().iconReference!=saved.iconReference};settle()
        assertEquals(before,store.shortcuts.value.map{if(it.id==saved.id)it.copy(iconReference=saved.iconReference)else it})
        compose.waitUntil(10_000){managed().size==1}
        assertEquals(store.shortcuts.value,TvAppShortcutStore(context).shortcuts.value)
    }
    @Test fun resetIconKeepsNameUriProviderIdentityAndPosition(){
        val saved=runtime.addCustomApp("Personal Player",google,"personal-player://working",draft())
        val before=store.shortcuts.value;show();edit()
        awaitDescription("Saved icon preview")
        compose.onNodeWithText("RESET ICON").performClick();settle()
        compose.onNodeWithContentDescription("Default icon preview").assertIsDisplayed()
        compose.onNodeWithText("SAVE ICON").performClick()
        compose.waitUntil(15_000){store.shortcuts.value.single().iconReference==null};settle()
        assertEquals(before.map{it.copy(iconReference=null)},store.shortcuts.value)
        assertEquals(saved.id,store.shortcuts.value.single().id)
        compose.waitUntil(10_000){managed().isEmpty()}
    }
    @Test fun canceledSelectionAndCanceledDraftNeverWriteFiles(){
        show();openCustom();nextUri=null;compose.onNodeWithText("ADD ICON").performClick();settle()
        compose.onNodeWithContentDescription("Default icon preview").assertIsDisplayed()
        assertTrue(managed().isEmpty());select();assertTrue(managed().isEmpty())
        nextUri=null;compose.onNodeWithText("ADD ICON").performClick();settle()
        compose.onNodeWithContentDescription("Selected icon preview").assertIsDisplayed()
        compose.onNodeWithText("CANCEL").performClick();settle()
        assertTrue(store.shortcuts.value.isEmpty());assertTrue(managed().isEmpty())
    }
    @Test fun rejectedReplacementKeepsSavedIconAndShortcut(){
        val saved=runtime.addCustomApp("Personal Player",google,"personal-player://working",draft())
        show();edit();awaitDescription("Saved icon preview")
        nextUri=Uri.parse("content://couchlink-test/invalid")
        Shadows.shadowOf(context.contentResolver).registerInputStream(nextUri!!,ByteArrayInputStream(byteArrayOf(1,2,3)))
        compose.onNodeWithText("ADD ICON").performClick()
        compose.waitUntil(15_000){settle();compose.onAllNodes(hasText("Use a valid PNG",substring=true)).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Saved icon preview").assertIsDisplayed()
        compose.onNodeWithText("SAVE ICON").assertIsNotEnabled()
        assertEquals(saved,store.shortcuts.value.single());assertEquals(1,managed().size)
    }
    @Test @Config(qualifiers="w360dp-h640dp") fun largeTextEditorKeepsIconControlsReachable(){
        runtime.addCustomApp("Personal Player",google,"personal-player://working",draft())
        RuntimeEnvironment.setFontScale(1.6f);show();edit();awaitDescription("Saved icon preview")
        assertEquals(1.6f,context.resources.configuration.fontScale,0.01f)
        compose.onNodeWithText("ADD ICON").assertIsDisplayed()
        compose.onNodeWithText("RESET ICON").assertIsDisplayed()
        compose.onNodeWithText("SAVE ICON").assertIsDisplayed()
        capture("d2-edit-large-text-host")
    }
    @Test fun knownServicesNeverExposeUserIconManagementAndMissingCustomIconFallsBack(){
        runtime.addKnownService("netflix",google)
        val saved=runtime.addCustomApp("Personal Player",google,"personal-player://working")
        store.update(saved.copy(iconReference="tvicon:0123456789abcdef0123456789abcdef"))
        show();compose.onNodeWithContentDescription("Launch Personal Player").assertIsDisplayed()
        compose.onNodeWithContentDescription("Custom app icon").assertDoesNotExist()
        compose.onNodeWithText("MANAGE").performClick();compose.onNodeWithContentDescription("Manage Netflix").performClick()
        compose.onNodeWithText("EDIT ICON").assertDoesNotExist();compose.onNodeWithText("ADD ICON").assertDoesNotExist()
    }
}
