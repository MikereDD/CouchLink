package dev.typezero.couchlink.remote.tv.launcher

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TvKnownServiceIconCacheTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val root get() = File(context.cacheDir, "tv-known-service-icons")

    @Before fun clean() {
        assertTrue(root.deleteRecursively())
    }

    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        val output = ByteArrayOutputStream()
        try {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            return output.toByteArray()
        } finally {
            bitmap.recycle()
            output.close()
        }
    }

    @Test fun recognizedServiceDownloadsAndCachesPng() = runBlocking {
        var requests = 0
        val image = png()
        val cache = TvKnownServiceIconCache(context) {
            requests++
            image
        }

        val first = cache.load("netflix")
        assertNotNull(first)
        assertEquals(64, first!!.width)
        assertEquals(1, requests)
        assertTrue(File(root, "netflix.png").isFile)

        assertNotNull(cache.load("netflix"))
        assertEquals(1, requests)
    }

    @Test fun persistedCacheWorksWithoutNetwork() = runBlocking {
        val image = png()

        val online = TvKnownServiceIconCache(context) { image }
        assertNotNull(online.load("apple-tv"))

        var requests = 0
        val offline = TvKnownServiceIconCache(context) {
            requests++
            null
        }

        assertNotNull(offline.load("apple-tv"))
        assertEquals(0, requests)
    }

    @Test fun invalidAndUnknownIconsFailSafely() = runBlocking {
        var requests = 0
        val cache = TvKnownServiceIconCache(context) {
            requests++
            byteArrayOf(1, 2, 3)
        }

        assertNull(cache.load("netflix"))
        assertFalse(File(root, "netflix.png").exists())
        assertEquals(1, requests)

        assertNull(cache.load("unknown-service"))
        assertNull(cache.load("youtube"))
        assertEquals(1, requests)
    }
}
