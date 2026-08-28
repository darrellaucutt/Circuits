package net.aucutt.circuits.wear.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticCuePlayer(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun playForPhase(phase: String) {
        val pattern = when (phase) {
            "PreWorkout" -> longArrayOf(0, 120, 80, 120)
            "Work" -> longArrayOf(0, 200)
            "Cooldown" -> longArrayOf(0, 80, 60, 80)
            "Finished" -> longArrayOf(0, 120, 80, 120, 80, 200)
            else -> return
        }
        val vibe = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibe.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(pattern, -1)
        }
    }
}
