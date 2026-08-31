package net.aucutt.circuits.wear.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.aucutt.circuits.sync.SyncTimerState

object WearMirrorRepository {

    private val _timerState = MutableStateFlow<SyncTimerState?>(null)
    val timerState: StateFlow<SyncTimerState?> = _timerState.asStateFlow()

    private val _phoneConnected = MutableStateFlow(false)
    val phoneConnected: StateFlow<Boolean> = _phoneConnected.asStateFlow()

    fun updateTimerState(state: SyncTimerState) {
        _timerState.value = state
        _phoneConnected.value = true
    }

    fun setPhoneConnected(connected: Boolean) {
        _phoneConnected.value = connected
    }

    fun markDisconnected() {
        _phoneConnected.value = false
    }
}
