package net.aucutt.circuits.sync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.aucutt.circuits.ui.timer.TimerUiState

/** Pushes live timer state from the phone to a paired watch. */
class WearStatePublisher private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dataClient = Wearable.getDataClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _watchConnected = MutableStateFlow(false)
    val watchConnected: StateFlow<Boolean> = _watchConnected.asStateFlow()

    fun publish(state: TimerUiState, circuitName: String) {
        scope.launch {
            refreshWatchConnected()
            if (!_watchConnected.value) return@launch

            val syncState = SyncTimerState(
                phase = state.phase.name,
                remainingSeconds = state.remainingSeconds,
                currentRound = state.currentRound,
                isPaused = state.isPaused,
                intervalMinutes = state.config.intervalMinutes,
                cooldownMinutes = state.config.cooldownMinutes,
                repeats = state.config.repeats,
                circuitName = circuitName,
            )
            val request = PutDataMapRequest.create(WearSyncPaths.TIMER_STATE).apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putByteArray(
                    "payload",
                    WearSyncCodec.timerStateToJson(syncState).toByteArray(),
                )
            }.asPutDataRequest().setUrgent()

            runCatching { dataClient.putDataItem(request).await() }
        }
    }

    private suspend fun refreshWatchConnected() {
        val nodes = runCatching {
            Wearable.getNodeClient(appContext).connectedNodes.await()
        }.getOrNull()
        _watchConnected.value = nodes?.isNotEmpty() == true
    }

    companion object {
        @Volatile
        private var instance: WearStatePublisher? = null

        fun getInstance(context: Context): WearStatePublisher {
            return instance ?: synchronized(this) {
                instance ?: WearStatePublisher(context.applicationContext).also { instance = it }
            }
        }
    }
}
