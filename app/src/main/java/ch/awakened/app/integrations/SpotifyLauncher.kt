package ch.awakened.app.integrations

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Öffnet eine Playlist in Spotify und startet die Wiedergabe.
 * Ohne registrierte Client-ID ist das App-Remote-SDK nicht nutzbar, darum:
 * URI-Intent + Media-Play-Taste als Fallback.
 */
object SpotifyLauncher {
    private const val SPOTIFY_PACKAGE = "com.spotify.music"
    private val ID_REGEX = Regex("(playlist|album|artist|track)[/:]([A-Za-z0-9]{10,})")

    /** Wandelt Web-Links (open.spotify.com/...) und URIs in eine spotify:-URI um. */
    fun toUri(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val m = ID_REGEX.find(trimmed) ?: return null
        return "spotify:${m.groupValues[1]}:${m.groupValues[2]}"
    }

    fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0); true }.getOrDefault(false)

    /**
     * @param returnTo Intent, mit dem nach dem Start der Musik zur App zurückgekehrt wird.
     * @return Fehlermeldung oder null bei Erfolg.
     */
    fun startPlaylist(context: Context, link: String, returnTo: Intent?): String? {
        val uri = toUri(link) ?: return "Kein gültiger Spotify-Link hinterlegt (Einstellungen → Spotify)."
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${context.packageName}"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isInstalled(context)) setPackage(SPOTIFY_PACKAGE)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val web = "https://open.spotify.com/" + uri.removePrefix("spotify:").replace(':', '/')
            return try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                null
            } catch (_: ActivityNotFoundException) {
                "Spotify ist nicht installiert."
            }
        }
        val handler = Handler(Looper.getMainLooper())
        val audio = context.getSystemService(AudioManager::class.java)
        handler.postDelayed({
            if (audio?.isMusicActive == false) sendPlay(audio)
        }, 2800)
        handler.postDelayed({
            if (audio?.isMusicActive == false) sendPlay(audio)
            returnTo?.let { runCatching { context.startActivity(it) } }
        }, 4800)
        return null
    }

    private fun sendPlay(audio: AudioManager) {
        val t = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
    }
}
