package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Private, on-demand cache for recognized streaming-service icons.
 * No third-party artwork is included in the APK.
 */
internal class TvKnownServiceIconCache(
    context: Context,
    private val fetch: (String) -> ByteArray? = ::downloadKnownServicePng,
) {
    private val root = File(
        context.applicationContext.cacheDir,
        "tv-known-service-icons",
    )
    private val mutex = Mutex()

    suspend fun load(serviceId: String?): Bitmap? {
        val service = serviceId?.let(TvKnownServiceCatalog::find)
            ?: return null
        val url = service.defaultIconUrl ?: return null

        return withContext(Dispatchers.IO) {
            try {
                mutex.withLock {
                    val cached = File(root, "${service.id}.png")

                    readValid(cached)?.let {
                        return@withLock it
                    }

                    if (cached.exists()) cached.delete()

                    val bytes = fetch(url)
                        ?: return@withLock null

                    val bitmap = decodePng(bytes)
                        ?: return@withLock null

                    if (root.isDirectory || root.mkdirs()) {
                        val temporary = File(
                            root,
                            "${service.id}-${UUID.randomUUID()}.tmp",
                        )

                        try {
                            FileOutputStream(temporary).use {
                                it.write(bytes)
                                it.fd.sync()
                            }
                            temporary.renameTo(cached)
                        } finally {
                            temporary.delete()
                        }
                    }

                    bitmap
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }
        }
    }

    private fun readValid(file: File): Bitmap? {
        if (!file.isFile ||
            file.length() !in 8L..MAX_BYTES.toLong()
        ) return null

        return decodePng(file.readBytes())
    }

    private fun decodePng(bytes: ByteArray): Bitmap? {
        if (bytes.size !in 33..MAX_BYTES) return null

        val signature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
        )

        if (!bytes.take(8).toByteArray().contentEquals(signature))
            return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, bounds
        )

        if (bounds.outMimeType != "image/png" ||
            bounds.outWidth !in 1..4096 ||
            bounds.outHeight !in 1..4096 ||
            bounds.outWidth.toLong() * bounds.outHeight > 8_000_000
        ) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) {
            sample *= 2
        }

        return BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

    private companion object {
        const val MAX_BYTES = 512 * 1024
    }
}

/** Fetches only approved catalog-hosted PNGs. Never follows redirects. */
private fun downloadKnownServicePng(source: String): ByteArray? {
    val url = URL(source)

    if (url.protocol != "https" || url.host !in setOf(
            "assets.nflxext.com",
            "tv.apple.com",
            "www.hulu.com",
        )
    ) return null

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "image/png")

        if (connection.responseCode != HttpURLConnection.HTTP_OK)
            return null

        if (!connection.contentType.orEmpty()
                .substringBefore(";")
                .trim()
                .equals("image/png", ignoreCase = true)
        ) return null

        if (connection.contentLengthLong > 512 * 1024)
            return null

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)

        connection.inputStream.use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) return null
                if (output.size() + count > 512 * 1024)
                    return null

                output.write(buffer, 0, count)
            }
        }

        return output.toByteArray()
    } finally {
        connection.disconnect()
    }
}
