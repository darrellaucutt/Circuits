package net.aucutt.circuits.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.aucutt.circuits.sync.WearCommandAction
import net.aucutt.circuits.wear.feedback.HapticCuePlayer
import net.aucutt.circuits.wear.service.WearWorkoutDisplayService
import net.aucutt.circuits.wear.sync.WearCommandSender
import net.aucutt.circuits.wear.sync.WearMirrorRepository
import net.aucutt.circuits.wear.sync.WearMirrorSync

class WearMirrorViewModel(application: Application) : AndroidViewModel(application) {

    private val mirrorSync = WearMirrorSync(application)
    private val commandSender = WearCommandSender(application)
    private val haptics = HapticCuePlayer(application)

    val timerState = WearMirrorRepository.timerState
    val phoneConnected = WearMirrorRepository.phoneConnected

    private var lastHapticPhase: String? = null
    private var lastHapticRound: Int = 0

    init {
        mirrorSync.start()
        viewModelScope.launch {
            timerState.collect {
                WearWorkoutDisplayService.sync(getApplication())
            }
        }
    }

    fun onPhaseChanged(phase: String, round: Int) {
        val phaseChanged = phase != lastHapticPhase
        val roundChanged = phase == "Work" && round != lastHapticRound
        if (!phaseChanged && !roundChanged) return

        lastHapticPhase = phase
        if (phase == "Work") lastHapticRound = round

        haptics.playForPhase(phase)
    }

    fun pause() = commandSender.send(WearCommandAction.PAUSE)

    fun resume() = commandSender.send(WearCommandAction.RESUME)

    fun stop() = commandSender.send(WearCommandAction.STOP)

    override fun onCleared() {
        mirrorSync.stop()
        super.onCleared()
    }
}
