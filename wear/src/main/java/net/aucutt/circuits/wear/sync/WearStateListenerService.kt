package net.aucutt.circuits.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import net.aucutt.circuits.sync.WearSyncCodec
import net.aucutt.circuits.sync.WearSyncPaths
import net.aucutt.circuits.sync.parseTimerStatePayload
import net.aucutt.circuits.wear.service.WearWorkoutDisplayService

class WearStateListenerService : WearableListenerService() {

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

    private fun applyPayload(state: net.aucutt.circuits.sync.SyncTimerState?) {
        if (state == null) return
        WearMirrorRepository.updateTimerState(state)
        WearMirrorRepository.setPhoneConnected(true)
        WearWorkoutDisplayService.sync(applicationContext)
    }
}
