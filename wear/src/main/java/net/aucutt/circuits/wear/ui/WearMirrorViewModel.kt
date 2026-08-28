package net.aucutt.circuits.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.aucutt.circuits.sync.WearCommandAction
import net.aucutt.circuits.wear.feedback.HapticCuePlayer
import net.aucutt.circuits.wear.feedback.TtsSpeaker
import net.aucutt.circuits.wear.sync.WearCommandSender
import net.aucutt.circuits.wear.sync.WearMirrorRepository

class WearMirrorViewModel(application: Application) : AndroidViewModel(application) {

    private val commandSender = WearCommandSender(application)
    private val haptics = HapticCuePlayer(application)
    private val tts = TtsSpeaker(application)

    val timerState = WearMirrorRepository.timerState
    val phoneConnected = WearMirrorRepository.phoneConnected

    private var lastAnnouncedPhase: String? = null
    private var lastAnnouncedRound: Int = 0

    fun onStateDisplayed(phase: String, round: Int) {
        val phaseChanged = phase != lastAnnouncedPhase
        val roundChanged = phase == "Work" && round != lastAnnouncedRound
        if (!phaseChanged && !roundChanged) return

        lastAnnouncedPhase = phase
        if (phase == "Work") lastAnnouncedRound = round

        haptics.playForPhase(phase)
        val text = when (phase) {
            "PreWorkout" -> "Activity will start in 30 seconds."
            "Work" -> "Work. Round $round."
            "Cooldown" -> "Cooldown."
            "Finished" -> "Circuit complete."
            else -> return
        }
        tts.speak(text)
    }

    fun pause() = commandSender.send(WearCommandAction.PAUSE)

    fun resume() = commandSender.send(WearCommandAction.RESUME)

    fun stop() = commandSender.send(WearCommandAction.STOP)

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}
