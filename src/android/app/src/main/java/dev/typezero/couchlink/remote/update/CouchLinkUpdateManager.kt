package dev.typezero.couchlink.remote.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import dev.typezero.couchlink.remote.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CouchLinkUpdateManager private constructor(private val context: Context) {
    private class NoPublishedReleaseException : Exception()

    data class State(
        val checking: Boolean = false,
        val testChannel: Boolean = false,
        val downloading: Boolean = false,
        val availableVersion: String? = null,
        val releaseNotes: String = "",
        val message: String = "Updates are delivered from the official MikereDD/CouchLink GitHub releases.",
        val stage: String = "Idle",
        val progressPercent: Int? = null,
        val updateAvailable: Boolean = false,
        val installPermissionRequired: Boolean = false,
        val downloadedApk: String? = null,
    )

    private data class ReleaseAsset(
        val name: String,
        val downloadUrl: String,
        val size: Long,
        val sha256: String,
    )

    private data class ReleaseInfo(
        val version: String,
        val body: String,
        val apk: ReleaseAsset,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(
        State(testChannel = preferences.getBoolean(TEST_CHANNEL_KEY, false))
    )
    val state: StateFlow<State> = mutableState.asStateFlow()
    private var release: ReleaseInfo? = null

    fun setTestChannel(enabled: Boolean) {
        preferences.edit().putBoolean(TEST_CHANNEL_KEY, enabled).apply()
        release = null
        mutableState.value = mutableState.value.copy(
            testChannel = enabled,
            availableVersion = null,
            releaseNotes = "",
            updateAvailable = false,
            downloadedApk = null,
            message = if (enabled) {
                "Test updates are checked against official CouchLink prereleases."
            } else {
                "Stable updates are checked against official CouchLink releases."
            },
        )
    }

    fun checkForUpdates() {
        if (mutableState.value.checking || mutableState.value.downloading) return
        scope.launch {
            mutableState.value = mutableState.value.copy(
                checking = true,
                message = "Checking GitHub Releases…",
                stage = "Checking release metadata",
                installPermissionRequired = false,
            )
            runCatching { fetchLatestRelease(mutableState.value.testChannel) }
                .onSuccess { latest ->
                    release = latest
                    val newer = compareVersions(latest.version, BuildConfig.VERSION_NAME) > 0
                    mutableState.value = mutableState.value.copy(
                        checking = false,
                        availableVersion = latest.version,
                        releaseNotes = cleanReleaseNotes(latest.body),
                        stage = if (newer) "Update available" else "Up to date",
                        updateAvailable = newer,
                        message = if (newer) {
                            "CouchLink ${latest.version} is available (${formatBytes(latest.apk.size)})."
                        } else {
                            "CouchLink ${BuildConfig.VERSION_NAME} is up to date."
                        },
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Update check failed", error)
                    val message = when (error) {
                        is NoPublishedReleaseException ->
                            "No published CouchLink release is available yet."
                        is java.net.UnknownHostException,
                        is java.net.ConnectException,
                        is java.net.SocketTimeoutException ->
                            "CouchLink could not reach GitHub. Check your internet connection and try again."
                        else ->
                            "CouchLink could not check for updates. Try again later."
                    }
                    mutableState.value = mutableState.value.copy(
                        checking = false,
                        updateAvailable = false,
                        stage = "Check failed",
                        message = message,
                    )
                }
        }
    }

    fun downloadAndInstall() {
        val current = release ?: run {
            checkForUpdates()
            return
        }
        if (!mutableState.value.updateAvailable || mutableState.value.downloading) return

        scope.launch {
            mutableState.value = mutableState.value.copy(
                downloading = true,
                progressPercent = 0,
                message = "Downloading ${current.apk.name}…",
                stage = "Downloading APK",
                installPermissionRequired = false,
            )
            runCatching { downloadVerifiedApk(current) }
                .onSuccess { apk ->
                    mutableState.value = mutableState.value.copy(
                        downloading = false,
                        progressPercent = null,
                        downloadedApk = apk.absolutePath,
                        stage = "Verified · Ready to install",
                        message = "Update verified and ready to install.",
                    )
                    requestInstall(apk)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        downloading = false,
                        progressPercent = null,
                        stage = "Download failed",
                        message = "Update download failed: ${error.message ?: "Unknown error"}",
                    )
                }
        }
    }

    fun continueInstall() {
        val path = mutableState.value.downloadedApk ?: return
        val apk = File(path)
        if (!apk.isFile) {
            mutableState.value = mutableState.value.copy(
                downloadedApk = null,
                message = "The downloaded update is no longer available. Download it again.",
            )
            return
        }
        requestInstall(apk)
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private suspend fun fetchLatestRelease(testChannel: Boolean): ReleaseInfo = withContext(Dispatchers.IO) {
        val connection = openConnection(if (testChannel) TEST_RELEASES_API else LATEST_RELEASE_API)
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw NoPublishedReleaseException()
            }
            check(connection.responseCode in 200..299) {
                "GitHub returned HTTP ${connection.responseCode}"
            }
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = if (testChannel) {
                val releases = JSONArray(responseText)
                (0 until releases.length())
                    .map { releases.getJSONObject(it) }
                    .firstOrNull { !it.optBoolean("draft", false) && it.optBoolean("prerelease", false) }
                    ?: throw NoPublishedReleaseException()
            } else {
                JSONObject(responseText)
            }
            check(!json.optBoolean("draft", false)) { "Latest release is still a draft" }
            check(testChannel || !json.optBoolean("prerelease", false)) {
                "Latest published release is a prerelease"
            }
            val version = json.getString("tag_name").removePrefix("v")
            val expectedName = "CouchLink-v$version.apk"
            val assets = json.getJSONArray("assets")
            var selected: ReleaseAsset? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.getString("name") != expectedName) continue
                val digest = asset.optString("digest")
                check(digest.startsWith("sha256:", ignoreCase = true)) {
                    "$expectedName does not include a GitHub SHA-256 digest"
                }
                val url = asset.getString("browser_download_url")
                check(url.startsWith(OFFICIAL_DOWNLOAD_PREFIX)) { "Unexpected release download host" }
                selected = ReleaseAsset(
                    name = expectedName,
                    downloadUrl = url,
                    size = asset.getLong("size"),
                    sha256 = digest.substringAfter(':').lowercase(Locale.US),
                )
                break
            }
            ReleaseInfo(
                version = version,
                body = json.optString("body"),
                apk = checkNotNull(selected) { "Release asset $expectedName was not found" },
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadVerifiedApk(info: ReleaseInfo): File = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "updates").apply {
            deleteRecursively()
            mkdirs()
        }
        val destination = File(updateDir, info.apk.name)
        val connection = openConnection(info.apk.downloadUrl)
        try {
            check(connection.responseCode in 200..299) {
                "GitHub download returned HTTP ${connection.responseCode}"
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.apk.size
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copied += read
                        if (total > 0) {
                            val progress = ((copied * 100) / total).toInt().coerceIn(0, 100)
                            mutableState.value = mutableState.value.copy(
                                progressPercent = progress,
                                stage = "Downloading APK · $progress%",
                            )
                        }
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual.equals(info.apk.sha256, ignoreCase = true)) {
                destination.delete()
                "SHA-256 verification failed"
            }
            mutableState.value = mutableState.value.copy(stage = "Verifying package and certificate")
            verifyPackageIdentity(destination)
            destination
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackageIdentity(apk: File) {
        val manager = context.packageManager
        val archive = manager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES,
        ) ?: error("Android could not read the downloaded APK")
        check(archive.packageName == context.packageName) { "Downloaded APK package name does not match CouchLink" }
        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            archive.versionCode.toLong()
        }
        check(archiveCode > BuildConfig.VERSION_CODE) { "Downloaded APK is not a newer build" }

        val installed = manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val currentSigner = installed.signingInfo?.apkContentsSigners?.firstOrNull()
            ?: error("Installed CouchLink signing certificate was unavailable")
        val updateSigner = archive.signingInfo?.apkContentsSigners?.firstOrNull()
            ?: error("Downloaded CouchLink signing certificate was unavailable")
        check(currentSigner.toByteArray().contentEquals(updateSigner.toByteArray())) {
            "Downloaded APK signing certificate does not match the installed app"
        }
    }

    private fun requestInstall(apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            mutableState.value = mutableState.value.copy(
                installPermissionRequired = true,
                stage = "Permission required",
                message = "Allow CouchLink to install updates, then return and choose Install update again.",
            )
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            apk,
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, false)
        }
        mutableState.value = mutableState.value.copy(stage = "Opening Android installer")
        context.startActivity(intent)
    }

    private fun cleanReleaseNotes(markdown: String): String = markdown
        .lineSequence()
        .map { line ->
            line.replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^\\s*[-*+]\\s+"), "• ")
                .replace(Regex("`([^`]+)`"), "$1")
                .replace("**", "")
                .replace("__", "")
        }
        .joinToString("\n")
        .trim()

    private fun openConnection(rawUrl: String): HttpURLConnection {
        val url = URL(rawUrl)
        check(url.protocol.equals("https", ignoreCase = true)) { "Only HTTPS update URLs are allowed" }
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "CouchLink-Android/${BuildConfig.VERSION_NAME}")
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        data class Parsed(val core: List<Int>, val rank: Int, val numbers: List<Int>)
        fun parse(raw: String): Parsed {
            val value = raw.removePrefix("v").removeSuffix("-debug").removeSuffix("-release")
            val coreText = value.substringBefore('-')
            val suffix = value.substringAfter('-', "")
            val core = coreText.split('.').map { it.toIntOrNull() ?: 0 }
            if (suffix.isBlank()) return Parsed(core, 4, emptyList())
            val tokens = suffix.split('.')
            val rank = when (tokens.firstOrNull()?.lowercase(Locale.US)) {
                "dev", "alpha" -> 0
                "beta" -> 1
                "rc" -> 2
                else -> 3
            }
            return Parsed(core, rank, tokens.drop(1).mapNotNull { it.toIntOrNull() })
        }
        val a = parse(left)
        val b = parse(right)
        for (index in 0 until maxOf(a.core.size, b.core.size)) {
            val result = a.core.getOrElse(index) { 0 }.compareTo(b.core.getOrElse(index) { 0 })
            if (result != 0) return result
        }
        if (a.rank != b.rank) return a.rank.compareTo(b.rank)
        for (index in 0 until maxOf(a.numbers.size, b.numbers.size)) {
            val result = a.numbers.getOrElse(index) { 0 }.compareTo(b.numbers.getOrElse(index) { 0 })
            if (result != 0) return result
        }
        return 0
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(Locale.US, bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(Locale.US, bytes / 1024.0)
        else -> "$bytes bytes"
    }

    companion object {
        private const val TAG = "CouchLinkUpdate"
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/MikereDD/CouchLink/releases/latest"
        private const val TEST_RELEASES_API =
            "https://api.github.com/repos/MikereDD/CouchLink/releases?per_page=20"
        private const val PREFERENCES = "couchlink_updates"
        private const val TEST_CHANNEL_KEY = "test_channel"
        private const val OFFICIAL_DOWNLOAD_PREFIX =
            "https://github.com/MikereDD/CouchLink/releases/download/"

        @Volatile private var instance: CouchLinkUpdateManager? = null

        fun get(context: Context): CouchLinkUpdateManager = instance ?: synchronized(this) {
            instance ?: CouchLinkUpdateManager(context.applicationContext).also { instance = it }
        }
    }
}
