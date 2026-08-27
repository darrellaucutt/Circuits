package net.aucutt.circuits.wear.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import net.aucutt.circuits.timer.TimerAnnouncement

class HapticCuePlayer(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun playFor(announcement: TimerAnnouncement) {
        val pattern = when (announcement) {
            TimerAnnouncement.PreWorkout -> longArrayOf(0, 120, 80, 120)
            is TimerAnnouncement.Work -> longArrayOf(0, 200)
            TimerAnnouncement.Cooldown -> longArrayOf(0, 80, 60, 80)
            TimerAnnouncement.Complete -> longArrayOf(0, 120, 80, 120, 80, 200)
        }
        vibrate(pattern)
    }

    private fun vibrate(pattern: LongArray) {
        val vibe = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibe.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(pattern, -1)
        }
    }
}
