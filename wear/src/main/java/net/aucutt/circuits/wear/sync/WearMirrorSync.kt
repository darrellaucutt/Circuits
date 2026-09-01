package net.aucutt.circuits.wear.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.aucutt.circuits.sync.WearSyncCodec
import net.aucutt.circuits.sync.WearSyncPaths
import net.aucutt.circuits.sync.parseTimerStatePayload
import net.aucutt.circuits.sync.timerStateDataUri
import net.aucutt.circuits.wear.service.WearWorkoutDisplayService

/** Actively syncs timer state from the phone when the watch app is open. */
class WearMirrorSync(context: Context) :
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var requestJob: Job? = null

    fun start() {
        dataClient.addListener(this, timerStateDataUri(), DataClient.FILTER_LITERAL)
        messageClient.addListener(this)
        scope.launch {
            refreshPhoneConnected()
            fetchLatestState()
            requestStateFromPhone()
        }
        requestJob = scope.launch {
            while (isActive) {
                delay(REQUEST_INTERVAL_MS)
                requestStateFromPhone()
            }
        }
    }

    fun stop() {
        requestJob?.cancel()
        requestJob = null
        dataClient.removeListener(this)
        messageClient.removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            applyPayload(parseTimerStatePayload(DataMapItem.fromDataItem(event.dataItem).dataMap))
        }
        dataEvents.release()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearSyncPaths.TIMER_STATE) return
        val json = messageEvent.data?.toString(Charsets.UTF_8) ?: return
        applyPayload(WearSyncCodec.timerStateFromJson(json))
    }

    private suspend fun fetchLatestState() {
        val buffer = runCatching {
            dataClient.getDataItems(timerStateDataUri()).await()
        }.getOrNull() ?: return

        try {
            buffer.forEach { item ->
                if (item.uri.path == WearSyncPaths.TIMER_STATE) {
                    applyPayload(parseTimerStatePayload(DataMapItem.fromDataItem(item).dataMap))
                }
            }
        } finally {
            buffer.release()
        }
    }

    private suspend fun requestStateFromPhone() {
        refreshPhoneConnected()
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull().orEmpty()
        nodes.forEach { node ->
            runCatching {
                messageClient.sendMessage(
                    node.id,
                    WearSyncPaths.TIMER_STATE_REQUEST,
                    ByteArray(0),
                ).await()
            }
        }
    }

    private suspend fun refreshPhoneConnected() {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull()
        WearMirrorRepository.setPhoneConnected(nodes?.isNotEmpty() == true)
    }

    private fun applyPayload(state: net.aucutt.circuits.sync.SyncTimerState?) {
        if (state == null) return
        WearMirrorRepository.updateTimerState(state)
        WearMirrorRepository.setPhoneConnected(true)
        WearWorkoutDisplayService.sync(appContext)
    }

    companion object {
        private const val REQUEST_INTERVAL_MS = 1_000L
    }
}
