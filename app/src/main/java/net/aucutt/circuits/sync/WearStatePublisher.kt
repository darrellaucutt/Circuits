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
import net.aucutt.circuits.timer.CircuitTimerEngine
import net.aucutt.circuits.ui.timer.TimerUiState

/** Pushes live timer state from the phone to paired watches. */
class WearStatePublisher private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _watchConnected = MutableStateFlow(false)
    val watchConnected: StateFlow<Boolean> = _watchConnected.asStateFlow()

    @Volatile
    var circuitName: String = "Custom"

    init {
        scope.launch {
            CircuitTimerEngine.uiState.collect { state ->
                publishInternal(state, circuitName)
            }
        }
    }

    fun publish(state: TimerUiState, circuitName: String) {
        this.circuitName = circuitName
        scope.launch {
            publishInternal(state, circuitName)
        }
    }

    fun publishNow() {
        scope.launch {
            publishInternal(CircuitTimerEngine.uiState.value, circuitName)
        }
    }

    /** Sends the current timer state directly to one watch node. */
    suspend fun sendStateToNode(nodeId: String) {
        val payloadBytes = encodeState(CircuitTimerEngine.uiState.value, circuitName)
        runCatching {
            messageClient.sendMessage(nodeId, WearSyncPaths.TIMER_STATE, payloadBytes).await()
        }
    }

    private suspend fun publishInternal(state: TimerUiState, circuitName: String) {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull().orEmpty()
        _watchConnected.value = nodes.isNotEmpty()

        val payloadJson = encodeStateJson(state, circuitName)
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)

        val request = PutDataMapRequest.create(WearSyncPaths.TIMER_STATE).apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
            dataMap.putString("payload", payloadJson)
        }.asPutDataRequest().setUrgent()

        runCatching { dataClient.putDataItem(request).await() }

        nodes.forEach { node ->
            runCatching {
                messageClient.sendMessage(node.id, WearSyncPaths.TIMER_STATE, payloadBytes).await()
            }
        }
    }

    private fun encodeState(state: TimerUiState, circuitName: String): ByteArray {
        return encodeStateJson(state, circuitName).toByteArray(Charsets.UTF_8)
    }

    private fun encodeStateJson(state: TimerUiState, circuitName: String): String {
        return WearSyncCodec.timerStateToJson(
            SyncTimerState(
                phase = state.phase.name,
                remainingSeconds = state.remainingSeconds,
                currentRound = state.currentRound,
                isPaused = state.isPaused,
                intervalMinutes = state.config.intervalMinutes,
                cooldownMinutes = state.config.cooldownMinutes,
                repeats = state.config.repeats,
                circuitName = circuitName,
            ),
        )
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
