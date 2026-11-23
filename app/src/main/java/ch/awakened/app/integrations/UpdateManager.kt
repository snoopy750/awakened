package ch.awakened.app.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import ch.awakened.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(val versionName: String, val build: Int, val apkUrl: String, val notes: String)

/**
 * Prüft auf GitHub, ob eine neuere Version vorliegt, lädt die APK herunter und
 * übergibt sie dem Android-Installer. Es werden keine Nutzerdaten gesendet.
 */
object UpdateManager {
    private const val LATEST = "https://api.github.com/repos/snoopy750/awakened/releases/latest"
    private const val APK_NAME = "awakened.apk"
    private val json = Json { ignoreUnknownKeys = true }

    val installedVersion: String get() = BuildConfig.VERSION_NAME
    private val installedBuild: Int get() = BuildConfig.VERSION_CODE

    /** Build-Nummer aus einem Tag wie „v1.0.12“. */
    fun buildFromTag(tag: String): Int? = tag.trim().removePrefix("v").split('.').lastOrNull()?.toIntOrNull()

    fun isNewer(release: ReleaseInfo): Boolean = release.build > installedBuild

    suspend fun fetchLatest(): ReleaseInfo = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "AWAKENED-Android")
        }
        try {
            if (conn.responseCode != 200) error("GitHub antwortet mit ${conn.responseCode}")
            val root = json.parseToJsonElement(conn.inputStream.bufferedReader().readText()).jsonObject
            val tag = root["tag_name"]?.jsonPrimitive?.content ?: error("Keine Version gefunden")
            val asset = root["assets"]?.jsonArray?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content == APK_NAME }
                ?: error("Keine APK im Release gefunden")
            ReleaseInfo(
                versionName = tag.removePrefix("v"),
                build = buildFromTag(tag) ?: 0,
                apkUrl = asset["browser_download_url"]!!.jsonPrimitive.content,
                notes = root["body"]?.jsonPrimitive?.content.orEmpty(),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Lädt die APK in den Cache. [onProgress] erhält 0..1 (oder -1, wenn die Grösse unbekannt ist). */
    suspend fun download(context: Context, release: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "awakened-${release.versionName}.apk")
        var url = URL(release.apkUrl)
        var conn: HttpURLConnection
        var redirects = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "AWAKENED-Android")
            }
            val code = conn.responseCode
            if (code in 300..399 && redirects < 5) {
                url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); redirects++; continue
            }
            if (code != 200) { conn.disconnect(); error("Download fehlgeschlagen ($code)") }
            break
        }
        try {
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        withContext(Dispatchers.Main) { onProgress(if (total > 0) done.toFloat() / total else -1f) }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        target
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
