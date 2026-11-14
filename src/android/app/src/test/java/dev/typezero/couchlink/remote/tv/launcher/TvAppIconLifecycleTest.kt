package dev.typezero.couchlink.remote.tv.launcher

import android.graphics.Bitmap
import android.graphics.Color
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import java.io.File
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
class TvAppIconLifecycleTest {
    @Test fun resettingLastReferenceCleansOnlyManagedFileAndPreservesShortcutFields() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("couchlink_tv_app_launcher", 0).edit().clear().commit()
        val store = TvAppShortcutStore(context)
        val root = File(context.noBackupFilesDir, "tv-shortcut-icons").apply { mkdirs() }
        val icon = File(root, "0123456789abcdef0123456789abcdef.png")
        val unrelated = File(root, "keep-user-file.txt").apply { writeText("not an app-managed icon") }
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        val original = store.add("Personal app", TvProviderId.GoogleAndroidTv, "personal-app://working", TvAppShortcutKind.CustomApp)
        icon.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        store.update(original.copy(iconReference = "tvicon:0123456789abcdef0123456789abcdef"))
        val before = store.shortcuts.value.single()
        assertTrue(icon.exists())
        store.update(before.copy(iconReference = null))
        assertEquals(before.copy(iconReference = null), TvAppShortcutStore(context).shortcuts.value.single())
        assertFalse("Reset must remove the last unreferenced imported icon", icon.exists())
        assertTrue("Never delete unrelated files", unrelated.exists())
    }
}
