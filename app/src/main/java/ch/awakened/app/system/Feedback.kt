package ch.awakened.app.system

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ch.awakened.app.R

/** Sound und Vibration für System-Meldungen. */
class Feedback(private val context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).build()

    private val levelUp = soundPool.load(context, R.raw.level_up, 1)
    private val questClear = soundPool.load(context, R.raw.quest_clear, 1)
    private val alert = soundPool.load(context, R.raw.system_alert, 1)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    enum class Kind { LEVEL_UP, QUEST, ALERT, TAP }

    fun play(kind: Kind, sound: Boolean, vibration: Boolean) {
        if (sound) {
            when (kind) {
                Kind.LEVEL_UP -> soundPool.play(levelUp, 1f, 1f, 1, 0, 1f)
                Kind.QUEST -> soundPool.play(questClear, 0.8f, 0.8f, 1, 0, 1f)
                Kind.ALERT -> soundPool.play(alert, 1f, 1f, 1, 0, 1f)
                Kind.TAP -> Unit
            }
        }
        if (vibration) vibrate(kind)
    }

    private fun vibrate(kind: Kind) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (kind) {
            Kind.LEVEL_UP -> VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80, 60, 250), -1)
            Kind.QUEST -> VibrationEffect.createWaveform(longArrayOf(0, 60, 50, 60), -1)
            Kind.ALERT -> VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
            Kind.TAP -> VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }
}
