package dev.typezero.couchlink.remote.tv.launcher

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TvCustomIconStorageTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun encoded(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, w: Int = 600, h: Int = 300): ByteArray {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawRect(w/2f, 0f, w.toFloat(), h.toFloat(), android.graphics.Paint().apply { color = Color.BLUE })
        val bytes = ByteArrayOutputStream().also { assertTrue(bitmap.compress(format, 95, it)) }.toByteArray()
        bitmap.recycle(); return bytes
    }
    private fun draft(bytes: ByteArray = encoded()): TvIconDraft =
        (TvCustomIconStorage(context).prepare(ByteArrayInputStream(bytes)) as TvIconImportResult.Ready).draft
    private fun root() = File(context.noBackupFilesDir, "tv-shortcut-icons")
    private fun file(ref: String) = File(root(), ref.removePrefix("tvicon:")+".png")
    private fun store(): TvAppShortcutStore {
        context.getSharedPreferences("couchlink_tv_app_launcher", 0).edit().clear().commit()
        return TvAppShortcutStore(context)
    }
    @Test fun pngJpegWebpNormalizeToBoundedTransparentPngWithoutPersistingDrafts() {
        val storage = TvCustomIconStorage(context)
        listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.JPEG, Bitmap.CompressFormat.WEBP_LOSSLESS).forEach {
            val ready = storage.prepare(ByteArrayInputStream(encoded(it))) as TvIconImportResult.Ready
            assertEquals(256, ready.draft.preview.width); assertEquals(256, ready.draft.preview.height)
            assertEquals(0, Color.alpha(ready.draft.preview.getPixel(0, 0)))
            assertTrue(ready.draft.png.size <= TvCustomIconStorage.MAX_FILE_BYTES)
        }
        assertTrue("Preview/cancel must not write files", root().listFiles().isNullOrEmpty())
    }
    @Test @Config(sdk = [28]) fun importerWorksAtMinimumSupportedApi() {
        assertEquals(256, draft().preview.width)
    }
    @Test fun exifRotationIsBakedIntoNormalizedPixels() {
        val jpg = encoded(Bitmap.CompressFormat.JPEG, 40, 20)
        val exif = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            .put(byteArrayOf(69,120,105,102,0,0,73,73)).putShort(42).putInt(8).putShort(1)
            .putShort(0x112).putShort(3).putInt(1).putShort(6).putShort(0).putInt(0).array()
        val rotated = jpg.take(2).toByteArray()+byteArrayOf(0xff.toByte(),0xe1.toByte(),0,34)+exif+jpg.drop(2).toByteArray()
        val image = draft(rotated).preview
        assertTrue("Rotated red half should be above blue", Color.red(image.getPixel(128,64)) > 180)
        assertTrue(Color.blue(image.getPixel(128,192)) > 180)
        assertEquals(0, Color.alpha(image.getPixel(0,128)))
    }
    @Test fun rejectsMalformedUnsupportedOversizedAndNonDocumentInputs() {
        val storage = TvCustomIconStorage(context)
        val gif = java.util.Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
        listOf(byteArrayOf(1,2,3), gif, ByteArray(TvCustomIconStorage.MAX_INPUT_BYTES+1), encoded(w=8193,h=1)).forEach {
            assertTrue(storage.prepare(ByteArrayInputStream(it)) is TvIconImportResult.Rejected)
        }
        assertTrue(storage.prepare(Uri.parse("https://example.invalid/icon.png")) is TvIconImportResult.Rejected)
        assertTrue(root().listFiles().isNullOrEmpty())
    }
    @Test fun savesOpaqueReferencesAndSurvivesFreshStoreInstances() {
        val store = store()
        val saved = store.add("Personal", TvProviderId.GoogleAndroidTv, "personal-app://working", TvAppShortcutKind.CustomApp, iconDraft=draft())
        assertTrue(saved.iconReference!!.matches(Regex("tvicon:[a-f0-9]{32}")))
        assertEquals(saved, TvAppShortcutStore(context).shortcuts.value.single())
        assertEquals(256, store.icons.load(saved.iconReference)!!.width)
        val payload = context.getSharedPreferences("couchlink_tv_app_launcher",0).getString("shortcut_library","")!!
        assertFalse(payload.contains("content://")); assertFalse(payload.contains(context.noBackupFilesDir.absolutePath))
    }
    @Test fun replaceResetAndDeleteRespectSharedReferencesAndUnrelatedFiles() {
        val store = store(); val google = TvProviderId.GoogleAndroidTv
        val a = store.add("A",google,"a://open",TvAppShortcutKind.CustomApp,iconDraft=draft())
        val b = store.add("B",TvProviderId.Roku,"b://open",TvAppShortcutKind.CustomApp)
        store.update(b.copy(iconReference=a.iconReference))
        val unrelated=File(root(),"user-kept.png").apply { writeText("untouched") }
        store.setCustomIcon(a.id,draft(encoded(w=128,h=128)))
        val changed=store.shortcuts.value.first()
        assertNotEquals(a.iconReference,changed.iconReference)
        assertTrue(file(a.iconReference!!).exists())
        store.remove(b.id)
        assertFalse(file(a.iconReference).exists())
        assertTrue(file(changed.iconReference!!).exists())
        store.setCustomIcon(a.id,null)
        assertFalse(file(changed.iconReference).exists()); assertTrue(unrelated.exists())
        assertEquals(a.copy(iconReference=null),store.shortcuts.value.single())
    }
    @Test fun iconOnlyUpdatesPreserveIdentityProviderUriAndOrderIncludingLegacyTargets() {
        val store=store(); val g=TvProviderId.GoogleAndroidTv
        val a=store.add("A",g,"a://open",TvAppShortcutKind.CustomApp)
        val b=store.add("B",g,"b://open",TvAppShortcutKind.CustomApp)
        store.update(b.copy(launchTarget="legacy target without scheme"))
        val before=store.shortcuts.value
        store.setCustomIcon(b.id,draft())
        val after=store.shortcuts.value
        assertEquals(before.map{it.id},after.map{it.id})
        assertEquals(before[0],after[0]); assertEquals(before[1],after[1].copy(iconReference=before[1].iconReference))
        store.setCustomIcon(b.id,null); assertEquals(before,store.shortcuts.value)
        assertEquals(a.id,store.shortcuts.value.first().id)
    }
    @Test fun missingCorruptAndUnsafeReferencesFallBackWithoutTraversal() {
        val storage=TvCustomIconStorage(context)
        assertNull(storage.load(null)); assertNull(storage.load("../../private"));assertNull(storage.load("content://provider/image"))
        assertNull(storage.load("tvicon:0123456789abcdef0123456789abcdef"))
        val ref=storage.write(draft());file(ref).writeText("broken png")
        assertNull(storage.load(ref))
    }
    @Test fun startupPrunesCrashOrphansButUnknownSchemaDeletesNothing() {
        val store=store();val orphan=store.icons.write(draft());assertTrue(file(orphan).exists())
        TvAppShortcutStore(context);assertFalse(file(orphan).exists())
        val keep=store.icons.write(draft())
        context.getSharedPreferences("couchlink_tv_app_launcher",0).edit().putString("shortcut_library","{\"version\":99,\"shortcuts\":[]}").commit()
        TvAppShortcutStore(context);assertTrue(file(keep).exists())
    }
    @Test fun failedMetadataCommitRestoresPreviousState() {
        val real = context
        real.getSharedPreferences("couchlink_tv_app_launcher",0).edit().clear().commit()
        var refuse = false
        val wrapped = object : android.content.ContextWrapper(real) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                val actual = real.getSharedPreferences(name,mode)
                return object : android.content.SharedPreferences by actual {
                    override fun edit(): android.content.SharedPreferences.Editor {
                        val editor = actual.edit()
                        return object : android.content.SharedPreferences.Editor by editor {
                            override fun putString(key: String, value: String?): android.content.SharedPreferences.Editor {
                                editor.putString(key,value); return this
                            }
                            override fun commit(): Boolean {
                                val result = editor.commit()
                                if (refuse) { refuse=false; return false }
                                return result
                            }
                        }
                    }
                }
            }
        }
        val store=TvAppShortcutStore(wrapped)
        val old=store.add("Saved",TvProviderId.GoogleAndroidTv,"saved://working",TvAppShortcutKind.CustomApp,iconDraft=draft())
        refuse=true
        assertThrows(java.io.IOException::class.java){store.setCustomIcon(old.id,draft())}
        assertEquals(old,store.shortcuts.value.single())
        assertEquals(old,TvAppShortcutStore(wrapped).shortcuts.value.single())
        assertNotNull(store.icons.load(old.iconReference))
        assertEquals(1,root().listFiles()!!.count{it.name.matches(Regex("[a-f0-9]{32}\\.png"))})
    }

    @Test fun knownServiceIconOverridePersistsAndResetPreservesShortcut() {
        val store=store()
        val runtime=TvAppLauncherRuntime(store){RecordingTvProvider()}
        val known=runtime.addKnownService("netflix",TvProviderId.GoogleAndroidTv)

        store.setCustomIcon(known.id,draft())
        val customized=store.shortcuts.value.single()
        val reference=requireNotNull(customized.iconReference)

        assertEquals(known,customized.copy(iconReference=null))
        assertNotNull(store.icons.load(reference))
        assertEquals(customized,TvAppShortcutStore(context).shortcuts.value.single())

        val filesBefore=root().listFiles().orEmpty().count { it.extension=="png" }
        assertThrows(IllegalArgumentException::class.java) {
            store.add("Invalid",TvProviderId.GoogleAndroidTv,"not a uri",
                TvAppShortcutKind.CustomApp,iconDraft=draft())
        }
        assertEquals(filesBefore,root().listFiles().orEmpty().count { it.extension=="png" })

        store.setCustomIcon(known.id,null)
        assertEquals(known,store.shortcuts.value.single())
        assertEquals(known,TvAppShortcutStore(context).shortcuts.value.single())
        assertFalse(file(reference).exists())
    }
}
