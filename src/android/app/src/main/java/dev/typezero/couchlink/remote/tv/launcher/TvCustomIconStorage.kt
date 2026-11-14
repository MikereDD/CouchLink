package dev.typezero.couchlink.remote.tv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Unsaved, bounded in-memory preview. Picking/canceling never creates a file. */
internal class TvIconDraft internal constructor(internal val png: ByteArray, val preview: Bitmap)

internal sealed interface TvIconImportResult {
    data class Ready(val draft: TvIconDraft) : TvIconImportResult
    data class Rejected(val message: String) : TvIconImportResult
}

/** Local-only normalized artwork. References never contain picker URIs or paths. */
internal class TvCustomIconStorage(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "tv-shortcut-icons")

    fun prepare(uri: Uri): TvIconImportResult {
        if (uri.scheme != "content") return TvIconImportResult.Rejected("Choose an image through the document picker.")
        return try {
            app.contentResolver.openInputStream(uri)?.use(::prepare)
                ?: TvIconImportResult.Rejected("That image is unavailable. Choose it again.")
        } catch (_: Exception) {
            TvIconImportResult.Rejected("Could not read that image. Choose a PNG, JPEG or WebP.")
        }
    }

    /** Caller performs this bounded read/decode on Dispatchers.IO. */
    fun prepare(input: InputStream): TvIconImportResult = try {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) throw IOException("No image data")
            require(bytes.size() + read <= MAX_INPUT_BYTES)
            bytes.write(buffer, 0, read)
        }
        val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes.toByteArray()))) { decoder, info, _ ->
            require(info.mimeType in MIME_TYPES)
            val w = info.size.width; val h = info.size.height
            require(w in 1..MAX_SOURCE_EDGE && h in 1..MAX_SOURCE_EDGE && w.toLong() * h <= MAX_SOURCE_PIXELS)
            val ratio = minOf(1f, SIZE.toFloat() / max(w, h))
            decoder.setTargetSize(max(1, (w * ratio).roundToInt()), max(1, (h * ratio).roundToInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            // ImageDecoder applies EXIF rotation/mirroring before output.
        }
        val normalized = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val ratio = SIZE.toFloat() / max(decoded.width, decoded.height)
        val w = decoded.width * ratio; val h = decoded.height * ratio
        Canvas(normalized).drawBitmap(decoded, null, RectF((SIZE-w)/2, (SIZE-h)/2, (SIZE+w)/2, (SIZE+h)/2), Paint(Paint.FILTER_BITMAP_FLAG))
        decoded.recycle()
        val png = ByteArrayOutputStream().also { check(normalized.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
        require(png.size <= MAX_FILE_BYTES)
        TvIconImportResult.Ready(TvIconDraft(png, normalized))
    } catch (_: OutOfMemoryError) {
        TvIconImportResult.Rejected("That image is too large. Choose a smaller image.")
    } catch (_: Exception) {
        TvIconImportResult.Rejected("Use a valid PNG, JPEG or WebP up to 8 MiB, 8192 pixels per edge and 32 megapixels.")
    }

    /** Atomically publish a new normalized PNG; metadata must commit before pruning. */
    internal fun write(draft: TvIconDraft): String = synchronized(LOCK) {
        require(draft.png.size in 1..MAX_FILE_BYTES && draft.preview.width == SIZE && draft.preview.height == SIZE)
        check(root.isDirectory || root.mkdirs()) { "Could not create icon storage." }
        val id = UUID.randomUUID().toString().replace("-", "")
        val temporary = File(root, "write-$id.tmp")
        val target = File(root, "$id.png")
        try {
            check(temporary.createNewFile())
            FileOutputStream(temporary).use { out -> out.write(draft.png); out.fd.sync() }
            check(temporary.renameTo(target)) { "Could not save icon." }
            "tvicon:$id"
        } finally { temporary.delete() }
    }

    fun load(reference: String?): Bitmap? = synchronized(LOCK) {
        val file = file(reference) ?: return@synchronized null
        try {
            if (!file.isFile || file.length() !in 1..MAX_FILE_BYTES.toLong()) return@synchronized null
            val bytes = file.readBytes()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth != SIZE || bounds.outHeight != SIZE || bounds.outMimeType != "image/png") return@synchronized null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
        catch (_: OutOfMemoryError) { null }
    }

    /** Null means persisted data could not be trusted: delete nothing. */
    internal fun prune(references: Set<String>?) = synchronized(LOCK) {
        if (references == null) return@synchronized
        val keep = references.mapNotNull { file(it)?.name }.toSet()
        root.listFiles()?.forEach { candidate ->
            if (candidate.isFile && candidate.name !in keep &&
                (MANAGED_FILE.matches(candidate.name) || TEMP_FILE.matches(candidate.name))) candidate.delete()
        }
    }

    private fun file(reference: String?): File? {
        val match = reference?.let(REFERENCE::matchEntire) ?: return null
        return File(root, match.groupValues[1] + ".png")
    }

    companion object {
        const val SIZE = 256
        const val MAX_INPUT_BYTES = 8 * 1024 * 1024
        const val MAX_FILE_BYTES = 512 * 1024
        private const val MAX_SOURCE_EDGE = 8192
        private const val MAX_SOURCE_PIXELS = 32_000_000L
        val MIME_TYPES = setOf("image/png", "image/jpeg", "image/webp")
        internal val LOCK = Any()
        private val REFERENCE = Regex("tvicon:([a-f0-9]{32})")
        private val MANAGED_FILE = Regex("[a-f0-9]{32}\\.png")
        private val TEMP_FILE = Regex("write-[a-f0-9]{32}\\.tmp")
    }
}
