package net.aucutt.circuits.sync

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
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
import net.aucutt.circuits.data.CircuitEntity
import net.aucutt.circuits.model.TimerConfig

class WearSyncManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dataClient: DataClient = Wearable.getDataClient(appContext)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _watchConnected = MutableStateFlow(false)
    val watchConnected: StateFlow<Boolean> = _watchConnected.asStateFlow()

    init {
        scope.launch {
            refreshWatchConnected()
        }
    }

    suspend fun pushCircuits(circuits: List<CircuitEntity>) {
        if (!hasConnectedWatch()) return
        val syncCircuits = circuits.map { circuit ->
            SyncCircuit(
                id = circuit.id,
                name = circuit.name,
                intervalMinutes = circuit.intervalMinutes,
                cooldownMinutes = circuit.cooldownMinutes,
                repeats = circuit.repeats,
            )
        }
        putData(WearSyncPaths.CIRCUITS, WearSyncCodec.circuitsToJson(syncCircuits).toByteArray())
    }

    suspend fun pushConfig(config: TimerConfig, name: String) {
        if (!hasConnectedWatch()) return
        val syncConfig = SyncConfig(
            name = name,
            intervalMinutes = config.intervalMinutes,
            cooldownMinutes = config.cooldownMinutes,
            repeats = config.repeats,
        )
        putData(WearSyncPaths.CONFIG, WearSyncCodec.configToJson(syncConfig).toByteArray())
    }

    suspend fun startOnWatch(config: TimerConfig, name: String) {
        if (!hasConnectedWatch()) return
        pushConfig(config, name)
        pushCommand(WearCommand(WearCommandAction.START))
    }

    private suspend fun pushCommand(command: WearCommand) {
        if (!hasConnectedWatch()) return
        putData(WearSyncPaths.COMMAND, WearSyncCodec.commandToJson(command).toByteArray())
    }

    private suspend fun putData(path: String, payload: ByteArray) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
            dataMap.putByteArray("payload", payload)
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    private suspend fun hasConnectedWatch(): Boolean {
        refreshWatchConnected()
        return _watchConnected.value
    }

    private suspend fun refreshWatchConnected() {
        val nodes = runCatching {
            capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE).await()
        }.getOrNull()
        _watchConnected.value = nodes?.isNotEmpty() == true
    }

    companion object {
        @Volatile
        private var instance: WearSyncManager? = null

        fun getInstance(context: Context): WearSyncManager {
            return instance ?: synchronized(this) {
                instance ?: WearSyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
